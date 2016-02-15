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


from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.engine_common import constants as oengcommcons
from ovirt_engine_setup.constants import classproperty
from ovirt_engine_setup.constants import osetupattrsclass
from ovirt_engine_setup.constants import osetupattrs


from . import config


@util.export
class FileLocations(object):
    SYSCONFDIR = '/etc'
    LOCALSTATEDIR = '/var'
    DATADIR = '/usr/share'

    OVIRT_ENGINE_SYSCONFDIR = config.ENGINE_SYSCONFDIR
    OVIRT_ENGINE_PKIDIR = config.ENGINE_PKIDIR
    OVIRT_ENGINE_DATADIR = config.ENGINE_DATADIR
    OVIRT_ENGINE_LOCALSTATEDIR = config.ENGINE_LOCALSTATEDIR
    OVIRT_ENGINE_LOGDIR = config.ENGINE_LOG
    OVIRT_ENGINE_SERVICE_CONFIG = config.ENGINE_SERVICE_CONFIG
    OVIRT_ENGINE_SERVICE_CONFIG_DEFAULTS = \
        config.ENGINE_SERVICE_CONFIG_DEFAULTS
    OVIRT_ENGINE_NOTIFIER_SERVICE_CONFIG = \
        config.ENGINE_NOTIFIER_SERVICE_CONFIG

    OVIRT_ENGINE_BINDIR = os.path.join(
        OVIRT_ENGINE_DATADIR,
        'bin',
    )

    OVIRT_ENGINE_DB_DIR = os.path.join(
        OVIRT_ENGINE_DATADIR,
        'dbscripts',
    )
    OVIRT_ENGINE_DB_SCHMA_TOOL = os.path.join(
        OVIRT_ENGINE_DB_DIR,
        'schema.sh',
    )
    OVIRT_ENGINE_DB_BACKUP_DIR = os.path.join(
        OVIRT_ENGINE_LOCALSTATEDIR,
        'backups',
    )

    OVIRT_ENGINE_DB_MD5_DIR = os.path.join(
        OVIRT_ENGINE_LOCALSTATEDIR,
        'dbmd5',
    )

    OVIRT_ENGINE_DB_UTILS_DIR = os.path.join(
        OVIRT_ENGINE_DATADIR,
        'setup',
        'dbutils'
    )

    OVIRT_ENGINE_DB_VALIDATOR = os.path.join(
        OVIRT_ENGINE_DB_UTILS_DIR,
        'validatedb.sh'
    )
    OVIRT_ENGINE_TASKCLEANER = os.path.join(
        OVIRT_ENGINE_DB_UTILS_DIR,
        'taskcleaner.sh'
    )

    OVIRT_ENGINE_DB_CHANGE_OWNER = os.path.join(
        OVIRT_ENGINE_DB_UTILS_DIR,
        'changedbowner.sh'
    )

    OVIRT_ENGINE_PKIKEYSDIR = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'keys',
    )
    OVIRT_ENGINE_PKICERTSDIR = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'certs',
    )
    OVIRT_ENGINE_PKIPRIVATEDIR = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'private',
    )
    OVIRT_ENGINE_PKI_CA_CREATE = os.path.join(
        OVIRT_ENGINE_BINDIR,
        'pki-create-ca.sh',
    )
    OVIRT_ENGINE_PKI_CA_ENROLL = os.path.join(
        OVIRT_ENGINE_BINDIR,
        'pki-enroll-pkcs12.sh',
    )
    OVIRT_ENGINE_PKI_PKCS12_EXTRACT = os.path.join(
        OVIRT_ENGINE_BINDIR,
        'pki-pkcs12-extract.sh',
    )

    OVIRT_ENGINE_PKI_ENGINE_STORE = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        'engine.p12',
    )
    OVIRT_ENGINE_PKI_ENGINE_CERT = os.path.join(
        OVIRT_ENGINE_PKICERTSDIR,
        'engine.cer',
    )
    OVIRT_ENGINE_PKI_ENGINE_SSH_KEY = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        'engine_id_rsa',
    )
    OVIRT_ENGINE_PKI_APACHE_STORE = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        'apache.p12',
    )
    OVIRT_ENGINE_PKI_LOCAL_WEBSOCKET_PROXY_STORE = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        'websocket-proxy.p12',
    )
    OVIRT_ENGINE_PKI_LOCAL_WEBSOCKET_PROXY_KEY = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        'websocket-proxy.key.nopass',
    )
    OVIRT_ENGINE_PKI_REPORTS_KEY = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        'reports.key.nopass',
    )
    OVIRT_ENGINE_PKI_JBOSS_STORE = os.path.join(
        OVIRT_ENGINE_PKIKEYSDIR,
        'jboss.p12',
    )
    OVIRT_ENGINE_PKI_JBOSS_CERT = os.path.join(
        OVIRT_ENGINE_PKICERTSDIR,
        'jboss.cer',
    )
    OVIRT_ENGINE_PKI_ENGINE_CA_CERT = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'ca.pem',
    )
    OVIRT_ENGINE_PKI_LOCAL_WEBSOCKET_PROXY_CERT = os.path.join(
        OVIRT_ENGINE_PKICERTSDIR,
        'websocket-proxy.cer',
    )
    OVIRT_ENGINE_PKI_ENGINE_TRUST_STORE = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        '.truststore',
    )
    OVIRT_ENGINE_PKI_CA_TEMPLATE = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'cacert.template.in',
    )
    OVIRT_ENGINE_PKI_CERT_TEMPLATE = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'cert.template.in',
    )
    OVIRT_ENGINE_PKI_CA_CERT_CONF = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'cacert.conf',
    )
    OVIRT_ENGINE_PKI_CERT_CONF = os.path.join(
        OVIRT_ENGINE_PKIDIR,
        'cert.conf',
    )
    OVIRT_ENGINE_PKI_ENGINE_CA_KEY = os.path.join(
        OVIRT_ENGINE_PKIPRIVATEDIR,
        'ca.pem',
    )

    NFS_RHEL_CONFIG = os.path.join(
        SYSCONFDIR,
        'sysconfig',
        'nfs',
    )
    NFS_EXPORT_FILE = os.path.join(
        SYSCONFDIR,
        'exports',
    )
    NFS_EXPORT_DIR = os.path.join(
        SYSCONFDIR,
        'exports.d',
    )
    OVIRT_NFS_EXPORT_FILE = os.path.join(
        NFS_EXPORT_DIR,
        'ovirt-engine-iso-domain.exports'
    )

    ISO_DOMAIN_DEFAULT_NFS_MOUNT_POINT = os.path.join(
        LOCALSTATEDIR,
        'lib',
        'exports',
        'iso',
    )

    DIR_HTTPD = os.path.join(
        osetupcons.FileLocations.SYSCONFDIR,
        'httpd',
    )

    HTTPD_CONF_OVIRT_ENGINE = os.path.join(
        DIR_HTTPD,
        'conf.d',
        'z-ovirt-engine-proxy.conf',
    )

    HTTPD_CONF_OVIRT_ENGINE_TEMPLATE = os.path.join(
        osetupcons.FileLocations.OVIRT_SETUP_DATADIR,
        'conf',
        'ovirt-engine-proxy.conf.v2.in',
    )

    OVIRT_ENGINE_SERVICE_CONFIGD = '%s.d' % OVIRT_ENGINE_SERVICE_CONFIG
    OVIRT_ENGINE_SERVICE_CONFIG_DATABASE = os.path.join(
        OVIRT_ENGINE_SERVICE_CONFIGD,
        '10-setup-database.conf',
    )
    OVIRT_ENGINE_SERVICE_CONFIG_PROTOCOLS = os.path.join(
        OVIRT_ENGINE_SERVICE_CONFIGD,
        '10-setup-protocols.conf',
    )
    OVIRT_ENGINE_SERVICE_CONFIG_JBOSS = os.path.join(
        OVIRT_ENGINE_SERVICE_CONFIGD,
        '10-setup-jboss.conf',
    )
    OVIRT_ENGINE_SERVICE_CONFIG_PKI = os.path.join(
        OVIRT_ENGINE_SERVICE_CONFIGD,
        '10-setup-pki.conf',
    )

    OVIRT_ENGINE_NOTIFIER_SERVICE_CONFIGD = (
        '%s.d' % OVIRT_ENGINE_NOTIFIER_SERVICE_CONFIG
    )
    OVIRT_ENGINE_NOTIFIER_SERVICE_CONFIG_JBOSS = os.path.join(
        OVIRT_ENGINE_NOTIFIER_SERVICE_CONFIGD,
        '10-setup-jboss.conf',
    )
    OVIRT_ENGINE_UNINSTALL_DIR = os.path.join(
        OVIRT_ENGINE_SYSCONFDIR,
        'uninstall.d'
    )

    AIO_VDSM_PATH = os.path.join(
        DATADIR,
        'vdsm',
    )
    AIO_STORAGE_DOMAIN_DEFAULT_DIR = os.path.join(
        LOCALSTATEDIR,
        'lib',
        'images',
    )
    AIO_POST_INSTALL_CONFIG = os.path.join(
        '%s.d' % osetupcons.FileLocations.OVIRT_OVIRT_SETUP_CONFIG_FILE,
        '20-setup-aio.conf'
    )


