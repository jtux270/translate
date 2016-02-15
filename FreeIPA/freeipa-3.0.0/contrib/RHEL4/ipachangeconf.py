#
# ipachangeconf - configuration file manipulation classes and functions
# partially based on authconfig code
# Copyright (c) 1999-2007 Red Hat, Inc.
# Author: Simo Sorce <ssorce@redhat.com>
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

import fcntl
import os
import string
import time
import shutil

def openLocked(filename, perms):
    fd = -1
    try:
        fd = os.open(filename, os.O_RDWR | os.O_CREAT, perms)
        
        fcntl.lockf(fd, fcntl.LOCK_EX)
    except OSError, (errno, strerr):
        if fd != -1:
            try:
                os.close(fd)
            except OSError:
                pass
        raise IOError(errno, strerr)
    return os.fdopen(fd, "r+")


    #TODO: add subsection as a concept
    #      (ex. REALM.NAME = { foo = x bar = y } )
    #TODO: put section delimiters as separating element of the list
    #      so that we can process multiple sections in one go
    #TODO: add a comment all but provided options as a section option
class IPAChangeConf:

    def __init__(self, name):
        self.progname = name
        self.indent = ("","","")
        self.assign = (" = ","=")
        self.dassign = self.assign[0]
        self.comment = ("#",)
        self.dcomment = self.comment[0]
        self.eol = ("\n",)
        self.deol = self.eol[0]
        self.sectnamdel = ("[","]")
        self.subsectdel = ("{","}")

    def setProgName(self, name):
        self.progname = name

    def setIndent(self, indent):
        if type(indent) is tuple:
            self.indent = indent
        elif type(indent) is str:
            self.indent = (indent, )
        else:
           raise ValueError, 'Indent must be a list of strings'

    def setOptionAssignment(self, assign):
        if type(assign) is tuple:
            self.assign = assign
        else:
            self.assign = (assign, )
        self.dassign = self.assign[0]

    def setCommentPrefix(self, comment):
        if type(comment) is tuple:
            self.comment = comment
        else:
            self.comment = (comment, )
        self.dcomment = self.comment[0]

    def setEndLine(self, eol):
        if type(eol) is tuple:
            self.eol = eol
        else:
            self.eol = (eol, )
        self.deol = self.eol[0]

    def setSectionNameDelimiters(self, delims):
        self.sectnamdel = delims

    def setSubSectionDelimiters(self, delims):
        self.subsectdel = delims

    def matchComment(self, line):
        for v in self.comment:
            if line.lstrip().startswith(v):
                return line.lstrip()[len(v):]
        return False

    def matchEmpty(self, line):
        if line.strip() == "":
            return True
        return False

    def matchSection(self, line):
        cl = "".join(line.strip().split()).lower()
        if len(self.sectnamdel) != 2:
            return False
        if not cl.startswith(self.sectnamdel[0]):
            return False
        if not cl.endswith(self.sectnamdel[1]):
            return False
        return cl[len(self.sectnamdel[0]):-len(self.sectnamdel[1])]            

    def matchSubSection(self, line):
        if self.matchComment(line):
            return False

        parts = line.split(self.dassign, 1)
        if len(parts) < 2:
            return False

        if parts[1].strip() == self.subsectdel[0]:
            return parts[0].strip()

        return False

    def matchSubSectionEnd(self, line):
        if self.matchComment(line):
            return False

        if line.strip() == self.subsectdel[1]:
            return True

        return False

    def getSectionLine(self, section):
        if len(self.sectnamdel) != 2:
            return section
        return self.sectnamdel[0]+section+self.sectnamdel[1]+self.deol

    def dump(self, options, level=0):
        output = ""
        if level >= len(self.indent):
            level = len(self.indent)-1

        for o in options:
            if o['type'] == "section":
                output += self.sectnamdel[0]+o['name']+self.sectnamdel[1]+self.deol
                output += self.dump(o['value'], level+1)
                continue
            if o['type'] == "subsection":
                output += self.indent[level]+o['name']+self.dassign+self.subsectdel[0]+self.deol
                output += self.dump(o['value'], level+1)
                output += self.indent[level]+self.subsectdel[1]+self.deol
                continue
            if o['type'] == "option":
                output += self.indent[level]+o['name']+self.dassign+o['value']+self.deol
                continue
            if o['type'] == "comment":
                output += self.dcomment+o['value']+self.deol
                continue
            if o['type'] == "empty":
                output += self.deol
                continue
            raise SyntaxError, 'Unknown type: ['+o['type']+']'

        return output

    def parseLine(self, line):

        if self.matchEmpty(line):
            return {'name':'empty', 'type':'empty'}

        value = self.matchComment(line)
        if value:
            return {'name':'comment', 'type':'comment', 'value':value.rstrip()} #pylint: disable=E1103

        parts = line.split(self.dassign, 1)
        if len(parts) < 2:
            raise SyntaxError, 'Syntax Error: Unknown line format'

        return {'name':parts[0].strip(), 'type':'option', 'value':parts[1].rstrip()}

    def findOpts(self, opts, type, name, exclude_sections=False):

        num = 0
        for o in opts:
            if o['type'] == type and o['name'] == name:
                return (num, o)
            if exclude_sections and (o['type'] == "section" or o['type'] == "subsection"):
                return (num, None)
            num += 1
        return (num, None)

    def commentOpts(self, inopts, level = 0):

        opts = []

        if level >= len(self.indent):
            level = len(self.indent)-1

        for o in inopts:
            if o['type'] == 'section':
                no = self.commentOpts(o['value'], level+1)
                val = self.dcomment+self.sectnamdel[0]+o['name']+self.sectnamdel[1]
                opts.append({'name':'comment', 'type':'comment', 'value':val})
                for n in no:
                    opts.append(n)
                continue
            if o['type'] == 'subsection':
                no = self.commentOpts(o['value'], level+1)
                val = self.indent[level]+o['name']+self.dassign+self.subsectdel[0]
                opts.append({'name':'comment', 'type':'comment', 'value':val})
                for n in no:
                    opts.append(n)
                val = self.indent[level]+self.subsectdel[1]
                opts.append({'name':'comment', 'type':'comment', 'value':val})
                continue
            if o['type'] == 'option':
                val = self.indent[level]+o['name']+self.dassign+o['value']
                opts.append({'name':'comment', 'type':'comment', 'value':val})
                continue
            if o['type'] == 'comment':
                opts.append(o)
                continue
            if o['type'] == 'empty':
                opts.append({'name':'comment', 'type':'comment', 'value':''})
                continue
            raise SyntaxError, 'Unknown type: ['+o['type']+']'

        return opts

    def mergeOld(self, oldopts, newopts):

        opts = []

        for o in oldopts:
            if o['type'] == "section" or o['type'] == "subsection":
                (num, no) = self.findOpts(newopts, o['type'], o['name'])
                if not no:
                    opts.append(o)
                    continue
                if no['action'] == "set":
                    mo = self.mergeOld(o['value'], no['value'])
                    opts.append({'name':o['name'], 'type':o['type'], 'value':mo})
                    continue
                if no['action'] == "comment":
                    co = self.commentOpts(o['value'])
                    for c in co:
                        opts.append(c)
                    continue
                if no['action'] == "remove":
                    continue
                raise SyntaxError, 'Unknown action: ['+no['action']+']'

            if o['type'] == "comment" or o['type'] == "empty":
                 opts.append(o)
                 continue

            if o['type'] == "option":
                (num, no) = self.findOpts(newopts, 'option', o['name'], True)
                if not no:
                    opts.append(o)
                    continue
                if no['action'] == 'comment' or no['action'] == 'remove':
                    if no['value'] != None and o['value'] != no['value']:
                        opts.append(o)
                        continue
                    if no['action'] == 'comment':
                       opts.append({'name':'comment', 'type':'comment',
                                    'value':self.dcomment+o['name']+self.dassign+o['value']})
                    continue
                if no['action'] == 'set':
                    opts.append(no)
                    continue
                raise SyntaxError, 'Unknown action: ['+o['action']+']'

            raise SyntaxError, 'Unknown type: ['+o['type']+']'

        return opts

    def mergeNew(self, opts, newopts):

        cline = 0

        for no in newopts:

            if no['type'] == "section" or no['type'] == "subsection":
                (num, o) = self.findOpts(opts, no['type'], no['name'])
                if not o:
                    if no['action'] == 'set':
                        opts.append(no)
                    continue
                if no['action'] == "set":
                    self.mergeNew(o['value'], no['value'])
                    continue
                cline = num+1
                continue

            if no['type'] == "option":
                (num, o) = self.findOpts(opts, no['type'], no['name'], True)
                if not o:
                    if no['action'] == 'set':
                        opts.append(no)
                    continue
                cline = num+1
                continue

            if no['type'] == "comment" or no['type'] == "empty":
                opts.insert(cline, no)
                cline += 1
                continue

            raise SyntaxError, 'Unknown type: ['+no['type']+']'


    def merge(self, oldopts, newopts):

        #Use a two pass strategy
        #First we create a new opts tree from oldopts removing/commenting
        #  the options as indicated by the contents of newopts
        #Second we fill in the new opts tree with options as indicated
        #  in the newopts tree (this is becaus eentire (sub)sections may
        #  exist in the newopts that do not exist in oldopts)

        opts = self.mergeOld(oldopts, newopts)
        self.mergeNew(opts, newopts)
        return opts

    #TODO: Make parse() recursive?
    def parse(self, f):

        opts = []
        sectopts = []
        section = None
        subsectopts = []
        subsection = None
        curopts = opts
        fatheropts = opts

        # Read in the old file.
        for line in f:

            # It's a section start.
            value = self.matchSection(line)
            if value:
                if section is not None:
                    opts.append({'name':section, 'type':'section', 'value':sectopts})
                sectopts = []
                curopts = sectopts
                fatheropts = sectopts
                section = value
                continue

            # It's a subsection start.
            value = self.matchSubSection(line)
            if value:
                if subsection is not None:
                    raise SyntaxError, 'nested subsections are not supported yet'
                subsectopts = []
                curopts = subsectopts
                subsection = value
                continue

            value = self.matchSubSectionEnd(line)
            if value:
                if subsection is None:
                    raise SyntaxError, 'Unmatched end subsection terminator found'
                fatheropts.append({'name':subsection, 'type':'subsection', 'value':subsectopts})
                subsection = None
                curopts = fatheropts
                continue

            # Copy anything else as is.
            curopts.append(self.parseLine(line))

        #Add last section if any
        if len(sectopts) is not 0:
            opts.append({'name':section, 'type':'section', 'value':sectopts})

        return opts

    # Write settings to configuration file
    # file is a path
    # options is a set of dictionaries in the form:
    #     [{'name': 'foo', 'value': 'bar', 'action': 'set/comment'}]
    # section is a section name like 'global'
    def changeConf(self, file, newopts):
        autosection = False
        savedsection = None
        done = False
        output = ""
        f = None
        try:
            #Do not catch an unexisting file error, we want to fail in that case
            shutil.copy2(file, file+".ipabkp")

            f = openLocked(file, 0644)

            oldopts = self.parse(f)

            options = self.merge(oldopts, newopts)

            output = self.dump(options)

            # Write it out and close it.
            f.seek(0)
            f.truncate(0)
            f.write(output)
        finally:
            try:
                if f:
                    f.close()
            except IOError:
                pass
        return True

    # Write settings to new file, backup old
    # file is a path
    # options is a set of dictionaries in the form:
    #     [{'name': 'foo', 'value': 'bar', 'action': 'set/comment'}]
    # section is a section name like 'global'
    def newConf(self, file, options):
        autosection = False
        savedsection = None
        done = False
        output = ""
        f = None
        try:
            try:
                shutil.copy2(file, file+".ipabkp")
            except IOError, err:
                if err.errno == 2:
                    # The orign file did not exist
                    pass

            f = openLocked(file, 0644)

            # Trunkate
            f.seek(0)
            f.truncate(0)

            output = self.dump(options)

            f.write(output)
        finally:
            try:
                if f:
                    f.close()
            except IOError:
                pass
        return True
