# Authors:
#   Pavel Zuna <pzuna@redhat.com>
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
Base classes for LDAP plugins.
"""

import re
import json
import time
from copy import deepcopy
import base64

from ipalib import api, crud, errors
from ipalib import Method, Object, Command
from ipalib import Flag, Int, Str
from ipalib.base import NameSpace
from ipalib.cli import to_cli, from_cli
from ipalib import output
from ipalib.text import _
from ipalib.util import json_serialize, validate_hostname
from ipapython.dn import DN, RDN

global_output_params = (
    Flag('has_password',
        label=_('Password'),
    ),
    Str('member',
        label=_('Failed members'),
    ),
    Str('member_user?',
        label=_('Member users'),
    ),
    Str('member_group?',
        label=_('Member groups'),
    ),
    Str('memberof_group?',
        label=_('Member of groups'),
    ),
    Str('member_host?',
        label=_('Member hosts'),
    ),
    Str('member_hostgroup?',
        label=_('Member host-groups'),
    ),
    Str('memberof_hostgroup?',
        label=_('Member of host-groups'),
    ),
    Str('memberof_permission?',
        label=_('Permissions'),
    ),
    Str('memberof_privilege?',
        label='Privileges',
    ),
    Str('memberof_role?',
        label=_('Roles'),
    ),
    Str('memberof_sudocmdgroup?',
        label=_('Sudo Command Groups'),
    ),
    Str('member_privilege?',
        label='Granted to Privilege',
    ),
    Str('member_role?',
        label=_('Granting privilege to roles'),
    ),
    Str('member_netgroup?',
        label=_('Member netgroups'),
    ),
    Str('memberof_netgroup?',
        label=_('Member of netgroups'),
    ),
    Str('member_service?',
        label=_('Member services'),
    ),
    Str('member_servicegroup?',
        label=_('Member service groups'),
    ),
    Str('memberof_servicegroup?',
        label='Member of service groups',
    ),
    Str('member_hbacsvc?',
        label=_('Member HBAC service'),
    ),
    Str('member_hbacsvcgroup?',
        label=_('Member HBAC service groups'),
    ),
    Str('memberof_hbacsvcgroup?',
        label='Member of HBAC service groups',
    ),
    Str('member_sudocmd?',
        label='Member Sudo commands',
    ),
    Str('memberof_sudorule?',
        label='Member of Sudo rule',
    ),
    Str('memberof_hbacrule?',
        label='Member of HBAC rule',
    ),
    Str('memberindirect_user?',
        label=_('Indirect Member users'),
    ),
    Str('memberindirect_group?',
        label=_('Indirect Member groups'),
    ),
    Str('memberindirect_host?',
        label=_('Indirect Member hosts'),
    ),
    Str('memberindirect_hostgroup?',
        label=_('Indirect Member host-groups'),
    ),
    Str('memberindirect_role?',
        label=_('Indirect Member of roles'),
    ),
    Str('memberindirect_permission?',
        label=_('Indirect Member permissions'),
    ),
    Str('memberindirect_hbacsvc?',
        label=_('Indirect Member HBAC service'),
    ),
    Str('memberindirect_hbacsvcgrp?',
        label=_('Indirect Member HBAC service group'),
    ),
    Str('memberindirect_netgroup?',
        label=_('Indirect Member netgroups'),
    ),
    Str('memberofindirect_group?',
        label='Indirect Member of group',
    ),
    Str('memberofindirect_netgroup?',
        label='Indirect Member of netgroup',
    ),
    Str('memberofindirect_hostgroup?',
        label='Indirect Member of host-group',
    ),
    Str('memberofindirect_role?',
        label='Indirect Member of role',
    ),
    Str('memberofindirect_sudorule?',
        label='Indirect Member of Sudo rule',
    ),
    Str('memberofindirect_hbacrule?',
        label='Indirect Member of HBAC rule',
    ),
    Str('sourcehost',
        label=_('Failed source hosts/hostgroups'),
    ),
    Str('memberhost',
        label=_('Failed hosts/hostgroups'),
    ),
    Str('memberuser',
        label=_('Failed users/groups'),
    ),
    Str('memberservice',
        label=_('Failed service/service groups'),
    ),
    Str('failed',
        label=_('Failed to remove'),
        flags=['suppress_empty'],
    ),
    Str('ipasudorunas',
        label=_('Failed RunAs'),
    ),
    Str('ipasudorunasgroup',
        label=_('Failed RunAsGroup'),
    ),
)


def validate_add_attribute(ugettext, attr):
    validate_attribute(ugettext, 'addattr', attr)

def validate_set_attribute(ugettext, attr):
    validate_attribute(ugettext, 'setattr', attr)

def validate_del_attribute(ugettext, attr):
    validate_attribute(ugettext, 'delattr', attr)

def validate_attribute(ugettext, name, attr):
    m = re.match("\s*(.*?)\s*=\s*(.*?)\s*$", attr)
    if not m or len(m.groups()) != 2:
        raise errors.ValidationError(
            name=name, error=_('Invalid format. Should be name=value'))

def get_effective_rights(ldap, dn, attrs=None):
    assert isinstance(dn, DN)
    if attrs is None:
        attrs = ['*', 'nsaccountlock', 'cospriority']
    rights = ldap.get_effective_rights(dn, attrs)
    rdict = {}
    if 'attributelevelrights' in rights[1]:
        rights = rights[1]['attributelevelrights']
        rights = rights[0].split(', ')
        for r in rights:
            (k,v) = r.split(':')
            rdict[k.strip().lower()] = v

    return rdict

def entry_from_entry(entry, newentry):
    """
    Python is more or less pass-by-value except for immutable objects. So if
    you pass in a dict to a function you are free to change members of that
    dict but you can't create a new dict in the function and expect to replace
    what was passed in.

    In some post-op plugins that is exactly what we want to do, so here is a
    clumsy way around the problem.
    """

    # Wipe out the current data
    for e in entry.keys():
        del entry[e]

    # Re-populate it with new wentry
    for e in newentry:
        entry[e] = newentry[e]

def wait_for_memberof(keys, entry_start, completed, show_command, adding=True):
    """
    When adding or removing reverse members we are faking an update to
    object A by updating the member attribute in object B. The memberof
    plugin makes this work by adding or removing the memberof attribute
    to/from object A, it just takes a little bit of time.

    This will loop for 6+ seconds, retrieving object A so we can see
    if all the memberof attributes have been updated.
    """
    if completed == 0:
        # nothing to do
        return api.Command[show_command](keys[-1])['result']

    if 'memberof' in entry_start:
        starting_memberof = len(entry_start['memberof'])
    else:
        starting_memberof = 0

    # Loop a few times to give the memberof plugin a chance to add the
    # entries. Don't sleep for more than 6 seconds.
    memberof = 0
    x = 0
    while x < 20:
        # sleep first because the first search, even on a quiet system,
        # almost always fails to have memberof set.
        time.sleep(.3)
        x = x + 1

        # FIXME: put a try/except around here? I think it is probably better
        # to just let the exception filter up to the caller.
        entry_attrs = api.Command[show_command](keys[-1])['result']
        if 'memberof' in entry_attrs:
            memberof = len(entry_attrs['memberof'])

        if adding:
            if starting_memberof + completed >= memberof:
                break
        else:
            if starting_memberof + completed <= memberof:
                break

    return entry_attrs

def wait_for_value(ldap, dn, attr, value):
    """
    389-ds postoperation plugins are executed after the data has been
    returned to a client. This means that plugins that add data in a
    postop are not included in data returned to the user.

    The downside of waiting is that this increases the time of the
    command.

    The updated entry is returned.
    """
    # Loop a few times to give the postop-plugin a chance to complete
    # Don't sleep for more than 6 seconds.
    x = 0
    while x < 20:
        # sleep first because the first search, even on a quiet system,
        # almost always fails.
        time.sleep(.3)
        x = x + 1

        # FIXME: put a try/except around here? I think it is probably better
        # to just let the exception filter up to the caller.
        (dn, entry_attrs) = ldap.get_entry( dn, ['*'])
        if attr in entry_attrs:
            if isinstance(entry_attrs[attr], (list, tuple)):
                values = map(lambda y:y.lower(), entry_attrs[attr])
                if value.lower() in values:
                    break
            else:
                if value.lower() == entry_attrs[attr].lower():
                    break

    return entry_attrs


def validate_externalhost(ugettext, hostname):
    try:
        validate_hostname(hostname, check_fqdn=False, allow_underscore=True)
    except ValueError, e:
        return unicode(e)


external_host_param = Str('externalhost*', validate_externalhost,
        label=_('External host'),
        flags=['no_option'],
)


def add_external_pre_callback(membertype, ldap, dn, keys, options):
    """
    Pre callback to validate external members.

    This should be called by a command pre callback directly.

    membertype is the type of member
    """
    assert isinstance(dn, DN)
    # validate hostname with allowed underscore characters, non-fqdn
    # hostnames are allowed
    def validate_host(hostname):
        validate_hostname(hostname, check_fqdn=False, allow_underscore=True)

    if options.get(membertype):
        if membertype == 'host':
            validator = validate_host
        else:
            validator = api.Object[membertype].primary_key
        for value in options[membertype]:
            try:
                validator(value)
            except errors.ValidationError as e:
                raise errors.ValidationError(name=membertype, error=e.error)
            except ValueError as e:
                raise errors.ValidationError(name=membertype, error=e)
    return dn

def add_external_post_callback(memberattr, membertype, externalattr, ldap, completed, failed, dn, entry_attrs, *keys, **options):
    """
    Post callback to add failed members as external members.

    This should be called by a commands post callback directly.

    memberattr is one of memberuser,
    membertype is the type of member: user,
    externalattr is one of externaluser,
    """
    assert isinstance(dn, DN)
    completed_external = 0
    normalize = options.get('external_callback_normalize', True)
    # Sift through the failures. We assume that these are all
    # entries that aren't stored in IPA, aka external entries.
    if memberattr in failed and membertype in failed[memberattr]:
        (dn, entry_attrs_) = ldap.get_entry(dn, [externalattr])
        assert isinstance(dn, DN)
        members = entry_attrs.get(memberattr, [])
        external_entries = entry_attrs_.get(externalattr, [])
        lc_external_entries = set(e.lower() for e in external_entries)
        failed_entries = []
        for entry in failed[memberattr][membertype]:
            membername = entry[0].lower()
            member_dn = api.Object[membertype].get_dn(membername)
            assert isinstance(member_dn, DN)
            if (membername not in lc_external_entries and
                member_dn not in members):
                # Not an IPA entry, assume external
                if normalize:
                    external_entries.append(membername)
                else:
                    external_entries.append(entry[0])
                lc_external_entries.add(membername)
                completed_external += 1
            elif (membername in lc_external_entries and
               member_dn not in members):
                # Already an external member, reset the error message
                msg = unicode(errors.AlreadyGroupMember().message)
                newerror = (entry[0], msg)
                ind = failed[memberattr][membertype].index(entry)
                failed[memberattr][membertype][ind] = newerror
                failed_entries.append(membername)
            else:
                # Really a failure
                failed_entries.append(membername)

        if completed_external:
            try:
                ldap.update_entry(dn, {externalattr: external_entries})
            except errors.EmptyModlist:
                pass
            failed[memberattr][membertype] = failed_entries
            entry_attrs[externalattr] = external_entries

    return (completed + completed_external, dn)

def remove_external_post_callback(memberattr, membertype, externalattr, ldap, completed, failed, dn, entry_attrs, *keys, **options):
    assert isinstance(dn, DN)
    # Run through the failures and gracefully remove any member defined
    # as an external member.
    if memberattr in failed and membertype in failed[memberattr]:
        (dn, entry_attrs_) = ldap.get_entry(dn, [externalattr])
        external_entries = entry_attrs_.get(externalattr, [])
        failed_entries = []
        completed_external = 0
        for entry in failed[memberattr][membertype]:
            membername = entry[0].lower()
            if membername in external_entries or entry[0] in external_entries:
                try:
                    external_entries.remove(membername)
                except ValueError:
                    external_entries.remove(entry[0])
                completed_external += 1
            else:
                failed_entries.append(membername)

        if completed_external:
            try:
                ldap.update_entry(dn, {externalattr: external_entries})
            except errors.EmptyModlist:
                pass
            failed[memberattr][membertype] = failed_entries
            entry_attrs[externalattr] = external_entries

    return (completed + completed_external, dn)

def host_is_master(ldap, fqdn):
    """
    Check to see if this host is a master.

    Raises an exception if a master, otherwise returns nothing.
    """
    master_dn = DN(('cn', fqdn), ('cn', 'masters'), ('cn', 'ipa'), ('cn', 'etc'), api.env.basedn)
    try:
        (dn, entry_attrs) = ldap.get_entry(master_dn, ['objectclass'])
        raise errors.ValidationError(name='hostname', error=_('An IPA master host cannot be deleted or disabled'))
    except errors.NotFound:
        # Good, not a master
        return


class LDAPObject(Object):
    """
    Object representing a LDAP entry.
    """
    backend_name = 'ldap2'

    parent_object = ''
    container_dn = ''
    normalize_dn = True
    object_name = _('entry')
    object_name_plural = _('entries')
    object_class = []
    object_class_config = None
    # If an objectclass is possible but not default in an entry. Needed for
    # collecting attributes for ACI UI.
    possible_objectclasses = []
    limit_object_classes = [] # Only attributes in these are allowed
    disallow_object_classes = [] # Disallow attributes in these
    search_attributes = []
    search_attributes_config = None
    default_attributes = []
    search_display_attributes = [] # attributes displayed in LDAPSearch
    hidden_attributes = ['objectclass', 'aci']
    # set rdn_attribute only if RDN attribute differs from primary key!
    rdn_attribute = ''
    uuid_attribute = ''
    attribute_members = {}
    rdn_is_primary_key = False # Do we need RDN change to do a rename?
    password_attributes = []
    # Can bind as this entry (has userPassword or krbPrincipalKey)
    bindable = False
    relationships = {
        # attribute: (label, inclusive param prefix, exclusive param prefix)
        'member': ('Member', '', 'no_'),
        'memberof': ('Member Of', 'in_', 'not_in_'),
        'memberindirect': (
            'Indirect Member', None, 'no_indirect_'
        ),
        'memberofindirect': (
            'Indirect Member Of', None, 'not_in_indirect_'
        ),
    }
    label = _('Entry')
    label_singular = _('Entry')

    container_not_found_msg = _('container entry (%(container)s) not found')
    parent_not_found_msg = _('%(parent)s: %(oname)s not found')
    object_not_found_msg = _('%(pkey)s: %(oname)s not found')
    already_exists_msg = _('%(oname)s with name "%(pkey)s" already exists')

    def get_dn(self, *keys, **kwargs):
        if self.parent_object:
            parent_dn = self.api.Object[self.parent_object].get_dn(*keys[:-1])
        else:
            parent_dn = self.container_dn
        if self.rdn_attribute:
            try:
                (dn, entry_attrs) = self.backend.find_entry_by_attr(
                    self.primary_key.name, keys[-1], self.object_class, [''],
                    self.container_dn
                )
            except errors.NotFound:
                pass
            else:
                return dn
        if self.primary_key and keys[-1] is not None:
            return self.backend.make_dn_from_attr(
                self.primary_key.name, keys[-1], parent_dn
            )
        assert isinstance(parent_dn, DN)
        return parent_dn

    def get_dn_if_exists(self, *keys, **kwargs):
        dn = self.get_dn(*keys, **kwargs)
        (dn, entry_attrs) = self.backend.get_entry(dn, [''])
        return dn

    def get_primary_key_from_dn(self, dn):
        assert isinstance(dn, DN)
        try:
            if self.rdn_attribute:
                (dn, entry_attrs) = self.backend.get_entry(
                    dn, [self.primary_key.name]
                )
                try:
                    return entry_attrs[self.primary_key.name][0]
                except (KeyError, IndexError):
                    return ''
        except errors.NotFound:
            pass
        try:
            return dn[self.primary_key.name]
        except KeyError:
            # The primary key is not in the DN.
            # This shouldn't happen, but we don't want a "show" command to
            # crash.
            # Just return the entire DN, it's all we have if the entry
            # doesn't exist
            return unicode(dn)

    def get_ancestor_primary_keys(self):
        if self.parent_object:
            parent_obj = self.api.Object[self.parent_object]
            for key in parent_obj.get_ancestor_primary_keys():
                yield key
            if parent_obj.primary_key:
                pkey = parent_obj.primary_key
                yield pkey.__class__(
                    parent_obj.name + pkey.name, required=True, query=True,
                    cli_name=parent_obj.name, label=pkey.label
                )

    def has_objectclass(self, classes, objectclass):
        oc = map(lambda x:x.lower(),classes)
        return objectclass.lower() in oc

    def convert_attribute_members(self, entry_attrs, *keys, **options):
        if options.get('raw', False):
            return
        for attr in self.attribute_members:
            for member in entry_attrs.setdefault(attr, []):
                for ldap_obj_name in self.attribute_members[attr]:
                    ldap_obj = self.api.Object[ldap_obj_name]
                    if ldap_obj.container_dn in member:
                        new_attr = '%s_%s' % (attr, ldap_obj.name)
                        entry_attrs.setdefault(new_attr, []).append(
                            ldap_obj.get_primary_key_from_dn(member)
                        )
            del entry_attrs[attr]

    def get_password_attributes(self, ldap, dn, entry_attrs):
        """
        Search on the entry to determine if it has a password or
        keytab set.

        A tuple is used to determine which attribute is set
        in entry_attrs. The value is set to True/False whether a
        given password type is set.
        """
        for (pwattr, attr) in self.password_attributes:
            search_filter = '(%s=*)' % pwattr
            try:
                (entries, truncated) = ldap.find_entries(
                    search_filter, [pwattr], dn, ldap.SCOPE_BASE
                )
                entry_attrs[attr] = True
            except errors.NotFound:
                entry_attrs[attr] = False

    def handle_not_found(self, *keys):
        pkey = ''
        if self.primary_key:
            pkey = keys[-1]
        raise errors.NotFound(
            reason=self.object_not_found_msg % {
                'pkey': pkey, 'oname': self.object_name,
            }
        )

    def handle_duplicate_entry(self, *keys):
        pkey = ''
        if self.primary_key:
            pkey = keys[-1]
        raise errors.DuplicateEntry(
            message=self.already_exists_msg % {
                'pkey': pkey, 'oname': self.object_name,
            }
        )

    # list of attributes we want exported to JSON
    json_friendly_attributes = (
        'parent_object', 'container_dn', 'object_name', 'object_name_plural',
        'object_class', 'object_class_config', 'default_attributes', 'label', 'label_singular',
        'hidden_attributes', 'uuid_attribute', 'attribute_members', 'name',
        'takes_params', 'rdn_attribute', 'bindable', 'relationships',
    )

    def __json__(self):
        ldap = self.backend
        json_dict = dict(
            (a, json_serialize(getattr(self, a))) for a in self.json_friendly_attributes
        )
        if self.primary_key:
            json_dict['primary_key'] = self.primary_key.name
        objectclasses = self.object_class
        if self.object_class_config:
            config = ldap.get_ipa_config()[1]
            objectclasses = config.get(
                self.object_class_config, objectclasses
            )
        objectclasses = objectclasses + self.possible_objectclasses
        # Get list of available attributes for this object for use
        # in the ACI UI.
        attrs = self.api.Backend.ldap2.schema.attribute_types(objectclasses)
        attrlist = []
        # Go through the MUST first
        for (oid, attr) in attrs[0].iteritems():
            attrlist.append(attr.names[0].lower())
        # And now the MAY
        for (oid, attr) in attrs[1].iteritems():
            attrlist.append(attr.names[0].lower())
        json_dict['aciattrs'] = attrlist
        attrlist.sort()
        json_dict['methods'] = [m for m in self.methods]
        return json_dict


# addattr can cause parameters to have more than one value even if not defined
# as multivalue, make sure this isn't the case
def _check_single_value_attrs(params, entry_attrs):
    for (a, v) in entry_attrs.iteritems():
        if isinstance(v, (list, tuple)) and len(v) > 1:
            if a in params and not params[a].multivalue:
                raise errors.OnlyOneValueAllowed(attr=a)

# setattr or --option='' can cause parameters to be empty that are otherwise
# required, make sure we enforce that.
def _check_empty_attrs(params, entry_attrs):
    for (a, v) in entry_attrs.iteritems():
        if v is None or (isinstance(v, basestring) and len(v) == 0):
            if a in params and params[a].required:
                raise errors.RequirementError(name=a)


def _check_limit_object_class(attributes, attrs, allow_only):
    """
    If the set of objectclasses is limited enforce that only those
    are updated in entry_attrs (plus dn)

    allow_only tells us what mode to check in:

    If True then we enforce that the attributes must be in the list of
    allowed.

    If False then those attributes are not allowed.
    """
    if len(attributes[0]) == 0 and len(attributes[1]) == 0:
        return
    limitattrs = deepcopy(attrs)
    # Go through the MUST first
    for (oid, attr) in attributes[0].iteritems():
        if attr.names[0].lower() in limitattrs:
            if not allow_only:
                raise errors.ObjectclassViolation(
                    info=_('attribute "%(attribute)s" not allowed') % dict(
                        attribute=attr.names[0].lower()))
            limitattrs.remove(attr.names[0].lower())
    # And now the MAY
    for (oid, attr) in attributes[1].iteritems():
        if attr.names[0].lower() in limitattrs:
            if not allow_only:
                raise errors.ObjectclassViolation(
                    info=_('attribute "%(attribute)s" not allowed') % dict(
                        attribute=attr.names[0].lower()))
            limitattrs.remove(attr.names[0].lower())
    if len(limitattrs) > 0 and allow_only:
        raise errors.ObjectclassViolation(
            info=_('attribute "%(attribute)s" not allowed') % dict(
                attribute=limitattrs[0]))


class CallbackInterface(Method):
    """Callback registration interface

    This class's subclasses allow different types of callbacks to be added and
    removed to them.
    Registering a callback is done either by ``register_callback``, or by
    defining a ``<type>_callback`` method.

    Subclasses should define the `_callback_registry` attribute as a dictionary
    mapping allowed callback types to (initially) empty dictionaries.
    """

    _callback_registry = dict()

    @classmethod
    def get_callbacks(cls, callback_type):
        """Yield callbacks of the given type"""
        # Use one shared callback registry, keyed on class, to avoid problems
        # with missing attributes being looked up in superclasses
        callbacks = cls._callback_registry[callback_type].get(cls, [None])
        for callback in callbacks:
            if callback is None:
                try:
                    yield getattr(cls, '%s_callback' % callback_type)
                except AttributeError:
                    pass
            else:
                yield callback

    @classmethod
    def register_callback(cls, callback_type, callback, first=False):
        """Register a callback

        :param callback_type: The callback type (e.g. 'pre', 'post')
        :param callback: The callable added
        :param first: If true, the new callback will be added before all
            existing callbacks; otherwise it's added after them

        Note that callbacks registered this way will be attached to this class
        only, not to its subclasses.
        """
        assert callable(callback)
        try:
            callbacks = cls._callback_registry[callback_type][cls]
        except KeyError:
            callbacks = cls._callback_registry[callback_type][cls] = [None]
        if first:
            callbacks.insert(0, callback)
        else:
            callbacks.append(callback)


class BaseLDAPCommand(CallbackInterface, Command):
    """
    Base class for Base LDAP Commands.
    """
    setattr_option = Str('setattr*', validate_set_attribute,
                         cli_name='setattr',
                         doc=_("""Set an attribute to a name/value pair. Format is attr=value.
