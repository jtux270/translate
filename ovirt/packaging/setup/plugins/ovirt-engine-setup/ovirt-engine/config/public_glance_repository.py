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


"""Plugin to add the oVirt public Glance repository."""


import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine import constants as oenginecons
from ovirt_engine_setup.engine_common \
    import constants as oengcommcons


@util.export
class Plugin(plugin.PluginBase):
    """Plugin to add the oVirt public Glance repository."""

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            osetupcons.ConfigEnv.ADD_OVIRT_GLANCE_REPOSITORY,
            True,
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        after=(
            oengcommcons.Stages.DB_CONNECTION_AVAILABLE,
        ),
        condition=lambda self: (
            self.environment[oenginecons.CoreEnv.ENABLE] and
            self.environment[osetupcons.ConfigEnv.ADD_OVIRT_GLANCE_REPOSITORY]
        )
    )
    def _misc(self):
        self.environment[oenginecons.EngineDBEnv.STATEMENT].execute(
            statement="""
                select inst_add_glance_provider(
                    %(provider_id)s,
                    %(provider_name)s,
                    %(provider_description)s,
                    %(provider_url)s,
                    %(storage_domain_id)s
                )
            """,
            args=dict(
                provider_id="ceab03af-7220-4d42-8f5c-9b557f5d29af",
                provider_name="ovirt-image-repository",
                provider_description="Public Glance repository for oVirt",
                provider_url="http://glance.ovirt.org:9292",
                storage_domain_id="072fbaa1-08f3-4a40-9f34-a5ca22dd1d74"
            ),
        )


# vim: expandtab tabstop=4 shiftwidth=4
