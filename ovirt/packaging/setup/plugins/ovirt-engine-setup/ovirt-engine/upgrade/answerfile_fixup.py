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


"""Answer file fixup plugin."""


import os
import glob
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine import constants as oenginecons


@util.export
class Plugin(plugin.PluginBase):
    """Temporary fixup for invalid permission of past answer files."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: self.environment[oenginecons.CoreEnv.ENABLE],
    )
    def _misc(self):
        for f in glob.glob(
            os.path.join(
                osetupcons.FileLocations.OVIRT_SETUP_ANSWERS_DIR,
                '*.conf',
            )
        ):
            try:
                os.chmod(f, 0o600)
            except:
                self.logger.debug(
                    "Cannot modify permission for '%s'",
                    f,
                    exc_info=True,
                )


# vim: expandtab tabstop=4 shiftwidth=4
