package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.TagsVmMapParameters;
import org.ovirt.engine.core.common.businessentities.TagsVmMap;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class UpdateTagsVmMapDefaultDisplayTypeCommand<T extends TagsVmMapParameters> extends TagsCommandBase<T> {
    public UpdateTagsVmMapDefaultDisplayTypeCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        TagsVmMap tagsVmMap;
        tagsVmMap = DbFacade.getInstance().getTagDao().getTagVmByTagIdAndByVmId(getParameters().getTagsVmMap().gettag_id(),
                getParameters().getTagsVmMap().getvm_id());
        if (tagsVmMap == null) {
            addCanDoActionMessage(VdcBllMessages.TAGS_SPECIFY_TAG_IS_NOT_EXISTS);
            return false;
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        DbFacade.getInstance().getTagDao().updateDefaultDisplayForVmTag(getParameters().getTagsVmMap());
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.UPDATE_TAGS_VM_DEFAULT_DISPLAY_TYPE
                : AuditLogType.UPDATE_TAGS_VM_DEFAULT_DISPLAY_TYPE_FAILED;
    }
}
