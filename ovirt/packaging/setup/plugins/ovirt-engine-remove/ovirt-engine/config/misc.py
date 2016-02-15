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


"""Engine-remove plugin."""


import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine import constants as oenginecons
from ovirt_engine_setup import dialog


@util.export
class Plugin(plugin.PluginBase):
    """Engine-remove plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            oenginecons.RemoveEnv.REMOVE_ENGINE,
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
        if (
            self.environment[
                oenginecons.RemoveEnv.REMOVE_ENGINE
            ] is None and
            self.environment[
                oenginecons.CoreEnv.ENABLE
            ]
        ):
            self.environment[
                oenginecons.RemoveEnv.REMOVE_ENGINE
            ] = dialog.queryBoolean(
                dialog=self.dialog,
                name='OVESETUP_REMOVE_ENGINE',
                note=_(
                    'Do you want to remove the engine? '
                    '(@VALUES@) [@DEFAULT@]: '
                ),
                prompt=True,
                true=_('Yes'),
                false=_('No'),
                default=True,
            )
            if self.environment[oenginecons.RemoveEnv.REMOVE_ENGINE]:
                self.environment[osetupcons.RemoveEnv.REMOVE_OPTIONS].append(
                    oenginecons.Const.ENGINE_PACKAGE_NAME
                )
                # TODO: avoid to hard-coded group names here.
                # we should modify all groups with some engine prefix so we
                # know what they are, then just iterate based on prefix.
                # alternatively have a group of groups.
                # Put as much information within uninstall so that the
                # uninstall will be as stupid as we can have.
                # as uninstall will be modified after upgrade, new groups will
                # be available there anyway... so we can modify names.
                # also, if there is some kind of a problem we can have
                # temporary mapping between old and new.
                # anything that will require update of both setup and remove
                # on regular basis.
                self.environment[
                    osetupcons.RemoveEnv.REMOVE_SPEC_OPTION_GROUP_LIST
                ].extend(
                    [
                        'ca_pki',
                        'exportfs',
                        'nfs_config',
                        'ca_pki',
                        'iso_domain',
                        'ca_config',
                        'ssl',
                        'versionlock',
                    ]
                )

    @plugin.event(
        stage=plugin.Stages.STAGE_CLOSEUP,
        before=(
            osetupcons.Stages.DIALOG_TITLES_E_SUMMARY,
        ),
        after=(
            osetupcons.Stages.DIALOG_TITLES_S_SUMMARY,
        ),
        condition=lambda self: (
            self.environment[
                osetupcons.RemoveEnv.REMOVE_ALL
            ] or
            self.environment[
                oenginecons.RemoveEnv.REMOVE_ENGINE
            ]
        ),
    )
    def _closeup(self):
        self.dialog.note(
            text=_(
                '{description} has been removed'
            ).format(
                description=oenginecons.Const.ENGINE_PACKAGE_NAME,
            ),
        )
        self.environment[
            oenginecons.CoreEnv.ENABLE
        ] = False


# vim: expandtab tabstop=4 shiftwidth=4
