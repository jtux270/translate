# Authors: Mark McLoughlin <markmc@redhat.com>
#
# Copyright (C) 2007  Red Hat
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
#

#
# This module provides a very simple API which allows
# ipa-xxx-install --uninstall to restore certain
# parts of the system configuration to the way it was
# before ipa-server-install was first run

import os
import os.path
import shutil
from ipapython.ipa_log_manager import *
import ConfigParser
import random
import string

from ipapython import ipautil
from ipapython import services as ipaservices

SYSRESTORE_PATH = "/tmp"
SYSRESTORE_INDEXFILE = "sysrestore.index"
SYSRESTORE_STATEFILE = "sysrestore.state"

class FileStore:
    """Class for handling backup and restore of files"""

    def __init__(self, path = SYSRESTORE_PATH, index_file = SYSRESTORE_INDEXFILE):
        """Create a _StoreFiles object, that uses @path as the
        base directory.

        The file @path/sysrestore.index is used to store information
        about the original location of the saved files.
        """
        self._path = path
        self._index = os.path.join(self._path, index_file)

        self.random = random.Random()

        self.files = {}
        self._load()

    def _load(self):
        """Load the file list from the index file. @files will
        be an empty dictionary if the file doesn't exist.
        """

        root_logger.debug("Loading Index file from '%s'", self._index)

        self.files = {}

        p = ConfigParser.SafeConfigParser()
        p.read(self._index)

        for section in p.sections():
            if section == "files":
                for (key, value) in p.items(section):
                    self.files[key] = value


    def save(self):
        """Save the file list to @_index. If @files is an empty
        dict, then @_index should be removed.
        """
        root_logger.debug("Saving Index File to '%s'", self._index)

        if len(self.files) == 0:
            root_logger.debug("  -> no files, removing file")
            if os.path.exists(self._index):
                os.remove(self._index)
            return

        p = ConfigParser.SafeConfigParser()

        p.add_section('files')
        for (key, value) in self.files.items():
            p.set('files', key, str(value))

        f = file(self._index, "w")
        p.write(f)
        f.close()

    def backup_file(self, path):
        """Create a copy of the file at @path - so long as a copy
        does not already exist - which will be restored to its
        original location by restore_files().
        """
        root_logger.debug("Backing up system configuration file '%s'", path)

        if not os.path.isabs(path):
            raise ValueError("Absolute path required")

        if not os.path.isfile(path):
            root_logger.debug("  -> Not backing up - '%s' doesn't exist", path)
            return

        (reldir, backupfile) = os.path.split(path)

        filename = ""
        for i in range(8):
            h = "%02x" % self.random.randint(0,255)
            filename += h
        filename += "-"+backupfile

        backup_path = os.path.join(self._path, filename)
        if os.path.exists(backup_path):
            root_logger.debug("  -> Not backing up - already have a copy of '%s'", path)
            return

        shutil.copy2(path, backup_path)

        stat = os.stat(path)

        self.files[filename] = string.join([str(stat.st_mode),str(stat.st_uid),str(stat.st_gid),path], ',')
        self.save()

    def has_file(self, path):
        """Checks whether file at @path was added to the file store

        Returns #True if the file exists in the file store, #False otherwise
        """
        result = False
        for (key, value) in self.files.items():
            (mode,uid,gid,filepath) = string.split(value, ',', 3)
            if (filepath == path):
                result = True
                break
        return result

    def restore_file(self, path, new_path = None):
        """Restore the copy of a file at @path to its original
        location and delete the copy.

        Takes optional parameter @new_path which specifies the
        location where the file is to be restored.

        Returns #True if the file was restored, #False if there
        was no backup file to restore
        """

        if new_path is None:
            root_logger.debug("Restoring system configuration file '%s'", path)
        else:
            root_logger.debug("Restoring system configuration file '%s' to '%s'", path, new_path)

        if not os.path.isabs(path):
            raise ValueError("Absolute path required")
        if new_path is not None and not os.path.isabs(new_path):
            raise ValueError("Absolute new path required")

        mode = None
        uid = None
        gid = None
        filename = None

        for (key, value) in self.files.items():
            (mode,uid,gid,filepath) = string.split(value, ',', 3)
            if (filepath == path):
                filename = key
                break

        if not filename:
            raise ValueError("No such file name in the index")

        backup_path = os.path.join(self._path, filename)
        if not os.path.exists(backup_path):
            root_logger.debug("  -> Not restoring - '%s' doesn't exist", backup_path)
            return False

        if new_path is not None:
            path = new_path

        shutil.move(backup_path, path)
        os.chown(path, int(uid), int(gid))
        os.chmod(path, int(mode))

        ipaservices.restore_context(path)

        del self.files[filename]
        self.save()

        return True

    def restore_all_files(self):
        """Restore the files in the inbdex to their original
        location and delete the copy.

        Returns #True if the file was restored, #False if there
        was no backup file to restore
        """

        if len(self.files) == 0:
            return False

        for (filename, value) in self.files.items():

            (mode,uid,gid,path) = string.split(value, ',', 3)

            backup_path = os.path.join(self._path, filename)
            if not os.path.exists(backup_path):
                root_logger.debug("  -> Not restoring - '%s' doesn't exist", backup_path)
                continue

            shutil.move(backup_path, path)
            os.chown(path, int(uid), int(gid))
            os.chmod(path, int(mode))

            ipaservices.restore_context(path)

	#force file to be deleted
        self.files = {}
        self.save()

        return True

    def has_files(self):
        """Return True or False if there are any files in the index

        Can be used to determine if a program is configured.
        """

        return len(self.files) > 0

    def untrack_file(self, path):
        """Remove file at path @path from list of backed up files.

        Does not remove any files from the filesystem.

        Returns #True if the file was untracked, #False if there
        was no backup file to restore
        """

        root_logger.debug("Untracking system configuration file '%s'", path)

        if not os.path.isabs(path):
            raise ValueError("Absolute path required")

        mode = None
        uid = None
        gid = None
        filename = None

        for (key, value) in self.files.items():
            (mode,uid,gid,filepath) = string.split(value, ',', 3)
            if (filepath == path):
                filename = key
                break

        if not filename:
            raise ValueError("No such file name in the index")

        backup_path = os.path.join(self._path, filename)
        if not os.path.exists(backup_path):
            root_logger.debug("  -> Not restoring - '%s' doesn't exist", backup_path)
            return False

        try:
            os.unlink(backup_path)
        except Exception, e:
            root_logger.error('Error removing %s: %s' % (backup_path, str(e)))

        del self.files[filename]
        self.save()

        return True

