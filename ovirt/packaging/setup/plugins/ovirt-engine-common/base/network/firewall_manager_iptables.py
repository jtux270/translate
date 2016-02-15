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


"""
Firewall manager iptables plugin.
"""

import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin
from otopi import constants as otopicons
from otopi import filetransaction


from ovirt_engine import util as outil


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup import firewall_manager_base


from . import process_firewalld_services


@util.export
class Plugin(plugin.PluginBase):
    """
    Firewall manager iptables plugin.
    """

    class _IpTablesManager(firewall_manager_base.FirewallManagerBase):

        _SERVICE = 'iptables'

        def _get_rules(self):
            if self._rules is None:
                self._rules = outil.processTemplate(
                    osetupcons.FileLocations.OVIRT_IPTABLES_DEFAULT,
                    subst={
                        '@CUSTOM_RULES@': (
                            process_firewalld_services.Process.getInstance(
                                environment=self.environment,
                            ).parseFirewalld(
                                format=(
                                    '-A INPUT -p {protocol} -m state '
                                    '--state NEW -m {protocol} '
                                    '--dport {port} -j ACCEPT\n'
                                ),
                                portSeparator=':',
                            )
                        ),
                    }
                )
            return self._rules

        def __init__(self, plugin):
            super(Plugin._IpTablesManager, self).__init__(plugin)
            self._rules = None

        @property
        def name(self):
            return osetupcons.Const.FIREWALL_MANAGER_IPTABLES

        def detect(self):
            return self.plugin.services.exists(self._SERVICE)

        def active(self):
            return self.plugin.services.status(self._SERVICE)

        def prepare_examples(self):
            content = self._get_rules()
            self.environment[otopicons.CoreEnv.MAIN_TRANSACTION].append(
                filetransaction.FileTransaction(
                    name=osetupcons.FileLocations.OVIRT_IPTABLES_EXAMPLE,
                    content=content,
                    modifiedList=self.environment[
                        otopicons.CoreEnv.MODIFIED_FILES
                    ],
                )
            )

        def enable(self):
            self.environment[otopicons.NetEnv.IPTABLES_ENABLE] = True
            self.environment[
                otopicons.NetEnv.IPTABLES_RULES
            ] = self._get_rules()
            # This file is updated by otopi. Here we just prevent it from
            # being deleted on cleanup.
            # TODO: copy/move some uninstall code from the engine to otopi
            # to allow just adding lines to iptables instead of replacing
            # the file and also remove these lines on cleanup.
            self.environment[
                osetupcons.CoreEnv.UNINSTALL_UNREMOVABLE_FILES
            ].append(
                osetupcons.FileLocations.SYSCONFIG_IPTABLES,
            )

        def print_manual_configuration_instructions(self):
            self.plugin.dialog.note(
                text=_(
                    'An example of the required configuration for iptables '
                    'can be found at:\n'
                    '    {example}'
                ).format(
                    example=osetupcons.FileLocations.OVIRT_IPTABLES_EXAMPLE
                )
            )

    @plugin.event(
        stage=plugin.Stages.STAGE_SETUP,
        before=(
            osetupcons.Stages.KEEP_ONLY_VALID_FIREWALL_MANAGERS,
        ),
    )
    def _setup(self):
        self.environment[
            osetupcons.ConfigEnv.FIREWALL_MANAGERS
        ].append(Plugin._IpTablesManager(self))


# vim: expandtab tabstop=4 shiftwidth=4
