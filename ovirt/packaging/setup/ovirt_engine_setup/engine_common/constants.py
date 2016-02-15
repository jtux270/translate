#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2013-2014 Red Hat, Inc.
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


"""Constants."""


import os
import platform
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util


from . import config
from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.constants import classproperty
from ovirt_engine_setup.constants import osetupattrsclass
from ovirt_engine_setup.constants import osetupattrs


@util.export
class FileLocations(object):
    SYSCONFDIR = '/etc'
    OVIRT_ENGINE_COMMON_DATADIR = config.ENGINE_COMMON_DATADIR
    OVIRT_ENGINE_PKIDIR = config.ENGINE_PKIDIR
    OVIRT_ENGINE_PKICERTSDIR = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'certs',
    )
    OVIRT_ENGINE_PKIKEYSDIR = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'keys',
    )

    DIR_HTTPD = os.path.join(
        osetupcons.FileLocations.SYSCONFDIR,
        'httpd',
    )
    HTTPD_CONF_OVIRT_ROOT = os.path.join(
        DIR_HTTPD,
        'conf.d',
        'ovirt-engine-root-redirect.conf',
    )
    HTTPD_CONF_OVIRT_ROOT_TEMPLATE = os.path.join(
        osetupcons.FileLocations.OVIRT_SETUP_DATADIR,
        'conf',
        'ovirt-engine-root-redirect.conf.in',
    )
    HTTPD_CONF_SSL = os.path.join(
        DIR_HTTPD,
        'conf.d',
        'ssl.conf',
    )
    JBOSS_HOME = os.path.join(
        osetupcons.FileLocations.DATADIR,
        'jboss-as',
    )
    OVIRT_ENGINE_SYSCTL = os.path.join(
        SYSCONFDIR,
        'sysctl.d',
        'ovirt-postgresql.conf',
    )
    OVIRT_ENGINE_PKI_APACHE_CA_CERT = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'apache-ca.pem',
    )
    OVIRT_ENGINE_PKI_APACHE_CERT = os.path.join(
        OVIRT_ENGINE_PKICERTSDIR,
        'apache.cer',
    )
    OVIRT_ENGINE_PKI_APACHE_KEY = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        'apache.key.nopass',
    )


@util.export
class Defaults(object):
    DEFAULT_SYSTEM_USER_ROOT = 'root'
    DEFAULT_SYSTEM_USER_VDSM = 'vdsm'
    DEFAULT_SYSTEM_GROUP_KVM = 'kvm'
    DEFAULT_SYSTEM_USER_APACHE = 'apache'
    DEFAULT_SYSTEM_USER_POSTGRES = 'postgres'

    @classproperty
    def DEFAULT_SYSTEM_SHMMAX(self):
        SHMMAX = {
            'x86_64': 68719476736,
            'i686': 4294967295,
            'ppc64':  137438953472,
            'default': 4294967295,
        }
        return SHMMAX.get(platform.machine(), SHMMAX['default'])

    DEFAULT_PKI_COUNTRY = 'US'
    DEFAULT_PKI_STORE_PASS = 'mypass'

    DEFAULT_NETWORK_HTTP_PORT = 80
    DEFAULT_NETWORK_HTTPS_PORT = 443
    DEFAULT_NETWORK_JBOSS_HTTP_PORT = 8080
    DEFAULT_NETWORK_JBOSS_HTTPS_PORT = 8443
    DEFAULT_NETWORK_JBOSS_AJP_PORT = 8702
    DEFAULT_NETWORK_JBOSS_DEBUG_ADDRESS = '127.0.0.1:8787'

    DEFAULT_HTTPD_SERVICE = 'httpd'

    DEFAULT_POSTGRES_PROVISIONING_PGDATA_DIR = os.path.join(
        osetupcons.FileLocations.LOCALSTATEDIR,
        'lib',
        'pgsql',
        'data',
    )

    DEFAULT_POSTGRES_PROVISIONING_PG_CONF = os.path.join(
        DEFAULT_POSTGRES_PROVISIONING_PGDATA_DIR,
        'postgresql.conf',
    )

    DEFAULT_POSTGRES_PROVISIONING_PG_HBA = os.path.join(
        DEFAULT_POSTGRES_PROVISIONING_PGDATA_DIR,
        'pg_hba.conf',
    )

    DEFAULT_POSTGRES_PROVISIONING_PG_VERSION = os.path.join(
        DEFAULT_POSTGRES_PROVISIONING_PGDATA_DIR,
        'PG_VERSION',
    )

    DEFAULT_POSTGRES_PROVISIONING_SERVICE = 'postgresql'
    DEFAULT_POSTGRES_PROVISIONING_MAX_CONN = 150
    DEFAULT_POSTGRES_PROVISIONING_LISTEN_ADDRESS = "'*'"


