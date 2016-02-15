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


"""Protocols plugin."""


import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import constants as otopicons
from otopi import util
from otopi import filetransaction
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine import constants as oenginecons
from ovirt_engine_setup.engine_common \
    import constants as oengcommcons


@util.export
class Plugin(plugin.PluginBase):
    """Protocols plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        condition=lambda self: self.environment[oenginecons.CoreEnv.ENABLE],
        after=(
            osetupcons.Stages.CONFIG_PROTOCOLS_CUSTOMIZATION,
        ),
        before=(
            oengcommcons.Stages.NETWORK_OWNERS_CONFIG_CUSTOMIZED,
        ),
    )
    def _customization(self):
        self.environment[
            oenginecons.ConfigEnv.ENGINE_FQDN
        ] = self.environment[
            osetupcons.ConfigEnv.FQDN
        ]

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: self.environment[oenginecons.CoreEnv.ENABLE],
    )
    def _misc(self):
        def flag(o):
            return 'true' if o else 'false'
        content = (
            'ENGINE_FQDN={fqdn}\n'
            'ENGINE_PROXY_ENABLED={proxyFlag}\n'
            'ENGINE_PROXY_HTTP_PORT={proxyHttpPort}\n'
            'ENGINE_PROXY_HTTPS_PORT={proxyHttpsPort}\n'
            'ENGINE_AJP_ENABLED={proxyFlag}\n'
            'ENGINE_AJP_PORT={ajpPort}\n'
            'ENGINE_HTTP_ENABLED={directFlag}\n'
            'ENGINE_HTTPS_ENABLED={directFlag}\n'
            'ENGINE_HTTP_PORT={directHttpPort}\n'
            'ENGINE_HTTPS_PORT={directHttpsPort}\n'
        ).format(
            fqdn=self.environment[osetupcons.ConfigEnv.FQDN],
            proxyFlag=flag(self.environment[
                oengcommcons.ConfigEnv.JBOSS_AJP_PORT
            ]),
            directFlag=flag(self.environment[
                oengcommcons.ConfigEnv.JBOSS_DIRECT_HTTP_PORT
            ]),
            proxyHttpPort=self.environment[
                oengcommcons.ConfigEnv.HTTP_PORT
            ],
            proxyHttpsPort=self.environment[
                oengcommcons.ConfigEnv.HTTPS_PORT
            ],
            directHttpPort=self.environment[
                oengcommcons.ConfigEnv.JBOSS_DIRECT_HTTP_PORT
            ],
            directHttpsPort=self.environment[
                oengcommcons.ConfigEnv.JBOSS_DIRECT_HTTPS_PORT
            ],
            ajpPort=self.environment[
                oengcommcons.ConfigEnv.JBOSS_AJP_PORT
            ],
        )

        if self.environment[osetupcons.CoreEnv.DEVELOPER_MODE]:
            content += (
                'ENGINE_DEBUG_ADDRESS={debugAddress}\n'
            ).format(
                debugAddress=self.environment[
                    oengcommcons.ConfigEnv.JBOSS_DEBUG_ADDRESS
                ],
            )

        self.environment[otopicons.CoreEnv.MAIN_TRANSACTION].append(
            filetransaction.FileTransaction(
                name=(
                    oenginecons.FileLocations.
                    OVIRT_ENGINE_SERVICE_CONFIG_PROTOCOLS
                ),
                content=content,
                modifiedList=self.environment[
                    otopicons.CoreEnv.MODIFIED_FILES
                ],
            )
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_CLOSEUP,
        before=(
            osetupcons.Stages.DIALOG_TITLES_E_SUMMARY,
        ),
        after=(
            osetupcons.Stages.DIALOG_TITLES_S_SUMMARY,
        ),
        condition=lambda self: self.environment[oenginecons.CoreEnv.ENABLE],
    )
    def _closeup(self):
        # TODO
        # layout of jboss and proxy should be the same
        if self.environment[oengcommcons.ConfigEnv.JBOSS_AJP_PORT]:
            engineURI = oenginecons.Const.ENGINE_URI
        else:
            engineURI = '/'

        self.dialog.note(
            text=_(
                'Web access is enabled at:\n'
                '    http://{fqdn}:{httpPort}{engineURI}\n'
                '    https://{fqdn}:{httpsPort}{engineURI}\n'
            ).format(
                fqdn=self.environment[osetupcons.ConfigEnv.FQDN],
                httpPort=self.environment[
                    oengcommcons.ConfigEnv.PUBLIC_HTTP_PORT
                ],
                httpsPort=self.environment[
                    oengcommcons.ConfigEnv.PUBLIC_HTTPS_PORT
                ],
                engineURI=engineURI,
            )
        )

        if self.environment[osetupcons.CoreEnv.DEVELOPER_MODE]:
            self.dialog.note(
                text=_(
                    'JBoss is listening for debug connection at: {address}'
                ).format(
                    address=self.environment[
                        oengcommcons.ConfigEnv.JBOSS_DEBUG_ADDRESS
                    ],
                )
            )


# vim: expandtab tabstop=4 shiftwidth=4