@util.export
class Defaults(object):
    DEFAULT_SYSTEM_MEMCHECK_MINIMUM_MB = 4096
    DEFAULT_SYSTEM_MEMCHECK_RECOMMENDED_MB = 16384
    DEFAULT_SYSTEM_MEMCHECK_THRESHOLD = 90

    DEFAULT_CONFIG_APPLICATION_MODE = 'Both'
    DEFAULT_CONFIG_STORAGE_IS_LOCAL = False

    DEFAULT_ISO_DOMAIN_NAME = 'ISO_DOMAIN'

    DEFAULT_CLEAR_TASKS_WAIT_PERIOD = 20

    DEFAULT_DB_HOST = 'localhost'
    DEFAULT_DB_PORT = 5432
    DEFAULT_DB_DATABASE = 'engine'
    DEFAULT_DB_USER = 'engine'
    DEFAULT_DB_PASSWORD = ''
    DEFAULT_DB_SECURED = False
    DEFAULT_DB_SECURED_HOST_VALIDATION = False


@util.export
class Stages(object):

    SYSTEM_NFS_CONFIG_AVAILABLE = 'osetup.system.nfs.available'

    CONFIG_ISO_DOMAIN_AVAILABLE = 'osetup.config.iso_domain.available'

    CORE_ENABLE = 'osetup.engine.core.enable'

    AIO_CONFIG_AVAILABLE = 'osetup.aio.config.available'
    AIO_CONFIG_NOT_AVAILABLE = 'osetup.aio.config.not.available'
    AIO_CONFIG_STORAGE = 'osetup.aio.config.storage'
    AIO_CONFIG_SSH = 'osetup.aio.config.ssh'
    AIO_CONFIG_VDSM = 'osetup.aio.config.vdsm'

    MEMORY_CHECK = 'osetup.memory.check'

    CA_ALLOWED = 'osetup.engine.pki.ca.allow'
    CA_AVAILABLE = 'osetup.pki.ca.available'

    POSTGRES_PROVISIONING_ALLOWED = 'osetup.engine.provisioning.pgsql.allow'
    NFS_CONFIG_ALLOWED = 'osetup.engine.system.nfs.allow'
    APPMODE_ALLOWED = 'osetup.engine.config.appmode.allow'
    KDUMP_ALLOW = 'osetup.engine.kdump.allow'
    CONNECTION_ALLOW = 'osetup.engine.db.connection.allow'


