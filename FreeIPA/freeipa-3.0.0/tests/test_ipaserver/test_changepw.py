# Authors:
#   Martin Kosek <mkosek@redhat.com>
#
# Copyright (C) 2012  Red Hat
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

import nose

from httptest import Unauthorized_HTTP_test
from tests.test_xmlrpc.xmlrpc_test import XMLRPC_test
from tests.util import assert_equal, assert_not_equal
from ipalib import api, errors
from ipapython.dn import DN
import ldap

testuser = u'tuser'
old_password = u'old_password'
new_password = u'new_password'

class test_changepw(XMLRPC_test, Unauthorized_HTTP_test):
    app_uri = '/ipa/session/change_password'

    def setUp(self):
        super(test_changepw, self).setUp()
        try:
            api.Command['user_add'](uid=testuser, givenname=u'Test', sn=u'User')
            api.Command['passwd'](testuser, password=u'old_password')
        except errors.ExecutionError, e:
            raise nose.SkipTest(
                'Cannot set up test user: %s' % e
            )

    def tearDown(self):
        try:
            api.Command['user_del']([testuser])
        except errors.NotFound:
            pass
        super(test_changepw, self).tearDown()

    def _changepw(self, user, old_password, new_password):
        return self.send_request(params={'user': str(user),
                                  'old_password' : str(old_password),
                                  'new_password' : str(new_password)},
                                 )

    def _checkpw(self, user, password):
        dn = str(DN(('uid', user), api.env.container_user, api.env.basedn))
        conn = ldap.initialize(api.env.ldap_uri)
        try:
            conn.simple_bind_s(dn, password)
        finally:
            conn.unbind_s()

    def test_bad_options(self):
        for params in (None,                    # no params
                      {'user': 'foo'},          # missing options
                      {'user': 'foo',
                       'old_password' : 'old'}, # missing option
                      {'user': 'foo',
                       'old_password' : 'old',
                       'new_password' : ''},    # empty option
                      ):
            response = self.send_request(params=params)
            assert_equal(response.status, 400)
            assert_equal(response.reason, 'Bad Request')

    def test_invalid_auth(self):
        response = self._changepw(testuser, 'wrongpassword', 'new_password')

        assert_equal(response.status, 200)
        assert_equal(response.getheader('X-IPA-Pwchange-Result'), 'invalid-password')

        # make sure that password is NOT changed
        self._checkpw(testuser, old_password)

    def test_pwpolicy_error(self):
        response = self._changepw(testuser, old_password, '1')

        assert_equal(response.status, 200)
        assert_equal(response.getheader('X-IPA-Pwchange-Result'), 'policy-error')
        assert_equal(response.getheader('X-IPA-Pwchange-Policy-Error'),
                     'Constraint violation: Password is too short')

        # make sure that password is NOT changed
        self._checkpw(testuser, old_password)

    def test_pwpolicy_success(self):
        response = self._changepw(testuser, old_password, new_password)

        assert_equal(response.status, 200)
        assert_equal(response.getheader('X-IPA-Pwchange-Result'), 'ok')

        # make sure that password IS changed
        self._checkpw(testuser, new_password)
