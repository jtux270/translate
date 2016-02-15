package org.ovirt.engine.ui.uicommonweb.models.vms.instancetypes;

import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.models.vms.CustomInstanceType;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;

public class NewPoolInstanceTypeManager extends InstanceTypeManager {

    public NewPoolInstanceTypeManager(UnitVmModel model) {
        super(model);
    }

    @Override
    protected VmBase getSource() {
        if (!(getModel().getInstanceTypes().getSelectedItem() instanceof CustomInstanceType)) {
            return (VmBase) getModel().getInstanceTypes().getSelectedItem();
        } else {
            return getModel().getTemplate().getSelectedItem();
        }
    }

    @Override
    protected void updateBalloon(VmBase vmBase, boolean continueWithNext) {
        if (!isSourceCustomInstanceTypeOrBlankTemplate()) {
            super.updateBalloon(vmBase, continueWithNext);
        } else if (continueWithNext) {
            updateRngDevice(vmBase);
        }
    }

    @Override
    protected void maybeSetSingleQxlPci(VmBase vmBase) {
        boolean customInstanceTypeUsed = getModel().getInstanceTypes().getSelectedItem() instanceof CustomInstanceType;
        boolean blankTemplateUsed =
                getModel().getTemplate().getSelectedItem() != null
                        && getModel().getTemplate().getSelectedItem().getId().equals(Guid.Empty);
        if (customInstanceTypeUsed && blankTemplateUsed) {
            maybeSetEntity(getModel().getIsSingleQxlEnabled(), getModel().getIsQxlSupported());
        } else {
            super.maybeSetSingleQxlPci(vmBase);
        }
    }
}
