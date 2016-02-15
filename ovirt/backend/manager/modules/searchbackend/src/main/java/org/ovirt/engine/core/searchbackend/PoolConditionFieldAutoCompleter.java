package org.ovirt.engine.core.searchbackend;

import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.VmPoolType;

public class PoolConditionFieldAutoCompleter extends BaseConditionFieldAutoCompleter {

    public static final String NAME = "NAME";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String TYPE = "TYPE";
    public static final String CLUSTER = "CLUSTER";
    public static final String DATACENTER = "DATACENTER";
    public static final String ARCHITECTURE = "ARCHITECTURE";
    public static final String ASSIGNED_VM_COUNT = "ASSIGNED_VM_COUNT";
    public static final String RUNNING_VM_COUNT = "RUNNING_VM_COUNT";

    public PoolConditionFieldAutoCompleter() {
        // Building the basic vervs Dict
        mVerbs.add(NAME);
        mVerbs.add(DESCRIPTION);
        mVerbs.add(TYPE);
        mVerbs.add(CLUSTER);
        mVerbs.add(DATACENTER);
        mVerbs.add(ARCHITECTURE);
        mVerbs.add(ASSIGNED_VM_COUNT);
        mVerbs.add(RUNNING_VM_COUNT);

        // Building the autoCompletion Dict
        buildCompletions();
        // Building the types dict
        getTypeDictionary().put(NAME, String.class);
        getTypeDictionary().put(DESCRIPTION, String.class);
        getTypeDictionary().put(TYPE, VmPoolType.class);
        getTypeDictionary().put(CLUSTER, String.class);
        getTypeDictionary().put(DATACENTER, String.class);
        getTypeDictionary().put(ARCHITECTURE, ArchitectureType.class);
        getTypeDictionary().put(ASSIGNED_VM_COUNT, Integer.class);
        getTypeDictionary().put(RUNNING_VM_COUNT, Integer.class);

        // building the ColumnName Dict
        columnNameDict.put(NAME, "vm_pool_name");
        columnNameDict.put(DESCRIPTION, "vm_pool_description");
        columnNameDict.put(TYPE, "vm_pool_type");
        columnNameDict.put(CLUSTER, "vds_group_name");
        columnNameDict.put(DATACENTER, "storage_pool_name");
        columnNameDict.put(ARCHITECTURE, "architecture");
        columnNameDict.put(ASSIGNED_VM_COUNT, "assigned_vm_count");
        columnNameDict.put(RUNNING_VM_COUNT, "vm_running_count");

        // Building the validation dict
        buildBasicValidationTable();
    }

    @Override
    public IAutoCompleter getFieldRelationshipAutoCompleter(String fieldName) {
        return StringConditionRelationAutoCompleter.INSTANCE;
    }

    @Override
    public IConditionValueAutoCompleter getFieldValueAutoCompleter(String fieldName) {
        IConditionValueAutoCompleter retval = null;
        if (TYPE.equals(fieldName)) {
            retval = new EnumValueAutoCompleter(VmPoolType.class);
        } else if (ARCHITECTURE.equals(fieldName)) {
            retval = new EnumValueAutoCompleter(ArchitectureType.class);
        }
        return retval;
    }
}
