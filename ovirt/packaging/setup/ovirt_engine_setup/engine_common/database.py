#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2013 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


import atexit
import os
import tempfile
import datetime
import socket
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


import psycopg2


from otopi import base
from otopi import util


from ovirt_engine import util as outil


from ovirt_engine_setup import dialog
from ovirt_engine_setup import util as osetuputil


@util.export
class Statement(base.Base):

    @property
    def environment(self):
        return self._environment

    def __init__(
        self,
        dbenvkeys,
        environment,
    ):
        super(Statement, self).__init__()
        self._environment = environment
        self._dbenvkeys = dbenvkeys

    def connect(
        self,
        host=None,
        port=None,
        secured=None,
        securedHostValidation=None,
        user=None,
        password=None,
        database=None,
    ):
        if host is None:
            host = self.environment[self._dbenvkeys['host']]
        if port is None:
            port = self.environment[self._dbenvkeys['port']]
        if secured is None:
            secured = self.environment[self._dbenvkeys['secured']]
        if securedHostValidation is None:
            securedHostValidation = self.environment[
                self._dbenvkeys['hostValidation']
            ]
        if user is None:
            user = self.environment[self._dbenvkeys['user']]
        if password is None:
            password = self.environment[self._dbenvkeys['password']]
        if database is None:
            database = self.environment[self._dbenvkeys['database']]

        sslmode = 'allow'
        if secured:
            if securedHostValidation:
                sslmode = 'verify-full'
            else:
                sslmode = 'require'

        #
        # old psycopg2 does not know how to ignore
        # uselss parameters
        #
        if not host:
            connection = psycopg2.connect(
                database=database,
            )
        else:
            #
            # port cast is required as old psycopg2
            # does not support unicode strings for port.
            # do not cast to int to avoid breaking usock.
            #
            connection = psycopg2.connect(
                host=host,
                port=str(port),
                user=user,
                password=password,
                database=database,
                sslmode=sslmode,
            )

        return connection

    def execute(
        self,
        statement,
        args=dict(),
        host=None,
        port=None,
        secured=None,
        securedHostValidation=None,
        user=None,
        password=None,
        database=None,
        ownConnection=False,
        transaction=True,
    ):
        # autocommit member is available at >= 2.4.2
        def __backup_autocommit(connection):
            if hasattr(connection, 'autocommit'):
                return connection.autocommit
            else:
                return connection.isolation_level

        def __restore_autocommit(connection, v):
            if hasattr(connection, 'autocommit'):
                connection.autocommit = v
            else:
                connection.set_isolation_level(v)

        def __set_autocommit(connection, autocommit):
            if hasattr(connection, 'autocommit'):
                connection.autocommit = autocommit
            else:
                connection.set_isolation_level(
                    psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT
                    if autocommit
                    else
                    psycopg2.extensions.ISOLATION_LEVEL_READ_COMMITTED
                )

        ret = []
        old_autocommit = None
        _connection = None
        cursor = None
        try:
            self.logger.debug(
                "Database: '%s', Statement: '%s', args: %s",
                database,
                statement,
                args,
            )
            if not ownConnection:
                connection = self.environment[self._dbenvkeys['connection']]
            else:
                self.logger.debug('Creating own connection')

                _connection = connection = self.connect(
                    host=host,
                    port=port,
                    secured=secured,
                    securedHostValidation=securedHostValidation,
                    user=user,
                    password=password,
                    database=database,
                )

            if not transaction:
                old_autocommit = __backup_autocommit(connection)
                __set_autocommit(connection, True)

            cursor = connection.cursor()
            cursor.execute(
                statement,
                args,
            )

            if cursor.description is not None:
                cols = [d[0] for d in cursor.description]
                while True:
                    entry = cursor.fetchone()
                    if entry is None:
                        break
                    ret.append(dict(zip(cols, entry)))

        except:
            if _connection is not None:
                _connection.rollback()
            raise
        else:
            if _connection is not None:
                _connection.commit()
        finally:
            if old_autocommit is not None and connection is not None:
                __restore_autocommit(connection, old_autocommit)
            if cursor is not None:
                cursor.close()
            if _connection is not None:
                _connection.close()

        self.logger.debug('Result: %s', ret)
        return ret


