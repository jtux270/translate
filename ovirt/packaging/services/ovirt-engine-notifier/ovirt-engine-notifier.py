#!/usr/bin/python

# Copyright 2012 Red Hat
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


import os
import subprocess
import sys
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine')


import config


from ovirt_engine import configfile
from ovirt_engine import service
from ovirt_engine import java


class Daemon(service.Daemon):

    def __init__(self):
        super(Daemon, self).__init__()
        self._defaults = os.path.abspath(
            os.path.join(
                os.path.dirname(sys.argv[0]),
                'ovirt-engine-notifier.conf',
            )
        )

    def _checkInstallation(
        self,
        pidfile,
        jbossModulesJar,
    ):
        # Check the required JBoss directories and files:
        self.check(
            name=self._config.get('JBOSS_HOME'),
            directory=True,
        )
        self.check(
            name=jbossModulesJar,
        )

        # Check the required engine directories and files:
        self.check(
            os.path.join(
                self._config.get('ENGINE_USR'),
                'services',
            ),
            directory=True,
        )
        self.check(
            os.path.join(
                self._config.get('ENGINE_LOG'),
                'notifier',
            ),
            directory=True,
            writable=True,
        )
        for log in ('notifier.log', 'console.log'):
            self.check(
                name=os.path.join(
                    self._config.get("ENGINE_LOG"),
                    'notifier',
                    log,
                ),
                mustExist=False,
                writable=True,
            )
        if pidfile is not None:
            self.check(
                name=pidfile,
                writable=True,
                mustExist=False,
            )

    def daemonSetup(self):

        if os.geteuid() == 0:
            raise RuntimeError(
                _('This service cannot be executed as root')
            )

        if not os.path.exists(self._defaults):
            raise RuntimeError(
                _(
                    "The configuration defaults file '{file}' "
                    "required but missing"
                ).format(
                    file=self._defaults,
                )
            )

        self._config = configfile.ConfigFile(
            (
                self._defaults,
                config.ENGINE_NOTIFIER_VARS,
            ),
        )

        #
        # the earliest so we can abort early.
        #
        self._executable = os.path.join(
            java.Java().getJavaHome(),
            'bin',
            'java',
        )

        jbossModulesJar = os.path.join(
            self._config.get('JBOSS_HOME'),
            'jboss-modules.jar',
        )

        self._checkInstallation(
            pidfile=self.pidfile,
            jbossModulesJar=jbossModulesJar,
        )

        self._engineArgs = [
            # The name of the process, as displayed by ps:
            'ovirt-engine-notifier',

            '-Dlog4j.configuration=file://%s/notifier/log4j.xml' % (
                self._config.get('ENGINE_ETC'),
            ),
            '-Djboss.modules.write-indexes=false',
        ]

        debugAddress = self._config.get('NOTIFIER_DEBUG_ADDRESS')
        if debugAddress:
            self._engineArgs.append(
                (
                    '-agentlib:jdwp=transport=dt_socket,address=%s,'
                    'server=y,suspend=n'
                ) % (
                    debugAddress
                )
            )

        self._engineArgs += [
            '-jar', jbossModulesJar,
            '-dependencies', 'org.ovirt.engine.core.tools',
            '-class', 'org.ovirt.engine.core.notifier.Notifier',
        ]

        self._engineEnv = os.environ.copy()
        self._engineEnv.update({
            'PATH': (
                '/usr/local/sbin:/usr/local/bin:'
                '/usr/sbin:/usr/bin:/sbin:/bin'
            ),
            'LANG': 'en_US.UTF-8',
            'LC_ALL': 'en_US.UTF-8',
            'CLASSPATH': '',
            'JAVA_MODULEPATH': '%s:%s' % (
                self._config.get('ENGINE_JAVA_MODULEPATH'),
                os.path.join(
                    self._config.get('JBOSS_HOME'),
                    'modules',
                )
            ),
            'ENGINE_DEFAULTS': config.ENGINE_DEFAULTS,
            'ENGINE_VARS': config.ENGINE_VARS,
            'ENGINE_NOTIFIER_DEFAULTS': self._defaults,
            'ENGINE_NOTIFIER_VARS': config.ENGINE_NOTIFIER_VARS,
        })

        self._validateConfig()

    def daemonStdHandles(self):
        consoleLog = open(
            os.path.join(
                self._config.get('ENGINE_LOG'),
                'notifier',
                'console.log'
            ),
            'w+',
        )
        return (consoleLog, consoleLog)

    def daemonContext(self):
        self.daemonAsExternalProcess(
            executable=self._executable,
            args=self._engineArgs,
            env=self._engineEnv,
            stopTime=self._config.getinteger(
                'NOTIFIER_STOP_TIME'
            ),
            stopInterval=self._config.getinteger(
                'NOTIFIER_STOP_INTERVAL'
            ),
        )

    def _validateConfig(self):
        args = self._engineArgs + ['validate']
        self.logger.debug('Executing: %s', args)
        proc = subprocess.Popen(
            args=args,
            executable=self._executable,
            env=self._engineEnv,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            close_fds=True,
        )

        stdout, stderr = proc.communicate()

        def printToBoth(msg):
            self.logger.debug('%s', msg)
            sys.stderr.write(msg + '\n')

        def printstd(std, prefix):
            if std:
                for l in std.decode('utf-8', 'replace').splitlines():
                    printToBoth(
                        '%s: %s' % (
                            prefix,
                            l,
                        )
                    )

        if stdout or stderr:
            sys.stderr.write('\n')
            printToBoth(_('Validation result:'))
            printstd(stdout, 'stdout')
            printstd(stderr, 'stderr')

        if proc.returncode != 0:
            printToBoth(
                _('Validation failed returncode is {rc}:').format(
                    rc=proc.returncode,
                )
            )
            raise RuntimeError(_('Configuration is invalid'))

if __name__ == '__main__':
    service.setupLogger()
    d = Daemon()
    d.run()


# vim: expandtab tabstop=4 shiftwidth=4
