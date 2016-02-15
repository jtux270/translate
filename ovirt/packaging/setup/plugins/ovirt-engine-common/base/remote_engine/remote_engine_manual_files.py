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


import os
import tempfile
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util
from otopi import plugin


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup import remote_engine_base


@util.export
class Plugin(plugin.PluginBase):

    class _ManualFiles(remote_engine_base.RemoteEngineBase):

        def __init__(self, plugin):
            super(Plugin._ManualFiles, self).__init__(plugin=plugin)
            self._plugin = plugin
            self._tempfiles = []

        @property
        def plugin(self):
            return self._plugin

        @property
        def dialog(self):
            return self._plugin.dialog

        @property
        def environment(self):
            return self._plugin.environment

        @property
        def logger(self):
            return self._plugin.logger

        @property
        def name(self):
            return osetupcons.Const.REMOTE_ENGINE_SETUP_STYLE_MANUAL_FILES

        def desc(self):
            return _(
                'Perform each action manually, use files to copy content '
                'around'
            )

        def configure(self, fqdn):
            self._fqdn = fqdn

        def execute_on_engine(self, cmd, timeout=60, text=None):
            self.dialog.note(
                text=text if text else _(
                    'Please run on the engine server:\n\n'
                    '{cmd}\n\n'
                ).format(
                    cmd=cmd
                )
            )

        def copy_from_engine(self, file_name, dialog_name):
            resfilename = self.dialog.queryString(
                name=dialog_name,
                note=_(
                    'Please copy {file_name} from the engine server to some '
                    'file here.\n'
                    'Please input the location of the local file where you '
                    'copied {file_name} from the engine server: '
                ),
                prompt=True,
            )
            with open(resfilename) as f:
                res = f.read()
            return res

        def copy_to_engine(self, file_name, content, inp_env_key):
            fname = self.environment.get(inp_env_key)
            with (
                open(fname, 'w') if fname
                else tempfile.NamedTemporaryFile(mode='w', delete=False)
            ) as inpfile:
                inpfile.write(content)
            self.dialog.note(
                text=_(
                    'Please copy {inpfile} from here to {file_name} on the '
                    'engine server.\n'
                ).format(
                    inpfile=inpfile.name,
                    file_name=file_name,
                )
            )
            self._tempfiles.append(fname)

        def cleanup(self):
            for f in self._tempfiles:
                if f is not None:
                    try:
                        os.unlink(f.name)
                    except OSError:
                        self.logger.debug(
                            "Failed to delete '%s'",
                            f.name,
                            exc_info=True,
                        )

    def __init__(self, context):
        super(Plugin, self).__init__(context=context)

    @plugin.event(
        stage=plugin.Stages.STAGE_SETUP,
    )
    def _setup(self):
        self.environment[
            osetupcons.ConfigEnv.REMOTE_ENGINE_SETUP_STYLES
        ].append(
            self._ManualFiles(
                plugin=self,
            )
        )


# vim: expandtab tabstop=4 shiftwidth=4
