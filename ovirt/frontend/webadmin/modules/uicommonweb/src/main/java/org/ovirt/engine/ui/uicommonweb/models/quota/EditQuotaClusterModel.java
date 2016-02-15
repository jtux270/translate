package org.ovirt.engine.ui.uicommonweb.models.quota;

import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IntegerValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

public class EditQuotaClusterModel extends EntityModel<QuotaVdsGroup> {
    EntityModel<Boolean> unlimitedMem;
    EntityModel<Boolean> unlimitedCpu;

    EntityModel<Boolean> specificMem;
    EntityModel<Boolean> specificCpu;

    EntityModel<Long> specificMemValue;
    EntityModel<Integer> specificCpuValue;

    public EntityModel<Boolean> getUnlimitedMem() {
        return unlimitedMem;
    }

    public void setUnlimitedMem(EntityModel<Boolean> unlimitedMem) {
        this.unlimitedMem = unlimitedMem;
    }

    public EntityModel<Boolean> getUnlimitedCpu() {
        return unlimitedCpu;
    }

    public void setUnlimitedCpu(EntityModel<Boolean> unlimitedCpu) {
        this.unlimitedCpu = unlimitedCpu;
    }

    public EntityModel<Boolean> getSpecificMem() {
        return specificMem;
    }

    public void setSpecificMem(EntityModel<Boolean> specificMem) {
        this.specificMem = specificMem;
    }

    public EntityModel<Boolean> getSpecificCpu() {
        return specificCpu;
    }

    public void setSpecificCpu(EntityModel<Boolean> specificCpu) {
        this.specificCpu = specificCpu;
    }

    public EntityModel<Long> getSpecificMemValue() {
        return specificMemValue;
    }

    public void setSpecificMemValue(EntityModel<Long> specificMemValue) {
        this.specificMemValue = specificMemValue;
    }

    public EntityModel<Integer> getSpecificCpuValue() {
        return specificCpuValue;
    }

    public void setSpecificCpuValue(EntityModel<Integer> specificCpuValue) {
        this.specificCpuValue = specificCpuValue;
    }

    public EditQuotaClusterModel() {
        setSpecificMem(new EntityModel<Boolean>());
        getSpecificMem().setEntity(true);
        setUnlimitedMem(new EntityModel<Boolean>());
        getUnlimitedMem().setEntity(false);
        setSpecificMemValue(new EntityModel<Long>());
        getUnlimitedMem().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (getUnlimitedMem().getEntity()) {
                    getSpecificMem().setEntity(false);
                    getSpecificMemValue().setIsChangable(false);
                }
            }
        });

        getSpecificMem().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (getSpecificMem().getEntity()) {
                    getUnlimitedMem().setEntity(false);
                    getSpecificMemValue().setIsChangable(true);
                }
            }
        });

        setSpecificCpu(new EntityModel<Boolean>());
        setUnlimitedCpu(new EntityModel<Boolean>());
        setSpecificCpuValue(new EntityModel<Integer>());
        getUnlimitedCpu().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (getUnlimitedCpu().getEntity()) {
                    getSpecificCpu().setEntity(false);
                    getSpecificCpuValue().setIsChangable(false);
                }
            }
        });

        getSpecificCpu().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (getSpecificCpu().getEntity()) {
                    getUnlimitedCpu().setEntity(false);
                    getSpecificCpuValue().setIsChangable(true);
                }
            }
        });
    }

    public boolean validate() {
        IntegerValidation intValidation = new IntegerValidation();
        intValidation.setMinimum(1);
        getSpecificMemValue().setIsValid(true);
        getSpecificCpuValue().setIsValid(true);
        if (getSpecificMem().getEntity()) {
            getSpecificMemValue().validateEntity(new IValidation[] { intValidation, new NotEmptyValidation() });
        }
        if (getSpecificCpu().getEntity()) {
            getSpecificCpuValue().validateEntity(new IValidation[] { intValidation, new NotEmptyValidation() });
        }
        return getSpecificMemValue().getIsValid() && getSpecificCpuValue().getIsValid();
    }
}