@util.export
@util.codegen
class Const(object):
    DOMAIN_INTERNAL = 'internal'

    ENGINE_PACKAGE_NAME = 'ovirt-engine'
    ENGINE_PACKAGE_SETUP_NAME = '%s-setup' % ENGINE_PACKAGE_NAME

    ENGINE_SERVICE_NAME = 'ovirt-engine'

    FENCE_KDUMP_LISTENER_SERVICE_NAME = 'ovirt-fence-kdump-listener'

    PKI_PASSWORD = 'mypass'
    MINIMUM_SPACE_ISODOMAIN_MB = 350
    ISO_DOMAIN_IMAGE_UID = '11111111-1111-1111-1111-111111111111'
    MAC_RANGE_BASE = '00:1a:4a'

    ENGINE_URI = '/ovirt-engine'
    ENGINE_PKI_CA_URI = (
        '%s/services/pki-resource?'
        'resource=ca-certificate&'
        'format=X509-PEM-CA'
    ) % (
        ENGINE_URI,
    )

    UPGRADE_YUM_GROUP_NAME = 'ovirt-engine-3.4'

    ENGINE_DB_BACKUP_PREFIX = 'engine'

    @classproperty
    def ENGINE_DB_ENV_KEYS(self):
        return {
            'host': EngineDBEnv.HOST,
            'port': EngineDBEnv.PORT,
            'secured': EngineDBEnv.SECURED,
            'hostValidation': EngineDBEnv.SECURED_HOST_VALIDATION,
            'user': EngineDBEnv.USER,
            'password': EngineDBEnv.PASSWORD,
            'database': EngineDBEnv.DATABASE,
            'connection': EngineDBEnv.CONNECTION,
            'pgpassfile': EngineDBEnv.PGPASS_FILE,
            'newDatabase': EngineDBEnv.NEW_DATABASE,
        }

    @classproperty
    def DEFAULT_ENGINE_DB_ENV_KEYS(self):
        return {
            'host': Defaults.DEFAULT_DB_HOST,
            'port': Defaults.DEFAULT_DB_PORT,
            'secured': Defaults.DEFAULT_DB_SECURED,
            'hostValidation': Defaults.DEFAULT_DB_SECURED_HOST_VALIDATION,
            'user': Defaults.DEFAULT_DB_USER,
            'password': Defaults.DEFAULT_DB_PASSWORD,
            'database': Defaults.DEFAULT_DB_DATABASE,
        }


