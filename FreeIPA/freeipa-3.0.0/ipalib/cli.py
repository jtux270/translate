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
Functionality for Command Line Interface.
"""

import re
import textwrap
import sys
import getpass
import code
import optparse
import socket
import fcntl
import termios
import struct
import base64
try:
    #pylint: disable=F0401
    import default_encoding_utf8
except ImportError:
    # This is a chicken-and-egg problem. The api can't be imported unless
    # this is already installed and since it is installed with IPA therein
    # lies the problem. Skip it for now so ipalib can be imported in-tree
    # even in cases that IPA isn't installed on the dev machine.
    pass

import frontend
import backend
import plugable
import util
from errors import (PublicError, CommandError, HelpError, InternalError,
        NoSuchNamespaceError, ValidationError, NotFound, NotConfiguredError,
        PromptFailed, ConversionError)
from constants import CLI_TAB
from parameters import Password, Bytes, File, Str, StrEnum
from text import _
from ipapython.version import API_VERSION


def to_cli(name):
    """
    Takes a Python identifier and transforms it into form suitable for the
    Command Line Interface.
    """
    assert isinstance(name, str)
    return name.replace('_', '-')


def from_cli(cli_name):
    """
    Takes a string from the Command Line Interface and transforms it into a
    Python identifier.
    """
    return str(cli_name).replace('-', '_')


class textui(backend.Backend):
    """
    Backend plugin to nicely format output to stdout.
    """

    def get_tty_width(self):
        """
        Return the width (in characters) of output tty.

        If stdout is not a tty, this method will return ``None``.
        """
        # /usr/include/asm/termios.h says that struct winsize has four
        # unsigned shorts, hence the HHHH
        if sys.stdout.isatty():
            try:
                winsize = fcntl.ioctl(sys.stdout, termios.TIOCGWINSZ,
                                      struct.pack('HHHH', 0, 0, 0, 0))
                return struct.unpack('HHHH', winsize)[1]
            except IOError:
                return None

    def max_col_width(self, rows, col=None):
        """
        Return the max width (in characters) of a specified column.

        For example:

        >>> ui = textui()
        >>> rows = [
        ...     ('a', 'package'),
        ...     ('an', 'egg'),
        ... ]
        >>> ui.max_col_width(rows, col=0)  # len('an')
        2
        >>> ui.max_col_width(rows, col=1)  # len('package')
        7
        >>> ui.max_col_width(['a', 'cherry', 'py'])  # len('cherry')
        6
        """
        if type(rows) not in (list, tuple):
            raise TypeError(
                'rows: need %r or %r; got %r' % (list, tuple, rows)
            )
        if len(rows) == 0:
            return 0
        if col is None:
            return max(len(row) for row in rows)
        return max(len(row[col]) for row in rows)

    def __get_encoding(self, stream):
        assert stream in (sys.stdin, sys.stdout)
        if getattr(stream, 'encoding', None) is None:
            return 'UTF-8'
        return stream.encoding

    def decode(self, value):
        """
        Decode text from stdin.
        """
        if type(value) is str:
            encoding = self.__get_encoding(sys.stdin)
            return value.decode(encoding)
        elif type(value) in (list, tuple):
            return tuple(self.decode(v) for v in value)
        return value

    def encode(self, unicode_text):
        """
        Encode text for output to stdout.
        """
        assert type(unicode_text) is unicode
        encoding = self.__get_encoding(sys.stdout)
        return unicode_text.encode(encoding)

    def choose_number(self, n, singular, plural=None):
        if n == 1 or plural is None:
            return singular % n
        return plural % n

    def encode_binary(self, value):
        """
        Convert a binary value to base64. We know a value is binary
        if it is a python str type, otherwise it is a plain string.
        """
        if type(value) is str:
            return base64.b64encode(value)
        else:
            return value

    def print_plain(self, string):
        """
        Print exactly like ``print`` statement would.
        """
        print unicode(string)

    def print_line(self, text, width=None):
        """
        Force printing on a single line, using ellipsis if needed.

        For example:

        >>> ui = textui()
        >>> ui.print_line('This line can fit!', width=18)
        This line can fit!
        >>> ui.print_line('This line wont quite fit!', width=18)
        This line wont ...

        The above example aside, you normally should not specify the
        ``width``.  When you don't, it is automatically determined by calling
        `textui.get_tty_width()`.
        """
        if width is None:
            width = self.get_tty_width()
        if width is not None and width < len(text):
            text = text[:width - 3] + '...'
        print unicode(text)

    def print_paragraph(self, text, width=None):
        """
        Print a paragraph, automatically word-wrapping to tty width.

        For example:

        >>> text = '''
        ... Python is a dynamic object-oriented programming language that can
        ... be used for many kinds of software development.
        ... '''
        >>> ui = textui()
        >>> ui.print_paragraph(text, width=45)
        Python is a dynamic object-oriented
        programming language that can be used for
        many kinds of software development.

        The above example aside, you normally should not specify the
        ``width``.  When you don't, it is automatically determined by calling
        `textui.get_tty_width()`.

        The word-wrapping is done using the Python ``textwrap`` module.  See:

            http://docs.python.org/library/textwrap.html
        """
        if width is None:
            width = self.get_tty_width()
        for line in textwrap.wrap(text.strip(), width):
            print line

    def print_indented(self, text, indent=1):
        """
        Print at specified indentation level.

        For example:

        >>> ui = textui()
        >>> ui.print_indented('One indentation level.')
          One indentation level.
        >>> ui.print_indented('Two indentation levels.', indent=2)
            Two indentation levels.
        >>> ui.print_indented('No indentation.', indent=0)
        No indentation.
        """
        print (CLI_TAB * indent + text)

    def print_keyval(self, rows, indent=1):
        """
        Print (key = value) pairs, one pair per line.

        For example:

        >>> items = [
        ...     ('in_server', True),
        ...     ('mode', u'production'),
        ... ]
        >>> ui = textui()
        >>> ui.print_keyval(items)
          in_server = True
          mode = u'production'
        >>> ui.print_keyval(items, indent=0)
        in_server = True
        mode = u'production'

        Also see `textui.print_indented`.
        """
        for (key, value) in rows:
            self.print_indented('%s = %r' % (key, self.encode_binary(value)), indent)

    def print_attribute(self, attr, value, format='%s: %s', indent=1, one_value_per_line=True):
        """
        Print an ldap attribute.

        For example:

        >>> attr = 'dn'
        >>> ui = textui()
        >>> ui.print_attribute(attr, u'dc=example,dc=com')
          dn: dc=example,dc=com
        >>> attr = 'objectClass'
        >>> ui.print_attribute(attr, [u'top', u'someClass'], one_value_per_line=False)
          objectClass: top, someClass
        >>> ui.print_attribute(attr, [u'top', u'someClass'])
          objectClass: top
          objectClass: someClass
        """
        assert isinstance(attr, basestring)
        if not isinstance(value, (list, tuple)):
            # single-value attribute
            self.print_indented(format % (attr, self.encode_binary(value)), indent)
        else:
            # multi-value attribute
            if one_value_per_line:
                for v in value:
                    self.print_indented(format % (attr, self.encode_binary(v)), indent)
            else:
                value = map(lambda v: self.encode_binary(v), value)
                if len(value) > 0 and type(value[0]) in (list, tuple):
                    # This is where we print failed add/remove members
                    for l in value:
                        text = ': '.join(l)
                        self.print_indented(format % (attr, self.encode_binary(text)), indent)
                    return
                else:
                    if len(value) > 0:
                        text = ', '.join(value)
                    else:
                        return
                line_len = self.get_tty_width()
                if line_len and text:
                    s_indent = '%s%s' % (
                        CLI_TAB * indent, ' ' * (len(attr) + 2)
                    )
                    line_len -= len(s_indent)
                    text = textwrap.wrap(
                        text, line_len, break_long_words=False
                    )
                    if len(text) == 0:
                        text = [u'']
                else:
                    text = [text]
                self.print_indented(format % (attr, text[0]), indent)
                for line in text[1:]:
                    self.print_plain('%s%s' % (s_indent, line))

    def print_entry1(self, entry, indent=1, attr_map={}, attr_order=['dn'],
            one_value_per_line=True):
        """
        Print an ldap entry dict.
        """
        assert isinstance(entry, dict)
        assert isinstance(attr_map, dict)
        assert isinstance(attr_order, (list, tuple))

        def print_attr(a):
            if attr in attr_map:
                self.print_attribute(
                    attr_map[attr], entry[attr], indent=indent, one_value_per_line=one_value_per_line
                )
            else:
                self.print_attribute(
                    attr, entry[attr], indent=indent, one_value_per_line=one_value_per_line
                )

        for attr in attr_order:
            if attr in entry:
                print_attr(attr)
                del entry[attr]
        for attr in sorted(entry):
            print_attr(attr)

    def print_entries(self, entries, order=None, labels=None, flags=None, print_all=True, format='%s: %s', indent=1):
        assert isinstance(entries, (list, tuple))
        first = True
        for entry in entries:
            if not first:
                print ''
            first = False
            self.print_entry(entry, order, labels, flags, print_all, format, indent)

    def print_entry(self, entry, order=None, labels=None, flags=None, print_all=True, format='%s: %s', indent=1):
        """
        """
        if isinstance(entry, (list, tuple)):
            entry = dict(entry)
        assert isinstance(entry, dict)
        if labels is None:
            labels = dict()
            one_value_per_line = True
        else:
            one_value_per_line = False
        if order is not None:
            for key in order:
                if key not in entry:
                    continue
                label = labels.get(key, key)
                flag = flags.get(key, [])
                value = entry[key]
                if 'suppress_empty' in flag and value in [u'', '', [], None]:
                    continue
                if isinstance(value, dict):
                    if frontend.entry_count(value) == 0:
                        continue
                    self.print_indented(format % (label, ''), indent)
                    self.print_entry(
                        value, order, labels, flags, print_all, format,
                        indent=indent+1
                    )
                else:
                    if isinstance(value, (list, tuple)) and \
                       all(isinstance(val, dict) for val in value):
                        # this is a list of entries (dicts), not values
                        self.print_attribute(label, u'', format, indent)
                        self.print_entries(value, order, labels, flags, print_all,
                                format, indent+1)
                    else:
                        self.print_attribute(
                            label, value, format, indent, one_value_per_line
                        )
                del entry[key]
        if print_all:
            for key in sorted(entry):
                label = labels.get(key, key)
                self.print_attribute(
                    key, entry[key], format, indent, one_value_per_line
                )

    def print_dashed(self, string, above=True, below=True, indent=0, dash='-'):
        """
        Print a string with a dashed line above and/or below.

        For example:

        >>> ui = textui()
        >>> ui.print_dashed('Dashed above and below.')
        -----------------------
        Dashed above and below.
        -----------------------
        >>> ui.print_dashed('Only dashed below.', above=False)
        Only dashed below.
        ------------------
        >>> ui.print_dashed('Only dashed above.', below=False)
        ------------------
        Only dashed above.
        """
        assert isinstance(dash, basestring)
        assert len(dash) == 1
        dashes = dash * len(string)
        if above:
            self.print_indented(dashes, indent)
        self.print_indented(string, indent)
        if below:
            self.print_indented(dashes, indent)

    def print_h1(self, text):
        """
        Print a primary header at indentation level 0.

        For example:

        >>> ui = textui()
        >>> ui.print_h1('A primary header')
        ================
        A primary header
        ================
        """
        self.print_dashed(text, indent=0, dash='=')

    def print_h2(self, text):
        """
        Print a secondary header at indentation level 1.

        For example:

        >>> ui = textui()
        >>> ui.print_h2('A secondary header')
          ------------------
          A secondary header
          ------------------
        """
        self.print_dashed(text, indent=1, dash='-')

    def print_name(self, name):
        """
        Print a command name.

        The typical use for this is to mark the start of output from a
        command.  For example, a hypothetical ``show_status`` command would
        output something like this:

        >>> ui = textui()
        >>> ui.print_name('show_status')
        ------------
        show-status:
        ------------
        """
        self.print_dashed('%s:' % to_cli(name))

    def print_header(self, msg, output):
        self.print_dashed(msg % output)

    def print_summary(self, msg):
        """
        Print a summary at the end of a comand's output.

        For example:

        >>> ui = textui()
        >>> ui.print_summary('Added user "jdoe"')
        -----------------
        Added user "jdoe"
        -----------------
        """
        self.print_dashed(msg)

    def print_count(self, count, singular, plural=None):
        """
        Print a summary count.

        The typical use for this is to print the number of items returned
        by a command, especially when this return count can vary.  This
        preferably should be used as a summary and should be the final text
        a command outputs.

        For example:

        >>> ui = textui()
        >>> ui.print_count(1, '%d goose', '%d geese')
        -------
        1 goose
        -------
        >>> ui.print_count(['Don', 'Sue'], 'Found %d user', 'Found %d users')
        -------------
        Found 2 users
        -------------

        If ``count`` is not an integer, it must be a list or tuple, and then
        ``len(count)`` is used as the count.
        """
        if type(count) is not int:
            assert type(count) in (list, tuple, dict)
            count = len(count)
        self.print_dashed(
            self.choose_number(count, singular, plural)
        )

    def print_error(self, text):
        print '  ** %s **' % unicode(text)

    def prompt_helper(self, prompt, label, prompt_func=raw_input):
        """Prompt user for input

        Handles encoding the prompt and decoding the input.
        On end of stream or ctrl+c, raise PromptFailed.
        """
        try:
            return self.decode(prompt_func(self.encode(prompt)))
        except (KeyboardInterrupt, EOFError):
            print
            raise PromptFailed(name=label)

    def print_prompt_attribute_error(self, attribute, error):
        self.print_plain('>>> %s: %s' % (attribute, error))

    def prompt(self, label, default=None, get_values=None, optional=False):
        """
        Prompt user for input.
        """
        # TODO: Add tab completion using readline
        if optional:
            prompt = u'[%s]' % label
        else:
            prompt = u'%s' % label
        if default is None:
            prompt = u'%s: ' % prompt
        else:
            prompt = u'%s [%s]: ' % (prompt, default)
        return self.prompt_helper(prompt, label)

    def prompt_yesno(self, label, default=None):
        """
        Prompt user for yes/no input. This method returns True/False according
        to user response.

        Parameter "default" should be True, False or None

        If Default parameter is not None, user can enter an empty input instead
        of Yes/No answer. Value passed to Default is returned in that case.

        If Default parameter is None, user is asked for Yes/No answer until
        a correct answer is provided. Answer is then returned.
        """

        default_prompt = None
        if default is not None:
            if default:
                default_prompt = "Yes"
            else:
                default_prompt = "No"

        if default_prompt:
            prompt = u'%s Yes/No (default %s): ' % (label, default_prompt)
        else:
            prompt = u'%s Yes/No: ' % label

        while True:
            data = self.prompt_helper(prompt, label).lower() #pylint: disable=E1103

            if data in (u'yes', u'y'):
                return True
            elif data in ( u'n', u'no'):
                return False
            elif default is not None and data == u'':
                return default

    def prompt_password(self, label, confirm=True):
        """
        Prompt user for a password or read it in via stdin depending
        on whether there is a tty or not.
        """
        if sys.stdin.isatty():
            prompt = u'%s: ' % unicode(label)
            repeat_prompt = unicode(_('Enter %(label)s again to verify: ') % dict(label=label))
            while True:
                pw1 = self.prompt_helper(prompt, label, prompt_func=getpass.getpass)
                if not confirm:
                    return pw1
                pw2 = self.prompt_helper(repeat_prompt, label, prompt_func=getpass.getpass)
                if pw1 == pw2:
                    return pw1
                self.print_error( _('Passwords do not match!'))
        else:
            return self.decode(sys.stdin.readline().strip())

    def select_entry(self, entries, format, attrs, display_count=True):
        """
        Display a list of lines in with formatting defined in ``format``.
        ``attrs`` is a list of attributes in the format.

        Prompt user for a selection and return the value (index of
        ``entries`` -1).

        If only one entry is provided then always return 0.

        Return: 0..n for the index of the selected entry
                -1 if all entries should be displayed
                -2 to quit, no entries to be displayed
        """
        if not self.env.interactive or not sys.stdout.isatty():
            return -1

        counter = len(entries)
        if counter == 0:
            raise NotFound(reason=_("No matching entries found"))

        i = 1
        for e in entries:
            # There is no guarantee that all attrs are in any given
            # entry
            d = {}
            for a in attrs:
                d[a] = e.get(a, '')
            self.print_line("%d: %s" % (i, format % d))
            i = i + 1

        if display_count:
            self.print_count(entries, 'Found %d match', 'Found %d matches')

        while True:
            try:
                resp = self.prompt("Choose one: (1 - %s), a for all, q to quit" % counter)
            except EOFError:
                return -2

            if resp.lower() == "q": #pylint: disable=E1103
                return -2
            if resp.lower() == "a": #pylint: disable=E1103
                return -1
            try:
                selection = int(resp) - 1
                if (selection >= 0 and selection < counter):
                    break
            except:
                # fall through to the error msg
                pass

            self.print_line("Please enter a number between 1 and %s" % counter)

        self.print_line('')
        return selection

class help(frontend.Local):
    """
    Display help for a command or topic.
    """

    takes_args = (Str('command?'),)

    has_output = tuple()

    _PLUGIN_BASE_MODULE = 'ipalib.plugins'

    def _get_command_module(self, module):
        """
        Return last part of ``module`` name, or ``None`` if module is this file.

        For example:
        """
        if module == __name__:
            return
        return module.split('.')[-1]

    def _get_module_topic(self, module_name):
        if not sys.modules[module_name]:
            __import__(module_name)
        module = sys.modules[module_name]

        topic = getattr(module, 'topic', None)
        if topic is None:
            topic = (self._get_command_module(module_name), None)

        return topic

    def _count_topic_mcl(self, topic_name, mod_name):
        mcl = max((self._topics[topic_name][1], len(mod_name)))
        self._topics[topic_name][1] = mcl

    def _on_finalize(self):
        # {topic: ["description", mcl, {"subtopic": ["description", mcl, [commands]]}]}
        # {topic: ["description", mcl, [commands]]}
        self._topics = {}
        # [builtin_commands]
        self._builtins = []

        # build help topics
        for c in self.Command():
            if c.NO_CLI:
                continue

            topic = self._get_module_topic(c.module)
            topic_name = topic[0]

            if topic_name:
                if topic[1] is None: # a module without grouping
                    if topic_name in self._topics:
                        self._topics[topic_name][2].append(c)
                    else:
                        m = '%s.%s' % (self._PLUGIN_BASE_MODULE, topic_name)
                        doc = (
                            unicode(_(sys.modules[m].__doc__)) or ''
                        ).strip().split('\n', 1)[0]
                        self._topics[topic_name] = [doc, 0, [c]]
                    mcl = max((self._topics[topic_name][1], len(c.name)))
                    self._topics[topic_name][1] = mcl
                else: # a module grouped in a topic
                    doc = (
                        unicode(_(sys.modules[c.module].__doc__)) or ''
                    ).strip().split('\n', 1)[0]
                    mod_name = c.module.rsplit('.',1)[1]
                    if topic_name in self._topics:
                        if mod_name in self._topics[topic_name][2]:
                            self._topics[topic_name][2][mod_name][2].append(c)
                        else:
                            self._topics[topic_name][2][mod_name] = [doc, 0, [c]]
                            self._count_topic_mcl(topic_name, mod_name)
                        # count mcl for for the subtopic
                        mcl = max((self._topics[topic_name][2][mod_name][1], len(c.name)))
                        self._topics[topic_name][2][mod_name][1] = mcl
                    else:
                        self._topics[topic_name] = [unicode(_(topic[1])), 0, {mod_name: [doc, 0, [c]]}]
                        self._count_topic_mcl(topic_name, mod_name)
            else:
                self._builtins.append(c)

        # compute maximum topic length
        self._mtl = max(
            len(s) for s in (self._topics.keys() + [c.name for c in self._builtins])
        )

        super(help, self)._on_finalize()

    def run(self, key):
        name = from_cli(key)
        mod_name = '%s.%s' % (self._PLUGIN_BASE_MODULE, name)
        if key is None or name == "topics":
            self.print_topics()
            return
        if name in self._topics:
            self.print_commands(name)
        elif name in self.Command:
            cmd = self.Command[name]
            if cmd.NO_CLI:
                raise HelpError(topic=name)
            print unicode(_('Purpose: %s')) % unicode(_(cmd.doc)).strip()
            self.Backend.cli.build_parser(cmd).print_help()
        elif mod_name in sys.modules:
            self.print_commands(name)
        elif name == "commands":
            mcl = max(len(s) for s in (self.Command))
            for cname in self.Command:
                cmd = self.Command[cname]
                if cmd.NO_CLI:
                    continue
                print '%s  %s' % (to_cli(cmd.name).ljust(mcl), cmd.summary)
        else:
            raise HelpError(topic=name)

    def print_topics(self):
        topics = sorted(self._topics.keys())

        print unicode(_('Usage: ipa [global-options] COMMAND ...'))
        print ''
        print unicode(_('Built-in commands:'))
        for c in self._builtins:
            print unicode(_('Help subtopics:'))
            print '  %s  %s' % (to_cli(c.name).ljust(self._mtl), c.summary)
        print ''
        print unicode(_('Help topics:'))
        for t in topics:
            topic = self._topics[t]
            print '  %s  %s' % (to_cli(t).ljust(self._mtl), topic[0])
        print ''
        print unicode(_('Try `ipa --help` for a list of global options.'))

    def print_commands(self, topic):
        if topic in self._topics and type(self._topics[topic][2]) is dict:
            # we want to display topic which has subtopics
            for subtopic in self._topics[topic][2]:
                doc = self._topics[topic][2][subtopic][0]
                mcl = self._topics[topic][1]
                print '  %s  %s' % (to_cli(subtopic).ljust(mcl), doc)
        else:
            # we want to display subtopic or a topic which has no subtopics
            if topic in self._topics:
                mcl = self._topics[topic][1]
                commands = self._topics[topic][2]
            else:
                commands = []
                for t in self._topics:
                    if type(self._topics[t][2]) is not dict:
                        continue
                    if topic not in self._topics[t][2]:
                        continue
                    mcl = self._topics[t][2][topic][1]
                    commands = self._topics[t][2][topic][2]
                    break

            m = '%s.%s' % (self._PLUGIN_BASE_MODULE, topic)
            doc = (unicode(_(sys.modules[m].__doc__)) or '').strip()

            if topic not in self.Command and len(commands) == 0:
                raise HelpError(topic=topic)

            print doc
            if commands:
                print ''
                print unicode(_('Topic commands:'))
                for c in commands:
                    print '  %s  %s' % (to_cli(c.name).ljust(mcl), c.summary)
            print "\n"

class show_mappings(frontend.Command):
    """
    Show mapping of LDAP attributes to command-line option.
    """
    takes_args = (
        Str('command_name',
            label=_('Command name'),
        ),
    )
    has_output = tuple()

    def run(self, command_name):
        command_name = from_cli(command_name)
        if command_name not in self.Command:
            raise CommandError(name=command_name)
        params = self.Command[command_name].options
        out = [('Parameter','LDAP attribute'),
               ('=========','==============')]
        mcl = len(out[0][0])
        for param in params():
            if param.exclude and 'webui' in param.exclude:
                continue
            out.append((param.cli_name, param.param_spec))
            mcl = max(mcl,len(param.cli_name))
        for item in out:
            print to_cli(item[0]).ljust(mcl)+' : '+item[1]


class console(frontend.Command):
    """Start the IPA interactive Python console."""

    has_output = tuple()

    def run(self):
        code.interact(
            '(Custom IPA interactive Python console)',
            local=dict(api=self.api)
        )


class show_api(frontend.Command):
    'Show attributes on dynamic API object'

    takes_args = ('namespaces*',)

    def run(self, namespaces):
        if namespaces is None:
            names = tuple(self.api)
        else:
            for name in namespaces:
                if name not in self.api:
                    raise NoSuchNamespaceError(name=name)
            names = namespaces
        lines = self.__traverse(names)
        ml = max(len(l[1]) for l in lines)
        self.Backend.textui.print_name('run')
        first = True
        for line in lines:
            if line[0] == 0 and not first:
                print ''
            if first:
                first = False
            print '%s%s %r' % (
                ' ' * line[0],
                line[1].ljust(ml),
                line[2],
            )
        if len(lines) == 1:
            s = '1 attribute shown.'
        else:
            s = '%d attributes show.' % len(lines)
        self.Backend.textui.print_dashed(s)

    def __traverse(self, names):
        lines = []
        for name in names:
            namespace = self.api[name]
            self.__traverse_namespace('%s' % name, namespace, lines)
        return lines

    def __traverse_namespace(self, name, namespace, lines, tab=0):
        lines.append((tab, name, namespace))
        for member_name in namespace:
            member = namespace[member_name]
            lines.append((tab + 1, member_name, member))
            if not hasattr(member, '__iter__'):
                continue
            for n in member:
                attr = member[n]
                if isinstance(attr, plugable.NameSpace) and len(attr) > 0:
                    self.__traverse_namespace(n, attr, lines, tab + 2)


cli_application_commands = (
    help,
    console,
    show_api,
)


class Collector(object):
    def __init__(self):
        object.__setattr__(self, '_Collector__options', {})

    def __setattr__(self, name, value):
        if name in self.__options:
            v = self.__options[name]
            if type(v) is tuple:
                value = v + (value,)
            else:
                value = (v, value)
        self.__options[name] = value
        object.__setattr__(self, name, value)

    def __todict__(self):
        return dict(self.__options)

class CLIOptionParserFormatter(optparse.IndentedHelpFormatter):
    def format_argument(self, name, help_string):
        result = []
        opt_width = self.help_position - self.current_indent - 2
        if len(name) > opt_width:
            name = "%*s%s\n" % (self.current_indent, "", name)
            indent_first = self.help_position
        else:                       # start help on same line as name
            name = "%*s%-*s  " % (self.current_indent, "", opt_width, name)
            indent_first = 0
        result.append(name)
        if help_string:
            help_lines = textwrap.wrap(help_string, self.help_width)
            result.append("%*s%s\n" % (indent_first, "", help_lines[0]))
            result.extend(["%*s%s\n" % (self.help_position, "", line)
                           for line in help_lines[1:]])
        elif name[-1] != "\n":
            result.append("\n")
        return "".join(result)

class CLIOptionParser(optparse.OptionParser):
    """
    This OptionParser subclass adds an ability to print positional
    arguments in CLI help. Custom formatter is used to format the argument
    list in the same way as OptionParser formats options.
    """
    def __init__(self, *args, **kwargs):
        self._arguments = []
        if 'formatter' not in kwargs:
            kwargs['formatter'] = CLIOptionParserFormatter()
        optparse.OptionParser.__init__(self, *args, **kwargs)

    def format_option_help(self, formatter=None):
        """
        Prepend argument help to standard OptionParser's option help
        """
        option_help = optparse.OptionParser.format_option_help(self, formatter)

        if isinstance(formatter, CLIOptionParserFormatter):
            heading = unicode(_("Positional arguments"))
            arguments = [formatter.format_heading(heading)]
            formatter.indent()
            for (name, help_string) in self._arguments:
                arguments.append(formatter.format_argument(name, help_string))
            formatter.dedent()
            if len(arguments) > 1:
                # there is more than just the heading
                arguments.append(u"\n")
            else:
                arguments = []
            option_help = "".join(arguments) + option_help
        return option_help

    def add_argument(self, name, help_string):
        self._arguments.append((name, help_string))

class cli(backend.Executioner):
    """
    Backend plugin for executing from command line interface.
    """

    def get_command(self, argv):
        """Given CLI arguments, return the Command to use

        On incorrect invocation, prints out a help message and returns None
        """
        if len(argv) == 0:
            self.Command.help()
            return
        (key, argv) = (argv[0], argv[1:])
        name = from_cli(key)
        if name not in self.Command and len(argv) == 0:
            try:
                self.Command.help(unicode(key))
                return
            except HelpError:
                pass
        if name not in self.Command or self.Command[name].NO_CLI:
            raise CommandError(name=key)
        cmd = self.Command[name]
        return cmd

    def argv_to_keyword_arguments(self, cmd, argv):
        """Get the keyword arguments for a Command"""
        kw = self.parse(cmd, argv)
        if self.env.interactive:
            self.prompt_interactively(cmd, kw)
        kw = cmd.split_csv(**kw)
        kw['version'] = API_VERSION
        self.load_files(cmd, kw)
        return kw

    def run(self, argv):
        cmd = self.get_command(argv)
        if cmd is None:
            return
        name = cmd.name
        if not isinstance(cmd, frontend.Local):
            self.create_context()
        try:
            kw = self.argv_to_keyword_arguments(cmd, argv[1:])
            result = self.execute(name, **kw)
            if callable(cmd.output_for_cli):
                for param in cmd.params():
                    if param.password and param.name in kw:
                        del kw[param.name]
                (args, options) = cmd.params_2_args_options(**kw)
                rv = cmd.output_for_cli(self.api.Backend.textui, result, *args, **options)
                if rv:
                    return rv
                else:
                    return 0
        finally:
            self.destroy_context()

    def parse(self, cmd, argv):
        parser = self.build_parser(cmd)
        (collector, args) = parser.parse_args(argv, Collector())
        options = collector.__todict__()
        kw = cmd.args_options_2_params(*args, **options)
        return dict(self.parse_iter(cmd, kw))

    # FIXME: Probably move decoding to Command, use same method regardless of
    # request source:
    def parse_iter(self, cmd, kw):
        """
        Decode param values if appropriate.
        """
        for (key, value) in kw.iteritems():
            yield (key, self.Backend.textui.decode(value))

    def build_parser(self, cmd):
        parser = CLIOptionParser(
            usage=' '.join(self.usage_iter(cmd))
        )
        option_groups = {}
        for option in cmd.options():
            kw = dict(
                dest=option.name,
                help=unicode(option.doc),
            )
            if 'no_option' in option.flags:
                continue
            if option.password and self.env.interactive:
                kw['action'] = 'store_true'
            elif option.type is bool and option.autofill:
                if option.default is True:
                    kw['action'] = 'store_false'
                else:
                    kw['action'] = 'store_true'
            elif isinstance(option, StrEnum):
                kw['metavar'] = metavar=map(lambda x: str(x), option.values)
            else:
                kw['metavar'] = metavar=option.__class__.__name__.upper()

            if option.cli_short_name:
                o = optparse.make_option('-%s' % option.cli_short_name, '--%s' % to_cli(option.cli_name), **kw)
            else:
                o = optparse.make_option('--%s' % to_cli(option.cli_name), **kw)

            if option.option_group is not None:
                option_group = option_groups.get(option.option_group)
                if option_group is None:
                    option_group = optparse.OptionGroup(parser,
                                                        option.option_group)
                    parser.add_option_group(option_group)
                    option_groups[option.option_group] = option_group

                option_group.add_option(o)
            else:
                parser.add_option(o)

        for arg in cmd.args():
            name = self.__get_arg_name(arg, format_name=False)
            if name is None:
                continue
            doc = unicode(arg.doc)
            parser.add_argument(name, doc)

        return parser

    def __get_arg_name(self, arg, format_name=True):
        if arg.password:
            return

        name = to_cli(arg.cli_name).upper()
        if not format_name:
            return name
        if arg.multivalue:
            name = '%s...' % name
        if arg.required:
            return name
        else:
            return '[%s]' % name

    def usage_iter(self, cmd):
        yield 'Usage: %%prog [global-options] %s' % to_cli(cmd.name)
        for arg in cmd.args():
            name = self.__get_arg_name(arg)
            if name is None:
                continue
            yield name
        yield '[options]'

    def prompt_interactively(self, cmd, kw):
        """
        Interactively prompt for missing or invalid values.

        By default this method will only prompt for *required* Param that
        have a missing or invalid value.  However, if
        ``self.env.prompt_all`` is ``True``, this method will prompt for any
        params that have a missing values, even if the param is optional.
        """
        honor_alwaysask = True
        for param in cmd.params():
            if param.alwaysask and param.name in kw:
                honor_alwaysask = False
                break
        for param in cmd.params():
            if (param.required and param.name not in kw) or \
                (param.alwaysask and honor_alwaysask) or self.env.prompt_all:
                if param.autofill:
                    kw[param.name] = cmd.get_default_of(param.name, **kw)
                if param.name in kw and kw[param.name] is not None:
                    continue
                if param.password:
                    kw[param.name] = self.Backend.textui.prompt_password(
                        param.label, param.confirm
                    )
                else:
                    default = cmd.get_default_of(param.name, **kw)
                    error = None
                    while True:
                        if error is not None:
                            self.Backend.textui.print_prompt_attribute_error(unicode(param.label),
                                                                             unicode(error))
                        raw = self.Backend.textui.prompt(param.label, default, optional=param.alwaysask or not param.required)
                        try:
                            value = param(raw, **kw)
                            if value is not None:
                                kw[param.name] = value
                            break
                        except (ValidationError, ConversionError), e:
                            error = e.error
            elif param.password and kw.get(param.name, False) is True:
                kw[param.name] = self.Backend.textui.prompt_password(
                    param.label, param.confirm
                )

        try:
            callbacks = cmd.get_callbacks('interactive_prompt')
        except AttributeError:
            pass
        else:
            for callback in callbacks:
                callback(cmd, kw)

    def load_files(self, cmd, kw):
        """
        Load files from File parameters.

        This has to be done after all required parameters have been read
        (i.e. after prompt_interactively has or would have been called)
        AND before they are passed to the command. This is because:
        1) we need to be sure no more files are going to be added
        2) we load files from the machine where the command was executed
        3) the webUI will use a different way of loading files
        """
        for p in cmd.params():
            if isinstance(p, File):
                # FIXME: this only reads the first file
                raw = None
                if p.name in kw:
                    if type(kw[p.name]) in (tuple, list):
                        fname = kw[p.name][0]
                    else:
                        fname = kw[p.name]
                    try:
                        f = open(fname, 'r')
                        raw = f.read()
                        f.close()
                    except IOError, e:
                        raise ValidationError(
                            name=to_cli(p.cli_name),
                            error='%s: %s:' % (fname, e[1])
                        )
                elif p.stdin_if_missing:
                    try:
                        raw = sys.stdin.read()
                    except IOError, e:
                        raise ValidationError(
                            name=to_cli(p.cli_name), error=e[1]
                        )
                if not raw:
                        raise ValidationError(
                            name=to_cli(p.cli_name), error=_('No file to read')
                        )
                kw[p.name] = self.Backend.textui.decode(raw)


cli_plugins = (
    cli,
    textui,
    console,
    help,
    show_mappings,
)


def run(api):
    error = None
    try:
        (options, argv) = api.bootstrap_with_global_options(context='cli')
        for klass in cli_plugins:
            api.register(klass)
        api.load_plugins()
        api.finalize()
        if not 'config_loaded' in api.env:
            raise NotConfiguredError()
        sys.exit(api.Backend.cli.run(argv))
    except KeyboardInterrupt:
        print ''
        api.log.info('operation aborted')
    except PublicError, e:
        error = e
    except StandardError, e:
        api.log.exception('%s: %s', e.__class__.__name__, str(e))
        error = InternalError()
    if error is not None:
        assert isinstance(error, PublicError)
        api.log.error(error.strerror)
        sys.exit(error.rval)
