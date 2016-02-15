package org.ovirt.engine.core.searchbackend;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.compat.StringFormat;
import org.ovirt.engine.core.searchbackend.gluster.GlusterVolumeConditionFieldAutoCompleter;
import org.ovirt.engine.core.searchbackend.gluster.GlusterVolumeCrossRefAutoCompleter;

public class SearchObjectAutoCompleter extends SearchObjectsBaseAutoCompleter {
    private final Map<String, String[]> mJoinDictionary = new HashMap<String, String[]>();

    public SearchObjectAutoCompleter() {

        mVerbs.add(SearchObjects.VM_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_POOL_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.VDS_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.TEMPLATE_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.AUDIT_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_USER_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_GROUP_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_CLUSTER_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.DISK_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_STORAGE_POOL_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME);
        mVerbs.add(SearchObjects.GLUSTER_VOLUME_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.QUOTA_OBJ_NAME);
        mVerbs.add(SearchObjects.NETWORK_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.PROVIDER_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.INSTANCE_TYPE_PLU_OBJ_NAME);
        mVerbs.add(SearchObjects.IMAGE_TYPE_PLU_OBJ_NAME);

        buildCompletions();
        mVerbs.add(SearchObjects.VM_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_POOL_OBJ_NAME);
        mVerbs.add(SearchObjects.DISK_OBJ_NAME);
        mVerbs.add(SearchObjects.VDS_OBJ_NAME);
        mVerbs.add(SearchObjects.TEMPLATE_OBJ_NAME);
        mVerbs.add(SearchObjects.AUDIT_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_USER_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_GROUP_OBJ_NAME);
        mVerbs.add(SearchObjects.VDC_CLUSTER_OBJ_NAME);
        mVerbs.add(SearchObjects.GLUSTER_VOLUME_OBJ_NAME);
        mVerbs.add(SearchObjects.NETWORK_OBJ_NAME);
        mVerbs.add(SearchObjects.PROVIDER_OBJ_NAME);
        mVerbs.add(SearchObjects.INSTANCE_TYPE_OBJ_NAME);
        mVerbs.add(SearchObjects.IMAGE_TYPE_OBJ_NAME);

        // vms - vds
        addJoin(SearchObjects.VM_OBJ_NAME,
                "run_on_vds",
                SearchObjects.VDS_OBJ_NAME,
                "vds_id");

        // vms - vmt
        addJoin(SearchObjects.VM_OBJ_NAME,
                "vmt_guid",
                SearchObjects.TEMPLATE_OBJ_NAME,
                "vmt_guid");

        // vms - users
        addJoin(SearchObjects.VM_OBJ_NAME,
                "vm_guid",
                SearchObjects.VDC_USER_OBJ_NAME,
                "vm_guid");

        // vms - audit
        addJoin(SearchObjects.VM_OBJ_NAME,
                "vm_guid",
                SearchObjects.AUDIT_OBJ_NAME,
                "vm_id");

        // vms - vm network interface
        addJoin(SearchObjects.VM_OBJ_NAME,
                "vm_guid",
                SearchObjects.VM_NETWORK_INTERFACE_OBJ_NAME,
                "vm_guid");

        // vms - storage domain
        addJoin(SearchObjects.VM_OBJ_NAME,
                "storage_id",
                SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME,
                "id");

        // templates - storage domain
        addJoin(SearchObjects.TEMPLATE_OBJ_NAME,
                "storage_id",
                SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME,
                "id");

        // image-type - storage domain
        addJoin(SearchObjects.IMAGE_TYPE_OBJ_NAME,
                "storage_id",
                SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME,
                "id");

        // templates - vm template network interface
        addJoin(SearchObjects.TEMPLATE_OBJ_NAME,
                "vmt_guid",
                SearchObjects.VM_NETWORK_INTERFACE_OBJ_NAME,
                "vmt_guid");

        // instance-types - vm template network interface
        addJoin(SearchObjects.INSTANCE_TYPE_OBJ_NAME,
                "vmt_guid",
                SearchObjects.VM_NETWORK_INTERFACE_OBJ_NAME,
                "vmt_guid");

        // vds - storage domain
        addJoin(SearchObjects.VDS_OBJ_NAME,
                "storage_id",
                SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME,
                "id");

        // cluster - storage domain
        addJoin(SearchObjects.VDC_CLUSTER_OBJ_NAME,
                "storage_id",
                SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME,
                "id");

        // disk - storage domain images
        addJoin(SearchObjects.DISK_OBJ_NAME,
                "image_guid",
                SearchObjects.VDC_STORAGE_DOMAIN_IMAGE_OBJ_NAME,
                "image_guid");

        // storage domain images - storage domain
        addJoin(SearchObjects.VDC_STORAGE_DOMAIN_IMAGE_OBJ_NAME,
                "id",
                SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME,
                "id");

        // vds - audit
        addJoin(SearchObjects.VDS_OBJ_NAME,
                "vds_id",
                SearchObjects.AUDIT_OBJ_NAME,
                "vds_id");

        // users - audit
        addJoin(SearchObjects.VDC_USER_OBJ_NAME,
                "user_id",
                SearchObjects.AUDIT_OBJ_NAME,
                "user_id");

        // Datacenter(Storage_pool) - Cluster(vds group)
        addJoin(SearchObjects.VDC_STORAGE_POOL_OBJ_NAME,
                "id",
                SearchObjects.VDC_CLUSTER_OBJ_NAME,
                "storage_pool_id");

        // Datacenter(Storage_pool) - Storage Domain
        addJoin(SearchObjects.VDC_STORAGE_POOL_OBJ_NAME,
                "id",
                SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME,
                "storage_pool_id");

        // Datacenter(Storage_pool) - Disk
        addJoin(SearchObjects.VDC_STORAGE_POOL_OBJ_NAME,
                "id",
                SearchObjects.DISK_OBJ_NAME,
                "storage_pool_id");

        // audit - cluster
        addJoin(SearchObjects.VDC_CLUSTER_OBJ_NAME,
                "vds_group_id",
                SearchObjects.AUDIT_OBJ_NAME,
                "vds_group_id");

        // gluster volume - cluster
        addJoin(SearchObjects.GLUSTER_VOLUME_OBJ_NAME,
                "cluster_id",
                SearchObjects.VDC_CLUSTER_OBJ_NAME,
                "vds_group_id");

        // cluster - network
        addJoin(SearchObjects.VDC_CLUSTER_OBJ_NAME,
                "vds_group_id",
                SearchObjects.NETWORK_CLUSTER_OBJ_NAME,
                "cluster_id");

        // network - cluster
        addJoin(SearchObjects.NETWORK_OBJ_NAME,
                "id",
                SearchObjects.NETWORK_CLUSTER_OBJ_NAME,
                "network_id");

        // network - host
        addJoin(SearchObjects.NETWORK_OBJ_NAME,
                "id",
                SearchObjects.NETWORK_HOST_OBJ_NAME,
                "network_id");

        // audit - gluster volume
        addJoin(SearchObjects.GLUSTER_VOLUME_OBJ_NAME,
                "id",
                SearchObjects.AUDIT_OBJ_NAME,
                "gluster_volume_id");

        // quota - audit
        addJoin(SearchObjects.AUDIT_OBJ_NAME,
                "quota_id",
                SearchObjects.QUOTA_OBJ_NAME,
                "quota_id");

        // data center - network
        addJoin(SearchObjects.VDC_STORAGE_POOL_OBJ_NAME,
                "id",
                SearchObjects.NETWORK_OBJ_NAME,
                "storage_pool_id");

        // host interface - host
        addJoin(SearchObjects.VDS_OBJ_NAME,
                "vds_id",
                SearchObjects.VDS_NETWORK_INTERFACE_OBJ_NAME,
                "vds_id");

        // cluster - vm pool
        addJoin(SearchObjects.VDC_CLUSTER_OBJ_NAME,
                "vds_group_id",
                SearchObjects.VDC_POOL_OBJ_NAME,
                "vds_group_id");

        // provider - network
        addJoin(SearchObjects.PROVIDER_OBJ_NAME,
                "id",
                SearchObjects.NETWORK_OBJ_NAME,
                "provider_network_provider_id");

        // users - template
        addJoin(SearchObjects.VDC_USER_OBJ_NAME,
                "vm_guid",
                SearchObjects.TEMPLATE_OBJ_NAME,
                "vmt_guid");

        // users - host
        addJoin(SearchObjects.VDC_USER_OBJ_NAME,
                "vm_guid",
                SearchObjects.VDS_OBJ_NAME,
                "vds_id");
    }

