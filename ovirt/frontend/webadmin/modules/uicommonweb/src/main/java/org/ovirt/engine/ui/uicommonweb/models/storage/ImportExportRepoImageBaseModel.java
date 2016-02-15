package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.comparators.NameableComparator;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.ICommandTarget;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.EventDefinition;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.UIConstants;

import java.util.Collections;
import java.util.List;


public abstract class ImportExportRepoImageBaseModel extends EntityModel implements ICommandTarget {

    protected static final EventDefinition selectedItemChangedEventDefinition;

    static {
        selectedItemChangedEventDefinition = new EventDefinition("SelectedItemChanged", ListModel.class); //$NON-NLS-1$
    }

    protected static final UIConstants constants = ConstantsManager.getInstance().getConstants();

    protected List<EntityModel> entities;
    private EntityModel<Boolean> importAsTemplate;

    public EntityModel<Boolean> getImportAsTemplate() {
        return importAsTemplate;
    }

    public void setImportAsTemplate(EntityModel<Boolean> importAsTemplate) {
        this.importAsTemplate = importAsTemplate;
    }

    private ListModel<StoragePool> dataCenter;
    private ListModel<VDSGroup> cluster;
    private ListModel<StorageDomain> storageDomain;
    private ListModel<Quota> quota;

    private UICommand okCommand;
    private UICommand cancelCommand;

