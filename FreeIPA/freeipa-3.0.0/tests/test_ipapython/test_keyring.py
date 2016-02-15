# Authors:
#   Rob Crittenden <rcritten@redhat.com>
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
"""
Test the `kernel_keyring.py` module.
"""

from nose.tools import raises, assert_raises  # pylint: disable=E0611
from ipapython import kernel_keyring

TEST_KEY = 'ipa_test'
TEST_VALUE = 'abc123'
UPDATE_VALUE = '123abc'

SIZE_256 = 'abcdefgh' * 32
SIZE_512 = 'abcdefgh' * 64
SIZE_1024 = 'abcdefgh' * 128

class test_keyring(object):
    """
    Test the kernel keyring interface
    """

    def setUp(self):
        try:
            kernel_keyring.del_key(TEST_KEY)
        except ValueError:
            pass
        try:
            kernel_keyring.del_key(SIZE_256)
        except ValueError:
            pass

    def test_01(self):
        """
        Add a new key and value, then remove it
        """
        kernel_keyring.add_key(TEST_KEY, TEST_VALUE)
        result = kernel_keyring.read_key(TEST_KEY)
        assert(result == TEST_VALUE)

        kernel_keyring.del_key(TEST_KEY)

        # Make sure it is gone
        try:
            result = kernel_keyring.read_key(TEST_KEY)
        except ValueError, e:
            assert e.message == 'key %s not found' % TEST_KEY

    def test_02(self):
        """
        Delete a non_existent key
        """
        try:
            kernel_keyring.del_key(TEST_KEY)
            raise AssertionError('key should not have been deleted')
        except ValueError:
            pass

    @raises(ValueError)
    def test_03(self):
        """
        Add a duplicate key
        """
        kernel_keyring.add_key(TEST_KEY, TEST_VALUE)
        kernel_keyring.add_key(TEST_KEY, TEST_VALUE)

    def test_04(self):
        """
        Update the value in a key
        """
        kernel_keyring.update_key(TEST_KEY, UPDATE_VALUE)
        result = kernel_keyring.read_key(TEST_KEY)
        assert(result == UPDATE_VALUE)

        # Now update it 10 times
        for i in xrange(10):
            kernel_keyring.update_key(TEST_KEY, 'test %d' %  i)
            result = kernel_keyring.read_key(TEST_KEY)
            assert(result == 'test %d' % i)

        kernel_keyring.del_key(TEST_KEY)

    @raises(ValueError)
    def test_05(self):
        """
        Read a non-existent key
        """
        result = kernel_keyring.read_key(TEST_KEY)

    def test_06(self):
        """
        See if a key is available
        """
        kernel_keyring.add_key(TEST_KEY, TEST_VALUE)

        result = kernel_keyring.has_key(TEST_KEY)
        assert(result == True)
        kernel_keyring.del_key(TEST_KEY)

        result = kernel_keyring.has_key(TEST_KEY)
        assert(result == False)

    def test_07(self):
        """
        Test a 256-byte key
        """
        kernel_keyring.add_key(SIZE_256, TEST_VALUE)
        result = kernel_keyring.read_key(SIZE_256)
        assert(result == TEST_VALUE)

        kernel_keyring.del_key(SIZE_256)

    def test_08(self):
        """
        Test 512-bytes of data
        """
        kernel_keyring.add_key(TEST_KEY, SIZE_512)
        result = kernel_keyring.read_key(TEST_KEY)
        assert(result == SIZE_512)

        kernel_keyring.del_key(TEST_KEY)

    def test_09(self):
        """
        Test 1k bytes of data
        """
        kernel_keyring.add_key(TEST_KEY, SIZE_1024)
        result = kernel_keyring.read_key(TEST_KEY)
        assert(result == SIZE_1024)

        kernel_keyring.del_key(TEST_KEY)
