#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2015 Red Hat, Inc.
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


"""vmconsole proxy configuration plugin."""

import gettext

from otopi import constants as otopicons
from otopi import filetransaction, plugin, util

from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup import hostname as osetuphostname
from ovirt_engine_setup import dialog
from ovirt_engine_setup.engine_common import constants as oengcommcons
from ovirt_engine_setup.vmconsole_proxy_helper import constants as ovmpcons


def _(m):
    return gettext.dgettext(message=m, domain='ovirt-engine-setup')


def _base_url_from_env(env):
    sslFlag = env[
        oengcommcons.ConfigEnv.JBOSS_DIRECT_HTTP_PORT
    ]
    proxyFlag = env[
        oengcommcons.ConfigEnv.JBOSS_AJP_PORT
    ]

    if sslFlag:
        proto = 'https'
        if proxyFlag:
            port = env[
                oengcommcons.ConfigEnv.HTTPS_PORT
            ]
        else:
            port = env[
                oengcommcons.ConfigEnv.JBOSS_DIRECT_HTTPS_PORT
            ]
    else:
        proto = 'http'
        if proxyFlag:
            port = env[
                oengcommcons.ConfigEnv.HTTP_PORT
            ]
        else:
            port = env[
                oengcommcons.ConfigEnv.JBOSS_DIRECT_HTTP_PORT
            ]

    return "{proto}://{fqdn}:{port}/ovirt-engine/".format(
        proto=proto,
        fqdn=env[osetupcons.ConfigEnv.FQDN],
        port=port,
    )


@util.export
class Plugin(plugin.PluginBase):
    """vmconsole proxy configuration plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            ovmpcons.ConfigEnv.VMCONSOLE_PROXY_CONFIG,
            None
        )
        self.environment.setdefault(
            ovmpcons.EngineConfigEnv.ENGINE_FQDN,
            None
        )
        self.environment.setdefault(
            ovmpcons.ConfigEnv.VMCONSOLE_PROXY_PORT,
            ovmpcons.Defaults.DEFAULT_VMCONSOLE_PROXY_PORT
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        before=(
            osetupcons.Stages.DIALOG_TITLES_E_PRODUCT_OPTIONS,
        ),
        after=(
            osetupcons.Stages.DIALOG_TITLES_S_PRODUCT_OPTIONS,
        ),
    )
    def _customization(self):
        if self.environment[
            ovmpcons.ConfigEnv.VMCONSOLE_PROXY_CONFIG
        ] is None:
            self.environment[
                ovmpcons.ConfigEnv.VMCONSOLE_PROXY_CONFIG
            ] = dialog.queryBoolean(
                dialog=self.dialog,
                name='OVESETUP_CONFIG_VMCONSOLE_PROXY',
                note=_(
                    'Configure VM Console Proxy on this host '
                    '(@VALUES@) [@DEFAULT@]: '
                ),
                prompt=True,
                default=True,
            )

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        after=(
            osetupcons.Stages.DIALOG_TITLES_S_NETWORK,
            oengcommcons.Stages.NETWORK_OWNERS_CONFIG_CUSTOMIZED,
        ),
        before=(
            osetupcons.Stages.DIALOG_TITLES_E_NETWORK,
        ),
        condition=lambda self: self.environment[
            ovmpcons.ConfigEnv.VMCONSOLE_PROXY_CONFIG
        ],
    )
    def _customizationNetwork(self):
        osetuphostname.Hostname(
            plugin=self,
        ).getHostname(
            envkey=ovmpcons.EngineConfigEnv.ENGINE_FQDN,
            whichhost=_('the engine'),
            supply_default=True,
        )
        self.environment[osetupcons.NetEnv.FIREWALLD_SERVICES].extend([
            {
                'name': 'ovirt-vmconsole-proxy',
                'directory': 'vmconsole-proxy'
            },
        ])
        self.environment[
            osetupcons.NetEnv.FIREWALLD_SUBST
        ].update({
            '@VMCONSOLE_PROXY_PORT@': self.environment[
                ovmpcons.ConfigEnv.VMCONSOLE_PROXY_PORT
            ],
        })

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: self.environment[
            ovmpcons.ConfigEnv.VMCONSOLE_PROXY_CONFIG
        ],
    )
    def _miscConfigVMConsoleHelper(self):
        content = (
            'ENGINE_BASE_URL={engine_base_url}\n'
            'TOKEN_CERTIFICATE={certificate}\n'
            'TOKEN_KEY={key}\n'
        ).format(
            engine_base_url=_base_url_from_env(self.environment),
            certificate=(
                ovmpcons.FileLocations.
                OVIRT_ENGINE_PKI_VMCONSOLE_PROXY_HELPER_CERT
            ),
            key=(
                ovmpcons.FileLocations.
                OVIRT_ENGINE_PKI_VMCONSOLE_PROXY_HELPER_KEY
            ),
        )

        self.environment[otopicons.CoreEnv.MAIN_TRANSACTION].append(
            filetransaction.FileTransaction(
                name=(
                    ovmpcons.FileLocations.
                    VMCONSOLE_PROXY_HELPER_VARS_SETUP
                ),
                content=content,
                modifiedList=self.environment[
                    otopicons.CoreEnv.MODIFIED_FILES
                ],
            )
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: (
            self.environment[
                ovmpcons.ConfigEnv.VMCONSOLE_PROXY_CONFIG
            ]
        ),
    )
    def _miscConfigVMConsoleProxy(self):
        with open(ovmpcons.FileLocations.OVIRT_VMCONSOLE_PROXY_CONFIG) as f:
            self.environment[otopicons.CoreEnv.MAIN_TRANSACTION].append(
                filetransaction.FileTransaction(
                    name=ovmpcons.FileLocations.VMCONSOLE_CONFIG,
                    content=f.read(),
                    modifiedList=self.environment[
                        otopicons.CoreEnv.MODIFIED_FILES
                    ],
                )
            )


# vim: expandtab tabstop=4 shiftwidth=4
