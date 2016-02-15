# Authors:
#   Rob Crittenden <rcritten@redhat.com>
#
# Copyright (C) 2009  Red Hat
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
Joining an IPA domain
"""

import krbV

from ipalib import api, util
from ipalib import Command, Str
from ipalib import errors
from ipalib import _
from ipaserver.install import installutils

def get_realm():
    """
    Returns the default kerberos realm configured for this server.
    """
    krbctx = krbV.default_context()

    return unicode(krbctx.default_realm)

def validate_host(ugettext, cn):
    """
    Require at least one dot in the hostname (to support localhost.localdomain)
    """
    dots = len(cn.split('.'))
    if dots < 2:
        return 'Fully-qualified hostname required'
    return None

class join(Command):
    """Join an IPA domain"""

    takes_args = (
        Str('cn',
            validate_host,
            cli_name='hostname',
            doc=_("The hostname to register as"),
            default_from=lambda: unicode(installutils.get_fqdn()),
            autofill=True,
            #normalizer=lamda value: value.lower(),
        ),
    )
    takes_options= (
        Str('realm',
            doc=_("The IPA realm"),
            default_from=lambda: get_realm(),
            autofill=True,
        ),
        Str('nshardwareplatform?',
            cli_name='platform',
            doc=_('Hardware platform of the host (e.g. Lenovo T61)'),
        ),
        Str('nsosversion?',
            cli_name='os',
            doc=_('Operating System and version of the host (e.g. Fedora 9)'),
        ),
    )

    has_output = tuple()
    use_output_validation = False

    def execute(self, hostname, **kw):
        """
        Execute the machine join operation.

        Returns the entry as it will be created in LDAP.

        :param hostname: The name of the host joined
        :param kw: Keyword arguments for the other attributes.
        """
        assert 'cn' not in kw
        ldap = self.api.Backend.ldap2

        host = None
        try:
            # First see if the host exists
            kw = {'fqdn': hostname, 'all': True}
            attrs_list = api.Command['host_show'](**kw)['result']
            dn = attrs_list['dn']

            # If no principal name is set yet we need to try to add
            # one.
            if 'krbprincipalname' not in attrs_list:
                service = "host/%s@%s" % (hostname, api.env.realm)
                api.Command['host_mod'](hostname, krbprincipalname=service)

            # It exists, can we write the password attributes?
            allowed = ldap.can_write(dn, 'krblastpwdchange')
            if not allowed:
                raise errors.ACIError(info=_("Insufficient 'write' privilege to the 'krbLastPwdChange' attribute of entry '%s'.") % dn)

            kw = {'fqdn': hostname, 'all': True}
            attrs_list = api.Command['host_show'](**kw)['result']
            dn = attrs_list['dn']
        except errors.NotFound:
            attrs_list = api.Command['host_add'](hostname, force=True)['result']
            dn = attrs_list['dn']

        config = api.Command['config_show']()['result']
        attrs_list['ipacertificatesubjectbase'] = config['ipacertificatesubjectbase']

        return (dn, attrs_list)

api.register(join)
