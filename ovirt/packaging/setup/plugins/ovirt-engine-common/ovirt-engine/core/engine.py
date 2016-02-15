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


"""Engine plugin."""


import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine import constants as oenginecons
from ovirt_engine_setup import dialog


@util.export
class Plugin(plugin.PluginBase):
    """Engine plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            oenginecons.CoreEnv.ENGINE_SERVICE_STOP,
            None
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_VALIDATION,
    )
    def _validation(self):
        if (
            self.services.exists(
                name=oenginecons.Const.ENGINE_SERVICE_NAME
            ) and self.services.status(
                name=oenginecons.Const.ENGINE_SERVICE_NAME
            )
        ):
            if self.environment[
                oenginecons.CoreEnv.ENGINE_SERVICE_STOP
            ] is None:
                self.environment[
                    oenginecons.CoreEnv.ENGINE_SERVICE_STOP
                ] = dialog.queryBoolean(
                    dialog=self.dialog,
                    name='OVESETUP_CORE_ENGINE_STOP',
                    note=_(
                        'During execution engine service will be stopped '
                        '(@VALUES@) [@DEFAULT@]: '
                    ),
                    prompt=True,
                    true=_('OK'),
                    false=_('Cancel'),
                    default=True,
                )

            if not self.environment[oenginecons.CoreEnv.ENGINE_SERVICE_STOP]:
                raise RuntimeError(
                    _('Engine service is running, no approval to stop')
                )

    @plugin.event(
        stage=plugin.Stages.STAGE_TRANSACTION_BEGIN,
        condition=lambda self: not self.environment[
            osetupcons.CoreEnv.DEVELOPER_MODE
        ],
    )
    def _transactionBegin(self):
        if self.services.exists(name=oenginecons.Const.ENGINE_SERVICE_NAME):
            self.logger.info(_('Stopping engine service'))
            self.services.state(
                name=oenginecons.Const.ENGINE_SERVICE_NAME,
                state=False
            )


# vim: expandtab tabstop=4 shiftwidth=4