class StateFile:
    """A metadata file for recording system state which can
    be backed up and later restored. The format is something
    like:

    [httpd]
    running=True
    enabled=False
    """

    def __init__(self, path = SYSRESTORE_PATH, state_file = SYSRESTORE_STATEFILE):
        """Create a StateFile object, loading from @path.

        The dictionary @modules, a member of the returned object,
        is where the state can be modified. @modules is indexed
        using a module name to return another dictionary containing
        key/value pairs with the saved state of that module.

        The keys in these latter dictionaries are arbitrary strings
        and the values may either be strings or booleans.
        """
        self._path = os.path.join(path, state_file)

        self.modules = {}

        self._load()

    def _load(self):
        """Load the modules from the file @_path. @modules will
        be an empty dictionary if the file doesn't exist.
        """
        root_logger.debug("Loading StateFile from '%s'", self._path)

        self.modules = {}

        p = ConfigParser.SafeConfigParser()
        p.read(self._path)

        for module in p.sections():
            self.modules[module] = {}
            for (key, value) in p.items(module):
                if value == str(True):
                    value = True
                elif value == str(False):
                    value = False
                self.modules[module][key] = value

    def save(self):
        """Save the modules to @_path. If @modules is an empty
        dict, then @_path should be removed.
        """
        root_logger.debug("Saving StateFile to '%s'", self._path)

        for module in self.modules.keys():
            if len(self.modules[module]) == 0:
                del self.modules[module]

        if len(self.modules) == 0:
            root_logger.debug("  -> no modules, removing file")
            if os.path.exists(self._path):
                os.remove(self._path)
            return

        p = ConfigParser.SafeConfigParser()

        for module in self.modules.keys():
            p.add_section(module)
            for (key, value) in self.modules[module].items():
                p.set(module, key, str(value))

        f = file(self._path, "w")
        p.write(f)
        f.close()

    def backup_state(self, module, key, value):
        """Backup an item of system state from @module, identified
        by the string @key and with the value @value. @value may be
        a string or boolean.
        """
        if not isinstance(value, (str, bool, unicode)):
            raise ValueError("Only strings, booleans or unicode strings are supported")

        if not self.modules.has_key(module):
            self.modules[module] = {}

        if not self.modules.has_key(key):
            self.modules[module][key] = value

        self.save()

    def get_state(self, module, key):
        """Return the value of an item of system state from @module,
        identified by the string @key.

        If the item doesn't exist, #None will be returned, otherwise
        the original string or boolean value is returned.
        """
        if not self.modules.has_key(module):
            return None

        return self.modules[module].get(key, None)

    def delete_state(self, module, key):
        """Delete system state from @module, identified by the string
        @key.

        If the item doesn't exist, no change is done.
        """
        try:
            del self.modules[module][key]
        except KeyError:
            pass
        else:
            self.save()

    def restore_state(self, module, key):
        """Return the value of an item of system state from @module,
        identified by the string @key, and remove it from the backed
        up system state.

        If the item doesn't exist, #None will be returned, otherwise
        the original string or boolean value is returned.
        """

        value = self.get_state(module, key)

        if value is not None:
            self.delete_state(module, key)

        return value

    def has_state(self, module):
        """Return True or False if there is any state stored for @module.

        Can be used to determine if a service is configured.
        """

        if self.modules.has_key(module):
            return True
        else:
            return False
