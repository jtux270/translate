#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2013-2014 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import os
import re
import tempfile
import gettext
_ = lambda m: gettext.dgettext(message=m, domain='ovirt-engine-setup')


from otopi import base
from otopi import util


@util.export
class StorageDomainType(object):
    # See: org.ovirt.engine.core.common.businessentities.StorageDomainType
    MASTER = 0
    DATA = 1
    ISO = 2
    IMPORT_EXPORT = 3
    UNKNOWN = 4


@util.export
class StorageType(object):
    # See: org.ovirt.engine.core.common.businessentities.StorageType
    UNKNOWN = 0
    NFS = 1
    FCP = 2
    ISCSI = 3
    LOCALFS = 4
    POSIXFS = 6
    GLUSTERFS = 7


@util.export
class DomainChecker(base.Base):
    """
    Domains utility.
    """

    _RE_VALID_PATH = re.compile(
        flags=re.VERBOSE,
        pattern=r"""
            ^
            /
            [\w_\-\s]+
            (
                /
                [\w_\-\s]+
            )*
            /?
            $
        """
    )

    def __init__(self):
        super(DomainChecker, self).__init__()

    def get_base_path(self, path):
        """
        Iterate up in the tree structure until we get an existing path
        """
        if os.path.exists(path):
            return path
        else:
            return self.get_base_path(
                os.path.dirname(path)
            )

    def check_valid_path(self, path):
        """
        Check if the specified path has to be a valid path
        """
        self.logger.debug("validate '%s' as a valid mount point", path)
        if self._RE_VALID_PATH.match(path) is None:
            raise ValueError(
                _('{path} is not a valid path').format(path=path)
            )

    def check_base_writable(self, path):
        """
        Ensure that the path is writable
        """
        try:
            base_path = self.get_base_path(path)
            self.logger.debug(
                'Attempting to write temp file to {path}'.format(
                    path=base_path
                )
            )
            tempfile.TemporaryFile(dir=os.path.dirname(base_path)).close()
        except EnvironmentError:
            self.logger.debug('exception', exc_info=True)
            raise RuntimeError(
                _('Error: mount point {path} is not writable').format(
                    path=path
                )
            )

    def check_available_space(self, path, minimum):
        """
        Ensure it is large enough for containing an image
        """
        base_path = self.get_base_path(path)
        self.logger.debug(
            'Checking available space on {path}'.format(path=base_path)
        )
        stat = os.statvfs(base_path)
        available_space_mb = (stat.f_bsize * stat.f_bavail) // pow(2, 20)
        self.logger.debug(
            'Available space on {path} is {space}Mb'.format(
                path=base_path,
                space=available_space_mb
            )
        )
        if available_space_mb < minimum:
            raise RuntimeError(
                _(
                    'Error: mount point {path} contains only {available}Mb of '
                    'available space while a minimum of {minimum}Mb is '
                    'required'
                ).format(
                    path=base_path,
                    available=available_space_mb,
                    minimum=minimum
                )
            )


# vim: expandtab tabstop=4 shiftwidth=4