@util.export
class Stages(object):
    APACHE_RESTART = 'osetup.apache.core.restart'

    CORE_ENGINE_START = 'osetup.core.engine.start'

    DB_CONNECTION_SETUP = 'osetup.db.connection.setup'
    DB_CONNECTION_CUSTOMIZATION = 'osetup.db.connection.customization'
    DB_OWNERS_CONNECTIONS_CUSTOMIZED = \
        'osetup.db.owners.connections.customized'
    DB_CONNECTION_STATUS = 'osetup.db.connection.status'
    DB_CREDENTIALS_AVAILABLE_EARLY = 'osetup.db.connection.credentials.early'
    DB_CREDENTIALS_AVAILABLE_LATE = 'osetup.db.connection.credentials.late'
    DB_CONNECTION_AVAILABLE = 'osetup.db.connection.available'
    DB_SCHEMA = 'osetup.db.schema'

    CONFIG_DB_ENCRYPTION_AVAILABLE = 'osetup.config.encryption.available'

    NETWORK_OWNERS_CONFIG_CUSTOMIZED = \
        'osetup.network.owners.config.customized'

    DIALOG_TITLES_S_ALLINONE = 'osetup.dialog.titles.allinone.start'
    DIALOG_TITLES_S_APACHE = 'osetup.dialog.titles.apache.start'
    DIALOG_TITLES_S_DATABASE = 'osetup.dialog.titles.database.start'
    DIALOG_TITLES_S_PKI = 'osetup.dialog.titles.pki.start'
    DIALOG_TITLES_E_ALLINONE = 'osetup.dialog.titles.allinone.end'
    DIALOG_TITLES_E_APACHE = 'osetup.dialog.titles.apache.end'
    DIALOG_TITLES_E_DATABASE = 'osetup.dialog.titles.database.end'
    DIALOG_TITLES_E_PKI = 'osetup.dialog.titles.pki.end'

    DIALOG_TITLES_S_ENGINE = 'osetup.dialog.titles.engine.start'
    DIALOG_TITLES_E_ENGINE = 'osetup.dialog.titles.engine.end'

    RENAME_PKI_CONF_MISC = 'osetup.rename.pki.conf.misc'


@util.export
@util.codegen
@osetupattrsclass
class SystemEnv(object):

    USER_APACHE = 'OVESETUP_SYSTEM/userApache'
    USER_POSTGRES = 'OVESETUP_SYSTEM/userPostgres'
    USER_ROOT = 'OVESETUP_SYSTEM/userRoot'
    USER_VDSM = 'OVESETUP_SYSTEM/userVdsm'
    GROUP_KVM = 'OVESETUP_SYSTEM/groupKvm'

    SHMMAX = 'OVESETUP_SYSTEM/shmmax'


@util.export
@util.codegen
@osetupattrsclass
class ConfigEnv(object):

    JAVA_HOME = 'OVESETUP_CONFIG/javaHome'
    JBOSS_HOME = 'OVESETUP_CONFIG/jbossHome'

    PUBLIC_HTTP_PORT = 'OVESETUP_CONFIG/publicHttpPort'  # internal use
    PUBLIC_HTTPS_PORT = 'OVESETUP_CONFIG/publicHttpsPort'  # internal use
    HTTP_PORT = 'OVESETUP_CONFIG/httpPort'
    HTTPS_PORT = 'OVESETUP_CONFIG/httpsPort'
    JBOSS_HTTP_PORT = 'OVESETUP_CONFIG/jbossHttpPort'
    JBOSS_HTTPS_PORT = 'OVESETUP_CONFIG/jbossHttpsPort'
    JBOSS_AJP_PORT = 'OVESETUP_CONFIG/jbossAjpPort'
    JBOSS_DIRECT_HTTP_PORT = 'OVESETUP_CONFIG/jbossDirectHttpPort'
    JBOSS_DIRECT_HTTPS_PORT = 'OVESETUP_CONFIG/jbossDirectHttpsPort'
    JBOSS_DEBUG_ADDRESS = 'OVESETUP_CONFIG/jbossDebugAddress'
    JBOSS_NEEDED = 'OVESETUP_CONFIG/jbossNeeded'
    JAVA_NEEDED = 'OVESETUP_CONFIG/javaNeeded'


@util.export
@util.codegen
@osetupattrsclass
class ProvisioningEnv(object):

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Configure local Engine database'),
    )
    def POSTGRES_PROVISIONING_ENABLED(self):
        return 'OVESETUP_PROVISIONING/postgresProvisioningEnabled'

    POSTGRES_CONF = 'OVESETUP_PROVISIONING/postgresConf'
    POSTGRES_PG_HBA = 'OVESETUP_PROVISIONING/postgresPgHba'
    POSTGRES_PG_VERSION = 'OVESETUP_PROVISIONING/postgresPgVersion'
    POSTGRES_SERVICE = 'OVESETUP_PROVISIONING/postgresService'
    POSTGRES_MAX_CONN = 'OVESETUP_PROVISIONING/postgresMaxConn'
    POSTGRES_LISTEN_ADDRESS = 'OVESETUP_PROVISIONING/postgresListenAddress'


@util.export
@util.codegen
@osetupattrsclass
class ApacheEnv(object):

    @osetupattrs(
        postinstallfile=True,
    )
    def CONFIGURED(self):
        return 'OVESETUP_APACHE/configured'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Set application as default page'),
    )
    def CONFIGURE_ROOT_REDIRECTION(self):
        return 'OVESETUP_APACHE/configureRootRedirection'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Configure Apache SSL'),
    )
    def CONFIGURE_SSL(self):
        return 'OVESETUP_APACHE/configureSsl'

    CONFIGURE_ROOT_REDIRECTIOND_DEFAULT = \
        'OVESETUP_APACHE/configureRootRedirectionDefault'
    ENABLE = 'OVESETUP_APACHE/enable'
    HTTPD_CONF_OVIRT_ROOT = 'OVESETUP_APACHE/configFileOvirtRoot'
    HTTPD_CONF_SSL = 'OVESETUP_APACHE/configFileSsl'
    HTTPD_SERVICE = 'OVESETUP_APACHE/httpdService'
    NEED_RESTART = 'OVESETUP_APACHE/needRestart'


# vim: expandtab tabstop=4 shiftwidth=4
