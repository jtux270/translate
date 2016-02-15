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


"""Local Postgres plugin."""


import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine_common \
    import constants as oengcommcons
from ovirt_engine_setup.engine import constants as oenginecons
from ovirt_engine_setup import dialog
from ovirt_engine_setup.engine_common import postgres


@util.export
class Plugin(plugin.PluginBase):
    """Local Postgres plugin."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)
        self._enabled = False
        self._renamedDBResources = False
        self._provisioning = postgres.Provisioning(
            plugin=self,
            dbenvkeys=oenginecons.Const.ENGINE_DB_ENV_KEYS,
            defaults=oenginecons.Const.DEFAULT_ENGINE_DB_ENV_KEYS,
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            oengcommcons.ProvisioningEnv.POSTGRES_PROVISIONING_ENABLED,
            None
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_SETUP,
        after=(
            oengcommcons.Stages.DB_CONNECTION_SETUP,
        ),
        condition=lambda self: (
            not self.environment[
                osetupcons.CoreEnv.DEVELOPER_MODE
            ] and
            self.environment[
                oenginecons.EngineDBEnv.NEW_DATABASE
            ]
        ),
    )
    def _setup(self):
        self._provisioning.detectCommands()

        self._enabled = self._provisioning.supported()

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        before=(
            oengcommcons.Stages.DIALOG_TITLES_E_DATABASE,
            oengcommcons.Stages.DB_CONNECTION_CUSTOMIZATION,
        ),
        after=(
            oengcommcons.Stages.DIALOG_TITLES_S_DATABASE,
        ),
        condition=lambda self: not self.environment[
            oenginecons.CoreEnv.ENABLE
        ],
        name=oenginecons.Stages.POSTGRES_PROVISIONING_ALLOWED,
    )
    def _customization_enable(self):
        self._enabled = False

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        before=(
            oengcommcons.Stages.DIALOG_TITLES_E_DATABASE,
            oengcommcons.Stages.DB_CONNECTION_CUSTOMIZATION,
        ),
        after=(
            oenginecons.Stages.POSTGRES_PROVISIONING_ALLOWED,
        ),
        condition=lambda self: self._enabled,
    )
    def _customization(self):
        if self.environment[
            oengcommcons.ProvisioningEnv.POSTGRES_PROVISIONING_ENABLED
        ] is None:
            local = dialog.queryBoolean(
                dialog=self.dialog,
                name='OVESETUP_PROVISIONING_POSTGRES_LOCATION',
                note=_(
                    'Where is the Engine database located? '
                    '(@VALUES@) [@DEFAULT@]: '
                ),
                prompt=True,
                true=_('Local'),
                false=_('Remote'),
                default=True,
            )
            if local:
                self.environment[oenginecons.EngineDBEnv.HOST] = 'localhost'
                self.environment[
                    oenginecons.EngineDBEnv.PORT
                ] = oenginecons.Defaults.DEFAULT_DB_PORT

                # TODO:
                # consider creating database and role
                # at engine_@RANDOM@
                self.environment[
                    oengcommcons.ProvisioningEnv.POSTGRES_PROVISIONING_ENABLED
                ] = dialog.queryBoolean(
                    dialog=self.dialog,
                    name='OVESETUP_PROVISIONING_POSTGRES_ENABLED',
                    note=_(
                        'Setup can configure the local postgresql server '
                        'automatically for the engine to run. This may '
                        'conflict with existing applications.\n'
                        'Would you like Setup to automatically configure '
                        'postgresql and create Engine database, '
                        'or prefer to perform that '
                        'manually? (@VALUES@) [@DEFAULT@]: '
                    ),
                    prompt=True,
                    true=_('Automatic'),
                    false=_('Manual'),
                    default=True,
                )

            else:
                self.environment[
                    oengcommcons.ProvisioningEnv.POSTGRES_PROVISIONING_ENABLED
                ] = False

        self._enabled = self.environment[
            oengcommcons.ProvisioningEnv.POSTGRES_PROVISIONING_ENABLED
        ]
        if self._enabled:
            self._provisioning.applyEnvironment()

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        priority=plugin.Stages.PRIORITY_LAST,
        condition=lambda self: (
            self.environment[
                oenginecons.EngineDBEnv.HOST
            ] == 'localhost'
        ),
    )
    def _customization_firewall(self):
        self.environment[osetupcons.NetEnv.FIREWALLD_SERVICES].extend([
            {
                'name': 'ovirt-postgres',
                'directory': 'ovirt-common'
            },
        ])

    @plugin.event(
        stage=plugin.Stages.STAGE_VALIDATION,
        condition=lambda self: self._enabled,
    )
    def _validation(self):
        self._provisioning.validate()

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        before=(
            oengcommcons.Stages.DB_CREDENTIALS_AVAILABLE_LATE,
            oengcommcons.Stages.DB_SCHEMA,
        ),
        after=(
            osetupcons.Stages.SYSTEM_SYSCTL_CONFIG_AVAILABLE,
        ),
        condition=lambda self: self._enabled,
    )
    def _misc(self):
        self._provisioning.provision()

    @plugin.event(
        stage=plugin.Stages.STAGE_CLOSEUP,
        before=(
            osetupcons.Stages.DIALOG_TITLES_E_SUMMARY,
        ),
        after=(
            osetupcons.Stages.DIALOG_TITLES_S_SUMMARY,
        ),
        condition=lambda self: self._provisioning.databaseRenamed,
    )
    def _closeup(self):
        self.dialog.note(
            text=_(
                'Engine database resources:\n'
                '    Database name:      {database}\n'
                '    Database user name: {user}\n'
            ).format(
                database=self.environment[
                    oenginecons.EngineDBEnv.DATABASE
                ],
                user=self.environment[
                    oenginecons.EngineDBEnv.USER
                ],
            )
        )


# vim: expandtab tabstop=4 shiftwidth=4