@util.export
class OvirtUtils(base.Base):

    _plainPassword = None

    @property
    def environment(self):
        return self._environment

    @property
    def command(self):
        return self._plugin.command

    @property
    def dialog(self):
        return self._plugin.dialog

    def __init__(
        self,
        plugin,
        dbenvkeys,
        environment=None,
    ):
        super(OvirtUtils, self).__init__()
        self._plugin = plugin
        self._environment = (
            self._plugin.environment
            if environment is None
            else environment
        )
        self._dbenvkeys = dbenvkeys

    def detectCommands(self):
        self.command.detect('pg_dump')
        self.command.detect('pg_restore')
        self.command.detect('psql')

    def createPgPass(self):

        #
        # we need client side psql library
        # version as at least in rhel for 8.4
        # the password within pgpassfile is
        # not escaped.
        # the simplest way is to checkout psql
        # utility version.
        #
        if type(self)._plainPassword is None:
            rc, stdout, stderr = self._plugin.execute(
                args=(
                    self.command.get('psql'),
                    '-V',
                ),
            )
            type(self)._plainPassword = ' 8.' in stdout[0]

        fd, pgpass = tempfile.mkstemp()
        atexit.register(os.unlink, pgpass)
        with os.fdopen(fd, 'w') as f:
            f.write(
                (
                    '# DB USER credentials.\n'
                    '{host}:{port}:{database}:{user}:{password}\n'
                ).format(
                    host=self.environment[self._dbenvkeys['host']],
                    port=self.environment[self._dbenvkeys['port']],
                    database=self.environment[self._dbenvkeys['database']],
                    user=self.environment[self._dbenvkeys['user']],
                    password=(
                        self.environment[self._dbenvkeys['password']]
                        if type(self)._plainPassword
                        else outil.escape(
                            self.environment[self._dbenvkeys['password']],
                            ':\\',
                        )
                    ),
                ),
            )
        self.environment[self._dbenvkeys['pgpassfile']] = pgpass

    def tryDatabaseConnect(self, environment=None):

        if environment is None:
            environment = self.environment

        try:
            statement = Statement(
                environment=environment,
                dbenvkeys=self._dbenvkeys,
            )
            statement.execute(
                statement="""
                    select 1
                """,
                ownConnection=True,
                transaction=False,
            )
            self.logger.debug('Connection succeeded')
        except psycopg2.OperationalError as e:
            self.logger.debug('Connection failed', exc_info=True)
            raise RuntimeError(
                _('Cannot connect to database: {error}').format(
                    error=e,
                )
            )

    def isNewDatabase(
        self,
        host=None,
        port=None,
        secured=None,
        user=None,
        password=None,
        database=None,
    ):
        statement = Statement(
            environment=self.environment,
            dbenvkeys=self._dbenvkeys,
        )
        ret = statement.execute(
            statement="""
                select count(*) as count
                from pg_catalog.pg_tables
                where schemaname = 'public';
            """,
            args=dict(),
            host=host,
            port=port,
            secured=secured,
            user=user,
            password=password,
            database=database,
            ownConnection=True,
            transaction=False,
        )
        return ret[0]['count'] == 0

    def createLanguage(self, language):
        statement = Statement(
            environment=self.environment,
            dbenvkeys=self._dbenvkeys,
        )

        if statement.execute(
            statement="""
                select count(*)
                from pg_language
                where lanname=%(language)s;
            """,
            args=dict(
                language=language,
            ),
            ownConnection=True,
            transaction=False,
        )[0]['count'] == 0:
            statement.execute(
                statement=(
                    """
                        create language {language};
                    """
                ).format(
                    language=language,
                ),
                args=dict(),
                ownConnection=True,
                transaction=False,
            )

    def clearDatabase(self):

        self.createLanguage('plpgsql')

        statement = Statement(
            environment=self.environment,
            dbenvkeys=self._dbenvkeys,
        )

        statement.execute(
            statement="""
                create or replace
                function
                    oesetup_generate_drop_all_syntax()
                    returns setof text
                AS $procedure$ begin
                    return query
                        select
                            'drop function if exists ' ||
                            ns.nspname ||
                            '.' ||
                            proname ||
                            '(' ||
                                oidvectortypes(proargtypes) ||
                            ') cascade;'
                        from
                            pg_proc inner join pg_namespace ns on (
                                pg_proc.pronamespace=ns.oid
                            )
                        where
                            ns.nspname = 'public'
                        union
                        select
                            'drop type if exists ' ||
                            c.relname::information_schema.sql_identifier ||
                            ' ' ||
                            'cascade;'
                        from
                            pg_namespace n, pg_class c, pg_type t
                        where
                            n.oid = c.relnamespace and t.typrelid = c.oid and
                            c.relkind = 'c'::"char" and n.nspname = 'public';
                end; $procedure$
                language plpgsql;
            """,
            args=dict(),
            ownConnection=True,
            transaction=False,
        )

        spdrops = statement.execute(
            statement="""
                select oesetup_generate_drop_all_syntax as drop
                from oesetup_generate_drop_all_syntax()
            """,
            ownConnection=True,
            transaction=False,
        )
        for spdrop in [t['drop'] for t in spdrops]:
            statement.execute(
                statement=spdrop,
                ownConnection=True,
                transaction=False,
            )

        tables = statement.execute(
            statement="""
                select table_name
                from information_schema.views
                where table_schema = %(schemaname)s
            """,
            args=dict(
                schemaname='public',
            ),
            ownConnection=True,
            transaction=False,
        )
        for view in [t['table_name'] for t in tables]:
            statement.execute(
                statement=(
                    """
                        drop view if exists {view} cascade
                    """
                ).format(
                    view=view,
                ),
                ownConnection=True,
                transaction=False,
            )

        seqs = statement.execute(
            statement="""
                select relname as seqname
                from pg_class
                where relkind=%(relkind)s
            """,
            args=dict(
                relkind='S',
            ),
            ownConnection=True,
            transaction=False,
        )
        for seq in [t['seqname'] for t in seqs]:
            statement.execute(
                statement=(
                    """
                        drop sequence if exists {sequence} cascade
                    """
                ).format(
                    sequence=seq,
                ),
                ownConnection=True,
                transaction=False,
            )

        tables = statement.execute(
            statement="""
                select tablename
                from pg_tables
                where schemaname = %(schemaname)s
            """,
            args=dict(
                schemaname='public',
            ),
            ownConnection=True,
            transaction=False,
        )
        for table in [t['tablename'] for t in tables]:
            statement.execute(
                statement=(
                    """
                        drop table if exists {table} cascade
                    """
                ).format(
                    table=table,
                ),
                ownConnection=True,
                transaction=False,
            )

    def backup(
        self,
        dir,
        prefix,
    ):
        fd, backupFile = tempfile.mkstemp(
            prefix='%s-%s.' % (
                prefix,
                datetime.datetime.now().strftime('%Y%m%d%H%M%S')
            ),
            suffix='.dump',
            dir=dir,
        )
        os.close(fd)

        self.logger.info(
            _("Backing up database {host}:{database} to '{file}'.").format(
                host=self.environment[self._dbenvkeys['host']],
                database=self.environment[self._dbenvkeys['database']],
                file=backupFile,
            )
        )
        self._plugin.execute(
            (
                self.command.get('pg_dump'),
                '-E', 'UTF8',
                '--disable-dollar-quoting',
                '--disable-triggers',
                '--format=c',
                '-U', self.environment[self._dbenvkeys['user']],
                '-h', self.environment[self._dbenvkeys['host']],
                '-p', str(self.environment[self._dbenvkeys['port']]),
                '-f', backupFile,
                self.environment[self._dbenvkeys['database']],
            ),
            envAppend={
                'PGPASSWORD': '',
                'PGPASSFILE': self.environment[self._dbenvkeys['pgpassfile']],
            },
        )

        return backupFile

    def restore(
        self,
        backupFile,
    ):
        self._plugin.execute(
            (
                self.command.get('pg_restore'),
                '-w',
                '-h', self.environment[self._dbenvkeys['host']],
                '-p', str(self.environment[self._dbenvkeys['port']]),
                '-U', self.environment[self._dbenvkeys['user']],
                '-d', self.environment[self._dbenvkeys['database']],
                '-j', '2',
                backupFile,
            ),
            envAppend={
                'PGPASSWORD': '',
                'PGPASSFILE': self.environment[self._dbenvkeys['pgpassfile']],
            },
            raiseOnError=False,
            # TODO: check stderr of this and raise an error if there are real
            # errors. We currently always have one about plpgsql already
            # existing. When doing that, verify with both pg 8 and 9.
        )

    def _checkDbEncoding(self, environment, name):

        statement = Statement(
            environment=environment,
            dbenvkeys=self._dbenvkeys,
        )
        encoding = statement.execute(
            statement="""
                show server_encoding
            """,
            ownConnection=True,
            transaction=False,
        )[0]['server_encoding']
        if encoding.lower() != 'utf8':
            raise RuntimeError(
                _(
                    'Encoding of the {name} database is {encoding}. '
                    '{name} installation is only supported on servers '
                    'with default encoding set to UTF8. Please fix the '
                    'default DB encoding before you continue'
                ).format(
                    encoding=encoding,
                    name=name,
                )
            )

    def getCredentials(
        self,
        name,
        queryprefix,
        defaultdbenvkeys,
        show_create_msg=False,
        note=None,
        credsfile=None,
    ):
        interactive = None in (
            self.environment[self._dbenvkeys['host']],
            self.environment[self._dbenvkeys['port']],
            self.environment[self._dbenvkeys['database']],
            self.environment[self._dbenvkeys['user']],
            self.environment[self._dbenvkeys['password']],
        )

        if interactive:
            if note is None and credsfile:
                note = _(
                    "\nPlease provide the following credentials for the "
                    "{name} database.\nThey should be found on the {name} "
                    "server in '{credsfile}'.\n\n"
                ).format(
                    name=name,
                    credsfile=credsfile,
                )

            if note:
                self.dialog.note(text=note)

            if show_create_msg:
                self.dialog.note(
                    text=_(
                        "\n"
                        "ATTENTION\n"
                        "\n"
                        "Manual action required.\n"
                        "Please create database for ovirt-engine use. "
                        "Use the following commands as an example:\n"
                        "\n"
                        "create role {user} with login encrypted password "
                        "'{user}';\n"
                        "create database {database} owner {user}\n"
                        " template template0\n"
                        " encoding 'UTF8' lc_collate 'en_US.UTF-8'\n"
                        " lc_ctype 'en_US.UTF-8';\n"
                        "\n"
                        "Make sure that database can be accessed remotely.\n"
                        "\n"
                    ).format(
                        user=defaultdbenvkeys['user'],
                        database=defaultdbenvkeys['database'],
                    ),
                )

        connectionValid = False
        while not connectionValid:
            host = self.environment[self._dbenvkeys['host']]
            port = self.environment[self._dbenvkeys['port']]
            secured = self.environment[self._dbenvkeys['secured']]
            securedHostValidation = self.environment[
                self._dbenvkeys['hostValidation']
            ]
            db = self.environment[self._dbenvkeys['database']]
            user = self.environment[self._dbenvkeys['user']]
            password = self.environment[self._dbenvkeys['password']]

            if host is None:
                while True:
                    host = self.dialog.queryString(
                        name='{qpref}HOST'.format(qpref=queryprefix),
                        note=_(
                            '{name} database host [@DEFAULT@]: '
                        ).format(
                            name=name,
                        ),
                        prompt=True,
                        default=defaultdbenvkeys['host'],
                    )
                    try:
                        socket.getaddrinfo(host, None)
                        break  # do while missing in python
                    except socket.error as e:
                        self.logger.error(
                            _('Host is invalid: {error}').format(
                                error=e.strerror
                            )
                        )

            if port is None:
                while True:
                    try:
                        port = osetuputil.parsePort(
                            self.dialog.queryString(
                                name='{qpref}PORT'.format(qpref=queryprefix),
                                note=_(
                                    '{name} database port [@DEFAULT@]: '
                                ).format(
                                    name=name,
                                ),
                                prompt=True,
                                default=defaultdbenvkeys['port'],
                            )
                        )
                        break  # do while missing in python
                    except ValueError:
                        pass

            if secured is None:
                secured = dialog.queryBoolean(
                    dialog=self.dialog,
                    name='{qpref}SECURED'.format(qpref=queryprefix),
                    note=_(
                        '{name} database secured connection (@VALUES@) '
                        '[@DEFAULT@]: '
                    ).format(
                        name=name,
                    ),
                    prompt=True,
                    default=defaultdbenvkeys['secured'],
                )

            if not secured:
                securedHostValidation = False

            if securedHostValidation is None:
                securedHostValidation = dialog.queryBoolean(
                    dialog=self.dialog,
                    name='{qpref}SECURED_HOST_VALIDATION'.format(
                        qpref=queryprefix
                    ),
                    note=_(
                        '{name} database host name validation in secured '
                        'connection (@VALUES@) [@DEFAULT@]: '
                    ).format(
                        name=name,
                    ),
                    prompt=True,
                    default=True,
                ) == 'yes'

            if db is None:
                db = self.dialog.queryString(
                    name='{qpref}DATABASE'.format(qpref=queryprefix),
                    note=_(
                        '{name} database name [@DEFAULT@]: '
                    ).format(
                        name=name,
                    ),
                    prompt=True,
                    default=defaultdbenvkeys['database'],
                )

            if user is None:
                user = self.dialog.queryString(
                    name='{qpref}USER'.format(qpref=queryprefix),
                    note=_(
                        '{name} database user [@DEFAULT@]: '
                    ).format(
                        name=name,
                    ),
                    prompt=True,
                    default=defaultdbenvkeys['user'],
                )

            if password is None:
                password = self.dialog.queryString(
                    name='{qpref}PASSWORD'.format(qpref=queryprefix),
                    note=_(
                        '{name} database password: '
                    ).format(
                        name=name,
                    ),
                    prompt=True,
                    hidden=True,
                )

            dbenv = {
                self._dbenvkeys['host']: host,
                self._dbenvkeys['port']: port,
                self._dbenvkeys['secured']: secured,
                self._dbenvkeys['hostValidation']: securedHostValidation,
                self._dbenvkeys['user']: user,
                self._dbenvkeys['password']: password,
                self._dbenvkeys['database']: db,
            }

            if interactive:
                try:
                    self.tryDatabaseConnect(dbenv)
                    self._checkDbEncoding(environment=dbenv, name=name)
                    self.environment.update(dbenv)
                    connectionValid = True
                except RuntimeError as e:
                    self.logger.error(
                        _('Cannot connect to {name} database: {error}').format(
                            name=name,
                            error=e,
                        )
                    )
            else:
                # this is usally reached in provisioning
                # or if full ansewr file
                self.environment.update(dbenv)
                connectionValid = True

        try:
            self.environment[
                self._dbenvkeys['newDatabase']
            ] = self.isNewDatabase()
        except:
            self.logger.debug('database connection failed', exc_info=True)


# vim: expandtab tabstop=4 shiftwidth=4