@util.export
@util.codegen
@osetupattrsclass
class EngineDBEnv(object):

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Engine database host'),
    )
    def HOST(self):
        return 'OVESETUP_DB/host'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Engine database port'),
    )
    def PORT(self):
        return 'OVESETUP_DB/port'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Engine database secured connection'),
    )
    def SECURED(self):
        return 'OVESETUP_DB/secured'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Engine database host name validation'),
    )
    def SECURED_HOST_VALIDATION(self):
        return 'OVESETUP_DB/securedHostValidation'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Engine database name'),
    )
    def DATABASE(self):
        return 'OVESETUP_DB/database'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Engine database user name'),
    )
    def USER(self):
        return 'OVESETUP_DB/user'

    @osetupattrs(
        answerfile=True,
        answerfile_condition=lambda env: not env.get(
            oengcommcons.ProvisioningEnv.POSTGRES_PROVISIONING_ENABLED
        ),
    )
    def PASSWORD(self):
        return 'OVESETUP_DB/password'

    CONNECTION = 'OVESETUP_DB/connection'
    STATEMENT = 'OVESETUP_DB/statement'
    PGPASS_FILE = 'OVESETUP_DB/pgPassFile'
    NEW_DATABASE = 'OVESETUP_DB/newDatabase'

    @osetupattrs(
        answerfile=True,
    )
    def FIX_DB_VIOLATIONS(self):
        return 'OVESETUP_DB/fixDbViolations'


