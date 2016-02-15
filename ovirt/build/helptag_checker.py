#!/usr/bin/python
"""
Check source code for potential problems with help tags.
"""


import argparse
import os
import re
import sys


SKIP_FILES = [
    "Model.java", "AbstractModelBoundPopupPresenterWidget.java",
    "AbstractModelBoundPopupView.java"
]

__RE_SETHASHNAME = re.compile(
    flags=re.VERBOSE,
    pattern=r"""
    .*
    setHashName
    \s*
    \(
        \s*
        (?P<hashname>[^\)]*)
        \s*
    \)
    .*
    """
)

__RE_SETHELPTAG = re.compile(
    flags=re.VERBOSE,
    pattern=r"""
    .*
    setHelpTag
    \s*
    \(
        \s*
        (?P<helptag>.*?)
        \s*
    \)\s*;
    .*
    """
)

__RE_QUOTED_STRING = re.compile(
    flags=re.VERBOSE,
    pattern=r'"[^"]*"'
)


def walkSource(sourcedir):
    """
    walk the source code and run func for each files's lines
    """
    for parent, dnames, fnames in os.walk(sourcedir):
        for fname in fnames:
            if fname not in SKIP_FILES:
                filename = os.path.join(parent, fname)
                if filename.endswith('.java') and os.path.isfile(filename):
                    with open(filename, 'r') as f:
                        lines = f.readlines()
                    yield (lines, fname)


def findVariableHashNames(sourcedir):
    for (lines, fname) in walkSource(sourcedir):
        for line in lines:
            m = __RE_SETHASHNAME.match(line)
            if m:
                hashname = m.group("hashname")
                m2 = __RE_QUOTED_STRING.match(hashname)
                if not m2:
                    warning("bad line: %s\n%s\n" % (fname, line))


def findHashNamesWithoutHelpTag(sourcedir):
    for (lines, fname) in walkSource(sourcedir):
        for i in range(0, len(lines)):
            m = __RE_SETHASHNAME.match(lines[i])
            if m:
                hashname = m.group("hashname").replace(
                    '"',
                    ''
                ).replace(
                    '-',
                    '_'
                )
                if (
                    lines[i - 1].find('.' + hashname) == -1 or
                    lines[i - 1].find('setHelpTag') == -1
                ):
                    warning(
                        "helptag hashname mismatch: %s\n%s%s\n"
                        % (fname, lines[i - 1], lines[i])
                    )


def findDuplicateHelpTagCalls(sourcedir):

    tags = {}
    for (lines, fname) in walkSource(sourcedir):
        for line in lines:
            m = __RE_SETHELPTAG.match(line)
            if m:
                helptag = m.group("helptag")
                tags.setdefault(helptag, []).append(fname)

    for helptag in tags.keys():
        files = tags[helptag]
        if (len(files) > 1):
            warning("duplicate helptag call:\n%s\n%s\n\n" % (helptag, files))


def warning(s):
    sys.stderr.write("WARNING: %s" % s)


def main():
    parser = argparse.ArgumentParser(
        description=(
            'Check source code for potential problems with help tags.'
        ),
    )
    parser.add_argument(
        '--sourcedir',
        metavar='DIRECTORY',
        dest='sourcedir',
        default='.',
        help='the source code dir to scan',
    )

    args = parser.parse_args()

    findVariableHashNames(args.sourcedir)
    findHashNamesWithoutHelpTag(args.sourcedir)
    findDuplicateHelpTagCalls(args.sourcedir)

    sys.exit(0)


if __name__ == "__main__":
    main()


# vim: expandtab tabstop=4 shiftwidth=4
