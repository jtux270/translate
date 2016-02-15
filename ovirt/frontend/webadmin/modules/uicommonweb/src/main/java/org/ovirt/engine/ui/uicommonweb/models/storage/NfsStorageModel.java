package org.ovirt.engine.ui.uicommonweb.models.storage;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.NfsVersion;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StorageType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IntegerValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LinuxMountPointValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NonUtfValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.UIConstants;

@SuppressWarnings("unused")
public class NfsStorageModel extends Model implements IStorageModel {

    //retrans nfs option max value
    private final static short RETRANS_MAX = 32767;
    //timeo nfs option max value
    private final static short TIMEOUT_MAX = 6000;

    public static final EventDefinition pathChangedEventDefinition;
    private Event pathChangedEvent;

    public Event getPathChangedEvent() {
        return pathChangedEvent;
    }

    private void setPathChangedEvent(Event value) {
        pathChangedEvent = value;
    }

    private UICommand updateCommand;

    @Override
    public UICommand getUpdateCommand() {
        return updateCommand;
    }

    private void setUpdateCommand(UICommand value) {
        updateCommand = value;
    }

    private StorageModel container;

    @Override
    public StorageModel getContainer() {
        return container;
    }

    @Override
    public void setContainer(StorageModel value) {
        if (container != value) {
            container = value;
            containerChanged();
        }
    }

    private StorageDomainType role = StorageDomainType.values()[0];

    @Override
    public StorageDomainType getRole() {
        return role;
    }

    @Override
    public void setRole(StorageDomainType value) {
        role = value;
    }

    private EntityModel<String> path;

    public EntityModel<String> getPath() {
        return path;
    }

    private void setPath(EntityModel<String> value) {
        path = value;
    }

    private EntityModel<Boolean> override;

    public EntityModel<Boolean> getOverride() {
        return override;
    }

    private void setOverride(EntityModel<Boolean> value) {
        override = value;
    }

    private ListModel<EntityModel<NfsVersion>> version;

    public ListModel<EntityModel<NfsVersion>> getVersion() {
        return version;
    }

    private void setVersion(ListModel<EntityModel<NfsVersion>> value) {
        version = value;
    }

    private EntityModel<Short> retransmissions;

    public EntityModel<Short> getRetransmissions() {
        return retransmissions;
    }

    private void setRetransmissions(EntityModel<Short> value) {
        retransmissions = value;
    }

    private EntityModel<Short> timeout;

    public EntityModel<Short> getTimeout() {
        return timeout;
    }

    private void setTimeout(EntityModel<Short> value) {
        timeout = value;
    }

    private EntityModel<String> mountOptions;

    public EntityModel<String> getMountOptions() {
        return mountOptions;
    }

    private void setMountOptions(EntityModel<String> value) {
        mountOptions = value;
    }

    static {

        pathChangedEventDefinition = new EventDefinition("PathChanged", NfsStorageModel.class); //$NON-NLS-1$
    }

    public NfsStorageModel() {

        setPathChangedEvent(new Event(pathChangedEventDefinition));

        setUpdateCommand(new UICommand("Update", this)); //$NON-NLS-1$

        setPath(new EntityModel<String>());
        getPath().getEntityChangedEvent().addListener(this);

        UIConstants constants = ConstantsManager.getInstance().getConstants();

        // Initialize version list.
        setVersion(new ListModel<EntityModel<NfsVersion>>());

        List<EntityModel<NfsVersion>> versionItems = new ArrayList<EntityModel<NfsVersion>>();
        // Items are shown in the UI in the order added; v3 is the default
        versionItems.add(new EntityModel<NfsVersion>(constants.nfsVersion3(), NfsVersion.V3));
        versionItems.add(new EntityModel<NfsVersion>(constants.nfsVersion4(), NfsVersion.V4));
        versionItems.add(new EntityModel<NfsVersion>(constants.nfsVersionAutoNegotiate(), NfsVersion.AUTO));
        getVersion().setItems(versionItems);

        setRetransmissions(new EntityModel<Short>());
        setTimeout(new EntityModel<Short>());
        setMountOptions(new EntityModel<String>());

        setOverride(new EntityModel<Boolean>());
        getOverride().getEntityChangedEvent().addListener(this);
        getOverride().setEntity(false);

    }

    private void override_EntityChanged(EventArgs e) {
        // Advanced options are editable only if override checkbox is enabled
        // and the dialog is not editing existing nfs storage.
        boolean isChangeable = (Boolean) getOverride().getEntity();
        getVersion().setIsChangable(isChangeable);
        getRetransmissions().setIsChangable(isChangeable);
        getTimeout().setIsChangable(isChangeable);
        getMountOptions().setIsChangable(isChangeable);
        getMountOptions().setTitle(isChangeable ? ConstantsManager.getInstance().getConstants().mountOptionsHint() : null);
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args) {
        super.eventRaised(ev, sender, args);
        if (ev.matchesDefinition(EntityModel.entityChangedEventDefinition) && sender == getPath()) {
            // Notify about path change.
            getPathChangedEvent().raise(this, EventArgs.EMPTY);
        }
        else if (ev.matchesDefinition(EntityModel.entityChangedEventDefinition) && sender == getOverride()) {
            override_EntityChanged(args);
        }
    }

    @Override
    public boolean validate() {
        getPath().validateEntity(new IValidation[] {
            new NotEmptyValidation(),
            new LinuxMountPointValidation(),
            new NonUtfValidation()
        });

        getRetransmissions().validateEntity(new IValidation[] {
            new IntegerValidation(0, RETRANS_MAX)
        });

        getTimeout().validateEntity(new IValidation[] {
            new IntegerValidation(1, TIMEOUT_MAX)
        });

        getMountOptions().validateEntity(new IValidation[] {
            new NonUtfValidation()
        });

        return getPath().getIsValid()
            && getRetransmissions().getIsValid()
            && getTimeout().getIsValid()
            && getMountOptions().getIsValid();
    }

    @Override
    public StorageType getType() {
        return StorageType.NFS;
    }

    private void containerChanged() {
        // Subscribe to the data center change.
        if (getContainer() == null) {
            return;
        }

        ListModel<StoragePool> dataCenter = getContainer().getDataCenter();
        dataCenter.getSelectedItemChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {

                containerDataCenterChanged();
            }
        });

        // Call handler if there some data center is already selected.
        if (dataCenter.getSelectedItem() != null) {
            containerDataCenterChanged();
        }
    }

    private void containerDataCenterChanged() {

        // Show advanced NFS options for <=3.1
        StoragePool dataCenter = getContainer().getDataCenter().getSelectedItem();
        Version ver31 = new Version(3, 1);

        boolean available = dataCenter != null && (dataCenter.getcompatibility_version().compareTo(ver31) >= 0 || dataCenter.getId().equals(Guid.Empty));

        getVersion().setIsAvailable(available);
        getRetransmissions().setIsAvailable(available);
        getTimeout().setIsAvailable(available);
        getMountOptions().setIsAvailable(available);
    }
}
