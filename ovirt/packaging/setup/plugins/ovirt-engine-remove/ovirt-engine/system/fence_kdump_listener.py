#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2014 Red Hat, Inc.
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


"""fence_kdump listener plugin."""


import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine import constants as oenginecons


@util.export
class Plugin(plugin.PluginBase):
    """fence_kdump listener plugin."""

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: (
            (
                self.environment[
                    osetupcons.RemoveEnv.REMOVE_ALL
                ] or
                self.environment[
                    oenginecons.RemoveEnv.REMOVE_ENGINE
                ]
            ) and
            not self.environment[
                osetupcons.CoreEnv.DEVELOPER_MODE
            ]
        ),
    )
    def _misc(self):
        self.services.startup(
            name=oenginecons.Const.FENCE_KDUMP_LISTENER_SERVICE_NAME,
            state=False,
        )


# vim: expandtab tabstop=4 shiftwidth=4
