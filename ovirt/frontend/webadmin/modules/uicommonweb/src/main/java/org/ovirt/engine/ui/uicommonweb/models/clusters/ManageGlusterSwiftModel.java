package org.ovirt.engine.ui.uicommonweb.models.clusters;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterServiceStatus;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

public class ManageGlusterSwiftModel extends EntityModel {

    private EntityModel<GlusterServiceStatus> swiftStatus;

    private EntityModel<Boolean> startSwift;

    private EntityModel<Boolean> stopSwift;

    private EntityModel<Boolean> restartSwift;

    private EntityModel<Boolean> isManageServerLevel;

    private ListModel<GlusterSwiftServiceModel> hostServicesList;

    public ManageGlusterSwiftModel() {
        setSwiftStatus(new EntityModel<GlusterServiceStatus>());
        setStartSwift(new EntityModel<Boolean>(Boolean.FALSE));
        setStopSwift(new EntityModel<Boolean>(Boolean.FALSE));
        setRestartSwift(new EntityModel<Boolean>(Boolean.FALSE));
        setIsManageServerLevel(new EntityModel<Boolean>());
        setHostServicesList(new ListModel<GlusterSwiftServiceModel>());

        getIsManageServerLevel().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                getHostServicesList().setIsChangable(getIsManageServerLevel().getEntity());
            }
        });
        getIsManageServerLevel().setEntity(Boolean.FALSE);
    }

    public EntityModel<GlusterServiceStatus> getSwiftStatus() {
        return swiftStatus;
    }

    public void setSwiftStatus(EntityModel<GlusterServiceStatus> swiftStatus) {
        this.swiftStatus = swiftStatus;
    }

    public EntityModel<Boolean> getStartSwift() {
        return startSwift;
    }

    public void setStartSwift(EntityModel<Boolean> startSwift) {
        this.startSwift = startSwift;
    }

    public EntityModel<Boolean> getStopSwift() {
        return stopSwift;
    }

    public void setStopSwift(EntityModel<Boolean> stopSwift) {
        this.stopSwift = stopSwift;
    }

    public EntityModel<Boolean> getRestartSwift() {
        return restartSwift;
    }

    public void setRestartSwift(EntityModel<Boolean> restartSwift) {
        this.restartSwift = restartSwift;
    }

    public EntityModel<Boolean> getIsManageServerLevel() {
        return isManageServerLevel;
    }

    public void setIsManageServerLevel(EntityModel<Boolean> isManageServerLevel) {
        this.isManageServerLevel = isManageServerLevel;
    }

    public ListModel<GlusterSwiftServiceModel> getHostServicesList() {
        return hostServicesList;
    }

    public void setHostServicesList(ListModel<GlusterSwiftServiceModel> hostServicesList) {
        this.hostServicesList = hostServicesList;
    }
}