    private void addJoin(String firstObj, String firstColumnName, String secondObj, String secondColumnName) {
        mJoinDictionary.put(firstObj + "." + secondObj, new String[] { firstColumnName, secondColumnName });
        mJoinDictionary.put(secondObj + "." + firstObj, new String[] { secondColumnName, firstColumnName });
    }

    private static final class EntitySearchInfo {
        public EntitySearchInfo(IAutoCompleter crossRefAutoCompleter,
                IConditionFieldAutoCompleter conditionFieldAutoCompleter,
                String relatedTableNameWithOutTags,
                String relatedTableName,
                String primeryKeyName,
                String defaultSort) {
            this.crossRefAutoCompleter = crossRefAutoCompleter;
            this.conditionFieldAutoCompleter = conditionFieldAutoCompleter;
            this.relatedTableNameWithOutTags = relatedTableNameWithOutTags;
            this.relatedTableName = relatedTableName;
            this.primeryKeyName = primeryKeyName;
            this.defaultSort = defaultSort;
        }

        final IAutoCompleter crossRefAutoCompleter;
        final IConditionFieldAutoCompleter conditionFieldAutoCompleter;
        final String relatedTableNameWithOutTags;
        final String relatedTableName;
        final String primeryKeyName;
        final String defaultSort;
    }

