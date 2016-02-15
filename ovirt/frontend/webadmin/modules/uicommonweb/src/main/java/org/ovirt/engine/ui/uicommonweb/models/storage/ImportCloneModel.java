package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

public class ImportCloneModel extends ConfirmationModel {

    EntityModel<Boolean> noClone;
    EntityModel<Boolean> clone;
    EntityModel<Boolean> applyToAll;

    EntityModel<String> suffix;
    EntityModel<String> name;

    public EntityModel<Boolean> getNoClone() {
        return noClone;
    }

    public void setNoClone(EntityModel<Boolean> noClone) {
        this.noClone = noClone;
    }

    public EntityModel<Boolean> getClone() {
        return clone;
    }

    public void setClone(EntityModel<Boolean> clone) {
        this.clone = clone;
    }

    public EntityModel<Boolean> getApplyToAll() {
        return applyToAll;
    }

    public void setApplyToAll(EntityModel<Boolean> applyToAll) {
        this.applyToAll = applyToAll;
    }

    public EntityModel<String> getName() {
        return name;
    }

    public void setName(EntityModel<String> name) {
        this.name = name;
    }

    public EntityModel<String> getSuffix() {
        return suffix;
    }

    public void setSuffix(EntityModel<String> suffix) {
        this.suffix = suffix;
    }

    public ImportCloneModel() {
        setNoClone(new EntityModel<Boolean>());
        getNoClone().setEntity(false);
        setClone(new EntityModel<Boolean>());
        getClone().setEntity(true);
        setName(new EntityModel<String>());
        setApplyToAll(new EntityModel<Boolean>());
        getApplyToAll().setEntity(false);
        setSuffix(new EntityModel<String>());
        getSuffix().setIsChangable(false);
        getSuffix().setEntity("_Copy"); //$NON-NLS-1$
        getClone().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean value = getClone().getEntity();
                if (value) {
                    getNoClone().setEntity(false);
                    if (getApplyToAll().getEntity()) {
                        getSuffix().setIsChangable(true);
                    } else {
                        getName().setIsChangable(true);
                    }
                }
            }
        });
        getNoClone().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                boolean value = getNoClone().getEntity();
                if (value) {
                    getClone().setEntity(false);
                    getName().setIsChangable(false);
                    getSuffix().setIsChangable(false);
                }
            }
        });
        getApplyToAll().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (!getNoClone().getEntity()) {
                    Boolean value = getApplyToAll().getEntity();
                    getSuffix().setIsChangable(value);
                    getName().setIsChangable(!value);
                }
            }
        });
    }
}
