# Authors:
#   Martin Nagy <mnagy@redhat.com>
#   Jason Gerard DeRose <jderose@redhat.com>
#
# Copyright (C) 2008  Red Hat
# see file 'COPYING' for use and warranty information
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""
All constants centralised in one file.
"""
import socket
from ipapython.dn import DN
from ipapython.version import VERSION
try:
    FQDN = socket.getfqdn()
except:
    try:
        FQDN = socket.gethostname()
    except:
        FQDN = None

# The parameter system treats all these values as None:
NULLS = (None, '', u'', tuple(), [])

# regular expression NameSpace member names must match:
NAME_REGEX = r'^[a-z][_a-z0-9]*[a-z0-9]$|^[a-z]$'

# Format for ValueError raised when name does not match above regex:
NAME_ERROR = 'name must match %r; got %r'

# Standard format for TypeError message:
TYPE_ERROR = '%s: need a %r; got %r (a %r)'

# Stardard format for TypeError message when a callable is expected:
CALLABLE_ERROR = '%s: need a callable; got %r (which is a %r)'

# Standard format for StandardError message when overriding an attribute:
OVERRIDE_ERROR = 'cannot override %s.%s value %r with %r'

# Standard format for AttributeError message when a read-only attribute is
# already locked:
SET_ERROR = 'locked: cannot set %s.%s to %r'
DEL_ERROR = 'locked: cannot delete %s.%s'

# Used for a tab (or indentation level) when formatting for CLI:
CLI_TAB = '  '  # Two spaces

# The section to read in the config files, i.e. [global]
CONFIG_SECTION = 'global'

# The default configuration for api.env
# This is a tuple instead of a dict so that it is immutable.
# To create a dict with this config, just "d = dict(DEFAULT_CONFIG)".
DEFAULT_CONFIG = (
    ('version', VERSION),

    # Domain, realm, basedn:
    ('domain', 'example.com'),
    ('realm', 'EXAMPLE.COM'),
    ('basedn', DN(('dc', 'example'), ('dc', 'com'))),

    # LDAP containers:
    ('container_accounts', DN(('cn', 'accounts'))),
    ('container_user', DN(('cn', 'users'), ('cn', 'accounts'))),
    ('container_group', DN(('cn', 'groups'), ('cn', 'accounts'))),
    ('container_service', DN(('cn', 'services'), ('cn', 'accounts'))),
    ('container_host', DN(('cn', 'computers'), ('cn', 'accounts'))),
    ('container_hostgroup', DN(('cn', 'hostgroups'), ('cn', 'accounts'))),
    ('container_rolegroup', DN(('cn', 'roles'), ('cn', 'accounts'))),
    ('container_permission', DN(('cn', 'permissions'), ('cn', 'pbac'))),
    ('container_privilege', DN(('cn', 'privileges'), ('cn', 'pbac'))),
    ('container_automount', DN(('cn', 'automount'))),
    ('container_policies', DN(('cn', 'policies'))),
    ('container_configs', DN(('cn', 'configs'), ('cn', 'policies'))),
    ('container_roles', DN(('cn', 'roles'), ('cn', 'policies'))),
    ('container_applications', DN(('cn', 'applications'), ('cn', 'configs'), ('cn', 'policies'))),
    ('container_policygroups', DN(('cn', 'policygroups'), ('cn', 'configs'), ('cn', 'policies'))),
    ('container_policylinks', DN(('cn', 'policylinks'), ('cn', 'configs'), ('cn', 'policies'))),
    ('container_netgroup', DN(('cn', 'ng'), ('cn', 'alt'))),
    ('container_hbac', DN(('cn', 'hbac'))),
    ('container_hbacservice', DN(('cn', 'hbacservices'), ('cn', 'hbac'))),
    ('container_hbacservicegroup', DN(('cn', 'hbacservicegroups'), ('cn', 'hbac'))),
    ('container_dns', DN(('cn', 'dns'))),
    ('container_virtual', DN(('cn', 'virtual operations'), ('cn', 'etc'))),
    ('container_sudorule', DN(('cn', 'sudorules'), ('cn', 'sudo'))),
    ('container_sudocmd', DN(('cn', 'sudocmds'), ('cn', 'sudo'))),
    ('container_sudocmdgroup', DN(('cn', 'sudocmdgroups'), ('cn', 'sudo'))),
    ('container_automember', DN(('cn', 'automember'), ('cn', 'etc'))),
    ('container_selinux', DN(('cn', 'usermap'), ('cn', 'selinux'))),
    ('container_s4u2proxy', DN(('cn', 's4u2proxy'), ('cn', 'etc'))),
    ('container_cifsdomains', DN(('cn', 'ad'), ('cn', 'etc'))),
    ('container_trusts', DN(('cn', 'trusts'))),
    ('container_adtrusts', DN(('cn', 'ad'), ('cn', 'trusts'))),
    ('container_ranges', DN(('cn', 'ranges'), ('cn', 'etc'))),
    ('container_dna', DN(('cn', 'dna'), ('cn', 'ipa'), ('cn', 'etc'))),
    ('container_dna_posix_ids', DN(('cn', 'posix-ids'), ('cn', 'dna'), ('cn', 'ipa'), ('cn', 'etc'))),

    # Ports, hosts, and URIs:
    # FIXME: let's renamed xmlrpc_uri to rpc_xml_uri
    ('xmlrpc_uri', 'http://localhost:8888/ipa/xml'),
    ('rpc_json_uri', 'http://localhost:8888/ipa/json'),
    ('ldap_uri', 'ldap://localhost:389'),

    # Define an inclusive range of SSL/TLS version support
    ('tls_version_min', 'tls1.0'),
    ('tls_version_max', 'tls1.2'),

    # Time to wait for a service to start, in seconds
    ('startup_timeout', 300),

    # Web Application mount points
    ('mount_ipa', '/ipa/'),

    # WebUI stuff:
    ('webui_prod', True),

    # Session stuff:

    # Maximum time before a session expires forcing credentials to be reacquired.
    ('session_auth_duration', '20 minutes'),
    # How a session expiration is computed, see SessionManager.set_session_expiration_time()
    ('session_duration_type', 'inactivity_timeout'),

    # Debugging:
    ('verbose', 0),
    ('debug', False),
    ('startup_traceback', False),
    ('mode', 'production'),

    # CA plugin:
    ('ca_host', FQDN),  # Set in Env._finalize_core()
    ('ca_port', 80),
    ('ca_agent_port', 443),
    ('ca_ee_port', 443),
    # For the following ports, None means a default specific to the installed
    # Dogtag version.
    ('ca_install_port', None),
    ('ca_agent_install_port', None),
    ('ca_ee_install_port', None),


    # Special CLI:
    ('prompt_all', False),
    ('interactive', True),
    ('fallback', True),
    ('delegate', False),

    # Enable certain optional plugins:
    ('enable_ra', False),
    ('ra_plugin', 'selfsign'),
    ('wait_for_attr', False),
    ('dogtag_version', 9),

    # Used when verifying that the API hasn't changed. Not for production.
    ('validate_api', False),

    # ********************************************************
    #  The remaining keys are never set from the values here!
    # ********************************************************
    #
    # Env._bootstrap() or Env._finalize_core() will have filled in all the keys
    # below by the time DEFAULT_CONFIG is merged in, so the values below are
    # never actually used.  They are listed both to provide a big picture and
    # also so DEFAULT_CONFIG contains at least all the keys that should be
    # present after Env._finalize_core() is called.
    #
    # Each environment variable below is sent to ``object``, which just happens
    # to be an invalid value for an environment variable, so if for some reason
    # any of these keys were set from the values here, an exception will be
    # raised.

    # Non-overridable vars set in Env._bootstrap():
    ('host', FQDN),
    ('ipalib', object),  # The directory containing ipalib/__init__.py
    ('site_packages', object),  # The directory contaning ipalib
    ('script', object),  # sys.argv[0]
    ('bin', object),  # The directory containing the script
    ('home', object),  # $HOME

    # Vars set in Env._bootstrap():
    ('in_tree', object),  # Whether or not running in-tree (bool)
    ('dot_ipa', object),  # ~/.ipa directory
    ('context', object),  # Name of context, default is 'default'
    ('confdir', object),  # Directory containing config files
    ('conf', object),  # File containing context specific config
    ('conf_default', object),  # File containing context independent config
    ('plugins_on_demand', object),  # Whether to finalize plugins on-demand (bool)

    # Set in Env._finalize_core():
    ('in_server', object),  # Whether or not running in-server (bool)
    ('logdir', object),  # Directory containing log files
    ('log', object),  # Path to context specific log file

)
