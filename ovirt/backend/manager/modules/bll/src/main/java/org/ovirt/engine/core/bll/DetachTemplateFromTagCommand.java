package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.AttachEntityToTagParameters;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class DetachTemplateFromTagCommand<T extends AttachEntityToTagParameters> extends TemplatesTagMapBase<T> {

    public DetachTemplateFromTagCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        if (getTagId() == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_TAG_ID_REQUIRED);
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        for (Guid templateGuid : getTemplatesList()) {
            if (DbFacade.getInstance().getTagDao().getTagTemplateByTagIdAndByTemplateId(getTagId(), templateGuid) != null) {
                VmTemplate template = DbFacade.getInstance().getVmTemplateDao().get(templateGuid);
                if (template != null) {
                    appendCustomValue("TemplatesNames", template.getName(), ", ");
                    DbFacade.getInstance().getTagDao().detachTemplateFromTag(getTagId(), templateGuid);
                }
            }
        }
        setSucceeded(true);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_DETACH_TEMPLATE_FROM_TAG : AuditLogType.USER_DETACH_TEMPLATE_FROM_TAG_FAILED;
    }
}