@util.export
@util.codegen
@osetupattrsclass
class CoreEnv(object):

    @osetupattrs(
        answerfile=True,
    )
    def ENGINE_SERVICE_STOP(self):
        return 'OVESETUP_CORE/engineStop'

    @osetupattrs(
        answerfile=True,
        postinstallfile=True,
        summary=True,
        description=_('Engine installation'),
    )
    def ENABLE(self):
        return 'OVESETUP_ENGINE_CORE/enable'


@util.export
@util.codegen
@osetupattrsclass
class SystemEnv(object):

    @osetupattrs(
        answerfile=True,
    )
    def MEMCHECK_ENABLED(self):
        return 'OVESETUP_SYSTEM/memCheckEnabled'

    MEMCHECK_MINIMUM_MB = 'OVESETUP_SYSTEM/memCheckMinimumMB'
    MEMCHECK_RECOMMENDED_MB = 'OVESETUP_SYSTEM/memCheckRecommendedMB'
    MEMCHECK_THRESHOLD = 'OVESETUP_SYSTEM/memCheckThreshold'

    NFS_SERVICE_NAME = 'OVESETUP_SYSTEM/nfsServiceName'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('NFS setup'),
        summary_condition=lambda env: env.get(
            SystemEnv.NFS_CONFIG_ENABLED
        ),
    )
    def NFS_CONFIG_ENABLED(self):
        return 'OVESETUP_SYSTEM/nfsConfigEnabled'

    #
    # In 3.3/3.4.0 the NFS_CONFIG_ENABLED was in postinstall file
    # and now removed.
    # At first upgrade from these versions we should not consider
    # its value from environment.
    # This variable will not be available at these versions, and
    # will set to False in future runs to allow us to
    # consider the value of NFS_CONFIG_ENABLED in later setups.
    #
    @osetupattrs(
        postinstallfile=True,
    )
    def NFS_CONFIG_ENABLED_LEGACY_IN_POSTINSTALL(self):
        return 'OVESETUP_SYSTEM/nfsConfigEnabled_legacyInPostInstall'


@util.export
@util.codegen
@osetupattrsclass
class PKIEnv(object):
    STORE_PASS = 'OVESETUP_PKI/storePassword'

    COUNTRY = 'OVESETUP_PKI/country'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('PKI organization'),
    )
    def ORG(self):
        return 'OVESETUP_PKI/organization'

    ENGINE_SSH_PUBLIC_KEY = 'OVESETUP_PKI/sshPublicKey'


@util.export
@util.codegen
@osetupattrsclass
class ConfigEnv(object):

    @osetupattrs(
        postinstallfile=True,
    )
    def ISO_DOMAIN_EXISTS(self):
        return 'OVESETUP_CONFIG/isoDomainExists'

    @osetupattrs(
        postinstallfile=True,
    )
    def ISO_DOMAIN_SD_UUID(self):
        return 'OVESETUP_CONFIG/isoDomainSdUuid'

    @osetupattrs(
        postinstallfile=True,
    )
    def ISO_DOMAIN_STORAGE_DIR(self):
        return 'OVESETUP_CONFIG/isoDomainStorageDir'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('NFS mount point'),
        postinstallfile=True,
    )
    def ISO_DOMAIN_NFS_MOUNT_POINT(self):
        return 'OVESETUP_CONFIG/isoDomainMountPoint'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('NFS export ACL'),
    )
    def ISO_DOMAIN_NFS_ACL(self):
        return 'OVESETUP_CONFIG/isoDomainACL'

    @osetupattrs(
        answerfile=True,
        postinstallfile=True
    )
    def ISO_DOMAIN_NAME(self):
        return 'OVESETUP_CONFIG/isoDomainName'

    ISO_DOMAIN_DEFAULT_NFS_MOUNT_POINT = \
        'OVESETUP_CONFIG/isoDomainDefaultMountPoint'

    MAC_RANGE_POOL = 'OVESETUP_CONFIG/macRangePool'

    ENGINE_FQDN = 'OVESETUP_ENGINE_CONFIG/fqdn'


