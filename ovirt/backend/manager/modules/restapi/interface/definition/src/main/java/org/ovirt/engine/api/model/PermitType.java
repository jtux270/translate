/*
* Copyright (c) 2010 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*           http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ovirt.engine.api.model;

public enum PermitType {
    CREATE_VM,
    DELETE_VM,
    EDIT_VM_PROPERTIES,
    VM_BASIC_OPERATIONS,
    CHANGE_VM_CD,
    MIGRATE_VM,
    CONNECT_TO_VM,
    IMPORT_EXPORT_VM,
    CONFIGURE_VM_NETWORK,
    CONFIGURE_VM_STORAGE,
    MOVE_VM,
    MANIPULATE_VM_SNAPSHOTS,
    RECONNECT_TO_VM,
    CHANGE_VM_CUSTOM_PROPERTIES,
    EDIT_ADMIN_VM_PROPERTIES,
    CREATE_INSTANCE,
    // host (vds) actions groups
    CREATE_HOST,
    EDIT_HOST_CONFIGURATION,
    DELETE_HOST,
    MANIPULATE_HOST,
    CONFIGURE_HOST_NETWORK,
    // templates actions groups
    CREATE_TEMPLATE,
    EDIT_TEMPLATE_PROPERTIES,
    EDIT_ADMIN_TEMPLATE_PROPERTIES,
    DELETE_TEMPLATE,
    COPY_TEMPLATE,
    CONFIGURE_TEMPLATE_NETWORK,
    // vm pools actions groups
    CREATE_VM_POOL,
    EDIT_VM_POOL_CONFIGURATION,
    DELETE_VM_POOL,
    VM_POOL_BASIC_OPERATIONS,
    // clusters actions groups
    CREATE_CLUSTER,
    EDIT_CLUSTER_CONFIGURATION,
    DELETE_CLUSTER,
    CONFIGURE_CLUSTER_NETWORK,
    // users and MLA actions groups
    MANIPULATE_USERS,
    MANIPULATE_ROLES,
    MANIPULATE_PERMISSIONS,
    ADD_USERS_AND_GROUPS_FROM_DIRECTORY,

    // storage domains actions groups
    CREATE_STORAGE_DOMAIN,
    EDIT_STORAGE_DOMAIN_CONFIGURATION,
    DELETE_STORAGE_DOMAIN,
    MANIPULATE_STORAGE_DOMAIN,
    // storage pool actions groups
    CREATE_STORAGE_POOL,
    DELETE_STORAGE_POOL,
    EDIT_STORAGE_POOL_CONFIGURATION,
    // network's actions groups
    CONFIGURE_STORAGE_POOL_NETWORK,
    CREATE_STORAGE_POOL_NETWORK,
    DELETE_STORAGE_POOL_NETWORK,
    ASSIGN_CLUSTER_NETWORK,

    // rhevm generic
    CONFIGURE_RHEVM,

    // Quota
    CONFIGURE_QUOTA,
    CONSUME_QUOTA,

    // Gluster
    CREATE_GLUSTER_VOLUME,
    MANIPULATE_GLUSTER_VOLUME,
    DELETE_GLUSTER_VOLUME,
    MANIPULATE_GLUSTER_HOOK,
    MANIPULATE_GLUSTER_SERVICE,

    // Disks action groups
    CREATE_DISK,
    ATTACH_DISK,
    EDIT_DISK_PROPERTIES,
    CONFIGURE_DISK_STORAGE,
    DELETE_DISK,
    CONFIGURE_SCSI_GENERIC_IO,
    ACCESS_IMAGE_STORAGE,

    // Vm Interface action groups
    CONFIGURE_NETWORK_VNIC_PROFILE,
    CREATE_NETWORK_VNIC_PROFILE,
    DELETE_NETWORK_VNIC_PROFILE,


    // Login action group
    LOGIN,
    INJECT_EXTERNAL_EVENTS,
    INJECT_EXTERNAL_TASKS,
    TAG_MANAGEMENT,
    BOOKMARK_MANAGEMENT,
    EVENT_NOTIFICATION_MANAGEMENT,
    AUDIT_LOG_MANAGEMENT,
    // affinity groups CRUD commands
    MANIPULATE_AFFINITY_GROUPS,

    // disk profile
    CONFIGURE_STORAGE_DISK_PROFILE,
    CREATE_STORAGE_DISK_PROFILE,
    DELETE_STORAGE_DISK_PROFILE,
    ATTACH_DISK_PROFILE;


    public String value() {
        return name().toLowerCase();
    }
}
