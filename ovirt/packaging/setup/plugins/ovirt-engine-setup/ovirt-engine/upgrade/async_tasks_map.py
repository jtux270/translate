#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2013 Red Hat, Inc.
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


"""ASYNC TASKS MAP Constant."""

ASYNC_TASKS_MAP = {
    '1': [
        'AddVmCommand',
        'Adding a virtual machine',
    ],
    '3': [
        'AddVmFromScratch',
        'Adding a virtual machine from scrtach',
    ],
    '2': [
        'AddVmFromTemplate',
        'Adding a virtual machine based on a template',
    ],
    '7': [
        'StopVmCommand',
        'Stopping a virtual machine',
    ],
    '8': [
        'ShutdownVmCommand',
        'Shutting down a virtual machine',
    ],
    '11': [
        'HibernateVmCommand',
        'Hibernating a virtual machine',
    ],
    '12': [
        'RunVmCommand',
        'Running a virtual machine',
    ],
    '13': [
        'RunVmOnceCommand',
        'Running a virtual machine once',
    ],
    '14': [
        'MigrateVmCommand',
        'Migrating a virtual machine',
    ],
    '16': [
        'MigrateVmToServerCommand',
        'Migrating a virtual machine to a dedicated server',
    ],
    '15': [
        'InternalMigrateVmCommand',
        'Migrating a virtual machine due to an internal engine flow',
    ],
    '23': [
        'ExportVmCommand',
        'Exporting a virtual machine to an export domain',
    ],
    '24': [
        'ExportVmTemplateCommand',
        'Exporting a template to an export domain',
    ],
    '26': [
        'ImportVmCommand',
        'Importing a virtual machine from an export domain',
    ],
    '31': [
        'AddDiskToVmCommand',
        'Adding disk to virtual machine',
    ],
    '32': [
        'RemoveDiskFromVmCommand',
        'Removing disk from a virtual machine',
    ],
    '39': [
        'ImportVmTemplateCommand',
        'Importing a temaplte from an export domain',
    ],
    '201': [
        'AddVmTemplateCommand',
        'Adding a template',
    ],
    '203': [
        'RemoveVmTemplateCommand',
        'Removing a template',
    ],
    '208': [
        'CreateSnapshotFromTemplateCommand',
        'Creating a snapshot of a template',
    ],
    '210': [
        'MergeSnapshotCommand',
        'Merging a snapshot of a virtual machine',
    ],
    '212': [
        'RemoveAllVmImagesCommand',
        'Removing all images from virtual machine',
    ],
    '216': [
        'RemoveAllVmTemplateImageTemplatesCommand',
        'Removing all images of a template',
    ],
    '226': [
        'MoveOrCopyTemplateCommand',
        'Moving or Copying a template image',
    ],
    '228': [
        'MoveMultipleImageGroupCommand',
        'Moving multiple disks',
    ],
    '314': [
        'RemoveVmFromPoolCommand ',
        'Removing virtual maching from pool',
    ],
    '1010': [
        'LiveMigrateDisk',
        'Migrating Live Disk',
    ],
    '1011': [
        'MoveDisk',
        'Move Disk',
    ],
}
