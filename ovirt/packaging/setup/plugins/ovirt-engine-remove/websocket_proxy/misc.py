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


"""Websocket-proxy plugin."""


import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.websocket_proxy import constants as owspcons
from ovirt_engine_setup import dialog


@util.export
class Plugin(plugin.PluginBase):
    """Websocket-proxy plugin."""

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            owspcons.RemoveEnv.REMOVE_WSP,
            None
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        name=osetupcons.Stages.REMOVE_CUSTOMIZATION_COMMON,
        condition=lambda self: not self.environment[
            osetupcons.RemoveEnv.REMOVE_ALL
        ],
    )
    def _customization(self):
        if self.environment[
            owspcons.RemoveEnv.REMOVE_WSP
        ] is None:
            self.environment[
                owspcons.RemoveEnv.REMOVE_WSP
            ] = dialog.queryBoolean(
                dialog=self.dialog,
                name='OVESETUP_REMOVE_ENGINE',
                note=_(
                    'Do you want to remove the WebSocket proxy? '
                    '(@VALUES@) [@DEFAULT@]: '
                ),
                prompt=True,
                true=_('Yes'),
                false=_('No'),
                default=True,
            )
            if self.environment[owspcons.RemoveEnv.REMOVE_WSP]:
                self.environment[osetupcons.RemoveEnv.REMOVE_OPTIONS].append(
                    owspcons.Const.WEBSOCKET_PROXY_PACKAGE_NAME
                )

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: (
            (
                self.environment[osetupcons.RemoveEnv.REMOVE_ALL] or
                self.environment[owspcons.RemoveEnv.REMOVE_WSP]
            ) and not self.environment[osetupcons.CoreEnv.DEVELOPER_MODE]
        ),
    )
    def _misc(self):
        if self.services.exists(
            name=owspcons.Const.WEBSOCKET_PROXY_SERVICE_NAME
        ):
            self.services.startup(
                name=owspcons.Const.WEBSOCKET_PROXY_SERVICE_NAME,
                state=False,
            )


# vim: expandtab tabstop=4 shiftwidth=4
