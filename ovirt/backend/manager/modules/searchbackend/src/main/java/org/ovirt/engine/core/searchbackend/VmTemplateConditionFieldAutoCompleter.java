package org.ovirt.engine.core.searchbackend;

import java.util.Date;
import java.util.UUID;

import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.DateEnumForSearch;
import org.ovirt.engine.core.common.businessentities.VmTemplateStatus;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;

public class VmTemplateConditionFieldAutoCompleter extends BaseConditionFieldAutoCompleter {

    public static final String NAME = "NAME";
    public static final String COMMENT = "COMMENT";
    public static final String OS = "OS";
    public static final String CREATIONDATE = "CREATIONDATE";
    public static final String CHILDCOUNT = "CHILDCOUNT";
    public static final String MEM = "MEM";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String STATUS = "STATUS";
    public static final String CLUSTER = "CLUSTER";
    public static final String DATACENTER = "DATACENTER";
    public static final String QUOTA = "QUOTA";
    public static final String ARCHITECTURE = "ARCHITECTURE";
    public static final String VMT_ID = "_VMT_ID";
    public static final String VERSION_NAME_AND_NUMBER = "VERSION_NAME_AND_NUMBER";

    public VmTemplateConditionFieldAutoCompleter() {
        mVerbs.add(NAME);
        mVerbs.add(COMMENT);
        mVerbs.add(OS);
        mVerbs.add(CREATIONDATE);
        mVerbs.add(CHILDCOUNT);
        mVerbs.add(MEM);
        mVerbs.add(DESCRIPTION);
        mVerbs.add(STATUS);
        mVerbs.add(CLUSTER);
        mVerbs.add(DATACENTER);
        mVerbs.add(QUOTA);
        mVerbs.add(ARCHITECTURE);
        mVerbs.add(VERSION_NAME_AND_NUMBER);

        buildCompletions();
        mVerbs.add(VMT_ID);
        // Building the types dict
        getTypeDictionary().put(NAME, String.class);
        getTypeDictionary().put(COMMENT, String.class);
        getTypeDictionary().put(OS, String.class);
        getTypeDictionary().put(CREATIONDATE, Date.class);
        getTypeDictionary().put(CHILDCOUNT, Integer.class);
        getTypeDictionary().put(MEM, Integer.class);
        getTypeDictionary().put(DESCRIPTION, String.class);
        getTypeDictionary().put(STATUS, VmTemplateStatus.class);
        getTypeDictionary().put(CLUSTER, String.class);
        getTypeDictionary().put(DATACENTER, String.class);
        getTypeDictionary().put(QUOTA, String.class);
        getTypeDictionary().put(VMT_ID, UUID.class);
        getTypeDictionary().put(DESCRIPTION, String.class);
        getTypeDictionary().put(ARCHITECTURE, ArchitectureType.class);
        getTypeDictionary().put(VERSION_NAME_AND_NUMBER, String.class);

        // building the ColumnName Dict
        columnNameDict.put(NAME, "name");
        columnNameDict.put(COMMENT, "free_text_comment");
        columnNameDict.put(OS, "os");
        columnNameDict.put(CREATIONDATE, "creation_date");
        columnNameDict.put(CHILDCOUNT, "child_count");
        columnNameDict.put(MEM, "mem_size_mb");
        columnNameDict.put(DESCRIPTION, "description");
        columnNameDict.put(STATUS, "status");
        columnNameDict.put(CLUSTER, "vds_group_name");
        columnNameDict.put(DATACENTER, "storage_pool_name");
        columnNameDict.put(QUOTA, "quota_name");
        columnNameDict.put(VMT_ID, "vmt_guid");
        columnNameDict.put(DESCRIPTION, "description");
        columnNameDict.put(ARCHITECTURE, "architecture");
        columnNameDict.put(VERSION_NAME_AND_NUMBER, "template_version_name, template_version_number");

        notFreeTextSearchableFieldsList.add(OS);
        // Building the validation dict
        buildBasicValidationTable();
    }

    @Override
    public IAutoCompleter getFieldRelationshipAutoCompleter(String fieldName) {
        if (CREATIONDATE.equals(fieldName)) {
            return TimeConditionRelationAutoCompleter.INSTANCE;
        } else if (CHILDCOUNT.equals(fieldName) || MEM.equals(fieldName)) {
            return NumericConditionRelationAutoCompleter.INSTANCE;
        } else {
            return StringConditionRelationAutoCompleter.INSTANCE;
        }
    }

    @Override
    public IConditionValueAutoCompleter getFieldValueAutoCompleter(String fieldName) {
        if (OS.equals(fieldName)) {
            return SimpleDependecyInjector.getInstance().get(OsValueAutoCompleter.class);
        } else if (CREATIONDATE.equals(fieldName)) {
            return new DateEnumValueAutoCompleter(DateEnumForSearch.class);
        } else if (STATUS.equals(fieldName)) {
            return new EnumValueAutoCompleter(VmTemplateStatus.class);
        } else if (QUOTA.equals(fieldName)) {
            return new NullableStringAutoCompleter();
        } else if (ARCHITECTURE.equals(fieldName)) {
            return new EnumValueAutoCompleter(ArchitectureType.class);
        }
        return null;
    }
}
