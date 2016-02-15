# -*- coding: utf-8 -*-
# Authors:
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
Test the `ipalib.parameters` module.
"""

import re
import sys
from types import NoneType
from decimal import Decimal
from inspect import isclass
from tests.util import raises, ClassChecker, read_only
from tests.util import dummy_ugettext, assert_equal
from tests.data import binary_bytes, utf8_bytes, unicode_str
from ipalib import parameters, text, errors, config
from ipalib.constants import TYPE_ERROR, CALLABLE_ERROR, NULLS
from ipalib.errors import ValidationError, ConversionError
from ipalib import _
from xmlrpclib import MAXINT, MININT

class test_DefaultFrom(ClassChecker):
    """
    Test the `ipalib.parameters.DefaultFrom` class.
    """
    _cls = parameters.DefaultFrom

    def test_init(self):
        """
        Test the `ipalib.parameters.DefaultFrom.__init__` method.
        """
        def callback(*args):
            return args
        keys = ('givenname', 'sn')
        o = self.cls(callback, *keys)
        assert read_only(o, 'callback') is callback
        assert read_only(o, 'keys') == keys
        lam = lambda first, last: first[0] + last
        o = self.cls(lam)
        assert read_only(o, 'keys') == ('first', 'last')

        # Test that TypeError is raised when callback isn't callable:
        e = raises(TypeError, self.cls, 'whatever')
        assert str(e) == CALLABLE_ERROR % ('callback', 'whatever', str)

        # Test that TypeError is raised when a key isn't an str:
        e = raises(TypeError, self.cls, callback, 'givenname', 17)
        assert str(e) == TYPE_ERROR % ('keys', str, 17, int)

        # Test that ValueError is raised when inferring keys from a callback
        # which has *args:
        e = raises(ValueError, self.cls, lambda foo, *args: None)
        assert str(e) == "callback: variable-length argument list not allowed"

        # Test that ValueError is raised when inferring keys from a callback
        # which has **kwargs:
        e = raises(ValueError, self.cls, lambda foo, **kwargs: None)
        assert str(e) == "callback: variable-length argument list not allowed"

    def test_repr(self):
        """
        Test the `ipalib.parameters.DefaultFrom.__repr__` method.
        """
        def stuff(one, two):
            pass

        o = self.cls(stuff)
        assert repr(o) == "DefaultFrom(stuff, 'one', 'two')"

        o = self.cls(stuff, 'aye', 'bee', 'see')
        assert repr(o) == "DefaultFrom(stuff, 'aye', 'bee', 'see')"

        cb = lambda first, last: first[0] + last

        o = self.cls(cb)
        assert repr(o) == "DefaultFrom(<lambda>, 'first', 'last')"

        o = self.cls(cb, 'aye', 'bee', 'see')
        assert repr(o) == "DefaultFrom(<lambda>, 'aye', 'bee', 'see')"

    def test_call(self):
        """
        Test the `ipalib.parameters.DefaultFrom.__call__` method.
        """
        def callback(givenname, sn):
            return givenname[0] + sn[0]
        keys = ('givenname', 'sn')
        o = self.cls(callback, *keys)
        kw = dict(
            givenname='John',
            sn='Public',
            hello='world',
        )
        assert o(**kw) == 'JP'
        assert o() is None
        for key in ('givenname', 'sn'):
            kw_copy = dict(kw)
            del kw_copy[key]
            assert o(**kw_copy) is None

        # Test using implied keys:
        o = self.cls(lambda first, last: first[0] + last)
        assert o(first='john', last='doe') == 'jdoe'
        assert o(first='', last='doe') is None
        assert o(one='john', two='doe') is None

        # Test that co_varnames slice is used:
        def callback2(first, last):
            letter = first[0]
            return letter + last
        o = self.cls(callback2)
        assert o.keys == ('first', 'last')
        assert o(first='john', last='doe') == 'jdoe'


def test_parse_param_spec():
    """
    Test the `ipalib.parameters.parse_param_spec` function.
    """
    f = parameters.parse_param_spec
    assert f('name') == ('name', dict(required=True, multivalue=False))
    assert f('name?') == ('name', dict(required=False, multivalue=False))
    assert f('name*') == ('name', dict(required=False, multivalue=True))
    assert f('name+') == ('name', dict(required=True, multivalue=True))

    # Make sure other "funny" endings are *not* treated special:
    assert f('name^') == ('name^', dict(required=True, multivalue=False))

    # Test that TypeError is raised if spec isn't an str:
    e = raises(TypeError, f, u'name?')
    assert str(e) == TYPE_ERROR % ('spec', str, u'name?', unicode)


class DummyRule(object):
    def __init__(self, error=None):
        assert error is None or type(error) is unicode
        self.error = error
        self.reset()

    def __call__(self, *args):
        self.calls.append(args)
        return self.error

    def reset(self):
        self.calls = []


class test_Param(ClassChecker):
    """
    Test the `ipalib.parameters.Param` class.
    """
    _cls = parameters.Param

    def test_init(self):
        """
        Test the `ipalib.parameters.Param.__init__` method.
        """
        name = 'my_param'
        o = self.cls(name)
        assert o.param_spec is name
        assert o.name is name
        assert o.nice == "Param('my_param')"
        assert o.password is False
        assert o.__islocked__() is True

        # Test default rules:
        assert o.rules == tuple()
        assert o.class_rules == tuple()
        assert o.all_rules == tuple()

        # Test default kwarg values:
        assert o.cli_name is name
        assert o.label.msg == 'my_param'
        assert o.doc.msg == 'my_param'
        assert o.required is True
        assert o.multivalue is False
        assert o.primary_key is False
        assert o.normalizer is None
        assert o.default is None
        assert o.default_from is None
        assert o.autofill is False
        assert o.query is False
        assert o.attribute is False
        assert o.include is None
        assert o.exclude is None
        assert o.flags == frozenset()
        assert o.sortorder == 2
        assert o.csv is False
        assert o.csv_separator == ','
        assert o.csv_skipspace is True

        # Test that doc defaults from label:
        o = self.cls('my_param', doc=_('Hello world'))
        assert o.label.msg == 'my_param'
        assert o.doc.msg == 'Hello world'

        o = self.cls('my_param', label='My Param')
        assert o.label == 'My Param'
        assert o.doc == 'My Param'


        # Test that ValueError is raised when a kwarg from a subclass
        # conflicts with an attribute:
        class Subclass(self.cls):
            kwargs = self.cls.kwargs + (
                ('convert', callable, None),
            )
        e = raises(ValueError, Subclass, name)
        assert str(e) == "kwarg 'convert' conflicts with attribute on Subclass"

        # Test type validation of keyword arguments:
        class Subclass(self.cls):
            kwargs = self.cls.kwargs + (
                ('extra1', bool, True),
                ('extra2', str, 'Hello'),
                ('extra3', (int, float), 42),
                ('extra4', callable, lambda whatever: whatever + 7),
            )
        o = Subclass('my_param')  # Test with no **kw:
        for (key, kind, default) in o.kwargs:
            # Test with a type invalid for all:
            value = object()
            kw = {key: value}
            e = raises(TypeError, Subclass, 'my_param', **kw)
            if kind is callable:
                assert str(e) == CALLABLE_ERROR % (key, value, type(value))
            else:
                assert str(e) == TYPE_ERROR % (key, kind, value, type(value))
            # Test with None:
            kw = {key: None}
            Subclass('my_param', **kw)

        # Test when using unknown kwargs:
        e = raises(TypeError, self.cls, 'my_param',
            flags=['hello', 'world'],
            whatever=u'Hooray!',
        )
        assert str(e) == \
            "Param('my_param'): takes no such kwargs: 'whatever'"
        e = raises(TypeError, self.cls, 'my_param', great='Yes', ape='he is!')
        assert str(e) == \
            "Param('my_param'): takes no such kwargs: 'ape', 'great'"

        # Test that ValueError is raised if you provide both include and
        # exclude:
        e = raises(ValueError, self.cls, 'my_param',
            include=['server', 'foo'],
            exclude=['client', 'bar'],
        )
        assert str(e) == '%s: cannot have both %s=%r and %s=%r' % (
            "Param('my_param')",
            'include', frozenset(['server', 'foo']),
            'exclude', frozenset(['client', 'bar']),
        )

        # Test that ValueError is raised if csv is set and multivalue is not set:
        e = raises(ValueError, self.cls, 'my_param', csv=True)
        assert str(e) == '%s: cannot have csv without multivalue' % "Param('my_param')"

        # Test that default_from gets set:
        call = lambda first, last: first[0] + last
        o = self.cls('my_param', default_from=call)
        assert type(o.default_from) is parameters.DefaultFrom
        assert o.default_from.callback is call

    def test_repr(self):
        """
        Test the `ipalib.parameters.Param.__repr__` method.
        """
        for name in ['name', 'name?', 'name*', 'name+']:
            o = self.cls(name)
            assert repr(o) == 'Param(%r)' % name
        o = self.cls('name', required=False)
        assert repr(o) == "Param('name', required=False)"
        o = self.cls('name', multivalue=True)
        assert repr(o) == "Param('name', multivalue=True)"

    def test_use_in_context(self):
        """
        Test the `ipalib.parameters.Param.use_in_context` method.
        """
        set1 = ('one', 'two', 'three')
        set2 = ('four', 'five', 'six')
        param1 = self.cls('param1')
        param2 = self.cls('param2', include=set1)
        param3 = self.cls('param3', exclude=set2)
        for context in set1:
            env = config.Env()
            env.context = context
            assert param1.use_in_context(env) is True, context
            assert param2.use_in_context(env) is True, context
            assert param3.use_in_context(env) is True, context
        for context in set2:
            env = config.Env()
            env.context = context
            assert param1.use_in_context(env) is True, context
            assert param2.use_in_context(env) is False, context
            assert param3.use_in_context(env) is False, context

    def test_safe_value(self):
        """
        Test the `ipalib.parameters.Param.safe_value` method.
        """
        values = (unicode_str, binary_bytes, utf8_bytes)
        o = self.cls('my_param')
        for value in values:
            assert o.safe_value(value) is value
        assert o.safe_value(None) is None
        p = parameters.Password('my_passwd')
        for value in values:
            assert_equal(p.safe_value(value), u'********')
        assert p.safe_value(None) is None

    def test_clone(self):
        """
        Test the `ipalib.parameters.Param.clone` method.
        """
        # Test with the defaults
        orig = self.cls('my_param')
        clone = orig.clone()
        assert clone is not orig
        assert type(clone) is self.cls
        assert clone.name is orig.name
        for (key, kind, default) in self.cls.kwargs:
            assert getattr(clone, key) is getattr(orig, key)

        # Test with a param spec:
        orig = self.cls('my_param*')
        assert orig.param_spec == 'my_param*'
        clone = orig.clone()
        assert clone.param_spec == 'my_param'
        assert clone is not orig
        assert type(clone) is self.cls
        for (key, kind, default) in self.cls.kwargs:
            assert getattr(clone, key) is getattr(orig, key)

        # Test with overrides:
        orig = self.cls('my_param*')
        assert orig.required is False
        assert orig.multivalue is True
        clone = orig.clone(required=True)
        assert clone is not orig
        assert type(clone) is self.cls
        assert clone.required is True
        assert clone.multivalue is True
        assert clone.param_spec == 'my_param'
        assert clone.name == 'my_param'

    def test_clone_rename(self):
        """
        Test the `ipalib.parameters.Param.clone` method.
        """
        new_name = 'my_new_param'

        # Test with the defaults
        orig = self.cls('my_param')
        clone = orig.clone_rename(new_name)
        assert clone is not orig
        assert type(clone) is self.cls
        assert clone.name == new_name
        for (key, kind, default) in self.cls.kwargs:
            assert getattr(clone, key) is getattr(orig, key)

        # Test with overrides:
        orig = self.cls('my_param*')
        assert orig.required is False
        assert orig.multivalue is True
        clone = orig.clone_rename(new_name, required=True)
        assert clone is not orig
        assert type(clone) is self.cls
        assert clone.required is True
        assert clone.multivalue is True
        assert clone.param_spec == new_name
        assert clone.name == new_name


    def test_convert(self):
        """
        Test the `ipalib.parameters.Param.convert` method.
        """
        okay = ('Hello', u'Hello', 0, 4.2, True, False, unicode_str)
        class Subclass(self.cls):
            def _convert_scalar(self, value, index=None):
                return value

        # Test when multivalue=False:
        o = Subclass('my_param')
        for value in NULLS:
            assert o.convert(value) is None
        assert o.convert(None) is None
        for value in okay:
            assert o.convert(value) is value

        # Test when multivalue=True:
        o = Subclass('my_param', multivalue=True)
        for value in NULLS:
            assert o.convert(value) is None
        assert o.convert(okay) == okay
        assert o.convert(NULLS) is None
        assert o.convert(okay + NULLS) == okay
        assert o.convert(NULLS + okay) == okay
        for value in okay:
            assert o.convert(value) == (value,)
            assert o.convert([None, value]) == (value,)
            assert o.convert([value, None]) == (value,)

    def test_convert_scalar(self):
        """
        Test the `ipalib.parameters.Param._convert_scalar` method.
        """
        dummy = dummy_ugettext()

        # Test with correct type:
        o = self.cls('my_param')
        assert o._convert_scalar(None) is None
        assert dummy.called() is False
        # Test with incorrect type
        e = raises(errors.ConversionError, o._convert_scalar, 'hello', index=17)

    def test_validate(self):
        """
        Test the `ipalib.parameters.Param.validate` method.
        """

        # Test in default state (with no rules, no kwarg):
        o = self.cls('my_param')
        e = raises(errors.RequirementError, o.validate, None, 'cli')
        assert e.name == 'my_param'

        # Test in default state that cli_name gets returned in the exception
        # when context == 'cli'
        o = self.cls('my_param', cli_name='short')
        e = raises(errors.RequirementError, o.validate, None, 'cli')
        assert e.name == 'short'

        # Test with required=False
        o = self.cls('my_param', required=False)
        assert o.required is False
        assert o.validate(None, 'cli') is None

        # Test with query=True:
        o = self.cls('my_param', query=True)
        assert o.query is True
        e = raises(errors.RequirementError, o.validate, None, 'cli')
        assert_equal(e.name, 'my_param')

        # Test with multivalue=True:
        o = self.cls('my_param', multivalue=True)
        e = raises(TypeError, o.validate, [], 'cli')
        assert str(e) == TYPE_ERROR % ('value', tuple, [], list)
        e = raises(ValueError, o.validate, tuple(), 'cli')
        assert str(e) == 'value: empty tuple must be converted to None'

        # Test with wrong (scalar) type:
        e = raises(ValidationError, o.validate, (None, None, 42, None), 'cli')
        assert str(e) == 'invalid %s' % (TYPE_ERROR % ('\'my_param\'', NoneType, 42, int))
        o = self.cls('my_param')
        e = raises(ValidationError, o.validate, 'Hello', 'cli')
        assert str(e) == 'invalid %s' % (TYPE_ERROR % ('\'my_param\'', NoneType, 'Hello', str))

        class Example(self.cls):
            type = int

        # Test with some rules and multivalue=False
        pass1 = DummyRule()
        pass2 = DummyRule()
        fail = DummyRule(u'no good')
        o = Example('example', pass1, pass2)
        assert o.multivalue is False
        assert o.validate(11, 'cli') is None
        assert pass1.calls == [(text.ugettext, 11)]
        assert pass2.calls == [(text.ugettext, 11)]
        pass1.reset()
        pass2.reset()
        o = Example('example', pass1, pass2, fail)
        e = raises(errors.ValidationError, o.validate, 42, 'cli')
        assert e.name == 'example'
        assert e.error == u'no good'
        assert e.index is None
        assert pass1.calls == [(text.ugettext, 42)]
        assert pass2.calls == [(text.ugettext, 42)]
        assert fail.calls == [(text.ugettext, 42)]

        # Test with some rules and multivalue=True
        pass1 = DummyRule()
        pass2 = DummyRule()
        fail = DummyRule(u'this one is not good')
        o = Example('example', pass1, pass2, multivalue=True)
        assert o.multivalue is True
        assert o.validate((3, 9), 'cli') is None
        assert pass1.calls == [
            (text.ugettext, 3),
            (text.ugettext, 9),
        ]
        assert pass2.calls == [
            (text.ugettext, 3),
            (text.ugettext, 9),
        ]
        pass1.reset()
        pass2.reset()
        o = Example('multi_example', pass1, pass2, fail, multivalue=True)
        assert o.multivalue is True
        e = raises(errors.ValidationError, o.validate, (3, 9), 'cli')
        assert e.name == 'multi_example'
        assert e.error == u'this one is not good'
        assert e.index == 0
        assert pass1.calls == [(text.ugettext, 3)]
        assert pass2.calls == [(text.ugettext, 3)]
        assert fail.calls == [(text.ugettext, 3)]

    def test_validate_scalar(self):
        """
        Test the `ipalib.parameters.Param._validate_scalar` method.
        """
        class MyParam(self.cls):
            type = bool
        okay = DummyRule()
        o = MyParam('my_param', okay)

        # Test that TypeError is appropriately raised:
        e = raises(ValidationError, o._validate_scalar, 0)
        assert str(e) == 'invalid %s' % (TYPE_ERROR % ('\'my_param\'', bool, 0, int))
        e = raises(ValidationError, o._validate_scalar, 'Hi', index=4)
        assert str(e) == 'invalid %s' % (TYPE_ERROR % ('\'my_param\'', bool, 'Hi', str))
        e = raises(TypeError, o._validate_scalar, True, index=3.0)
        assert str(e) == TYPE_ERROR % ('index', int, 3.0, float)

        # Test with passing rule:
        assert o._validate_scalar(True, index=None) is None
        assert o._validate_scalar(False, index=None) is None
        assert okay.calls == [
            (text.ugettext, True),
            (text.ugettext, False),
        ]

        # Test with a failing rule:
        okay = DummyRule()
        fail = DummyRule(u'this describes the error')
        o = MyParam('my_param', okay, fail)
        e = raises(errors.ValidationError, o._validate_scalar, True)
        assert e.name == 'my_param'
        assert e.error == u'this describes the error'
        assert e.index is None
        e = raises(errors.ValidationError, o._validate_scalar, False, index=2)
        assert e.name == 'my_param'
        assert e.error == u'this describes the error'
        assert e.index == 2
        assert okay.calls == [
            (text.ugettext, True),
            (text.ugettext, False),
        ]
        assert fail.calls == [
            (text.ugettext, True),
            (text.ugettext, False),
        ]

    def test_get_default(self):
        """
        Test the `ipalib.parameters.Param.get_default` method.
        """
        class PassThrough(object):
            value = None

            def __call__(self, value):
                assert self.value is None
                assert value is not None
                self.value = value
                return value

            def reset(self):
                assert self.value is not None
                self.value = None

        class Str(self.cls):
            type = unicode

            def __init__(self, name, **kw):
                self._convert_scalar = PassThrough()
                super(Str, self).__init__(name, **kw)

        # Test with only a static default:
        o = Str('my_str',
            normalizer=PassThrough(),
            default=u'Static Default',
        )
        assert_equal(o.get_default(), u'Static Default')
        assert o._convert_scalar.value is None
        assert o.normalizer.value is None

        # Test with default_from:
        o = Str('my_str',
            normalizer=PassThrough(),
            default=u'Static Default',
            default_from=lambda first, last: first[0] + last,
        )
        assert_equal(o.get_default(), u'Static Default')
        assert o._convert_scalar.value is None
        assert o.normalizer.value is None
        default = o.get_default(first=u'john', last='doe')
        assert_equal(default, u'jdoe')
        assert o._convert_scalar.value is default
        assert o.normalizer.value is default

    def test_split_csv(self):
        """
        Test the `ipalib.parameters.Param.split_csv` method with csv.
        """
        o = self.cls('my_list+', csv=True)
        n = o.split_csv('a,b')
        assert type(n) is tuple
        assert len(n) is 2

        n = o.split_csv('bar,   "hi, there",foo')
        assert type(n) is tuple
        assert len(n) is 3

    def test_split_csv_separator(self):
        """
        Test the `ipalib.parameters.Param.split_csv` method with csv and a separator.
        """
        o = self.cls('my_list+', csv=True, csv_separator='|')

        n = o.split_csv('a')
        assert type(n) is tuple
        assert len(n) is 1

        n = o.split_csv('a|b')
        assert type(n) is tuple
        assert len(n) is 2

    def test_split_csv_skipspace(self):
        """
        Test the `ipalib.parameters.Param.split_csv` method with csv without skipping spaces.
        """
        o = self.cls('my_list+', csv=True, csv_skipspace=False)

        n = o.split_csv('a')
        assert type(n) is tuple
        assert len(n) is 1

        n = o.split_csv('a, "b,c", d')
        assert type(n) is tuple
        # the output w/o skipspace is ['a',' "b','c"',' d']
        assert len(n) is 4


class test_Flag(ClassChecker):
    """
    Test the `ipalib.parameters.Flag` class.
    """
    _cls = parameters.Flag

    def test_init(self):
        """
        Test the `ipalib.parameters.Flag.__init__` method.
        """
        # Test with no kwargs:
        o = self.cls('my_flag')
        assert o.type is bool
        assert isinstance(o, parameters.Bool)
        assert o.autofill is True
        assert o.default is False

        # Test that TypeError is raise if default is not a bool:
        e = raises(TypeError, self.cls, 'my_flag', default=None)
        assert str(e) == TYPE_ERROR % ('default', bool, None, NoneType)

        # Test with autofill=False, default=True
        o = self.cls('my_flag', autofill=False, default=True)
        assert o.autofill is True
        assert o.default is True

        # Test when cloning:
        orig = self.cls('my_flag')
        for clone in [orig.clone(), orig.clone(autofill=False)]:
            assert clone.autofill is True
            assert clone.default is False
            assert clone is not orig
            assert type(clone) is self.cls

        # Test when cloning with default=True/False
        orig = self.cls('my_flag')
        assert orig.clone().default is False
        assert orig.clone(default=True).default is True
        orig = self.cls('my_flag', default=True)
        assert orig.clone().default is True
        assert orig.clone(default=False).default is False


class test_Data(ClassChecker):
    """
    Test the `ipalib.parameters.Data` class.
    """
    _cls = parameters.Data

    def test_init(self):
        """
        Test the `ipalib.parameters.Data.__init__` method.
        """
        o = self.cls('my_data')
        assert o.type is NoneType
        assert o.password is False
        assert o.rules == tuple()
        assert o.class_rules == tuple()
        assert o.all_rules == tuple()
        assert o.minlength is None
        assert o.maxlength is None
        assert o.length is None
        assert o.pattern is None

        # Test mixing length with minlength or maxlength:
        o = self.cls('my_data', length=5)
        assert o.length == 5
        permutations = [
            dict(minlength=3),
            dict(maxlength=7),
            dict(minlength=3, maxlength=7),
        ]
        for kw in permutations:
            o = self.cls('my_data', **kw)
            for (key, value) in kw.iteritems():
                assert getattr(o, key) == value
            e = raises(ValueError, self.cls, 'my_data', length=5, **kw)
            assert str(e) == \
                "Data('my_data'): cannot mix length with minlength or maxlength"

        # Test when minlength or maxlength are less than 1:
        e = raises(ValueError, self.cls, 'my_data', minlength=0)
        assert str(e) == "Data('my_data'): minlength must be >= 1; got 0"
        e = raises(ValueError, self.cls, 'my_data', maxlength=0)
        assert str(e) == "Data('my_data'): maxlength must be >= 1; got 0"

        # Test when minlength > maxlength:
        e = raises(ValueError, self.cls, 'my_data', minlength=22, maxlength=15)
        assert str(e) == \
            "Data('my_data'): minlength > maxlength (minlength=22, maxlength=15)"

        # Test when minlength == maxlength
        e = raises(ValueError, self.cls, 'my_data', minlength=7, maxlength=7)
        assert str(e) == \
            "Data('my_data'): minlength == maxlength; use length=7 instead"


class test_Bytes(ClassChecker):
    """
    Test the `ipalib.parameters.Bytes` class.
    """
    _cls = parameters.Bytes

    def test_init(self):
        """
        Test the `ipalib.parameters.Bytes.__init__` method.
        """
        o = self.cls('my_bytes')
        assert o.type is str
        assert o.password is False
        assert o.rules == tuple()
        assert o.class_rules == tuple()
        assert o.all_rules == tuple()
        assert o.minlength is None
        assert o.maxlength is None
        assert o.length is None
        assert o.pattern is None
        assert o.re is None

        # Test mixing length with minlength or maxlength:
        o = self.cls('my_bytes', length=5)
        assert o.length == 5
        assert len(o.class_rules) == 1
        assert len(o.rules) == 0
        assert len(o.all_rules) == 1
        permutations = [
            dict(minlength=3),
            dict(maxlength=7),
            dict(minlength=3, maxlength=7),
        ]
        for kw in permutations:
            o = self.cls('my_bytes', **kw)
            assert len(o.class_rules) == len(kw)
            assert len(o.rules) == 0
            assert len(o.all_rules) == len(kw)
            for (key, value) in kw.iteritems():
                assert getattr(o, key) == value
            e = raises(ValueError, self.cls, 'my_bytes', length=5, **kw)
            assert str(e) == \
                "Bytes('my_bytes'): cannot mix length with minlength or maxlength"

        # Test when minlength or maxlength are less than 1:
        e = raises(ValueError, self.cls, 'my_bytes', minlength=0)
        assert str(e) == "Bytes('my_bytes'): minlength must be >= 1; got 0"
        e = raises(ValueError, self.cls, 'my_bytes', maxlength=0)
        assert str(e) == "Bytes('my_bytes'): maxlength must be >= 1; got 0"

        # Test when minlength > maxlength:
        e = raises(ValueError, self.cls, 'my_bytes', minlength=22, maxlength=15)
        assert str(e) == \
            "Bytes('my_bytes'): minlength > maxlength (minlength=22, maxlength=15)"

        # Test when minlength == maxlength
        e = raises(ValueError, self.cls, 'my_bytes', minlength=7, maxlength=7)
        assert str(e) == \
            "Bytes('my_bytes'): minlength == maxlength; use length=7 instead"

    def test_rule_minlength(self):
        """
        Test the `ipalib.parameters.Bytes._rule_minlength` method.
        """
        o = self.cls('my_bytes', minlength=3)
        assert o.minlength == 3
        rule = o._rule_minlength
        translation = u'minlength=%(minlength)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in ('abc', 'four', '12345'):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in ('', 'a', '12'):
            assert_equal(
                rule(dummy, value),
                translation % dict(minlength=3)
            )
            assert dummy.message == 'must be at least %(minlength)d bytes'
            assert dummy.called() is True
            dummy.reset()

    def test_rule_maxlength(self):
        """
        Test the `ipalib.parameters.Bytes._rule_maxlength` method.
        """
        o = self.cls('my_bytes', maxlength=4)
        assert o.maxlength == 4
        rule = o._rule_maxlength
        translation = u'maxlength=%(maxlength)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in ('ab', '123', 'four'):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in ('12345', 'sixsix'):
            assert_equal(
                rule(dummy, value),
                translation % dict(maxlength=4)
            )
            assert dummy.message == 'can be at most %(maxlength)d bytes'
            assert dummy.called() is True
            dummy.reset()

    def test_rule_length(self):
        """
        Test the `ipalib.parameters.Bytes._rule_length` method.
        """
        o = self.cls('my_bytes', length=4)
        assert o.length == 4
        rule = o._rule_length
        translation = u'length=%(length)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in ('1234', 'four'):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in ('ab', '123', '12345', 'sixsix'):
            assert_equal(
                rule(dummy, value),
                translation % dict(length=4),
            )
            assert dummy.message == 'must be exactly %(length)d bytes'
            assert dummy.called() is True
            dummy.reset()

    def test_rule_pattern(self):
        """
        Test the `ipalib.parameters.Bytes._rule_pattern` method.
        """
        # Test our assumptions about Python re module and Unicode:
        pat = '\w+$'
        r = re.compile(pat)
        assert r.match('Hello_World') is not None
        assert r.match(utf8_bytes) is None
        assert r.match(binary_bytes) is None

        # Create instance:
        o = self.cls('my_bytes', pattern=pat)
        assert o.pattern is pat
        rule = o._rule_pattern
        translation = u'pattern=%(pattern)r'
        dummy = dummy_ugettext(translation)

        # Test with passing values:
        for value in ('HELLO', 'hello', 'Hello_World'):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in ('Hello!', 'Hello World', utf8_bytes, binary_bytes):
            assert_equal(
                rule(dummy, value),
                translation % dict(pattern=pat),
            )
            assert_equal(dummy.message, 'must match pattern "%(pattern)s"')
            assert dummy.called() is True
            dummy.reset()


class test_Str(ClassChecker):
    """
    Test the `ipalib.parameters.Str` class.
    """
    _cls = parameters.Str

    def test_init(self):
        """
        Test the `ipalib.parameters.Str.__init__` method.
        """
        o = self.cls('my_str')
        assert o.type is unicode
        assert o.password is False
        assert o.minlength is None
        assert o.maxlength is None
        assert o.length is None
        assert o.pattern is None

    def test_convert_scalar(self):
        """
        Test the `ipalib.parameters.Str._convert_scalar` method.
        """
        o = self.cls('my_str')
        mthd = o._convert_scalar
        for value in (u'Hello', 42, 1.2, unicode_str):
            assert mthd(value) == unicode(value)
        bad = [True, 'Hello', dict(one=1), utf8_bytes]
        for value in bad:
            e = raises(errors.ConversionError, mthd, value)
            assert e.name == 'my_str'
            assert e.index is None
            assert_equal(unicode(e.error), u'must be Unicode text')
            e = raises(errors.ConversionError, mthd, value, index=18)
            assert e.name == 'my_str'
            assert e.index == 18
            assert_equal(unicode(e.error), u'must be Unicode text')
        bad = [(u'Hello',), [42.3]]
        for value in bad:
            e = raises(errors.ConversionError, mthd, value)
            assert e.name == 'my_str'
            assert e.index is None
            assert_equal(unicode(e.error), u'Only one value is allowed')
        assert o.convert(None) is None

    def test_rule_minlength(self):
        """
        Test the `ipalib.parameters.Str._rule_minlength` method.
        """
        o = self.cls('my_str', minlength=3)
        assert o.minlength == 3
        rule = o._rule_minlength
        translation = u'minlength=%(minlength)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in (u'abc', u'four', u'12345'):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in (u'', u'a', u'12'):
            assert_equal(
                rule(dummy, value),
                translation % dict(minlength=3)
            )
            assert dummy.message == 'must be at least %(minlength)d characters'
            assert dummy.called() is True
            dummy.reset()

    def test_rule_maxlength(self):
        """
        Test the `ipalib.parameters.Str._rule_maxlength` method.
        """
        o = self.cls('my_str', maxlength=4)
        assert o.maxlength == 4
        rule = o._rule_maxlength
        translation = u'maxlength=%(maxlength)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in (u'ab', u'123', u'four'):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in (u'12345', u'sixsix'):
            assert_equal(
                rule(dummy, value),
                translation % dict(maxlength=4)
            )
            assert dummy.message == 'can be at most %(maxlength)d characters'
            assert dummy.called() is True
            dummy.reset()

    def test_rule_length(self):
        """
        Test the `ipalib.parameters.Str._rule_length` method.
        """
        o = self.cls('my_str', length=4)
        assert o.length == 4
        rule = o._rule_length
        translation = u'length=%(length)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in (u'1234', u'four'):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in (u'ab', u'123', u'12345', u'sixsix'):
            assert_equal(
                rule(dummy, value),
                translation % dict(length=4),
            )
            assert dummy.message == 'must be exactly %(length)d characters'
            assert dummy.called() is True
            dummy.reset()

    def test_rule_pattern(self):
        """
        Test the `ipalib.parameters.Str._rule_pattern` method.
        """
        # Test our assumptions about Python re module and Unicode:
        pat = '\w{5}$'
        r1 = re.compile(pat)
        r2 = re.compile(pat, re.UNICODE)
        assert r1.match(unicode_str) is None
        assert r2.match(unicode_str) is not None

        # Create instance:
        o = self.cls('my_str', pattern=pat)
        assert o.pattern is pat
        rule = o._rule_pattern
        translation = u'pattern=%(pattern)r'
        dummy = dummy_ugettext(translation)

        # Test with passing values:
        for value in (u'HELLO', u'hello', unicode_str):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in (u'H LLO', u'***lo', unicode_str + unicode_str):
            assert_equal(
                rule(dummy, value),
                translation % dict(pattern=pat),
            )
            assert_equal(dummy.message, 'must match pattern "%(pattern)s"')
            assert dummy.called() is True
            dummy.reset()


class test_Password(ClassChecker):
    """
    Test the `ipalib.parameters.Password` class.
    """
    _cls = parameters.Password

    def test_init(self):
        """
        Test the `ipalib.parameters.Password.__init__` method.
        """
        o = self.cls('my_password')
        assert o.type is unicode
        assert o.minlength is None
        assert o.maxlength is None
        assert o.length is None
        assert o.pattern is None
        assert o.password is True

    def test_convert_scalar(self):
        """
        Test the `ipalib.parameters.Password._convert_scalar` method.
        """
        o = self.cls('my_password')
        e = raises(errors.PasswordMismatch, o._convert_scalar, [u'one', u'two'])
        assert e.name == 'my_password'
        assert e.index is None
        assert o._convert_scalar([u'one', u'one']) == u'one'
        assert o._convert_scalar(u'one') == u'one'


class test_StrEnum(ClassChecker):
    """
    Test the `ipalib.parameters.StrEnum` class.
    """
    _cls = parameters.StrEnum

    def test_init(self):
        """
        Test the `ipalib.parameters.StrEnum.__init__` method.
        """
        values = (u'Hello', u'naughty', u'nurse!')
        o = self.cls('my_strenum', values=values)
        assert o.type is unicode
        assert o.values is values
        assert o.class_rules == (o._rule_values,)
        assert o.rules == tuple()
        assert o.all_rules == (o._rule_values,)

        badvalues = (u'Hello', 'naughty', u'nurse!')
        e = raises(TypeError, self.cls, 'my_enum', values=badvalues)
        assert str(e) == TYPE_ERROR % (
            "StrEnum('my_enum') values[1]", unicode, 'naughty', str
        )

        # Test that ValueError is raised when list of values is empty
        badvalues = tuple()
        e = raises(ValueError, self.cls, 'empty_enum', values=badvalues)
        assert_equal(str(e), "StrEnum('empty_enum'): list of values must not "
                "be empty")

    def test_rules_values(self):
        """
        Test the `ipalib.parameters.StrEnum._rule_values` method.
        """
        values = (u'Hello', u'naughty', u'nurse!')
        o = self.cls('my_enum', values=values)
        rule = o._rule_values
        translation = u"values='Hello', 'naughty', 'nurse!'"
        dummy = dummy_ugettext(translation)

        # Test with passing values:
        for v in values:
            assert rule(dummy, v) is None
            assert dummy.called() is False

        # Test with failing values:
        for val in (u'Howdy', u'quiet', u'library!'):
            assert_equal(
                rule(dummy, val),
                translation % dict(values=values),
            )
            assert_equal(dummy.message, "must be one of %(values)s")
            dummy.reset()

        # test a special case when we have just one allowed value
        values = (u'Hello', )
        o = self.cls('my_enum', values=values)
        rule = o._rule_values
        translation = u"value='Hello'"
        dummy = dummy_ugettext(translation)

        for val in (u'Howdy', u'quiet', u'library!'):
            assert_equal(
                rule(dummy, val),
                translation % dict(values=values),
            )
            assert_equal(dummy.message, "must be '%(value)s'")
            dummy.reset()


class test_Number(ClassChecker):
    """
    Test the `ipalib.parameters.Number` class.
    """
    _cls = parameters.Number

    def test_init(self):
        """
        Test the `ipalib.parameters.Number.__init__` method.
        """
        o = self.cls('my_number')
        assert o.type is NoneType
        assert o.password is False
        assert o.rules == tuple()
        assert o.class_rules == tuple()
        assert o.all_rules == tuple()



class test_Int(ClassChecker):
    """
    Test the `ipalib.parameters.Int` class.
    """
    _cls = parameters.Int

    def test_init(self):
        """
        Test the `ipalib.parameters.Int.__init__` method.
        """
        # Test with no kwargs:
        o = self.cls('my_number')
        assert o.type is int
        assert isinstance(o, parameters.Int)
        assert o.minvalue == int(MININT)
        assert o.maxvalue == int(MAXINT)

        # Test when min > max:
        e = raises(ValueError, self.cls, 'my_number', minvalue=22, maxvalue=15)
        assert str(e) == \
            "Int('my_number'): minvalue > maxvalue (minvalue=22, maxvalue=15)"

    def test_rule_minvalue(self):
        """
        Test the `ipalib.parameters.Int._rule_minvalue` method.
        """
        o = self.cls('my_number', minvalue=3)
        assert o.minvalue == 3
        rule = o._rule_minvalue
        translation = u'minvalue=%(minvalue)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in (4, 99, 1001):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in (-1, 0, 2):
            assert_equal(
                rule(dummy, value),
                translation % dict(minvalue=3)
            )
            assert dummy.message == 'must be at least %(minvalue)d'
            assert dummy.called() is True
            dummy.reset()

    def test_rule_maxvalue(self):
        """
        Test the `ipalib.parameters.Int._rule_maxvalue` method.
        """
        o = self.cls('my_number', maxvalue=4)
        assert o.maxvalue == 4
        rule = o._rule_maxvalue
        translation = u'maxvalue=%(maxvalue)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in (-1, 0, 4):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in (5, 99, 1009):
            assert_equal(
                rule(dummy, value),
                translation % dict(maxvalue=4)
            )
            assert dummy.message == 'can be at most %(maxvalue)d'
            assert dummy.called() is True
            dummy.reset()

    def test_convert_scalar(self):
        """
        Test the `ipalib.parameters.Int._convert_scalar` method.
        Assure radix prefixes work, str objects fail,
        floats (native & string) are truncated,
        large magnitude values are promoted to long,
        empty strings & invalid numerical representations fail
        """
        o = self.cls('my_number')
        # Assure invalid inputs raise error
        for bad in ['hello', u'hello', True, None, u'', u'.']:
            e = raises(errors.ConversionError, o._convert_scalar, bad)
            assert e.name == 'my_number'
            assert e.index is None
        # Assure large magnatude values are handled correctly
        assert type(o._convert_scalar(sys.maxint*2)) == long
        assert o._convert_scalar(sys.maxint*2) == sys.maxint*2
        assert o._convert_scalar(unicode(sys.maxint*2)) == sys.maxint*2
        assert o._convert_scalar(long(16)) == 16
        # Assure normal conversions produce expected result
        assert o._convert_scalar(u'16.99') == 16
        assert o._convert_scalar(16.99)    == 16
        assert o._convert_scalar(u'16')    == 16
        assert o._convert_scalar(u'0x10')  == 16
        assert o._convert_scalar(u'020')   == 16

class test_Decimal(ClassChecker):
    """
    Test the `ipalib.parameters.Decimal` class.
    """
    _cls = parameters.Decimal

    def test_init(self):
        """
        Test the `ipalib.parameters.Decimal.__init__` method.
        """
        # Test with no kwargs:
        o = self.cls('my_number')
        assert o.type is Decimal
        assert isinstance(o, parameters.Decimal)
        assert o.minvalue is None
        assert o.maxvalue is None

        # Test when min > max:
        e = raises(ValueError, self.cls, 'my_number', minvalue=Decimal('22.5'), maxvalue=Decimal('15.1'))
        assert str(e) == \
            "Decimal('my_number'): minvalue > maxvalue (minvalue=22.5, maxvalue=15.1)"

    def test_rule_minvalue(self):
        """
        Test the `ipalib.parameters.Decimal._rule_minvalue` method.
        """
        o = self.cls('my_number', minvalue='3.1')
        assert o.minvalue == Decimal('3.1')
        rule = o._rule_minvalue
        translation = u'minvalue=%(minvalue)s'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in (Decimal('3.2'), Decimal('99.0')):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in (Decimal('-1.2'), Decimal('0.0'), Decimal('3.0')):
            assert_equal(
                rule(dummy, value),
                translation % dict(minvalue=Decimal('3.1'))
            )
            assert dummy.message == 'must be at least %(minvalue)s'
            assert dummy.called() is True
            dummy.reset()

    def test_rule_maxvalue(self):
        """
        Test the `ipalib.parameters.Decimal._rule_maxvalue` method.
        """
        o = self.cls('my_number', maxvalue='4.7')
        assert o.maxvalue == Decimal('4.7')
        rule = o._rule_maxvalue
        translation = u'maxvalue=%(maxvalue)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation

        # Test with passing values:
        for value in (Decimal('-1.0'), Decimal('0.1'), Decimal('4.2')):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # Test with failing values:
        for value in (Decimal('5.3'), Decimal('99.9')):
            assert_equal(
                rule(dummy, value),
                translation % dict(maxvalue=Decimal('4.7'))
            )
            assert dummy.message == 'can be at most %(maxvalue)s'
            assert dummy.called() is True
            dummy.reset()

    def test_precision(self):
        """
        Test the `ipalib.parameters.Decimal` precision attribute
        """
        # precission is None
        param = self.cls('my_number')

        for value in (Decimal('0'), Decimal('4.4'), Decimal('4.67')):
            assert_equal(
                param(value),
                value)

        # precision is 0
        param = self.cls('my_number', precision=0)
        for original,expected in ((Decimal('0'), '0'),
                                  (Decimal('1.1'), '1'),
                                  (Decimal('4.67'), '5')):
            assert_equal(
                str(param(original)),
                expected)

        # precision is 1
        param = self.cls('my_number', precision=1)
        for original,expected in ((Decimal('0'), '0.0'),
                                  (Decimal('1.1'), '1.1'),
                                  (Decimal('4.67'), '4.7')):
            assert_equal(
                str(param(original)),
                expected)

        # value has too many digits
        param = self.cls('my_number', precision=1)
        e = raises(ConversionError, param, '123456789012345678901234567890')

        assert str(e) == \
        "invalid 'my_number': quantize result has too many digits for current context"

    def test_exponential(self):
        """
        Test the `ipalib.parameters.Decimal` exponential attribute
        """
        param = self.cls('my_number', exponential=True)
        for original,expected in ((Decimal('0'), '0'),
                                  (Decimal('1E3'), '1E+3'),
                                  (Decimal('3.4E2'), '3.4E+2')):
            assert_equal(
                str(param(original)),
                expected)


        param = self.cls('my_number', exponential=False)
        for original,expected in ((Decimal('0'), '0'),
                                  (Decimal('1E3'), '1000'),
                                  (Decimal('3.4E2'), '340')):
            assert_equal(
                str(param(original)),
                expected)

    def test_numberclass(self):
        """
        Test the `ipalib.parameters.Decimal` numberclass attribute
        """
        # test default value: '-Normal', '+Zero', '+Normal'
        param = self.cls('my_number')
        for value,raises_verror in ((Decimal('0'), False),
                                    (Decimal('-0'), True),
                                    (Decimal('1E8'), False),
                                    (Decimal('-1.1'), False),
                                    (Decimal('-Infinity'), True),
                                    (Decimal('+Infinity'), True),
                                    (Decimal('NaN'), True)):
            if raises_verror:
                raises(ValidationError, param, value)
            else:
                param(value)


        param = self.cls('my_number', exponential=True,
                numberclass=('-Normal', '+Zero', '+Infinity'))
        for value,raises_verror in ((Decimal('0'), False),
                                    (Decimal('-0'), True),
                                    (Decimal('1E8'), True),
                                    (Decimal('-1.1'), False),
                                    (Decimal('-Infinity'), True),
                                    (Decimal('+Infinity'), False),
                                    (Decimal('NaN'), True)):
            if raises_verror:
                raises(ValidationError, param, value)
            else:
                param(value)

class test_AccessTime(ClassChecker):
    """
    Test the `ipalib.parameters.AccessTime` class.
    """
    _cls = parameters.AccessTime

    def test_init(self):
        """
        Test the `ipalib.parameters.AccessTime.__init__` method.
        """
        # Test with no kwargs:
        o = self.cls('my_time')
        assert o.type is unicode
        assert isinstance(o, parameters.AccessTime)
        assert o.multivalue is False
        translation = u'length=%(length)r'
        dummy = dummy_ugettext(translation)
        assert dummy.translation is translation
        rule = o._rule_required

        # Check some good rules
        for value in (u'absolute 201012161032 ~ 201012161033',
                      u'periodic monthly week 2 day Sat,Sun 0900-1300',
                      u'periodic yearly month 4 day 1-31 0800-1400',
                      u'periodic weekly day 7 0800-1400',
                      u'periodic daily 0800-1400',
            ):
            assert rule(dummy, value) is None
            assert dummy.called() is False

        # And some bad ones
        for value in (u'absolute 201012161032 - 201012161033',
                      u'absolute 201012161032 ~',
                      u'periodic monthly day Sat,Sun 0900-1300',
                      u'periodical yearly month 4 day 1-31 0800-1400',
                      u'periodic weekly day 8 0800-1400',
            ):
            e = raises(ValidationError, o._rule_required, None, value)

def test_create_param():
    """
    Test the `ipalib.parameters.create_param` function.
    """
    f = parameters.create_param

    # Test that Param instances are returned unchanged:
    params = (
        parameters.Param('one?'),
        parameters.Int('two+'),
        parameters.Str('three*'),
        parameters.Bytes('four'),
    )
    for p in params:
        assert f(p) is p

    # Test that the spec creates an Str instance:
    for spec in ('one?', 'two+', 'three*', 'four'):
        (name, kw) = parameters.parse_param_spec(spec)
        p = f(spec)
        assert p.param_spec is spec
        assert p.name == name
        assert p.required is kw['required']
        assert p.multivalue is kw['multivalue']

    # Test that TypeError is raised when spec is neither a Param nor a str:
    for spec in (u'one', 42, parameters.Param, parameters.Str):
        e = raises(TypeError, f, spec)
        assert str(e) == \
            TYPE_ERROR % ('spec', (str, parameters.Param), spec, type(spec))


def test_messages():
    """
    Test module level message in `ipalib.parameters`.
    """
    for name in dir(parameters):
        if name.startswith('_'):
            continue
        attr = getattr(parameters, name)
        if not (isclass(attr) and issubclass(attr, parameters.Param)):
            continue
        assert type(attr.type_error) is str
        assert attr.type_error in parameters.__messages


class test_IA5Str(ClassChecker):
    """
    Test the `ipalib.parameters.IA5Str` class.
    """
    _cls = parameters.IA5Str

    def test_convert_scalar(self):
        """
        Test the `ipalib.parameters.IA5Str._convert_scalar` method.
        """
        o = self.cls('my_str')
        mthd = o._convert_scalar
        for value in (u'Hello', 42, 1.2):
            assert mthd(value) == unicode(value)
        bad = ['Helloá']
        for value in bad:
            e = raises(errors.ConversionError, mthd, value)
            assert e.name == 'my_str'
            assert e.index is None
            assert_equal(e.error, "The character \''\\xc3'\' is not allowed.")