    @SuppressWarnings("serial")
    private final static Map<String, EntitySearchInfo> entitySearchInfo = Collections.unmodifiableMap(
            new HashMap<String, SearchObjectAutoCompleter.EntitySearchInfo>() {
                {
                    put(SearchObjects.AUDIT_OBJ_NAME, new EntitySearchInfo(new AuditCrossRefAutoCompleter(),
                            new AuditLogConditionFieldAutoCompleter(),
                            null,
                            "audit_log",
                            "audit_log_id",
                            "audit_log_id DESC "));
                    put(SearchObjects.TEMPLATE_OBJ_NAME, new EntitySearchInfo(new TemplateCrossRefAutoCompleter(),
                            new VmTemplateConditionFieldAutoCompleter(),
                            "vm_templates_view",
                            "vm_templates_storage_domain",
                            "vmt_guid",
                            "name ASC "));
                    put(SearchObjects.INSTANCE_TYPE_OBJ_NAME, new EntitySearchInfo(new TemplateCrossRefAutoCompleter(),
                            new VmTemplateConditionFieldAutoCompleter(),
                            "instance_types_view",
                            "instance_types_storage_domain",
                            "vmt_guid",
                            "name ASC "));
                    put(SearchObjects.IMAGE_TYPE_OBJ_NAME, new EntitySearchInfo(new TemplateCrossRefAutoCompleter(),
                            new VmTemplateConditionFieldAutoCompleter(),
                            "image_types_view",
                            "image_types_storage_domain",
                            "vmt_guid",
                            "name ASC "));
                    put(SearchObjects.VDC_USER_OBJ_NAME, new EntitySearchInfo(new UserCrossRefAutoCompleter(),
                            new VdcUserConditionFieldAutoCompleter(),
                            "vdc_users",
                            "vdc_users_with_tags",
                            "user_id",
                            "name ASC "));
                    put(SearchObjects.VDC_GROUP_OBJ_NAME, new EntitySearchInfo(
                            null,
                            new VdcGroupConditionFieldAutoCompleter(),
                            "ad_groups",
                            "ad_groups",
                            "id",
                            "name ASC "));
                    put(SearchObjects.VDS_OBJ_NAME, new EntitySearchInfo(new VdsCrossRefAutoCompleter(),
                            new VdsConditionFieldAutoCompleter(),
                            "vds",
                            "vds_with_tags",
                            "vds_id",
                            "vds_name ASC "));
                    put(SearchObjects.VM_OBJ_NAME, new EntitySearchInfo(new VmCrossRefAutoCompleter(),
                            new VmConditionFieldAutoCompleter(),
                            "vms",
                            "vms_with_tags",
                            "vm_guid",
                            "vm_name ASC "));
                    put(SearchObjects.VDC_CLUSTER_OBJ_NAME, new EntitySearchInfo(new ClusterCrossRefAutoCompleter(),
                            new ClusterConditionFieldAutoCompleter(),
                            "vds_groups_view",
                            "vds_groups_storage_domain",
                            "vds_group_id",
                            "name ASC"));
                    put(SearchObjects.QUOTA_OBJ_NAME, new EntitySearchInfo(new QuotaConditionFieldAutoCompleter(),
                            new QuotaConditionFieldAutoCompleter(),
                            "quota_view",
                            "quota_view",
                            "quota_id",
                            "quota_name ASC"));
                    put(SearchObjects.VDC_STORAGE_POOL_OBJ_NAME,
                            new EntitySearchInfo(new StoragePoolCrossRefAutoCompleter(),
                                    new StoragePoolFieldAutoCompleter(),
                                    "storage_pool",
                                    "storage_pool_with_storage_domain",
                                    "id",
                                    "name ASC "));
                    put(SearchObjects.DISK_OBJ_NAME, new EntitySearchInfo(new DiskCrossRefAutoCompleter(),
                            new DiskConditionFieldAutoCompleter(),
                            "all_disks",
                            "all_disks",
                            "disk_id",
                            "disk_alias ASC, disk_id ASC "));
                    put(SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME,
                            new EntitySearchInfo(new StorageDomainCrossRefAutoCompleter(),
                                    new StorageDomainFieldAutoCompleter(),
                                    "storage_domains_for_search",
                                    "storage_domains_with_hosts_view",
                                    "id",
                                    "storage_name ASC "));
                    put(SearchObjects.VDC_STORAGE_DOMAIN_IMAGE_OBJ_NAME,
                            new EntitySearchInfo(null,
                                    null,
                                    null,
                                    "vm_images_storage_domains_view",
                                    "image_guid",
                                    "disk_alias ASC, disk_id ASC "));
                    put(SearchObjects.GLUSTER_VOLUME_OBJ_NAME,
                            new EntitySearchInfo(GlusterVolumeCrossRefAutoCompleter.INSTANCE,
                                    GlusterVolumeConditionFieldAutoCompleter.INSTANCE,
                                    null,
                                    "gluster_volumes_view",
                                    "id",
                                    "vol_name ASC "));
                    put(SearchObjects.VDC_POOL_OBJ_NAME, new EntitySearchInfo(null,
                            new PoolConditionFieldAutoCompleter(),
                            null,
                            "vm_pools_full_view",
                            "vm_pool_id",
                            "vm_pool_name ASC "));
                    put(SearchObjects.NETWORK_OBJ_NAME, new EntitySearchInfo(new NetworkCrossRefAutoCompleter(),
                            new NetworkConditionFieldAutoCompleter(),
                            "network_view",
                            "network_view",
                            "id",
                            "storage_pool_name ASC, name ASC"));
                    put(SearchObjects.VDS_NETWORK_INTERFACE_OBJ_NAME, new EntitySearchInfo(null,
                            new NetworkInterfaceConditionFieldAutoCompleter(),
                            "vds_interface",
                            "vds_interface",
                            "vds_id",
                            "name ASC"));
                    put(SearchObjects.VM_NETWORK_INTERFACE_OBJ_NAME, new EntitySearchInfo(null,
                            new NetworkInterfaceConditionFieldAutoCompleter(),
                            "vm_interface_view",
                            "vm_interface_view",
                            "name",
                            "vm_id ASC"));
                    put(SearchObjects.NETWORK_CLUSTER_OBJ_NAME,
                            new EntitySearchInfo(null,
                            new NetworkClusterConditionFieldAutoCompleter(),
                            "network_cluster_view",
                            "network_cluster_view",
                            "vds_group_id",
                            "vds_group_name ASC"));
                    put(SearchObjects.NETWORK_HOST_OBJ_NAME, new EntitySearchInfo(null,
                            new NetworkHostConditionFieldAutoCompleter(),
                            "network_vds_view",
                            "network_vds_view",
                            "vds_name",
                            "network_name ASC"));
                    put(SearchObjects.PROVIDER_OBJ_NAME, new EntitySearchInfo(null,
                            new ProviderConditionFieldAutoCompleter(),
                            "providers",
                            "providers",
                            "id",
                            "name ASC"));
                }
            });

