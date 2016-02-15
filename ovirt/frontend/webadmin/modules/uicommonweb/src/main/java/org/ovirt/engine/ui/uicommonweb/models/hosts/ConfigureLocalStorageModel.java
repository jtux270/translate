package org.ovirt.engine.ui.uicommonweb.models.hosts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.ServerCpu;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.ListUtils;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.IntegerCompat;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterModel;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.DataCenterModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.LocalStorageModel;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.RegexValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public class ConfigureLocalStorageModel extends Model {

    private LocalStorageModel privateStorage;

    public LocalStorageModel getStorage() {
        return privateStorage;
    }

    private void setStorage(LocalStorageModel value) {
        privateStorage = value;
    }

    private DataCenterModel privateDataCenter;

    public DataCenterModel getDataCenter() {
        return privateDataCenter;
    }

    private void setDataCenter(DataCenterModel value) {
        privateDataCenter = value;
    }

    private ClusterModel privateCluster;

    public ClusterModel getCluster() {
        return privateCluster;
    }

    private void setCluster(ClusterModel value) {
        privateCluster = value;
    }

    private EntityModel<String> privateFormattedStorageName;

    public EntityModel<String> getFormattedStorageName() {
        return privateFormattedStorageName;
    }

    private void setFormattedStorageName(EntityModel<String> value) {
        privateFormattedStorageName = value;
    }

    private StoragePool candidateDataCenter;

    public StoragePool getCandidateDataCenter() {
        return candidateDataCenter;
    }

    public void setCandidateDataCenter(StoragePool value) {
        candidateDataCenter = value;
    }

    private VDSGroup candidateCluster;

    public VDSGroup getCandidateCluster() {
        return candidateCluster;
    }

    public void setCandidateCluster(VDSGroup value) {
        candidateCluster = value;
    }

    private String privateCommonName;

    private String getCommonName() {
        return privateCommonName;
    }

    private void setCommonName(String value) {
        privateCommonName = value;
    }

    private boolean isGeneralTabValid;

    public boolean getIsGeneralTabValid() {
        return isGeneralTabValid;
    }

    public void setIsGeneralTabValid(boolean value) {

        if (isGeneralTabValid != value) {
            isGeneralTabValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsGeneralTabValid")); //$NON-NLS-1$
        }
    }

    private String frontendHash = getHashName() + new Date();

    public ConfigureLocalStorageModel() {

        setStorage(new LocalStorageModel());

        setDataCenter(new DataCenterModel());
        getDataCenter().getVersion().getSelectedItemChangedEvent().addListener(this);

        setCluster(new ClusterModel());
        getCluster().init(false);
        getCluster().setIsNew(true);

        setFormattedStorageName(new EntityModel<String>());

        // Subscribe to the Frontend events.
        Frontend.getInstance().getQueryStartedEvent().addListener(this);
        Frontend.getInstance().getQueryCompleteEvent().addListener(this);
        Frontend.getInstance().subscribe(new VdcQueryType[] {
                VdcQueryType.Search,
                VdcQueryType.GetVdsGroupsByStoragePoolId,
                VdcQueryType.GetAllVdsGroups,
                VdcQueryType.GetVdsGroupById,
                VdcQueryType.GetStoragePoolById,
        });

        // Set the storage type to be Local.
        for (Boolean bool : getDataCenter().getStoragePoolType().getItems()) {
            if (bool) {
                getDataCenter().getStoragePoolType().setSelectedItem(bool);
                break;
            }
        }

        setIsGeneralTabValid(true);
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args) {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(ListModel.selectedItemChangedEventDefinition) && sender == getDataCenter().getVersion()) {
            dataCenterVersion_SelectedItemChanged();
        } else if (ev.matchesDefinition(Frontend.getInstance().getQueryStartedEventDefinition())
                && ObjectUtils.objectsEqual(Frontend.getInstance().getCurrentContext(), frontendHash)) {
            frontend_QueryStarted();
        } else if (ev.matchesDefinition(Frontend.getInstance().getQueryCompleteEventDefinition())
                && ObjectUtils.objectsEqual(Frontend.getInstance().getCurrentContext(), frontendHash)) {
            frontend_QueryComplete();
        }
    }

    private void dataCenterVersion_SelectedItemChanged() {
        Version version = getDataCenter().getVersion().getSelectedItem();

        // Keep in sync version for data center and cluster.
        getCluster().getVersion().setSelectedItem(version);
    }

    public boolean validate() {

        RegexValidation validation = new RegexValidation();
        validation.setExpression("^[A-Za-z0-9_-]+$"); //$NON-NLS-1$
        validation.setMessage(ConstantsManager.getInstance().getConstants().asciiNameValidationMsg());
        getFormattedStorageName().validateEntity(new IValidation[] { validation });

        if (getFormattedStorageName().getEntity() != null
                && Linq.firstOrDefault(context.storageList,
                        new Linq.StorageNamePredicate((String) getFormattedStorageName().getEntity())) != null) {

            getFormattedStorageName().setIsValid(false);
            getFormattedStorageName().getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getConstants()
                    .nameMustBeUniqueInvalidReason());
        }

        boolean isStorageValid = getStorage().validate() && getFormattedStorageName().getIsValid();
        boolean isDataCenterValid = true;
        if (getCandidateDataCenter() == null) {
            isDataCenterValid = getDataCenter().validate();
        }
        boolean isClusterValid = true;
        if (getCandidateCluster() == null) {
            isClusterValid = getCluster().validate(false, true, false);
        }

        setIsGeneralTabValid(isStorageValid && isDataCenterValid && isClusterValid);

        return isStorageValid && isDataCenterValid && isClusterValid;
    }

    private void setDefaultNames8() {

        VDS host = context.host;
        ArrayList<StoragePool> dataCenters = context.dataCenterList;
        ArrayList<VDSGroup> clusters = context.clusterList;

        setCommonName(host.getName().replace('.', '-') + "-Local"); //$NON-NLS-1$

        StoragePool candidate = null;

        // Check if current settings suitable for local setup (in case just SD creation failed - re-using the same
        // setup)
        boolean useCurrentSettings = false;
        if (host.getStoragePoolId() != null) {

            StoragePool tempCandidate = context.hostDataCenter;
            if (isLocalDataCenterEmpty(tempCandidate)) {

                candidate = tempCandidate;
                useCurrentSettings = true;
            } else {

                if (tempCandidate != null && tempCandidate.isLocal()) {
                    setMessage(ConstantsManager.getInstance()
                            .getConstants()
                            .noteLocalStorageAlreadyConfiguredForThisHostMsg()
                            + " " + host.getStoragePoolName() + " " + ConstantsManager.getInstance().getConstants().withLocalStorageDomainMsg()); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        // Check if there is other DC suitable for re-use
        if (candidate == null) {
            for (StoragePool dataCenter : dataCenters) {

                // Need to check if the new DC is without host.
                if (isLocalDataCenterEmpty(dataCenter)
                        && context.localStorageHostByDataCenterMap.get(dataCenter) == null) {
                    candidate = dataCenter;
                    break;
                }
            }
        }

        ArrayList<String> names;

        // In case we found a suitable candidate for re-use:
        if (candidate != null) {

            getDataCenter().setDataCenterId(candidate.getId());
            getDataCenter().getName().setEntity(candidate.getName());
            getDataCenter().getDescription().setEntity(candidate.getdescription());

            Version version = candidate.getcompatibility_version();
            getDataCenter().getVersion().setSelectedItem(version);
            getCluster().getVersion().setSelectedItem(version);

            setCandidateDataCenter(candidate);

            // If we use current settings there is no need to create cluster.
            if (useCurrentSettings) {

                getCluster().setClusterId(host.getVdsGroupId());
                getCluster().getName().setEntity(host.getVdsGroupName());

                VDSGroup cluster = context.hostCluster;
                if (cluster != null) {

                    getCluster().getDescription().setEntity(cluster.getdescription());

                    ServerCpu cpu = new ServerCpu();
                    cpu.setCpuName(cluster.getcpu_name());

                    getCluster().getCPU().setSelectedItem(cpu);
                }

                setCandidateCluster(cluster);
            }
            // Use different cluster
            else {

                // Check the DC cluster list (for re-use)
                clusters = context.clusterListByDataCenterMap.get(candidate);

                // No clusters available - pick up new name.
                if (clusters == null || clusters.isEmpty()) {

                    names = new ArrayList<String>();

                    ArrayList<VDSGroup> listClusters = context.clusterList;
                    for (VDSGroup cluster : listClusters) {
                        names.add(cluster.getName());
                    }

                    getCluster().getName().setEntity(availableName(names));
                } else {

                    // Use the DC cluster.
                    VDSGroup cluster = Linq.firstOrDefault(clusters);

                    getCluster().setClusterId(cluster.getId());
                    getCluster().getName().setEntity(cluster.getName());
                    getCluster().getDescription().setEntity(cluster.getdescription());

                    cluster =
                            Linq.firstOrDefault(context.clusterList,
                                    new Linq.ClusterPredicate(getCluster().getClusterId()));
                    if (cluster != null) {

                        ServerCpu cpu = new ServerCpu();
                        cpu.setCpuName(cluster.getcpu_name());

                        getCluster().getCPU().setSelectedItem(cpu);
                    }

                    setCandidateCluster(cluster);
                }
            }
        } else {

            // Didn't found DC to re-use, so we select new names.
            names = new ArrayList<String>();

            for (StoragePool dataCenter : dataCenters) {
                names.add(dataCenter.getName());
            }

            getDataCenter().getName().setEntity(availableName(names));

            // Choose a Data Center version corresponding to the host.
            if (!StringHelper.isNullOrEmpty(host.getSupportedClusterLevels())) {

                // The supported_cluster_levels are sorted.
                String[] array = host.getSupportedClusterLevels().split("[,]", -1); //$NON-NLS-1$
                Version maxVersion = null;

                for (int i = 0; i < array.length; i++) {

                    Version vdsVersion = new Version(array[i]);

                    for (Version version : (List<Version>) getDataCenter().getVersion().getItems()) {

                        if (version.equals(vdsVersion) && version.compareTo(maxVersion) > 0) {
                            maxVersion = version;
                        }
                    }
                }

                if (maxVersion != null) {
                    getDataCenter().getVersion().setSelectedItem(maxVersion);
                    getCluster().getVersion().setSelectedItem(maxVersion);
                }
            }

            names = new ArrayList<String>();
            if (clusters != null) {
                for (VDSGroup cluster : clusters) {
                    names.add(cluster.getName());
                }
            }
            getCluster().getName().setEntity(availableName(names));
        }

        // Choose default CPU name to match host.
        List<ServerCpu> serverCpus = (List<ServerCpu>) getCluster().getCPU().getItems();
        if (host.getCpuName() != null) {
            getCluster().getCPU().setSelectedItem(Linq.firstOrDefault(
                    serverCpus, new Linq.ServerCpuPredicate(host.getCpuName().getCpuName())));
        }
        else {
            getCluster().getCPU().setSelectedItem(serverCpus.isEmpty() ? null : serverCpus.get(0));
        }

        // Always choose a available storage name.
        ArrayList<StorageDomain> storages = context.storageList;
        names = new ArrayList<String>();

        for (StorageDomain storageDomain : storages) {
            names.add(storageDomain.getStorageName());
        }
        getFormattedStorageName().setEntity(availableName(names));
    }

    private void setDefaultNames7() {

        // Get all clusters.
        AsyncDataProvider.getStorageDomainList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        context.storageList = (ArrayList<StorageDomain>) returnValue;
                        setDefaultNames8();
                    }
                },
                frontendHash));
    }

    public void setDefaultNames6() {

        // Fill map of local storage host by data center.

        context.clusterListByDataCenterMap = new HashMap<StoragePool, ArrayList<VDSGroup>>();

        AsyncIterator<StoragePool> iterator = new AsyncIterator<StoragePool>(context.dataCenterList);

        iterator.setComplete(
                new AsyncIteratorComplete<StoragePool>() {
                    @Override
                    public void run(StoragePool item, Object value) {

                        setDefaultNames7();
                    }
                });

        iterator.iterate(
                new AsyncIteratorFunc<StoragePool>() {
                    @Override
                    public void run(StoragePool item, AsyncIteratorCallback callback) {

                        AsyncDataProvider.getClusterList(callback.getAsyncQuery(), item.getId());
                    }
                },
                new AsyncIteratorPredicate<StoragePool>() {
                    @Override
                    public boolean match(StoragePool item, Object value) {

                        context.clusterListByDataCenterMap.put(item, (ArrayList<VDSGroup>) value);
                        return false;
                    }
                },
                frontendHash
                );
    }

    public void setDefaultNames5() {

        // Fill map of local storage host by data center.

        context.localStorageHostByDataCenterMap = new HashMap<StoragePool, VDS>();

        AsyncIterator<StoragePool> iterator = new AsyncIterator<StoragePool>(context.dataCenterList);

        iterator.setComplete(
                new AsyncIteratorComplete<StoragePool>() {
                    @Override
                    public void run(StoragePool item, Object value) {

                        setDefaultNames6();
                    }
                });

        iterator.iterate(
                new AsyncIteratorFunc<StoragePool>() {
                    @Override
                    public void run(StoragePool item, AsyncIteratorCallback callback) {

                        AsyncDataProvider.getLocalStorageHost(callback.getAsyncQuery(), item.getName());
                    }
                },
                new AsyncIteratorPredicate<StoragePool>() {
                    @Override
                    public boolean match(StoragePool item, Object value) {

                        context.localStorageHostByDataCenterMap.put(item, (VDS) value);
                        return false;
                    }
                },
                frontendHash
                );
    }

    public void setDefaultNames4() {

        // Get data centers containing 'local' in name.
        AsyncDataProvider.getDataCenterListByName(new AsyncQuery(null,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        context.dataCenterList = (ArrayList<StoragePool>) returnValue;
                        setDefaultNames5();
                    }
                },
                frontendHash),
                getCommonName() + "*"); //$NON-NLS-1$
    }

    public void setDefaultNames3() {

        // Get all clusters.
        AsyncDataProvider.getClusterList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        context.clusterList = (ArrayList<VDSGroup>) returnValue;
                        setDefaultNames4();
                    }
                },
                frontendHash));
    }

    public void setDefaultNames2() {

        VDS host = context.host;

        // Get cluster of the host.
        if (host.getVdsGroupId() != null) {
            AsyncDataProvider.getClusterById(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target, Object returnValue) {

                            context.hostCluster = (VDSGroup) returnValue;
                            setDefaultNames3();
                        }
                    },
                    frontendHash),
                    host.getVdsGroupId());
        } else {
            setDefaultNames3();
        }
    }

    public void setDefaultNames1() {

        VDS host = context.host;

        // Get data center of the host.
        if (host.getStoragePoolId() != null) {
            AsyncDataProvider.getDataCenterById(new AsyncQuery(this,
                    new INewAsyncCallback() {
                        @Override
                        public void onSuccess(Object target, Object returnValue) {

                            context.hostDataCenter = (StoragePool) returnValue;
                            setDefaultNames2();
                        }
                    },
                    frontendHash),
                    host.getStoragePoolId());
        } else {
            setDefaultNames2();
        }
    }

    public void setDefaultNames(VDS host) {

        context.host = host;

        setCommonName(host.getName().replace('.', '-') + "-Local"); //$NON-NLS-1$

        setDefaultNames1();
    }

    private boolean isLocalDataCenterEmpty(StoragePool dataCenter) {

        if (dataCenter != null && dataCenter.isLocal()
                && dataCenter.getStatus() == StoragePoolStatus.Uninitialized) {
            return true;
        }
        return false;
    }

    private String availableName(ArrayList<String> list) {

        String commonName = getCommonName();
        ArrayList<Integer> notAvailableNumberList = new ArrayList<Integer>();

        String temp;
        for (String str : list) {

            temp = str.replace(getCommonName(), ""); //$NON-NLS-1$
            if (StringHelper.isNullOrEmpty(temp)) {
                temp = "0"; //$NON-NLS-1$
            }

            ListUtils.nullSafeElemAdd(notAvailableNumberList, IntegerCompat.tryParse(temp));
        }

        Collections.sort(notAvailableNumberList);
        int i;
        for (i = 0; i < notAvailableNumberList.size(); i++) {
            if (notAvailableNumberList.get(i) == i) {
                continue;
            }
            break;
        }

        if (i > 0) {
            commonName = getCommonName() + String.valueOf(i);
        }

        return commonName;
    }

    private int queryCounter;

    private void frontend_QueryStarted() {
        queryCounter++;
        if (getProgress() == null) {
            startProgress(null);
        }
    }

    private void frontend_QueryComplete() {
        queryCounter--;
        if (queryCounter == 0) {
            stopProgress();
        }
    }

    private final Context context = new Context();

    public static final class Context {

        public VDS host;
        public StoragePool hostDataCenter;
        public VDSGroup hostCluster;
        public ArrayList<StoragePool> dataCenterList;
        public ArrayList<VDSGroup> clusterList;
        public ArrayList<StorageDomain> storageList;
        public HashMap<StoragePool, VDS> localStorageHostByDataCenterMap;
        public HashMap<StoragePool, ArrayList<VDSGroup>> clusterListByDataCenterMap;
    }
}
