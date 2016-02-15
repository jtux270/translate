#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2014-2015 Red Hat, Inc.
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


"""Reconfigure env plugin."""


import gettext

from otopi import constants as otopicons
from otopi import plugin, util

from ovirt_engine_setup import constants as osetupcons


def _(m):
    return gettext.dgettext(message=m, domain='ovirt-engine-setup')


@util.export
class Plugin(plugin.PluginBase):
    """Reconfigure env plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
        after=(
            otopicons.Stages.CORE_CONFIG_INIT,
        ),
    )
    def _init(self):
        self.environment.setdefault(
            osetupcons.CoreEnv.RECONFIGURE_OPTIONAL_COMPONENTS,
            None
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        before=(
            osetupcons.Stages.DIALOG_TITLES_S_PRODUCT_OPTIONS,
        ),
    )
    def _customization(self):
        if self.environment[
            osetupcons.CoreEnv.RECONFIGURE_OPTIONAL_COMPONENTS
        ]:
            consts = []
            for constobj in self.environment[
                osetupcons.CoreEnv.SETUP_ATTRS_MODULES
            ]:
                consts.extend(constobj.__dict__['__osetup_attrs__'])
            for c in consts:
                for k in c.__dict__.values():
                    if (
                        hasattr(k, '__osetup_attrs__') and
                        k.__osetup_attrs__['reconfigurable']
                    ):
                        k = k.fget(None)
                        if (
                            k in self.environment and
                            # We reset it only if it's disabled.
                            # Can't currently use this code to
                            # disable already-enabled components.
                            not self.environment[k]
                        ):
                            self.logger.debug(
                                'Resetting optional component env key {key} '
                                'old value was {val}'.format(
                                    key=k,
                                    val=self.environment[k],
                                )
                            )
                            self.environment[k] = None


# vim: expandtab tabstop=4 shiftwidth=4