For multi-valued attributes, the command replaces the values already present."""),
                         exclude='webui',
                        )
    addattr_option = Str('addattr*', validate_add_attribute,
                         cli_name='addattr',
                         doc=_("""Add an attribute/value pair. Format is attr=value. The attribute
must be part of the schema."""),
                         exclude='webui',
                        )
    delattr_option = Str('delattr*', validate_del_attribute,
                         cli_name='delattr',
                         doc=_("""Delete an attribute/value pair. The option will be evaluated
last, after all sets and adds."""),
                         exclude='webui',
                        )

    _callback_registry = dict(pre={}, post={}, exc={}, interactive_prompt={})

    def _convert_2_dict(self, ldap, attrs):
        """
        Convert a string in the form of name/value pairs into a dictionary.

        :param attrs: A list of name/value pair strings, in the "name=value"
            format. May also be a single string, or None.
        """

        newdict = {}
        if attrs is None:
            attrs = []
        elif not type(attrs) in (list, tuple):
            attrs = [attrs]
        for a in attrs:
            m = re.match("\s*(.*?)\s*=\s*(.*?)\s*$", a)
            attr = str(m.group(1)).lower()
            value = m.group(2)
            if attr in self.obj.params and attr not in self.params:
                # The attribute is managed by IPA, but it didn't get cloned
                # to the command. This happens with no_update/no_create attrs.
                raise errors.ValidationError(
                    name=attr, error=_('attribute is not configurable'))
            if len(value) == 0:
                # None means "delete this attribute"
                value = None

            if ldap.has_dn_syntax(attr):
                try:
                    value = DN(value)
                except ValueError:
                    raise errors.InvalidSyntax(attr=attr)

            if attr in newdict:
                if type(value) in (tuple,):
                    newdict[attr] += list(value)
                else:
                    newdict[attr].append(value)
            else:
                if type(value) in (tuple,):
                    newdict[attr] = list(value)
                else:
                    newdict[attr] = [value]
        return newdict

    def process_attr_options(self, entry_attrs, dn, keys, options):
        """
        Process all --setattr, --addattr, and --delattr options and add the
        resulting value to the list of attributes. --setattr is processed first,
        then --addattr and finally --delattr.

        When --setattr is not used then the original LDAP object is looked up
        (of course, not when dn is None) and the changes are applied to old
        object values.

        Attribute values deleted by --delattr may be deleted from attribute
        values set or added by --setattr, --addattr. For example, the following
        attributes will result in a NOOP:

        --addattr=attribute=foo --delattr=attribute=foo

        AttrValueNotFound exception may be raised when an attribute value was
        not found either by --setattr and --addattr nor in existing LDAP object.

        :param entry_attrs: A list of attributes that will be updated
        :param dn: dn of updated LDAP object or None if a new object is created
        :param keys: List of command arguments
        :param options: List of options
        """

        if all(k not in options for k in ("setattr", "addattr", "delattr")):
            return

        ldap = self.obj.backend

        adddict = self._convert_2_dict(ldap, options.get('addattr', []))
        setdict = self._convert_2_dict(ldap, options.get('setattr', []))
        deldict = self._convert_2_dict(ldap, options.get('delattr', []))

        setattrs = set(setdict.keys())
        addattrs = set(adddict.keys())
        delattrs = set(deldict.keys())

        if dn is None:
            direct_add = addattrs
            direct_del = delattrs
            needldapattrs = []
        else:
            assert isinstance(dn, DN)
            direct_add = setattrs & addattrs
            direct_del = setattrs & delattrs
            needldapattrs = list((addattrs | delattrs) - setattrs)

        for attr, val in setdict.iteritems():
            entry_attrs[attr] = val

        for attr in direct_add:
            try:
                val = entry_attrs[attr]
            except KeyError:
                val = []
            else:
                if not isinstance(val, (list, tuple)):
                    val = [val]
                elif isinstance(val, tuple):
                    val = list(val)
            val.extend(adddict[attr])
            entry_attrs[attr] = val

        for attr in direct_del:
            for delval in deldict[attr]:
                try:
                    entry_attrs[attr].remove(delval)
                except ValueError:
                    raise errors.AttrValueNotFound(attr=attr, value=delval)

        if needldapattrs:
            try:
                (dn, old_entry) = self._exc_wrapper(keys, options, ldap.get_entry)(
                    dn, needldapattrs, normalize=self.obj.normalize_dn
                )
            except errors.NotFound:
                self.obj.handle_not_found(*keys)

            # Provide a nice error message when user tries to delete an
            # attribute that does not exist on the entry (and user is not
            # adding it)
            names = set(n.lower() for n in old_entry)
            del_nonexisting = delattrs - (names | setattrs | addattrs)
            if del_nonexisting:
                raise errors.ValidationError(name=del_nonexisting.pop(),
                    error=_('No such attribute on this entry'))

            for attr in needldapattrs:
                entry_attrs[attr] = old_entry.get(attr, [])

                if attr in addattrs:
                    entry_attrs[attr].extend(adddict.get(attr, []))

                for delval in deldict.get(attr, []):
                    try:
                        entry_attrs[attr].remove(delval)
                    except ValueError:
                        if isinstance(delval, str):
                            # This is a Binary value, base64 encode it
                            delval = unicode(base64.b64encode(delval))
                        raise errors.AttrValueNotFound(attr=attr, value=delval)

        # normalize all values
        changedattrs = setattrs | addattrs | delattrs
        for attr in changedattrs:
            if attr in self.params and self.params[attr].attribute:
                # convert single-value params to scalars
                param = self.params[attr]
                value = entry_attrs[attr]
                if not param.multivalue:
                    if len(value) == 1:
                        value = value[0]
                    elif not value:
                        value = None
                    else:
                        raise errors.OnlyOneValueAllowed(attr=attr)
                # validate, convert and encode params
                try:
                   value = param(value)
                except errors.ValidationError, err:
                    raise errors.ValidationError(name=attr, error=err.error)
                except errors.ConversionError, err:
                    raise errors.ConversionError(name=attr, error=err.error)
                entry_attrs[attr] = value
            else:
                # unknown attribute: remove duplicite and invalid values
                entry_attrs[attr] = list(set([val for val in entry_attrs[attr] if val]))
                if not entry_attrs[attr]:
                    entry_attrs[attr] = None
                elif isinstance(entry_attrs[attr], (tuple, list)) and len(entry_attrs[attr]) == 1:
                    entry_attrs[attr] = entry_attrs[attr][0]

    @classmethod
    def register_pre_callback(cls, callback, first=False):
        """Shortcut for register_callback('pre', ...)"""
        cls.register_callback('pre', callback, first)

    @classmethod
    def register_post_callback(cls, callback, first=False):
        """Shortcut for register_callback('post', ...)"""
        cls.register_callback('post', callback, first)

    @classmethod
    def register_exc_callback(cls, callback, first=False):
        """Shortcut for register_callback('exc', ...)"""
        cls.register_callback('exc', callback, first)

    @classmethod
    def register_interactive_prompt_callback(cls, callback, first=False):
        """Shortcut for register_callback('interactive_prompt', ...)"""
        cls.register_callback('interactive_prompt', callback, first)

    def _exc_wrapper(self, keys, options, call_func):
        """Function wrapper that automatically calls exception callbacks"""
        def wrapped(*call_args, **call_kwargs):
            # call call_func first
            func = call_func
            callbacks = list(self.get_callbacks('exc'))
            while True:
                try:
                    return func(*call_args, **call_kwargs)
                except errors.ExecutionError, e:
                    if not callbacks:
                        raise
                    # call exc_callback in the next loop
                    callback = callbacks.pop(0)
                    def exc_func(*args, **kwargs):
                        return callback(
                            self, keys, options, e, call_func, *args, **kwargs)
                    func = exc_func
        return wrapped

    def get_options(self):
        for param in super(BaseLDAPCommand, self).get_options():
            yield param
        if self.obj.attribute_members:
            for o in self.has_output:
                if isinstance(o, (output.Entry, output.ListOfEntries)):
                    yield Flag('no_members',
                        doc=_('Suppress processing of membership attributes.'),
                        exclude='webui',
                        flags=['no_option', 'no_output'],
                    )
                    break

class LDAPCreate(BaseLDAPCommand, crud.Create):
    """
    Create a new entry in LDAP.
    """
    takes_options = (BaseLDAPCommand.setattr_option, BaseLDAPCommand.addattr_option)

    def get_args(self):
        #pylint: disable=E1003
        for key in self.obj.get_ancestor_primary_keys():
            yield key
        if self.obj.primary_key:
            yield self.obj.primary_key.clone(attribute=True)
        for arg in super(crud.Create, self).get_args():
            yield arg

    has_output_params = global_output_params

    def execute(self, *keys, **options):
        ldap = self.obj.backend

        entry_attrs = self.args_options_2_entry(*keys, **options)

        self.process_attr_options(entry_attrs, None, keys, options)

        entry_attrs['objectclass'] = deepcopy(self.obj.object_class)

        if self.obj.object_class_config:
            config = ldap.get_ipa_config()[1]
            entry_attrs['objectclass'] = config.get(
                self.obj.object_class_config, entry_attrs['objectclass']
            )

        if self.obj.uuid_attribute:
            entry_attrs[self.obj.uuid_attribute] = 'autogenerate'

        dn = self.obj.get_dn(*keys, **options)
        assert isinstance(dn, DN)
        if self.obj.rdn_attribute:
            try:
                dn_attr = dn[0].attr
            except (IndexError, KeyError):
                dn_attr = None
            if dn_attr != self.obj.primary_key.name:
                self.obj.handle_duplicate_entry(*keys)
            dn = ldap.make_dn(
                entry_attrs, self.obj.rdn_attribute, self.obj.container_dn
            )

        if options.get('all', False):
            attrs_list = ['*'] + self.obj.default_attributes
        else:
            attrs_list = set(self.obj.default_attributes)
            attrs_list.update(entry_attrs.keys())
            if options.get('no_members', False):
                attrs_list.difference_update(self.obj.attribute_members)
            attrs_list = list(attrs_list)

        for callback in self.get_callbacks('pre'):
            dn = callback(
                self, ldap, dn, entry_attrs, attrs_list, *keys, **options)
            assert isinstance(dn, DN)

        _check_single_value_attrs(self.params, entry_attrs)
        _check_limit_object_class(self.api.Backend.ldap2.schema.attribute_types(self.obj.limit_object_classes), entry_attrs.keys(), allow_only=True)
        _check_limit_object_class(self.api.Backend.ldap2.schema.attribute_types(self.obj.disallow_object_classes), entry_attrs.keys(), allow_only=False)

        try:
            self._exc_wrapper(keys, options, ldap.add_entry)(dn, entry_attrs, normalize=self.obj.normalize_dn)
        except errors.NotFound:
            parent = self.obj.parent_object
            if parent:
                raise errors.NotFound(
                    reason=self.obj.parent_not_found_msg % {
                        'parent': keys[-2],
                        'oname': self.api.Object[parent].object_name,
                    }
                )
            raise errors.NotFound(
                reason=self.obj.container_not_found_msg % {
                    'container': self.obj.container_dn,
                }
            )
        except errors.DuplicateEntry:
            self.obj.handle_duplicate_entry(*keys)

        try:
            if self.obj.rdn_attribute:
                # make sure objectclass is either set or None
                if self.obj.object_class:
                    object_class = self.obj.object_class
                else:
                    object_class = None
                (dn, entry_attrs) = self._exc_wrapper(keys, options, ldap.find_entry_by_attr)(
                    self.obj.primary_key.name, keys[-1], object_class, attrs_list,
                    self.obj.container_dn
                )
                assert isinstance(dn, DN)
            else:
                (dn, entry_attrs) = self._exc_wrapper(keys, options, ldap.get_entry)(
                    dn, attrs_list, normalize=self.obj.normalize_dn
                )
                assert isinstance(dn, DN)
        except errors.NotFound:
            self.obj.handle_not_found(*keys)

        for callback in self.get_callbacks('post'):
            dn = callback(self, ldap, dn, entry_attrs, *keys, **options)

        assert isinstance(dn, DN)
        entry_attrs['dn'] = dn

        self.obj.convert_attribute_members(entry_attrs, *keys, **options)
        if self.obj.primary_key and keys[-1] is not None:
            return dict(result=entry_attrs, value=keys[-1])
        return dict(result=entry_attrs, value=u'')

    def pre_callback(self, ldap, dn, entry_attrs, attrs_list, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def post_callback(self, ldap, dn, entry_attrs, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def exc_callback(self, keys, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return

    # list of attributes we want exported to JSON
    json_friendly_attributes = (
        'takes_args',
    )

    def __json__(self):
        json_dict = dict(
            (a, getattr(self, a)) for a in self.json_friendly_attributes
        )
        json_dict['takes_options'] = list(self.get_json_options())
        return json_dict

class LDAPQuery(BaseLDAPCommand, crud.PKQuery):
    """
    Base class for commands that need to retrieve an existing entry.
    """
    def get_args(self):
        #pylint: disable=E1003
        for key in self.obj.get_ancestor_primary_keys():
            yield key
        if self.obj.primary_key:
            yield self.obj.primary_key.clone(attribute=True, query=True)
        for arg in super(crud.PKQuery, self).get_args():
            yield arg

    # list of attributes we want exported to JSON
    json_friendly_attributes = (
        'takes_args',
    )

    def __json__(self):
        json_dict = dict(
            (a, getattr(self, a)) for a in self.json_friendly_attributes
        )
        json_dict['takes_options'] = list(self.get_json_options())
        return json_dict

class LDAPMultiQuery(LDAPQuery):
    """
    Base class for commands that need to retrieve one or more existing entries.
    """
    takes_options = (
        Flag('continue',
            cli_name='continue',
            doc=_('Continuous mode: Don\'t stop on errors.'),
        ),
    )

    def get_args(self):
        #pylint: disable=E1003
        for key in self.obj.get_ancestor_primary_keys():
            yield key
        if self.obj.primary_key:
            yield self.obj.primary_key.clone(
                attribute=True, query=True, multivalue=True
            )
        for arg in super(crud.PKQuery, self).get_args():
            yield arg


class LDAPRetrieve(LDAPQuery):
    """
    Retrieve an LDAP entry.
    """
    has_output = output.standard_entry
    has_output_params = global_output_params

    takes_options = (
        Flag('rights',
            label=_('Rights'),
            doc=_('Display the access rights of this entry (requires --all). See ipa man page for details.'),
        ),
    )

    def execute(self, *keys, **options):
        ldap = self.obj.backend

        dn = self.obj.get_dn(*keys, **options)
        assert isinstance(dn, DN)

        if options.get('all', False):
            attrs_list = ['*'] + self.obj.default_attributes
        else:
            attrs_list = set(self.obj.default_attributes)
            if options.get('no_members', False):
                attrs_list.difference_update(self.obj.attribute_members)
            attrs_list = list(attrs_list)

        for callback in self.get_callbacks('pre'):
            dn = callback(self, ldap, dn, attrs_list, *keys, **options)
            assert isinstance(dn, DN)

        try:
            (dn, entry_attrs) = self._exc_wrapper(keys, options, ldap.get_entry)(
                dn, attrs_list, normalize=self.obj.normalize_dn
            )
            assert isinstance(dn, DN)
        except errors.NotFound:
            self.obj.handle_not_found(*keys)

        if options.get('rights', False) and options.get('all', False):
            entry_attrs['attributelevelrights'] = get_effective_rights(ldap, dn)

        for callback in self.get_callbacks('post'):
            dn = callback(self, ldap, dn, entry_attrs, *keys, **options)
            assert isinstance(dn, DN)

        self.obj.convert_attribute_members(entry_attrs, *keys, **options)
        assert isinstance(dn, DN)
        entry_attrs['dn'] = dn
        if self.obj.primary_key and keys[-1] is not None:
            return dict(result=entry_attrs, value=keys[-1])
        return dict(result=entry_attrs, value=u'')

    def pre_callback(self, ldap, dn, attrs_list, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def post_callback(self, ldap, dn, entry_attrs, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def exc_callback(self, keys, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return


class LDAPUpdate(LDAPQuery, crud.Update):
    """
    Update an LDAP entry.
    """

    takes_options = (
        BaseLDAPCommand.setattr_option,
        BaseLDAPCommand.addattr_option,
        BaseLDAPCommand.delattr_option,
        Flag('rights',
            label=_('Rights'),
            doc=_('Display the access rights of this entry (requires --all). See ipa man page for details.'),
        ),
    )

    has_output_params = global_output_params

    def _get_rename_option(self):
        rdnparam = getattr(self.obj.params, self.obj.primary_key.name)
        return rdnparam.clone_rename('rename',
            cli_name='rename', required=False, label=_('Rename'),
            doc=_('Rename the %(ldap_obj_name)s object') % dict(
                ldap_obj_name=self.obj.object_name
            )
        )

    def get_options(self):
        for option in super(LDAPUpdate, self).get_options():
            yield option
        if self.obj.rdn_is_primary_key:
            yield self._get_rename_option()

    def execute(self, *keys, **options):
        ldap = self.obj.backend

        if len(options) == 2: # 'all' and 'raw' are always sent
            raise errors.EmptyModlist()

        dn = self.obj.get_dn(*keys, **options)
        assert isinstance(dn, DN)

        entry_attrs = self.args_options_2_entry(**options)

        self.process_attr_options(entry_attrs, dn, keys, options)

        if options.get('all', False):
            attrs_list = ['*'] + self.obj.default_attributes
        else:
            attrs_list = set(self.obj.default_attributes)
            attrs_list.update(entry_attrs.keys())
            if options.get('no_members', False):
                attrs_list.difference_update(self.obj.attribute_members)
            attrs_list = list(attrs_list)

        _check_single_value_attrs(self.params, entry_attrs)
        _check_empty_attrs(self.obj.params, entry_attrs)

        for callback in self.get_callbacks('pre'):
            dn = callback(
                self, ldap, dn, entry_attrs, attrs_list, *keys, **options)
            assert isinstance(dn, DN)

        _check_limit_object_class(self.api.Backend.ldap2.schema.attribute_types(self.obj.limit_object_classes), entry_attrs.keys(), allow_only=True)
        _check_limit_object_class(self.api.Backend.ldap2.schema.attribute_types(self.obj.disallow_object_classes), entry_attrs.keys(), allow_only=False)

        rdnupdate = False
        try:
            if self.obj.rdn_is_primary_key and 'rename' in options:
                if not options['rename']:
                    raise errors.ValidationError(name='rename', error=u'can\'t be empty')
                entry_attrs[self.obj.primary_key.name] = options['rename']

            if self.obj.rdn_is_primary_key and self.obj.primary_key.name in entry_attrs:
                # RDN change
                self._exc_wrapper(keys, options, ldap.update_entry_rdn)(dn,
                    RDN((self.obj.primary_key.name, entry_attrs[self.obj.primary_key.name])))
                rdnkeys = keys[:-1] + (entry_attrs[self.obj.primary_key.name], )
                dn = self.obj.get_dn(*rdnkeys)
                assert isinstance(dn, DN)
                del entry_attrs[self.obj.primary_key.name]
                options['rdnupdate'] = True
                rdnupdate = True

            # Exception callbacks will need to test for options['rdnupdate']
            # to decide what to do. An EmptyModlist in this context doesn't
            # mean an error occurred, just that there were no other updates to
            # perform.
            assert isinstance(dn, DN)
            self._exc_wrapper(keys, options, ldap.update_entry)(dn, entry_attrs, normalize=self.obj.normalize_dn)
        except errors.EmptyModlist, e:
            if not rdnupdate:
                raise e
        except errors.NotFound:
            self.obj.handle_not_found(*keys)

        try:
            (dn, entry_attrs) = self._exc_wrapper(keys, options, ldap.get_entry)(
                dn, attrs_list, normalize=self.obj.normalize_dn
            )
        except errors.NotFound:
            raise errors.MidairCollision(
                format=_('the entry was deleted while being modified')
            )

        if options.get('rights', False) and options.get('all', False):
            entry_attrs['attributelevelrights'] = get_effective_rights(ldap, dn)

        for callback in self.get_callbacks('post'):
            dn = callback(self, ldap, dn, entry_attrs, *keys, **options)
            assert isinstance(dn, DN)

        self.obj.convert_attribute_members(entry_attrs, *keys, **options)
        if self.obj.primary_key and keys[-1] is not None:
            return dict(result=entry_attrs, value=keys[-1])
        return dict(result=entry_attrs, value=u'')

    def pre_callback(self, ldap, dn, entry_attrs, attrs_list, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def post_callback(self, ldap, dn, entry_attrs, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def exc_callback(self, keys, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return


class LDAPDelete(LDAPMultiQuery):
    """
    Delete an LDAP entry and all of its direct subentries.
    """
    has_output = output.standard_delete

    has_output_params = global_output_params

    def execute(self, *keys, **options):
        ldap = self.obj.backend

        def delete_entry(pkey):
            nkeys = keys[:-1] + (pkey, )
            dn = self.obj.get_dn(*nkeys, **options)
            assert isinstance(dn, DN)

            for callback in self.get_callbacks('pre'):
                dn = callback(self, ldap, dn, *nkeys, **options)
                assert isinstance(dn, DN)

            def delete_subtree(base_dn):
                assert isinstance(base_dn, DN)
                truncated = True
                while truncated:
                    try:
                        (subentries, truncated) = ldap.find_entries(
                            None, [''], base_dn, ldap.SCOPE_ONELEVEL
                        )
                    except errors.NotFound:
                        break
                    else:
                        for (dn_, entry_attrs) in subentries:
                            delete_subtree(dn_)
                try:
                    self._exc_wrapper(nkeys, options, ldap.delete_entry)(base_dn, normalize=self.obj.normalize_dn)
                except errors.NotFound:
                    self.obj.handle_not_found(*nkeys)

            try:
                self._exc_wrapper(nkeys, options, ldap.delete_entry)(dn, normalize=self.obj.normalize_dn)
            except errors.NotFound:
                self.obj.handle_not_found(*nkeys)
            except errors.NotAllowedOnNonLeaf:
                # this entry is not a leaf entry, delete all child nodes
                delete_subtree(dn)

            for callback in self.get_callbacks('post'):
                result = callback(self, ldap, dn, *nkeys, **options)

            return result

        if not self.obj.primary_key or not isinstance(keys[-1], (list, tuple)):
            pkeyiter = (keys[-1], )
        else:
            pkeyiter = keys[-1]

        deleted = []
        failed = []
        result = True
        for pkey in pkeyiter:
            try:
                if not delete_entry(pkey):
                    result = False
            except errors.ExecutionError:
                if not options.get('continue', False):
                    raise
                failed.append(pkey)
            else:
                deleted.append(pkey)

        if self.obj.primary_key and pkeyiter[0] is not None:
            return dict(result=dict(failed=u','.join(failed)), value=u','.join(deleted))
        return dict(result=dict(failed=u''), value=u'')

    def pre_callback(self, ldap, dn, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def post_callback(self, ldap, dn, *keys, **options):
        assert isinstance(dn, DN)
        return True

    def exc_callback(self, keys, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return


class LDAPModMember(LDAPQuery):
    """
    Base class for member manipulation.
    """
    member_attributes = ['member']
    member_param_doc = _('comma-separated list of %s')
    member_count_out = ('%i member processed.', '%i members processed.')

    def get_options(self):
        for option in super(LDAPModMember, self).get_options():
            yield option
        for attr in self.member_attributes:
            for ldap_obj_name in self.obj.attribute_members[attr]:
                ldap_obj = self.api.Object[ldap_obj_name]
                name = to_cli(ldap_obj_name)
                doc = self.member_param_doc % ldap_obj.object_name_plural
                yield Str('%s*' % name, cli_name='%ss' % name, doc=doc,
                          label=_('member %s') % ldap_obj.object_name,
                          csv=True, alwaysask=True)

    def get_member_dns(self, **options):
        dns = {}
        failed = {}
        for attr in self.member_attributes:
            dns[attr] = {}
            failed[attr] = {}
            for ldap_obj_name in self.obj.attribute_members[attr]:
                dns[attr][ldap_obj_name] = []
                failed[attr][ldap_obj_name] = []
                names = options.get(to_cli(ldap_obj_name), [])
                if not names:
                    continue
                for name in names:
                    if not name:
                        continue
                    ldap_obj = self.api.Object[ldap_obj_name]
                    try:
                        dns[attr][ldap_obj_name].append(ldap_obj.get_dn(name))
                    except errors.PublicError, e:
                        failed[attr][ldap_obj_name].append((name, unicode(e)))
        return (dns, failed)


class LDAPAddMember(LDAPModMember):
    """
    Add other LDAP entries to members.
    """
    member_param_doc = _('comma-separated list of %s to add')
    member_count_out = ('%i member added.', '%i members added.')
    allow_same = False

    has_output = (
        output.Entry('result'),
        output.Output('failed',
            type=dict,
            doc=_('Members that could not be added'),
        ),
        output.Output('completed',
            type=int,
            doc=_('Number of members added'),
        ),
    )

    has_output_params = global_output_params

    def execute(self, *keys, **options):
        ldap = self.obj.backend

        (member_dns, failed) = self.get_member_dns(**options)

        dn = self.obj.get_dn(*keys, **options)
        assert isinstance(dn, DN)

        for callback in self.get_callbacks('pre'):
            dn = callback(self, ldap, dn, member_dns, failed, *keys, **options)
            assert isinstance(dn, DN)

        completed = 0
        for (attr, objs) in member_dns.iteritems():
            for ldap_obj_name in objs:
                for m_dn in member_dns[attr][ldap_obj_name]:
                    assert isinstance(m_dn, DN)
                    if not m_dn:
                        continue
                    try:
                        ldap.add_entry_to_group(m_dn, dn, attr, allow_same=self.allow_same)
                    except errors.PublicError, e:
                        ldap_obj = self.api.Object[ldap_obj_name]
                        failed[attr][ldap_obj_name].append((
                            ldap_obj.get_primary_key_from_dn(m_dn),
                            unicode(e),)
                        )
                    else:
                        completed += 1

        if options.get('all', False):
            attrs_list = ['*'] + self.obj.default_attributes
        else:
            attrs_list = set(self.obj.default_attributes)
            attrs_list.update(member_dns.keys())
            if options.get('no_members', False):
                attrs_list.difference_update(self.obj.attribute_members)
            attrs_list = list(attrs_list)

        try:
            (dn, entry_attrs) = self._exc_wrapper(keys, options, ldap.get_entry)(
                dn, attrs_list, normalize=self.obj.normalize_dn
            )
        except errors.NotFound:
            self.obj.handle_not_found(*keys)

        for callback in self.get_callbacks('post'):
            (completed, dn) = callback(
                self, ldap, completed, failed, dn, entry_attrs, *keys,
                **options)
            assert isinstance(dn, DN)

        assert isinstance(dn, DN)
        entry_attrs['dn'] = dn
        self.obj.convert_attribute_members(entry_attrs, *keys, **options)
        return dict(
            completed=completed,
            failed=failed,
            result=entry_attrs,
        )

    def pre_callback(self, ldap, dn, found, not_found, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def post_callback(self, ldap, completed, failed, dn, entry_attrs, *keys, **options):
        assert isinstance(dn, DN)
        return (completed, dn)

    def exc_callback(self, keys, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return


class LDAPRemoveMember(LDAPModMember):
    """
    Remove LDAP entries from members.
    """
    member_param_doc = _('comma-separated list of %s to remove')
    member_count_out = ('%i member removed.', '%i members removed.')

    has_output = (
        output.Entry('result'),
        output.Output('failed',
            type=dict,
            doc=_('Members that could not be removed'),
        ),
        output.Output('completed',
            type=int,
            doc=_('Number of members removed'),
        ),
    )

    has_output_params = global_output_params

    def execute(self, *keys, **options):
        ldap = self.obj.backend

        (member_dns, failed) = self.get_member_dns(**options)

        dn = self.obj.get_dn(*keys, **options)
        assert isinstance(dn, DN)

        for callback in self.get_callbacks('pre'):
            dn = callback(self, ldap, dn, member_dns, failed, *keys, **options)
            assert isinstance(dn, DN)

        completed = 0
        for (attr, objs) in member_dns.iteritems():
            for ldap_obj_name, m_dns in objs.iteritems():
                for m_dn in m_dns:
                    assert isinstance(m_dn, DN)
                    if not m_dn:
                        continue
                    try:
                        ldap.remove_entry_from_group(m_dn, dn, attr)
                    except errors.PublicError, e:
                        ldap_obj = self.api.Object[ldap_obj_name]
                        failed[attr][ldap_obj_name].append((
                            ldap_obj.get_primary_key_from_dn(m_dn),
                            unicode(e),)
                        )
                    else:
                        completed += 1

        if options.get('all', False):
            attrs_list = ['*'] + self.obj.default_attributes
        else:
            attrs_list = set(self.obj.default_attributes)
            attrs_list.update(member_dns.keys())
            if options.get('no_members', False):
                attrs_list.difference_update(self.obj.attribute_members)
            attrs_list = list(attrs_list)

        # Give memberOf a chance to update entries
        time.sleep(.3)

        try:
            (dn, entry_attrs) = self._exc_wrapper(keys, options, ldap.get_entry)(
                dn, attrs_list, normalize=self.obj.normalize_dn
            )
        except errors.NotFound:
            self.obj.handle_not_found(*keys)

        for callback in self.get_callbacks('post'):
            (completed, dn) = callback(
                self, ldap, completed, failed, dn, entry_attrs, *keys,
                **options)
            assert isinstance(dn, DN)

        assert isinstance(dn, DN)
        entry_attrs['dn'] = dn

        self.obj.convert_attribute_members(entry_attrs, *keys, **options)
        return dict(
            completed=completed,
            failed=failed,
            result=entry_attrs,
        )

    def pre_callback(self, ldap, dn, found, not_found, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def post_callback(self, ldap, completed, failed, dn, entry_attrs, *keys, **options):
        assert isinstance(dn, DN)
        return (completed, dn)

    def exc_callback(self, keys, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return


def gen_pkey_only_option(cli_name):
    return Flag('pkey_only?',
                label=_('Primary key only'),
                doc=_('Results should contain primary key attribute only ("%s")') \
                    % to_cli(cli_name),)

class LDAPSearch(BaseLDAPCommand, crud.Search):
    """
    Retrieve all LDAP entries matching the given criteria.
    """
    member_attributes = []
    member_param_incl_doc = _('Search for %(searched_object)s with these %(relationship)s %(ldap_object)s.')
    member_param_excl_doc = _('Search for %(searched_object)s without these %(relationship)s %(ldap_object)s.')

    # LDAPSearch sorts all matched records in the end using their primary key
    # as a key attribute
    # Set the following attribute to False to turn sorting off
    sort_result_entries = True

    takes_options = (
        Int('timelimit?',
            label=_('Time Limit'),
            doc=_('Time limit of search in seconds'),
            flags=['no_display'],
            minvalue=0,
            autofill=False,
        ),
        Int('sizelimit?',
            label=_('Size Limit'),
            doc=_('Maximum number of entries returned'),
            flags=['no_display'],
            minvalue=0,
            autofill=False,
        ),
    )

    def get_args(self):
        #pylint: disable=E1003
        for key in self.obj.get_ancestor_primary_keys():
            yield key
        yield Str('criteria?',
                  noextrawhitespace=False,
                  doc=_('A string searched in all relevant object attributes'))
        for arg in super(crud.Search, self).get_args():
            yield arg

    def get_member_options(self, attr):
        for ldap_obj_name in self.obj.attribute_members[attr]:
            ldap_obj = self.api.Object[ldap_obj_name]
            relationship = self.obj.relationships.get(
                attr, ['member', '', 'no_']
            )
            doc = self.member_param_incl_doc % dict(
                searched_object=self.obj.object_name_plural,
                relationship=relationship[0].lower(),
                ldap_object=ldap_obj.object_name_plural
            )
            name = '%s%s' % (relationship[1], to_cli(ldap_obj_name))
            yield Str(
                '%s*' % name, cli_name='%ss' % name, doc=doc,
                label=ldap_obj.object_name, csv=True
            )
            doc = self.member_param_excl_doc % dict(
                searched_object=self.obj.object_name_plural,
                relationship=relationship[0].lower(),
                ldap_object=ldap_obj.object_name_plural
            )
            name = '%s%s' % (relationship[2], to_cli(ldap_obj_name))
            yield Str(
                '%s*' % name, cli_name='%ss' % name, doc=doc,
                label=ldap_obj.object_name, csv=True
            )

    def get_options(self):
        for option in super(LDAPSearch, self).get_options():
            yield option
        if self.obj.primary_key and \
                'no_output' not in self.obj.primary_key.flags:
            yield gen_pkey_only_option(self.obj.primary_key.cli_name)
        for attr in self.member_attributes:
            for option in self.get_member_options(attr):
                yield option

    def get_member_filter(self, ldap, **options):
        filter = ''
        for attr in self.member_attributes:
            for ldap_obj_name in self.obj.attribute_members[attr]:
                ldap_obj = self.api.Object[ldap_obj_name]
                relationship = self.obj.relationships.get(
                    attr, ['member', '', 'no_']
                )
                # Handle positive (MATCH_ALL) and negative (MATCH_NONE)
                # searches similarly
                param_prefixes = relationship[1:]  # e.g. ('in_', 'not_in_')
                rules = ldap.MATCH_ALL, ldap.MATCH_NONE
                for param_prefix, rule in zip(param_prefixes, rules):
                    param_name = '%s%s' % (param_prefix, to_cli(ldap_obj_name))
                    if options.get(param_name):
                        dns = []
                        for pkey in options[param_name]:
                            dns.append(ldap_obj.get_dn(pkey))
                        flt = ldap.make_filter_from_attr(attr, dns, rule)
                        filter = ldap.combine_filters(
                            (filter, flt), ldap.MATCH_ALL
                        )
        return filter

    has_output_params = global_output_params

    def execute(self, *args, **options):
        ldap = self.obj.backend

        term = args[-1]
        if self.obj.parent_object:
            base_dn = self.api.Object[self.obj.parent_object].get_dn(*args[:-1])
        else:
            base_dn = self.obj.container_dn
        assert isinstance(base_dn, DN)

        search_kw = self.args_options_2_entry(**options)

        if self.obj.search_display_attributes:
            defattrs = self.obj.search_display_attributes
        else:
            defattrs = self.obj.default_attributes

        if options.get('pkey_only', False):
            attrs_list = [self.obj.primary_key.name]
        elif options.get('all', False):
            attrs_list = ['*'] + defattrs
        else:
            attrs_list = set(defattrs)
            attrs_list.update(search_kw.keys())
            if options.get('no_members', False):
                attrs_list.difference_update(self.obj.attribute_members)
            attrs_list = list(attrs_list)

        if self.obj.search_attributes:
            search_attrs = self.obj.search_attributes
        else:
            search_attrs = self.obj.default_attributes
        if self.obj.search_attributes_config:
            config = ldap.get_ipa_config()[1]
            config_attrs = config.get(
                self.obj.search_attributes_config, [])
            if len(config_attrs) == 1 and (
                isinstance(config_attrs[0], basestring)):
                search_attrs = config_attrs[0].split(',')

        search_kw['objectclass'] = self.obj.object_class
        attr_filter = ldap.make_filter(search_kw, rules=ldap.MATCH_ALL)

        search_kw = {}
        for a in search_attrs:
            search_kw[a] = term
        term_filter = ldap.make_filter(search_kw, exact=False)

        member_filter = self.get_member_filter(ldap, **options)

        filter = ldap.combine_filters(
            (term_filter, attr_filter, member_filter), rules=ldap.MATCH_ALL
        )

        scope = ldap.SCOPE_ONELEVEL
        for callback in self.get_callbacks('pre'):
            (filter, base_dn, scope) = callback(
                self, ldap, filter, attrs_list, base_dn, scope, *args, **options)
            assert isinstance(base_dn, DN)

        try:
            (entries, truncated) = self._exc_wrapper(args, options, ldap.find_entries)(
                filter, attrs_list, base_dn, scope,
                time_limit=options.get('timelimit', None),
                size_limit=options.get('sizelimit', None)
            )
        except errors.NotFound:
            (entries, truncated) = ([], False)

        for callback in self.get_callbacks('post'):
            truncated = callback(self, ldap, entries, truncated, *args, **options)

        if self.sort_result_entries:
            if self.obj.primary_key:
                def sort_key(x):
                    return x[1][self.obj.primary_key.name][0].lower()
                entries.sort(key=sort_key)

        if not options.get('raw', False):
            for e in entries:
                self.obj.convert_attribute_members(e[1], *args, **options)

        for e in entries:
            assert isinstance(e[0], DN)
            e[1]['dn'] = e[0]
        entries = [e for (dn, e) in entries]

        return dict(
            result=entries,
            count=len(entries),
            truncated=truncated,
        )

    def pre_callback(self, ldap, filters, attrs_list, base_dn, scope, *args, **options):
        assert isinstance(base_dn, DN)
        return (filters, base_dn, scope)

    def post_callback(self, ldap, entries, truncated, *args, **options):
        return truncated

    def exc_callback(self, args, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return

    # list of attributes we want exported to JSON
    json_friendly_attributes = (
        'takes_args',
    )

    def __json__(self):
        json_dict = dict(
            (a, getattr(self, a)) for a in self.json_friendly_attributes
        )
        json_dict['takes_options'] = list(self.get_json_options())
        return json_dict

class LDAPModReverseMember(LDAPQuery):
    """
    Base class for reverse member manipulation.
    """
    reverse_attributes = ['member']
    reverse_param_doc = _('comma-separated list of %s')
    reverse_count_out = ('%i member processed.', '%i members processed.')

    has_output_params = global_output_params

    def get_options(self):
        for option in super(LDAPModReverseMember, self).get_options():
            yield option
        for attr in self.reverse_attributes:
            for ldap_obj_name in self.obj.reverse_members[attr]:
                ldap_obj = self.api.Object[ldap_obj_name]
                name = to_cli(ldap_obj_name)
                doc = self.reverse_param_doc % ldap_obj.object_name_plural
                yield Str('%s*' % name, cli_name='%ss' % name, doc=doc,
                          label=ldap_obj.object_name, csv=True,
                          alwaysask=True)


class LDAPAddReverseMember(LDAPModReverseMember):
    """
    Add other LDAP entries to members in reverse.

    The call looks like "add A to B" but in fact executes
    add B to A to handle reverse membership.
    """
    member_param_doc = _('comma-separated list of %s to add')
    member_count_out = ('%i member added.', '%i members added.')

    show_command = None
    member_command = None
    reverse_attr = None
    member_attr = None

    has_output = (
        output.Entry('result'),
        output.Output('failed',
            type=dict,
            doc=_('Members that could not be added'),
        ),
        output.Output('completed',
            type=int,
            doc=_('Number of members added'),
        ),
    )

    has_output_params = global_output_params

    def execute(self, *keys, **options):
        ldap = self.obj.backend

        # Ensure our target exists
        result = self.api.Command[self.show_command](keys[-1])['result']
        dn = result['dn']
        assert isinstance(dn, DN)

        for callback in self.get_callbacks('pre'):
            dn = callback(self, ldap, dn, *keys, **options)
            assert isinstance(dn, DN)

        if options.get('all', False):
            attrs_list = ['*'] + self.obj.default_attributes
        else:
            attrs_list = set(self.obj.default_attributes)
            if options.get('no_members', False):
                attrs_list.difference_update(self.obj.attribute_members)
            attrs_list = list(attrs_list)

        # Pull the record as it is now so we can know how many members
        # there are.
        entry_start = self.api.Command[self.show_command](keys[-1])['result']
        completed = 0
        failed = {'member': {self.reverse_attr: []}}
        for attr in options.get(self.reverse_attr) or []:
            try:
                options = {'%s' % self.member_attr: keys[-1]}
                try:
                    result = self._exc_wrapper(keys, options, self.api.Command[self.member_command])(attr, **options)
                    if result['completed'] == 1:
                        completed = completed + 1
                    else:
                        failed['member'][self.reverse_attr].append((attr, result['failed']['member'][self.member_attr][0][1]))
                except errors.NotFound, e:
                    msg = str(e)
                    (attr, msg) = msg.split(':', 1)
                    failed['member'][self.reverse_attr].append((attr, unicode(msg.strip())))

            except errors.PublicError, e:
                failed['member'][self.reverse_attr].append((attr, unicode(msg)))

        # Wait for the memberof plugin to update the entry
        try:
            entry_attrs = wait_for_memberof(keys, entry_start, completed, self.show_command, adding=True)
        except Exception, e:
            raise errors.ReverseMemberError(verb=_('added'), exc=str(e))

        for callback in self.get_callbacks('post'):
            (completed, dn) = callback(
                self, ldap, completed, failed, dn, entry_attrs, *keys,
                **options)
            assert isinstance(dn, DN)

        assert isinstance(dn, DN)
        entry_attrs['dn'] = dn
        return dict(
            completed=completed,
            failed=failed,
            result=entry_attrs,
        )

    def pre_callback(self, ldap, dn, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def post_callback(self, ldap, completed, failed, dn, entry_attrs, *keys, **options):
        assert isinstance(dn, DN)
        return (completed, dn)

    def exc_callback(self, keys, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return

class LDAPRemoveReverseMember(LDAPModReverseMember):
    """
    Remove other LDAP entries from members in reverse.

    The call looks like "remove A from B" but in fact executes
    remove B from A to handle reverse membership.
    """
    member_param_doc = _('comma-separated list of %s to remove')
    member_count_out = ('%i member removed.', '%i members removed.')

    show_command = None
    member_command = None
    reverse_attr = None
    member_attr = None

    has_output = (
        output.Entry('result'),
        output.Output('failed',
            type=dict,
            doc=_('Members that could not be removed'),
        ),
        output.Output('completed',
            type=int,
            doc=_('Number of members removed'),
        ),
    )

    has_output_params = global_output_params

    def execute(self, *keys, **options):
        ldap = self.obj.backend

        # Ensure our target exists
        result = self.api.Command[self.show_command](keys[-1])['result']
        dn = result['dn']
        assert isinstance(dn, DN)

        for callback in self.get_callbacks('pre'):
            dn = callback(self, ldap, dn, *keys, **options)
            assert isinstance(dn, DN)

        if options.get('all', False):
            attrs_list = ['*'] + self.obj.default_attributes
        else:
            attrs_list = set(self.obj.default_attributes)
            if options.get('no_members', False):
                attrs_list.difference_update(self.obj.attribute_members)
            attrs_list = list(attrs_list)

        # Pull the record as it is now so we can know how many members
        # there are.
        entry_start = self.api.Command[self.show_command](keys[-1])['result']
        completed = 0
        failed = {'member': {self.reverse_attr: []}}
        for attr in options.get(self.reverse_attr) or []:
            try:
                options = {'%s' % self.member_attr: keys[-1]}
                try:
                    result = self._exc_wrapper(keys, options, self.api.Command[self.member_command])(attr, **options)
                    if result['completed'] == 1:
                        completed = completed + 1
                    else:
                        failed['member'][self.reverse_attr].append((attr, result['failed']['member'][self.member_attr][0][1]))
                except errors.NotFound, e:
                    msg = str(e)
                    (attr, msg) = msg.split(':', 1)
                    failed['member'][self.reverse_attr].append((attr, unicode(msg.strip())))

            except errors.PublicError, e:
                failed['member'][self.reverse_attr].append((attr, unicode(msg)))

        # Wait for the memberof plugin to update the entry
        try:
            entry_attrs = wait_for_memberof(keys, entry_start, completed, self.show_command, adding=False)
        except Exception, e:
            raise errors.ReverseMemberError(verb=_('removed'), exc=str(e))

        for callback in self.get_callbacks('post'):
            (completed, dn) = callback(
                self, ldap, completed, failed, dn, entry_attrs, *keys,
                **options)
            assert isinstance(dn, DN)

        assert isinstance(dn, DN)
        entry_attrs['dn'] = dn
        return dict(
            completed=completed,
            failed=failed,
            result=entry_attrs,
        )

    def pre_callback(self, ldap, dn, *keys, **options):
        assert isinstance(dn, DN)
        return dn

    def post_callback(self, ldap, completed, failed, dn, entry_attrs, *keys, **options):
        assert isinstance(dn, DN)
        return (completed, dn)

    def exc_callback(self, keys, options, exc, call_func, *call_args, **call_kwargs):
        raise exc

    def interactive_prompt_callback(self, kw):
        return
