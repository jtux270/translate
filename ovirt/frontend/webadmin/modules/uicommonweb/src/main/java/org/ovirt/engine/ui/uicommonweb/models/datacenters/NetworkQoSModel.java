package org.ovirt.engine.ui.uicommonweb.models.datacenters;

import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.network.NetworkQoS;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.AsciiNameValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public abstract class NetworkQoSModel extends BaseNetworkQosModel {

    public static final NetworkQoS EMPTY_QOS;

    static {
        EMPTY_QOS = new NetworkQoS();
        EMPTY_QOS.setName(ConstantsManager.getInstance().getConstants().unlimitedQoSTitle());
        EMPTY_QOS.setId(Guid.Empty);
    }

    private final Model sourceModel;
    private ListModel<StoragePool> dataCenters;
    private EntityModel<String> name;

    public NetworkQoSModel(Model sourceModel, StoragePool dataCenter) {
        this.sourceModel = sourceModel;
        setName(new EntityModel<String>());
        setDataCenters(new ListModel<StoragePool>());
        getDataCenters().setSelectedItem(dataCenter);
        getDataCenters().setIsChangable(false);
        getInbound().getAverage()
                .setEntity((Integer) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.QoSInboundAverageDefaultValue));
        getInbound().getPeak()
                .setEntity((Integer) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.QoSInboundPeakDefaultValue));
        getInbound().getBurst()
                .setEntity((Integer) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.QoSInboundBurstDefaultValue));
        getOutbound().getAverage()
                .setEntity((Integer) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.QoSOutboundAverageDefaultValue));
        getOutbound().getPeak()
                .setEntity((Integer) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.QoSOutboundPeakDefaultValue));
        getOutbound().getBurst()
                .setEntity((Integer) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.QoSOutboundBurstDefaultValue));
        addCommands();
    }

    @Override
    public boolean validate() {
        super.validate();
        getName().validateEntity(new IValidation[] { new NotEmptyValidation(), new AsciiNameValidation() });

        setIsValid(getIsValid() && getName().getIsValid());
        return getIsValid();
    }

    protected void addCommands() {
        UICommand tempVar2 = new UICommand("OnSave", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar2.setIsDefault(true);
        getCommands().add(tempVar2);
        UICommand tempVar3 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar3.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar3.setIsCancel(true);
        getCommands().add(tempVar3);
    }

    public StoragePool getSelectedDc() {
        return getDataCenters().getSelectedItem();
    }

    @Override
    public NetworkQoS flush() {
        super.flush();
        networkQoS.setName((String) getName().getEntity());
        networkQoS.setStoragePoolId(((StoragePool)getDataCenters().getSelectedItem()).getId());
        return networkQoS;
    }

    protected abstract void executeSave();

    protected void cancel() {
        sourceModel.setWindow(null);
        sourceModel.setConfirmWindow(null);
    }

    public void onSave() {
        if (!validate()) {
            return;
        }

        // Save changes.
        flush();

        // Execute all the required commands (detach, attach, update) to save the updates
        executeSave();
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if ("OnSave".equals(command.getName())) { //$NON-NLS-1$
            onSave();
        } else if ("Cancel".equals(command.getName())) { //$NON-NLS-1$
            cancel();
        }
    }

    protected void postSaveAction(boolean succeeded) {
        if (succeeded) {
            cancel();
        }
        stopProgress();
    }

    public ListModel<StoragePool> getDataCenters() {
        return dataCenters;
    }

    public void setDataCenters(ListModel<StoragePool> dataCenters) {
        this.dataCenters = dataCenters;
    }

    public EntityModel<String> getName() {
        return name;
    }

    public void setName(EntityModel<String> name) {
        this.name = name;
    }
}
