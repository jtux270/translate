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


"""Constants."""


import os
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import util


from ovirt_engine_setup.constants import osetupattrsclass
from ovirt_engine_setup.constants import osetupattrs


from . import config as wspconfig


@util.export
class Const(object):
    WEBSOCKET_PROXY_SERVICE_NAME = 'ovirt-websocket-proxy'
    WEBSOCKET_PROXY_PACKAGE_NAME = 'ovirt-engine-websocket-proxy'
    WEBSOCKET_PROXY_SETUP_PACKAGE_NAME = \
        'ovirt-engine-setup-plugin-websocket-proxy'
    WEBSOCKET_PROXY_CERT_NAME = 'websocket-proxy'


@util.export
class FileLocations(object):

    OVIRT_ENGINE_WEBSOCKET_PROXY_CONFIG = \
        wspconfig.ENGINE_WEBSOCKET_PROXY_CONFIG

    OVIRT_ENGINE_WEBSOCKET_PROXY_CONFIGD = (
        '%s.d' % OVIRT_ENGINE_WEBSOCKET_PROXY_CONFIG
    )
    OVIRT_ENGINE_WEBSOCKET_PROXY_CONFIG_SETUP = os.path.join(
        OVIRT_ENGINE_WEBSOCKET_PROXY_CONFIGD,
        '10-setup.conf',
    )

    OVIRT_ENGINE_PKIDIR = wspconfig.ENGINE_PKIDIR

    OVIRT_ENGINE_PKIKEYSDIR = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'keys',
    )
    OVIRT_ENGINE_PKICERTSDIR = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'certs',
    )
    OVIRT_ENGINE_PKIREQUESTSDIR = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'requests',
    )

    OVIRT_ENGINE_PKI_WEBSOCKET_PROXY_KEY = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        '%s.key.nopass' % Const.WEBSOCKET_PROXY_CERT_NAME,
    )
    OVIRT_ENGINE_PKI_WEBSOCKET_PROXY_CERT = os.path.join(
        OVIRT_ENGINE_PKICERTSDIR,
        '%s.cer' % Const.WEBSOCKET_PROXY_CERT_NAME,
    )

    OVIRT_ENGINE_PKI_WEBSOCKET_PROXY_REQ = os.path.join(
        OVIRT_ENGINE_PKICERTSDIR,
        '%s.req' % Const.WEBSOCKET_PROXY_CERT_NAME,
    )
    OVIRT_ENGINE_PKI_ENGINE_CERT = os.path.join(
        OVIRT_ENGINE_PKICERTSDIR,
        'engine.cer',
    )


@util.export
class Stages(object):

    CA_AVAILABLE = 'osetup.pki.ca.available'

    CONFIG_WEBSOCKET_PROXY_CUSTOMIZATION = \
        'setup.config.websocket-proxy.customization'

    REMOTE_VDC = 'setup.config.websocket-proxy.remote_vdc'

    # sync with engine
    ENGINE_CORE_ENABLE = 'osetup.engine.core.enable'


@util.export
class Defaults(object):
    DEFAULT_KEY_SIZE = 2048


@util.export
@util.codegen
@osetupattrsclass
class ConfigEnv(object):

    WEBSOCKET_PROXY_HOST = 'OVESETUP_CONFIG/websocketProxyHost'

    WEBSOCKET_PROXY_PORT = 'OVESETUP_CONFIG/websocketProxyPort'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Configure WebSocket Proxy'),
        postinstallfile=True,
    )
    def WEBSOCKET_PROXY_CONFIG(self):
        return 'OVESETUP_CONFIG/websocketProxyConfig'

    CERTIFICATE_ENROLLMENT = 'OVESETUP_CONFIG/certificateEnrollment'

    KEY_SIZE = 'OVESETUP_CONFIG/keySize'

    REMOTE_ENGINE_HOST = 'OVESETUP_CONFIG/remoteEngineHost'

    WSP_CERTIFICATE_CHAIN = 'OVESETUP_CONFIG/wspCertificateChain'
    REMOTE_ENGINE_CER = 'OVESETUP_CONFIG/remoteEngineCer'

    PKI_WSP_CSR_FILENAME = 'OVESETUP_CONFIG/pkiWSPCSRFilename'


@util.export
@util.codegen
@osetupattrsclass
class EngineCoreEnv(object):
    """Sync with ovirt-engine"""
    ENABLE = 'OVESETUP_ENGINE_CORE/enable'


@util.export
@util.codegen
@osetupattrsclass
class RemoveEnv(object):
    @osetupattrs(
        answerfile=True,
    )
    def REMOVE_WSP(self):
        return 'OVESETUP_REMOVE/removeWsp'


@util.export
@util.codegen
@osetupattrsclass
class RPMDistroEnv(object):
    PACKAGES = 'OVESETUP_WSP_RPMDISRO_PACKAGES'
    PACKAGES_SETUP = 'OVESETUP_WSP_RPMDISRO_PACKAGES_SETUP'


@util.export
@util.codegen
class Displays(object):
    CERTIFICATE_REQUEST = 'WSP_CERTIFICATE_REQUEST'


@util.export
@util.codegen
@osetupattrsclass
class EngineConfigEnv(object):
    """Sync with ovirt-engine"""

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Engine Host FQDN'),
        postinstallfile=True,
    )
    def ENGINE_FQDN(self):
        return 'OVESETUP_ENGINE_CONFIG/fqdn'


# vim: expandtab tabstop=4 shiftwidth=4