@util.export
@util.codegen
@osetupattrsclass
class RPMDistroEnv(object):
    ENGINE_PACKAGES = 'OVESETUP_RPMDISTRO/enginePackages'
    ENGINE_SETUP_PACKAGES = 'OVESETUP_RPMDISTRO/engineSetupPackages'
    UPGRADE_YUM_GROUP = 'OVESETUP_RPMDISTRO/upgradeYumGroup'


@util.export
@util.codegen
@osetupattrsclass
class AIOEnv(object):
    ENABLE = 'OVESETUP_AIO/enable'
    CONTINUE_WITHOUT_AIO = 'OVESETUP_AIO/continueWithoutAIO'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Configure VDSM on this host'),
    )
    def CONFIGURE(self):
        return 'OVESETUP_AIO/configure'

    LOCAL_DATA_CENTER = 'OVESETUP_AIO/localDataCenter'
    LOCAL_CLUSTER = 'OVESETUP_AIO/localCluster'
    LOCAL_HOST = 'OVESETUP_AIO/localHost'
    VDSM_CPU = 'OVESETUP_AIO/vdsmCpu'
    SUPPORTED = 'OVESETUP_AIO/supported'

    STORAGE_DOMAIN_SD_UUID = 'OVESETUP_AIO/storageDomainSdUuid'
    STORAGE_DOMAIN_DEFAULT_DIR = 'OVESETUP_AIO/storageDomainDefaultDir'

    @osetupattrs(
        answerfile=True,
        summary=True,
        description=_('Local storage domain directory'),
    )
    def STORAGE_DOMAIN_DIR(self):
        return 'OVESETUP_AIO/storageDomainDir'

    @osetupattrs(
        answerfile=True,
        summary=False,
        description=_('Local storage domain name'),
    )
    def STORAGE_DOMAIN_NAME(self):
        return 'OVESETUP_AIO/storageDomainName'

    SSHD_PORT = 'OVESETUP_AIO/sshdPort'
    DEFAULT_SSH_PORT = 22


@util.export
class AIODefaults(object):
    DEFAULT_LOCAL_DATA_CENTER = 'local_datacenter'
    DEFAULT_LOCAL_CLUSTER = 'local_cluster'
    DEFAULT_LOCAL_HOST = 'local_host'
    DEFAULT_STORAGE_DOMAIN_NAME = 'local_storage'


@util.export
@util.codegen
class AIOConst(object):
    MINIMUM_SPACE_STORAGEDOMAIN_MB = 10240


@util.export
@util.codegen
@osetupattrsclass
class ApacheEnv(object):

    HTTPD_CONF_OVIRT_ENGINE = 'OVESETUP_APACHE/configFileOvirtEngine'


@util.export
@util.codegen
class AsyncTasksEnv(object):

    @osetupattrs(
        answerfile=True,
    )
    def CLEAR_TASKS(self):
        return 'OVESETUP_ASYNC/clearTasks'

    @osetupattrs(
        answerfile=True,
    )
    def CLEAR_TASKS_WAIT_PERIOD(self):
        return 'OVESETUP_ASYNC/clearTasksWait'


@util.export
@util.codegen
@osetupattrsclass
class RemoveEnv(object):
    @osetupattrs(
        answerfile=True,
    )
    def REMOVE_ENGINE(self):
        return 'OVESETUP_REMOVE/removeEngine'

    @osetupattrs(
        answerfile=True,
    )
    def REMOVE_ENGINE_DATABASE(self):
        return 'OVESETUP_REMOVE/engineDatabase'


# vim: expandtab tabstop=4 shiftwidth=4
