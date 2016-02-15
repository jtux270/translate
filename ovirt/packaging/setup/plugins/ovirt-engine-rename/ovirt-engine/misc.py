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


"""Misc plugin."""


import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import constants as otopicons
from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons


@util.export
class Plugin(plugin.PluginBase):
    """Misc plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_BOOT,
        before=(
            otopicons.Stages.CORE_LOG_INIT,
        ),
    )
    def _preinit(self):
        self.environment.setdefault(
            otopicons.CoreEnv.LOG_DIR,
            osetupcons.FileLocations.OVIRT_SETUP_LOGDIR
        )
        self.environment.setdefault(
            otopicons.CoreEnv.LOG_FILE_NAME_PREFIX,
            osetupcons.FileLocations.OVIRT_OVIRT_RENAME_LOG_PREFIX
        )
        self.environment[
            osetupcons.CoreEnv.ACTION
        ] = osetupcons.Const.ACTION_RENAME

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            osetupcons.RenameEnv.FQDN,
            None
        )
        self.dialog.note(
            text=_(
                '\n'
                'Welcome to the ovirt-engine-rename utility\n'
                '\n'
                'More details about the operation and possible implications\n'
                'of running this utility can be found here:\n'
                'http://www.ovirt.org/Changing_Engine_Hostname\n'
                '\n'
            ),
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_VALIDATION,
    )
    def _validation(self):
        if self.environment[
            osetupcons.RenameEnv.FQDN
        ] is None:
            self.environment[
                osetupcons.RenameEnv.FQDN
            ] = self.dialog.queryString(
                name='OVESETUP_RENAME_FQDN',
                note=_('New fully qualified server name: '),
                prompt=True,
            )
            # TODO validate host name syntax
            # TODO check resolve?
        self.environment[osetupcons.ConfigEnv.FQDN] = self.environment[
            osetupcons.RenameEnv.FQDN
        ]

    @plugin.event(
        stage=plugin.Stages.STAGE_CLOSEUP,
        before=(
            osetupcons.Stages.DIALOG_TITLES_E_SUMMARY,
        ),
        after=(
            osetupcons.Stages.DIALOG_TITLES_S_SUMMARY,
        ),
    )
    def _closeup(self):
        self.dialog.note(
            text=_(
                'Rename completed successfully'
            ),
        )


# vim: expandtab tabstop=4 shiftwidth=4
