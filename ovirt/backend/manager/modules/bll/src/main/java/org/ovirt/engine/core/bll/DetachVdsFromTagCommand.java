package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.AttachVdsToTagParameters;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class DetachVdsFromTagCommand<T extends AttachVdsToTagParameters> extends VdsTagMapBase<T> {

    public DetachVdsFromTagCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        for (Guid vdsId : getVdsList()) {
            if (getTagId() != null && DbFacade.getInstance().getTagDao().getTagVdsByTagIdAndByVdsId(getTagId(), vdsId) != null) {
                VDS vds = DbFacade.getInstance().getVdsDao().get(vdsId);
                if (vds != null) {
                    appendCustomValue("VdsNames", vds.getName(), ", ");
                }
                DbFacade.getInstance().getTagDao().detachVdsFromTag(getTagId(), vdsId);
                setSucceeded(true);
            }
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_DETACH_VDS_FROM_TAG : AuditLogType.USER_DETACH_VDS_FROM_TAG_FAILED;
    }
}