    public List<EntityModel> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityModel> entities) {
        if (entities == this.entities) {
            return;
        }
        this.entities = entities;
        onPropertyChanged(new PropertyChangedEventArgs("ImportExportEntities")); //$NON-NLS-1$
    }

    public ListModel<StoragePool> getDataCenter()
    {
        return dataCenter;
    }

    public void setDataCenter(ListModel<StoragePool> value)
    {
       dataCenter = value;
    }

    public ListModel<VDSGroup> getCluster()
    {
        return cluster;
    }

    public void setCluster(ListModel<VDSGroup> value)
    {
        cluster = value;
    }

    public ListModel<StorageDomain> getStorageDomain() {
        return storageDomain;
    }

    public void setStorageDomain(ListModel<StorageDomain> storageDomain) {
        this.storageDomain = storageDomain;
    }

    public ListModel<Quota> getQuota() {
        return quota;
    }

    public void setQuota(ListModel<Quota> quota) {
        this.quota = quota;
    }


    public UICommand getOkCommand() {
        return okCommand;
    }

    public void setOkCommand(UICommand okCommand) {
        this.okCommand = okCommand;
    }

    public UICommand getCancelCommand() {
        return cancelCommand;
    }

    public void setCancelCommand(UICommand cancelCommand) {
        this.cancelCommand = cancelCommand;
    }

    public ImportExportRepoImageBaseModel() {
        setDataCenter(new ListModel<StoragePool>());
        getDataCenter().setIsEmpty(true);
        getDataCenter().getSelectedItemChangedEvent().addListener(this);
        setCluster(new ListModel<VDSGroup>());
        getCluster().setIsEmpty(true);
        getCluster().getSelectedItemChangedEvent().addListener(this);

        setStorageDomain(new ListModel<StorageDomain>());
        getStorageDomain().setIsEmpty(true);
        getStorageDomain().getSelectedItemChangedEvent().addListener(this);

        setQuota(new ListModel<Quota>());
        getQuota().setIsEmpty(true);
        setImportAsTemplate(new EntityModel<Boolean>());
        getImportAsTemplate().setEntity(false);

        setOkCommand(new UICommand("Ok", this));  //$NON-NLS-1$
        getOkCommand().setTitle(constants.ok());
        getOkCommand().setIsDefault(true);
        getOkCommand().setIsExecutionAllowed(false);

        getCommands().add(getOkCommand());
    }

    protected void updateDataCenters() {
        INewAsyncCallback callback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                ImportExportRepoImageBaseModel model = (ImportExportRepoImageBaseModel) target;
                List<StoragePool> dataCenters = (List<StoragePool>) returnValue;

                // Sorting by name
                Collections.sort(dataCenters, new NameableComparator());

                model.getDataCenter().setItems(dataCenters);
                model.getDataCenter().setIsEmpty(dataCenters.isEmpty());
                model.updateControlsAvailability();

                stopProgress();
            }
        };

        startProgress(null);
        AsyncDataProvider.getDataCenterList(new AsyncQuery(this, callback));
    }

    protected void updateControlsAvailability() {
        getDataCenter().setIsChangable(!getDataCenter().getIsEmpty());
        getStorageDomain().setIsChangable(!getStorageDomain().getIsEmpty());
        getCluster().setIsAvailable(getImportAsTemplate().getEntity());
        getCluster().setIsChangable(!getCluster().getIsEmpty());
        getQuota().setIsChangable(!getQuota().getIsEmpty());
        getOkCommand().setIsExecutionAllowed(!getStorageDomain().getIsEmpty());
        setMessage(getStorageDomain().getIsEmpty() ? constants.noStorageDomainAvailableMsg() : null);
    }

    protected abstract List<StorageDomain> filterStorageDomains(List<StorageDomain> storageDomains);

    protected void updateStorageDomains(Guid storagePoolId) {
        INewAsyncCallback callback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                ImportExportRepoImageBaseModel model = (ImportExportRepoImageBaseModel) target;
                List<StorageDomain> storageDomains = model.filterStorageDomains((List<StorageDomain>) returnValue);
                model.getStorageDomain().setItems(storageDomains);
                model.getStorageDomain().setIsEmpty(storageDomains.isEmpty());
                model.updateControlsAvailability();
                stopProgress();
            }
        };

        startProgress(null);

        if (storagePoolId != null) {
            AsyncDataProvider.getStorageDomainList(new AsyncQuery(this, callback), storagePoolId);
        } else {
            AsyncDataProvider.getStorageDomainList(new AsyncQuery(this, callback));
        }
    }

    protected void updateClusters(Guid storagePoolId) {
        INewAsyncCallback callback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                ImportExportRepoImageBaseModel model = (ImportExportRepoImageBaseModel) target;
                List<VDSGroup> clusters = AsyncDataProvider.filterClustersWithoutArchitecture((List<VDSGroup>) returnValue);
                model.getCluster().setItems(clusters);
                model.getCluster().setIsEmpty(clusters.isEmpty());
                model.updateControlsAvailability();
                stopProgress();
            }
        };

        startProgress(null);

        if (storagePoolId != null) {
            AsyncDataProvider.getClusterList(new AsyncQuery(this, callback), storagePoolId);
        } else {
            AsyncDataProvider.getClusterList(new AsyncQuery(this, callback));
        }
    }

    protected QuotaEnforcementTypeEnum getDataCenterQuotaEnforcementType() {
        StoragePool dataCenter = getDataCenter().getSelectedItem();
        return (dataCenter == null) ? null : dataCenter.getQuotaEnforcementType();
    }

    private void updateQuotas() {
        StorageDomain storageDomain = getStorageDomain().getSelectedItem();

        if (getDataCenterQuotaEnforcementType() == QuotaEnforcementTypeEnum.DISABLED || storageDomain == null) {
            getQuota().setItems(null);
            getQuota().setIsEmpty(true);
            updateControlsAvailability();
            return;
        }

        INewAsyncCallback callback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                ImportExportRepoImageBaseModel model = (ImportExportRepoImageBaseModel) target;
                List<Quota> quotas = ((VdcQueryReturnValue) returnValue).getReturnValue();
                model.getQuota().setItems(quotas);
                model.getQuota().setIsEmpty(quotas.isEmpty());
                model.updateControlsAvailability();
                stopProgress();
            }
        };

        startProgress(null);
        Frontend.getInstance().runQuery(VdcQueryType.GetAllRelevantQuotasForStorage,
                new IdQueryParameters(storageDomain.getId()),
                new AsyncQuery(this, callback));
    }

    protected void cancel() {
        stopProgress();
        ((ListModel) getEntity()).setWindow(null);
    }

    protected void dataCenter_SelectedItemChanged() {
        updateStorageDomains(getDataCenter().getSelectedItem().getId());
        updateClusters(getDataCenter().getSelectedItem().getId());
    }

    protected void storageDomain_SelectedItemChanged() {
        updateQuotas();
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(selectedItemChangedEventDefinition)) {
            if (sender == getDataCenter()) {
                dataCenter_SelectedItemChanged();
            }
            if (sender == getStorageDomain()) {
                storageDomain_SelectedItemChanged();
            }
        }
    }

    public abstract boolean showImportAsTemplateOptions();

}