    static EntitySearchInfo getEntitySearchInfo(String key) {
        return entitySearchInfo.get(singular(key));
    }

    @SuppressWarnings("serial")
    private final static Map<String, String> singulars = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put(SearchObjects.AUDIT_PLU_OBJ_NAME, SearchObjects.AUDIT_OBJ_NAME);
            put(SearchObjects.TEMPLATE_PLU_OBJ_NAME, SearchObjects.TEMPLATE_OBJ_NAME);
            put(SearchObjects.INSTANCE_TYPE_PLU_OBJ_NAME, SearchObjects.INSTANCE_TYPE_OBJ_NAME);
            put(SearchObjects.IMAGE_TYPE_PLU_OBJ_NAME, SearchObjects.IMAGE_TYPE_OBJ_NAME);
            put(SearchObjects.VDC_USER_PLU_OBJ_NAME, SearchObjects.VDC_USER_OBJ_NAME);
            put(SearchObjects.VDC_GROUP_PLU_OBJ_NAME, SearchObjects.VDC_GROUP_OBJ_NAME);
            put(SearchObjects.VDS_PLU_OBJ_NAME, SearchObjects.VDS_OBJ_NAME);
            put(SearchObjects.VM_PLU_OBJ_NAME, SearchObjects.VM_OBJ_NAME);
            put(SearchObjects.VDC_CLUSTER_PLU_OBJ_NAME, SearchObjects.VDC_CLUSTER_OBJ_NAME);
            put(SearchObjects.QUOTA_PLU_OBJ_NAME, SearchObjects.QUOTA_OBJ_NAME);
            put(SearchObjects.DISK_PLU_OBJ_NAME, SearchObjects.DISK_OBJ_NAME);
            put(SearchObjects.GLUSTER_VOLUME_PLU_OBJ_NAME, SearchObjects.GLUSTER_VOLUME_OBJ_NAME);
            put(SearchObjects.VDC_POOL_PLU_OBJ_NAME, SearchObjects.VDC_POOL_OBJ_NAME);
            put(SearchObjects.VDC_STORAGE_DOMAIN_PLU_OBJ_NAME, SearchObjects.VDC_STORAGE_DOMAIN_OBJ_NAME);
            put(SearchObjects.NETWORK_PLU_OBJ_NAME, SearchObjects.NETWORK_OBJ_NAME);
            put(SearchObjects.PROVIDER_PLU_OBJ_NAME, SearchObjects.PROVIDER_OBJ_NAME);
        }
    });

    static String singular(String key) {
        return singulars.containsKey(key) ? singulars.get(key) : key;
    }

    public IAutoCompleter getCrossRefAutoCompleter(String obj) {
        if (obj == null) {
            return null;
        }
        if (getEntitySearchInfo(obj) != null) {
            return getEntitySearchInfo(obj).crossRefAutoCompleter;
        }

        else {
            return null;
        }
    }

    public boolean isCrossReference(String text, String obj) {
        IAutoCompleter completer = getCrossRefAutoCompleter(obj);
        if (completer != null) {
            return completer.validate(text);
        }
        return false;
    }

    public String getInnerJoin(String searchObj, String crossRefObj, boolean useTags) {
        final String[] joinKey = mJoinDictionary.get(StringFormat.format("%1$s.%2$s", searchObj, crossRefObj));
        // For joins, the table we join with is always the full view (including the tags)
        final String crossRefTable = getRelatedTableName(crossRefObj, true);
        final String searchObjTable = getRelatedTableName(searchObj, useTags);

        return StringFormat.format(" LEFT OUTER JOIN %3$s ON %1$s.%2$s=%3$s.%4$s ", searchObjTable, joinKey[0],
                crossRefTable, joinKey[1]);
    }

    public IConditionFieldAutoCompleter getFieldAutoCompleter(String obj) {
        if (obj == null) {
            return null;
        }
        if (getEntitySearchInfo(obj) != null) {
            return getEntitySearchInfo(obj).conditionFieldAutoCompleter;
        }
        return null;
    }

    public String getRelatedTableName(String obj, String fieldName) {
        return getRelatedTableName(obj, fieldName == null || fieldName.length() == 0
                || fieldName.toLowerCase().equalsIgnoreCase("tag"));
    }

    public String getRelatedTableName(String obj, boolean useTags) {
        if (useTags) {
            return getRelatedTableName(obj);
        }

        return getRelatedTableNameWithoutTags(obj);
    }

    private String getRelatedTableNameWithoutTags(String obj) {
        if (obj == null) {
            return null;
        } else if (getEntitySearchInfo(obj) != null && getEntitySearchInfo(obj).relatedTableNameWithOutTags != null) {
            return getEntitySearchInfo(obj).relatedTableNameWithOutTags;
        }
        else {
            return getRelatedTableName(obj);
        }
    }

    private String getRelatedTableName(String obj) {
        if (obj == null) {
            return null;
        } else if (getEntitySearchInfo(obj) != null) {
            return getEntitySearchInfo(obj).relatedTableName;
        }
        return null;
    }

    public String getPrimeryKeyName(String obj) {
        if (getEntitySearchInfo(obj) != null) {
            return getEntitySearchInfo(obj).primeryKeyName;
        }
        return null;
    }

    public IAutoCompleter getFieldRelationshipAutoCompleter(String obj, String fieldName) {
        IConditionFieldAutoCompleter curConditionFieldAC = getFieldAutoCompleter(obj);
        if (curConditionFieldAC != null) {
            return curConditionFieldAC.getFieldRelationshipAutoCompleter(fieldName);
        }
        return null;
    }

    public IAutoCompleter getObjectRelationshipAutoCompleter() {
        return StringConditionRelationAutoCompleter.INSTANCE;
    }

    public IConditionValueAutoCompleter getFieldValueAutoCompleter(String obj, String fieldName) {
        final IConditionFieldAutoCompleter curConditionFieldAC = getFieldAutoCompleter(obj);
        if (curConditionFieldAC != null) {
            return curConditionFieldAC.getFieldValueAutoCompleter(fieldName);
        }
        return null;
    }

    public String getDefaultSort(String obj) {
        if (obj == null) {
            return "";
        }
        if (getEntitySearchInfo(obj) != null) {
            return getEntitySearchInfo(obj).defaultSort;
        }
        return "";
    }
}
