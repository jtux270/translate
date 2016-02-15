package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.BootSequence;
import org.ovirt.engine.core.common.businessentities.BusinessEntitiesDefinitions;
import org.ovirt.engine.core.common.businessentities.ConsoleDisconnectAction;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.InstanceType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.NumaTuneMode;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.SerialNumberPolicy;
import org.ovirt.engine.core.common.businessentities.SsoMethod;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VmNumaNode;
import org.ovirt.engine.core.common.businessentities.VmPoolType;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.businessentities.VmWatchdogAction;
import org.ovirt.engine.core.common.businessentities.VmWatchdogType;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.ICommandTarget;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.TypeResolver;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.auth.CurrentUserRole;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.HasEntity;
import org.ovirt.engine.ui.uicommonweb.models.HasValidatedTabs;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemType;
import org.ovirt.engine.ui.uicommonweb.models.TabName;
import org.ovirt.engine.ui.uicommonweb.models.ValidationCompleteEvent;
import org.ovirt.engine.ui.uicommonweb.models.hosts.numa.NumaSupportModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.numa.VmNumaSupportModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.DisksAllocationModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateWithVersion;
import org.ovirt.engine.ui.uicommonweb.models.vms.instancetypes.InstanceTypeManager;
import org.ovirt.engine.ui.uicommonweb.models.vms.key_value.KeyValueModel;
import org.ovirt.engine.ui.uicommonweb.validation.GuidValidation;
import org.ovirt.engine.ui.uicommonweb.validation.I18NExtraNameOrNoneValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IconWithOsDefaultValidation;
import org.ovirt.engine.ui.uicommonweb.validation.IntegerValidation;
import org.ovirt.engine.ui.uicommonweb.validation.LengthValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NoTrimmingWhitespacesValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyQuotaValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotNullIntegerValidation;
import org.ovirt.engine.ui.uicommonweb.validation.SpecialAsciiI18NOrNoneValidation;
import org.ovirt.engine.ui.uicommonweb.validation.ValidationResult;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.UIConstants;

public class UnitVmModel extends Model implements HasValidatedTabs {

    public static final int VM_TEMPLATE_AND_INSTANCE_TYPE_NAME_MAX_LIMIT = 40;
    public static final int DESCRIPTION_MAX_LIMIT = 255;

    final UIConstants constants = ConstantsManager.getInstance().getConstants();

    private boolean privateIsNew;

    private EntityModel<Boolean> valid;

    public EntityModel<Boolean> getValid() {
        return valid;
    }

    public void setValid(EntityModel<Boolean> valid) {
        this.valid = valid;
    }

    /**
     * All dialogs which want to have the previous advanced/basic mode remembered in local storage need to have
     * a key to local storage set.
     */
    private String isAdvancedModeLocalStorageKey;

    public String getIsAdvancedModeLocalStorageKey() {
        return isAdvancedModeLocalStorageKey;
    }

    public void setIsAdvancedModeLocalStorageKey(String isAdvancedModeLocalStorageKey) {
        this.isAdvancedModeLocalStorageKey = isAdvancedModeLocalStorageKey;
    }

    private EntityModel<Boolean> attachedToInstanceType;

    public EntityModel<Boolean> getAttachedToInstanceType() {
        return attachedToInstanceType;
    }

    public void setAttachedToInstanceType(EntityModel<Boolean> attachedToInstanceType) {
        this.attachedToInstanceType = attachedToInstanceType;
    }

    private EntityModel<String> spiceProxy;

    public EntityModel<String> getSpiceProxy() {
        return spiceProxy;
    }

    public void setSpiceProxy(EntityModel<String> spiceProxy) {
        this.spiceProxy = spiceProxy;
    }

    private EntityModel<Boolean> spiceProxyEnabled;

    public EntityModel<Boolean> getSpiceProxyEnabled() {
        return spiceProxyEnabled;
    }

    public void setSpiceProxyEnabled(EntityModel<Boolean> spiceProxyEnabled) {
        this.spiceProxyEnabled = spiceProxyEnabled;
    }

    private EntityModel<Boolean> editingEnabled;

    public EntityModel<Boolean> getEditingEnabled() {
        return editingEnabled;
    }

    public void setEditingEnabled(EntityModel<Boolean> editingEnabled) {
        this.editingEnabled = editingEnabled;
    }

    public boolean getIsNew() {
        return privateIsNew;
    }

    public void setIsNew(boolean value) {
        privateIsNew = value;
    }

    private boolean vmAttachedToPool;

    public boolean isVmAttachedToPool() {
        return vmAttachedToPool;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> isSoundcardEnabled;

    public EntityModel<Boolean> getIsSoundcardEnabled() {
        return isSoundcardEnabled;
    }

    private void setIsSoundcardEnabled(NotChangableForVmInPoolEntityModel<Boolean> isSoundcardEnabled) {
        this.isSoundcardEnabled = isSoundcardEnabled;
    }

    private NotChangableForVmInPoolListModel<DataCenterWithCluster> dataCenterWithClustersList;

    public ListModel<DataCenterWithCluster> getDataCenterWithClustersList() {
        return dataCenterWithClustersList;
    }

    private void setDataCenterWithClustersList(NotChangableForVmInPoolListModel<DataCenterWithCluster> dataCenterWithClustersList) {
        this.dataCenterWithClustersList = dataCenterWithClustersList;
    }

    private VnicInstancesModel nicsWithLogicalNetworks;

    public VnicInstancesModel getNicsWithLogicalNetworks() {
        return nicsWithLogicalNetworks;
    }

    public void setNicsWithLogicalNetworks(VnicInstancesModel nicsWithLogicalNetworks) {
        this.nicsWithLogicalNetworks = nicsWithLogicalNetworks;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> ioThreadsEnabled;

    public EntityModel<Boolean> getIoThreadsEnabled() {
        return ioThreadsEnabled;
    }

    public void setIoThreadsEnabled(NotChangableForVmInPoolEntityModel<Boolean> ioThreadsEnabled) {
        this.ioThreadsEnabled = ioThreadsEnabled;
    }

    private NotChangableForVmInPoolEntityModel<Integer> numOfIoThreads;

    public EntityModel<Integer> getNumOfIoThreads() {
        return numOfIoThreads;
    }

    public void setNumOfIoThreads(NotChangableForVmInPoolEntityModel<Integer> numOfIoThreads) {
        this.numOfIoThreads = numOfIoThreads;
    }

    /**
     * VM icon
     */
    private NotChangableForVmInPoolEntityModel<IconWithOsDefault> icon;

    public EntityModel<IconWithOsDefault> getIcon() {
        return icon;
    }

    public void setIcon(NotChangableForVmInPoolEntityModel<IconWithOsDefault> icon) {
        this.icon = icon;
    }

    /**
     * Note: We assume that this method is called only once, on the creation stage of the model. if this assumption is
     * changed (i.e the VM can attached/detached from a pool after the model is created), this method should be modified
     */
    public void setVmAttachedToPool(boolean value) {
        if (value) {
            // ==General Tab==
            getDataCenterWithClustersList().setIsChangeable(!value);
            getQuota().setIsChangeable(false);
            getCpuProfiles().setIsChangeable(false);

            getVmId().setIsChangeable(false);

            getNumOfDesktops().setIsChangeable(false);
            getPrestartedVms().setIsChangeable(false);
            getMaxAssignedVmsPerUser().setIsChangeable(false);

            getBaseTemplate().setIsChangeable(false);
            getTemplateWithVersion().setIsChangeable(false);
            getInstanceTypes().setIsChangeable(false);
            getMemSize().setIsChangeable(false);
            getTotalCPUCores().setIsChangeable(false);

            getCustomCpu().setIsChangeable(false);
            getEmulatedMachine().setIsChangeable(false);

            getCoresPerSocket().setIsChangeable(false);
            getNumOfSockets().setIsChangeable(false);
            getSerialNumberPolicy().setIsChangeable(false);

            getOSType().setIsChangeable(false);
            getIsStateless().setIsChangeable(false);
            getIsRunAndPause().setIsChangeable(false);
            getIsDeleteProtected().setIsChangeable(false);

            // ==Initial run Tab==
            getTimeZone().setIsChangeable(false);

            // ==Console Tab==
            getDisplayType().setIsChangeable(false);
            getGraphicsType().setIsChangeable(false);
            getUsbPolicy().setIsChangeable(false);
            getConsoleDisconnectAction().setIsChangeable(false);
            getNumOfMonitors().setIsChangeable(false);
            getIsSingleQxlEnabled().setIsChangeable(false);
            getIsSmartcardEnabled().setIsChangeable(false);
            getAllowConsoleReconnect().setIsChangeable(false);
            getVncKeyboardLayout().setIsChangeable(false);
            getSsoMethodNone().setIsChangeable(false);
            getSsoMethodGuestAgent().setIsChangeable(false);

            // ==Host Tab==
            getIsAutoAssign().setIsChangeable(false);
            getDefaultHost().setIsChangeable(false);
            getHostCpu().setIsChangeable(false);
            getMigrationMode().setIsChangeable(false);
            getCpuPinning().setIsChangeable(false);
            getMigrationDowntime().setIsChangeable(false);

            // ==Resource Allocation Tab==
            getMinAllocatedMemory().setIsChangeable(false);
            getProvisioning().setIsChangeable(false);
            getProvisioningThin_IsSelected().setIsChangeable(false);
            getProvisioningClone_IsSelected().setIsChangeable(false);
            getDisksAllocationModel().setIsChangeable(false);
            getIoThreadsEnabled().setIsChangeable(false);
            getNumOfIoThreads().setIsChangeable(false);

            // ==Boot Options Tab==
            getFirstBootDevice().setIsChangeable(false);
            getSecondBootDevice().setIsChangeable(false);
            getCdAttached().setIsChangeable(false);
            getCdImage().setIsChangeable(false);
            getKernel_path().setIsChangeable(false);
            getInitrd_path().setIsChangeable(false);
            getKernel_parameters().setIsChangeable(false);

            // ==Random Generator Tab==
            getIsRngEnabled().setIsChangeable(false);
            getRngPeriod().setIsChangeable(false);
            getRngBytes().setIsChangeable(false);
            getRngSourceRandom().setIsChangeable(false);
            getRngSourceHwrng().setIsChangeable(false);

            // ==Custom Properties Tab==
            getCustomProperties().setIsChangeable(false);
            getCustomPropertySheet().setIsChangeable(false);

            // ==Icon Tab==
            getIcon().setIsChangeable(false);

            vmAttachedToPool = true;
        }
    }
    private boolean isWindowsOS;

    public boolean getIsWindowsOS() {
        return isWindowsOS;
    }

    public void setIsWindowsOS(boolean value) {
        if (isWindowsOS != value) {
            isWindowsOS = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsWindowsOS")); //$NON-NLS-1$
        }
    }

    private boolean isLinuxOS;

    public boolean getIsLinuxOS() {
        return isLinuxOS;
    }

    public void setIsLinuxOS(boolean value) {
        if (isLinuxOS != value) {
            isLinuxOS = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsLinuxOS")); //$NON-NLS-1$
        }
    }

    private String cpuNotification;

    public String getCPUNotification() {
        return cpuNotification;
    }

    public void setCPUNotification(String value) {
        if (!ObjectUtils.objectsEqual(cpuNotification, value)) {
            cpuNotification = value;
            onPropertyChanged(new PropertyChangedEventArgs("CPUNotification")); //$NON-NLS-1$
        }
    }

    public boolean isCPUsAmountValid;

    public boolean getIsCPUsAmountValid() {
        return isCPUsAmountValid;
    }

    public void setIsCPUsAmountValid(boolean value) {
        if (isCPUsAmountValid != value) {
            isCPUsAmountValid = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsCPUsAmountValid")); //$NON-NLS-1$
        }
    }

    private NotChangableForVmInPoolListModel<StorageDomain> privateStorageDomain;

    public ListModel<StorageDomain> getStorageDomain() {
        return privateStorageDomain;
    }

    private void setStorageDomain(NotChangableForVmInPoolListModel<StorageDomain> value) {
        privateStorageDomain = value;
    }

    private NotChangableForVmInPoolListModel<TemplateWithVersion> templateWithVersion;

    public ListModel<TemplateWithVersion> getTemplateWithVersion() {
        return templateWithVersion;
    }

    public void setTemplateWithVersion(NotChangableForVmInPoolListModel<TemplateWithVersion> templateWithVersion) {
        this.templateWithVersion = templateWithVersion;
    }

    private NotChangableForVmInPoolListModel<VmTemplate> baseTemplate;

    public ListModel<VmTemplate> getBaseTemplate() {
        return baseTemplate;
    }

    private void setBaseTemplate(NotChangableForVmInPoolListModel<VmTemplate> value) {
        baseTemplate = value;
    }

    private NotChangableForVmInPoolListModel<InstanceType> instanceTypes;

    public void setInstanceTypes(NotChangableForVmInPoolListModel<InstanceType> instanceTypes) {
        this.instanceTypes = instanceTypes;
    }

    public ListModel<InstanceType> getInstanceTypes() {
        return instanceTypes;
    }

    private InstanceImagesModel instanceImages;

    public InstanceImagesModel getInstanceImages() {
        return instanceImages;
    }

    public void setInstanceImages(InstanceImagesModel instanceImages) {
        this.instanceImages = instanceImages;
    }

    private NotChangableForVmInPoolListModel<VmType> vmType;

    public void setVmType(NotChangableForVmInPoolListModel<VmType> vmType) {
        this.vmType = vmType;
    }

    public ListModel<VmType> getVmType() {
        return vmType;
    }

    private EntityModel<String> privateName;

    public EntityModel<String> getName() {
        return privateName;
    }

    private void setName(EntityModel<String> value) {
        privateName = value;
    }

    private NotChangableForVmInPoolListModel<String> emulatedMachine;

    public ListModel<String> getEmulatedMachine() {
        return emulatedMachine;
    }

    private void setEmulatedMachine(NotChangableForVmInPoolListModel<String> emulatedMachine) {
        this.emulatedMachine = emulatedMachine;
    }

    private NotChangableForVmInPoolListModel<String> customCpu;

    public ListModel<String> getCustomCpu() {
        return customCpu;
    }

    private void setCustomCpu(NotChangableForVmInPoolListModel<String> customCpu) {
        this.customCpu = customCpu;
    }

    private NotChangableForVmInPoolListModel<Integer> privateOSType;

    public ListModel<Integer> getOSType() {
        return privateOSType;
    }

    private void setOSType(NotChangableForVmInPoolListModel<Integer> value) {
        privateOSType = value;
    }

    private NotChangableForVmInPoolListModel<Integer> privateNumOfMonitors;

    public ListModel<Integer> getNumOfMonitors() {
        return privateNumOfMonitors;
    }

    private void setNumOfMonitors(NotChangableForVmInPoolListModel<Integer> value) {
        privateNumOfMonitors = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateIsSingleQxlEnabled;

    public EntityModel<Boolean> getIsSingleQxlEnabled() {
        return privateIsSingleQxlEnabled;
    }

    private void setIsSingleQxlEnabled(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateIsSingleQxlEnabled = value;
    }


    private NotChangableForVmInPoolEntityModel<Boolean> privateAllowConsoleReconnect;

    public EntityModel<Boolean> getAllowConsoleReconnect() {
        return privateAllowConsoleReconnect;
    }

    private NotChangableForVmInPoolEntityModel<String> privateVmId;

    public EntityModel<String> getVmId() {
        return privateVmId;
    }

    private void setVmId(NotChangableForVmInPoolEntityModel<String> value) {
        privateVmId = value;
    }

    private void setAllowConsoleReconnect(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateAllowConsoleReconnect = value;
    }

    private NotChangableForVmInPoolEntityModel<String> privateDescription;

    public EntityModel<String> getDescription() {
        return privateDescription;
    }

    private void setDescription(NotChangableForVmInPoolEntityModel<String> value) {
        privateDescription = value;
    }

    private NotChangableForVmInPoolEntityModel<String> privateComment;

    public EntityModel<String> getComment() {
        return privateComment;
    }

    private void setComment(NotChangableForVmInPoolEntityModel<String> value) {
        privateComment = value;
    }

    private NotChangableForVmInPoolEntityModel<String> templateVersionName;

    public EntityModel<String> getTemplateVersionName() {
        return templateVersionName;
    }

    private void setTemplateVersionName(NotChangableForVmInPoolEntityModel<String> value) {
        templateVersionName = value;
    }

    private NotChangableForVmInPoolEntityModel<Integer> privateMemSize;

    public EntityModel<Integer> getMemSize() {
        return privateMemSize;
    }

    private void setMemSize(NotChangableForVmInPoolEntityModel<Integer> value) {
        privateMemSize = value;
    }

    private NotChangableForVmInPoolEntityModel<Integer> privateMinAllocatedMemory;

    public EntityModel<Integer> getMinAllocatedMemory() {
        return privateMinAllocatedMemory;
    }

    private void setMinAllocatedMemory(NotChangableForVmInPoolEntityModel<Integer> value) {
        privateMinAllocatedMemory = value;
    }

    private NotChangableForVmInPoolListModel<Quota> privateQuota;

    public ListModel<Quota> getQuota() {
        return privateQuota;
    }

    private void setQuota(NotChangableForVmInPoolListModel<Quota> value) {
        privateQuota = value;
    }

    private NotChangableForVmInPoolListModel<UsbPolicy> privateUsbPolicy;

    public ListModel<UsbPolicy> getUsbPolicy() {
        return privateUsbPolicy;
    }

    private void setUsbPolicy(NotChangableForVmInPoolListModel<UsbPolicy> value) {
        privateUsbPolicy = value;
    }

    private NotChangableForVmInPoolListModel<ConsoleDisconnectAction> consoleDisconnectAction;

    public ListModel<ConsoleDisconnectAction> getConsoleDisconnectAction() {
        return consoleDisconnectAction;
    }

    private void setConsoleDisconnectAction(NotChangableForVmInPoolListModel<ConsoleDisconnectAction> value) {
        consoleDisconnectAction = value;
    }

    private NotChangableForVmInPoolListModel<TimeZoneModel> privateTimeZone;

    public ListModel<TimeZoneModel> getTimeZone() {
        return privateTimeZone;
    }

    private void setTimeZone(NotChangableForVmInPoolListModel<TimeZoneModel> value) {
        privateTimeZone = value;
    }

    private NotChangableForVmInPoolListModel<Integer> privateNumOfSockets;

    public ListModel<Integer> getNumOfSockets() {
        return privateNumOfSockets;
    }

    private void setNumOfSockets(NotChangableForVmInPoolListModel<Integer> value) {
        privateNumOfSockets = value;
    }

    private NotChangableForVmInPoolEntityModel<String> privateTotalCPUCores;

    public EntityModel<String> getTotalCPUCores() {
        return privateTotalCPUCores;
    }

    private void setTotalCPUCores(NotChangableForVmInPoolEntityModel<String> value) {
        privateTotalCPUCores = value;
    }

    private NotChangableForVmInPoolListModel<Integer> privateCoresPerSocket;

    public ListModel<Integer> getCoresPerSocket() {
        return privateCoresPerSocket;
    }

    private void setCoresPerSocket(NotChangableForVmInPoolListModel<Integer> value) {
        privateCoresPerSocket = value;
    }

    private NotChangableForVmInPoolListModel<VDS> privateDefaultHost;

    public ListModel<VDS> getDefaultHost() {
        return privateDefaultHost;
    }

    private void setDefaultHost(NotChangableForVmInPoolListModel<VDS> value) {
        privateDefaultHost = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateisSmartcardEnabled;

    public EntityModel<Boolean> getIsSmartcardEnabled() {
        return privateisSmartcardEnabled;
    }

    private void setIsSmartcardEnabled(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateisSmartcardEnabled = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> isConsoleDeviceEnabled;

    public void setRngDevice(VmRngDevice dev) {
        maybeSetEntity(rngBytes, dev.getBytes() == null ? null : dev.getBytes());
        maybeSetEntity(rngPeriod, dev.getPeriod() == null ? null : dev.getPeriod());
        maybeSetEntity(rngSourceRandom, dev.getSource() == VmRngDevice.Source.RANDOM);
        maybeSetEntity(rngSourceHwrng, dev.getSource() == VmRngDevice.Source.HWRNG);

        // post check - at least one source must be selected
        // if, for example, instance type has forbidden source checked, maybeSetEntity doesn't select any source, which
        // is invalid
        if (!Boolean.TRUE.equals(rngSourceRandom.getEntity()) && !Boolean.TRUE.equals(rngSourceHwrng.getEntity())) {
            getBehavior().deactivateInstanceTypeManager();

            EntityModel[] entityModels = {rngSourceRandom, rngSourceHwrng};
            for (EntityModel entityModel : entityModels) {
                if (entityModel.getIsAvailable() && entityModel.getIsChangable()) {
                    entityModel.setEntity(Boolean.TRUE); // select first available
                    break;
                }
            }

            getBehavior().activateInstanceTypeManager();
        }
    }

    private <T> void maybeSetEntity(EntityModel<T> entityModel, T value) {
        if (entityModel != null && entityModel.getIsChangable() && entityModel.getIsAvailable()) {
            entityModel.setEntity(value);
        }
    }

    public VmRngDevice generateRngDevice() {
        VmRngDevice dev = new VmRngDevice();
        dev.setBytes(rngBytes.getEntity());
        dev.setPeriod(rngPeriod.getEntity());
        dev.setSource(Boolean.TRUE.equals(rngSourceRandom.getEntity()) ? VmRngDevice.Source.RANDOM : VmRngDevice.Source.HWRNG);
        return dev;
    }

    private NotChangableForVmInPoolEntityModel<Integer> rngPeriod;

    public EntityModel<Integer> getRngPeriod() {
        return rngPeriod;
    }

    public void setRngPeriod(NotChangableForVmInPoolEntityModel<Integer> rngPeriod) {
        this.rngPeriod = rngPeriod;
    }

    public EntityModel<Integer> getRngBytes() {
        return rngBytes;
    }

    public void setRngBytes(NotChangableForVmInPoolEntityModel<Integer> rngBytes) {
        this.rngBytes = rngBytes;
    }

    public EntityModel<Boolean> getRngSourceHwrng() {
        return rngSourceHwrng;
    }

    public void setRngSourceHwrng(NotChangableForVmInPoolEntityModel<Boolean> rngSourceHwrng) {
        this.rngSourceHwrng = rngSourceHwrng;
    }

    public EntityModel<Boolean> getRngSourceRandom() {
        return rngSourceRandom;
    }

    public void setRngSourceRandom(NotChangableForVmInPoolEntityModel<Boolean> rngSourceRandom) {
        this.rngSourceRandom = rngSourceRandom;
    }

    private NotChangableForVmInPoolEntityModel<Integer> rngBytes;

    private NotChangableForVmInPoolEntityModel<Boolean> rngSourceRandom;
    private NotChangableForVmInPoolEntityModel<Boolean> rngSourceHwrng;

    private NotChangableForVmInPoolEntityModel<Boolean> isRngEnabled;

    public EntityModel<Boolean> getIsRngEnabled() {
        return isRngEnabled;
    }

    public void setIsRngEnabled(NotChangableForVmInPoolEntityModel<Boolean> rngEnabled) {
        isRngEnabled = rngEnabled;
    }

    public EntityModel<Boolean> getIsConsoleDeviceEnabled() {
        return isConsoleDeviceEnabled;
    }

    private void setConsoleDeviceEnabled(NotChangableForVmInPoolEntityModel<Boolean> consoleDeviceEnabled) {
        this.isConsoleDeviceEnabled = consoleDeviceEnabled;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateIsStateless;

    public EntityModel<Boolean> getIsStateless() {
        return privateIsStateless;
    }

    private void setIsStateless(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateIsStateless = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateIsRunAndPause;

    public EntityModel<Boolean> getIsRunAndPause() {
        return privateIsRunAndPause;
    }

    private void setIsRunAndPause(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateIsRunAndPause = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateIsDeleteProtected;

    public EntityModel<Boolean> getIsDeleteProtected() {
        return privateIsDeleteProtected;
    }

    public void setIsDeleteProtected(NotChangableForVmInPoolEntityModel<Boolean> deleteProtected) {
        this.privateIsDeleteProtected = deleteProtected;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> ssoMethodNone;

    public EntityModel<Boolean> getSsoMethodNone() {
        return ssoMethodNone;
    }

    public void setSsoMethodNone(NotChangableForVmInPoolEntityModel<Boolean> ssoMethodNone) {
        this.ssoMethodNone = ssoMethodNone;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> ssoMethodGuestAgent;

    public EntityModel<Boolean> getSsoMethodGuestAgent() {
        return ssoMethodGuestAgent;
    }

    public void setSsoMethodGuestAgent(NotChangableForVmInPoolEntityModel<Boolean> ssoMethodGuestAgent) {
        this.ssoMethodGuestAgent = ssoMethodGuestAgent;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> copyPermissions;

    public EntityModel<Boolean> getCopyPermissions() {
        return copyPermissions;
    }

    private void setCopyPermissions(NotChangableForVmInPoolEntityModel<Boolean> copyPermissions) {
        this.copyPermissions = copyPermissions;
    }

    private EntityModel<Boolean> memoryBalloonDeviceEnabled;

    public EntityModel<Boolean> getMemoryBalloonDeviceEnabled() {
        return memoryBalloonDeviceEnabled;
    }

    public void setMemoryBalloonDeviceEnabled(EntityModel<Boolean> memoryBalloonDeviceEnabled) {
        this.memoryBalloonDeviceEnabled = memoryBalloonDeviceEnabled;
    }

    private NotChangableForVmInPoolListModel<DisplayType> displayType;

    public ListModel<DisplayType> getDisplayType() {
        return displayType;
    }

    private void setDisplayType(NotChangableForVmInPoolListModel<DisplayType> value) {
        displayType = value;
    }

    private NotChangableForVmInPoolListModel<GraphicsTypes> graphicsType;

    public ListModel<GraphicsTypes> getGraphicsType() {
        return graphicsType;
    }

    /**
     * Enum for representing (possibly multiple) graphics device of a VM.
     */
    public enum GraphicsTypes {
        NONE(),
        SPICE(GraphicsType.SPICE),
        VNC(GraphicsType.VNC),
        SPICE_AND_VNC(GraphicsType.SPICE, GraphicsType.VNC);

        private Set<GraphicsType> backingTypes;

        private GraphicsTypes(GraphicsType ... backingTypes) {
            this.backingTypes = new HashSet<>();
            if (backingTypes != null) {
                for (GraphicsType backingType : backingTypes) {
                    this.backingTypes.add(backingType);
                }
            }
        }

        public Collection<GraphicsType> getBackingGraphicsTypes() {
            return backingTypes;
        }

        public static GraphicsTypes fromGraphicsType(GraphicsType type) {
            switch (type) {
                case SPICE:
                    return SPICE;
                case VNC:
                    return VNC;
            }
            return null;
        }

        public static GraphicsTypes fromGraphicsTypes(Set<GraphicsType> types) {
            for (GraphicsTypes myTypes : values()) {
                if (myTypes.getBackingGraphicsTypes().equals(types)) {
                    return myTypes;
                }
            }

            return NONE;
        }
    }

    private void setGraphicsType(NotChangableForVmInPoolListModel<GraphicsTypes> graphicsType) {
        this.graphicsType = graphicsType;
    }

    /**
     * Template provisioning: clone / thin - how to copy template disk.
     * <ul>
     *     <li>true - Clone</li>
     *     <li>false - Thin</li>
     * </ul>
     * Aggregation of {@link #privateProvisioningThin_IsSelected}
     * and {@link #privateProvisioningClone_IsSelected}.
     */
    private NotChangableForVmInPoolEntityModel<Boolean> privateProvisioning;

    public EntityModel<Boolean> getProvisioning() {
        return privateProvisioning;
    }

    private void setProvisioning(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateProvisioning = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateProvisioningThin_IsSelected;

    public EntityModel<Boolean> getProvisioningThin_IsSelected() {
        return privateProvisioningThin_IsSelected;
    }

    public void setProvisioningThin_IsSelected(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateProvisioningThin_IsSelected = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateProvisioningClone_IsSelected;

    public EntityModel<Boolean> getProvisioningClone_IsSelected() {
        return privateProvisioningClone_IsSelected;
    }

    public void setProvisioningClone_IsSelected(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateProvisioningClone_IsSelected = value;
    }

    private EntityModel<Boolean> isVirtioScsiEnabled;

    public EntityModel<Boolean> getIsVirtioScsiEnabled() {
        return isVirtioScsiEnabled;
    }

    public void setIsVirtioScsiEnabled(EntityModel<Boolean> virtioScsiEnabled) {
        this.isVirtioScsiEnabled = virtioScsiEnabled;
    }

    private NotChangableForVmInPoolListModel<EntityModel<Integer>> privatePriority;

    public ListModel<EntityModel<Integer>> getPriority() {
        return privatePriority;
    }

    private void setPriority(NotChangableForVmInPoolListModel<EntityModel<Integer>> value) {
        privatePriority = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateIsHighlyAvailable;

    public EntityModel<Boolean> getIsHighlyAvailable() {
        return privateIsHighlyAvailable;
    }

    private void setIsHighlyAvailable(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateIsHighlyAvailable = value;
    }

    private NotChangableForVmInPoolListModel<EntityModel<BootSequence>> privateFirstBootDevice;

    public ListModel<EntityModel<BootSequence>> getFirstBootDevice() {
        return privateFirstBootDevice;
    }

    private void setFirstBootDevice(NotChangableForVmInPoolListModel<EntityModel<BootSequence>> value) {
        privateFirstBootDevice = value;
    }

    private NotChangableForVmInPoolListModel<EntityModel<BootSequence>> privateSecondBootDevice;

    public ListModel<EntityModel<BootSequence>> getSecondBootDevice() {
        return privateSecondBootDevice;
    }

    private void setSecondBootDevice(NotChangableForVmInPoolListModel<EntityModel<BootSequence>> value) {
        privateSecondBootDevice = value;
    }

    private NotChangableForVmInPoolListModel<String> privateCdImage;

    public ListModel<String> getCdImage() {
        return privateCdImage;
    }

    private void setCdImage(NotChangableForVmInPoolListModel<String> value) {
        privateCdImage = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> isSubTemplate;

    public EntityModel<Boolean> getIsSubTemplate() {
        return isSubTemplate;
    }

    public void setIsSubTemplate(NotChangableForVmInPoolEntityModel<Boolean> value) {
        isSubTemplate = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> cdAttached;

    public EntityModel<Boolean> getCdAttached() {
        return cdAttached;
    }

    public void setCdAttached(NotChangableForVmInPoolEntityModel<Boolean> value) {
        cdAttached = value;
    }

    private NotChangableForVmInPoolEntityModel<String> privateInitrd_path;

    public EntityModel<String> getInitrd_path() {
        return privateInitrd_path;
    }

    private void setInitrd_path(NotChangableForVmInPoolEntityModel<String> value) {
        privateInitrd_path = value;
    }

    private NotChangableForVmInPoolEntityModel<String> privateKernel_path;

    public EntityModel<String> getKernel_path() {
        return privateKernel_path;
    }

    private void setKernel_path(NotChangableForVmInPoolEntityModel<String> value) {
        privateKernel_path = value;
    }

    private NotChangableForVmInPoolEntityModel<String> privateKernel_parameters;

    public EntityModel<String> getKernel_parameters() {
        return privateKernel_parameters;
    }

    private void setKernel_parameters(NotChangableForVmInPoolEntityModel<String> value) {
        privateKernel_parameters = value;
    }

    private NotChangableForVmInPoolEntityModel<String> privateCustomProperties;

    public EntityModel<String> getCustomProperties() {
        return privateCustomProperties;
    }

    private void setCustomProperties(NotChangableForVmInPoolEntityModel<String> value) {
        privateCustomProperties = value;
    }

    private EntityModel<Boolean> vmInitEnabled;

    public EntityModel<Boolean> getVmInitEnabled() {
        return vmInitEnabled;
    }

    public void setVmInitEnabled(EntityModel<Boolean> vmInitEnabled) {
        this.vmInitEnabled = vmInitEnabled;
    }

    private EntityModel<Boolean> cloudInitEnabled;

    private EntityModel<Boolean> sysprepEnabled;

    public EntityModel<Boolean> getCloudInitEnabled() {
        return cloudInitEnabled;
    }

    public void setCloudInitEnabled(EntityModel<Boolean> cloudInitEnabled) {
        this.cloudInitEnabled = cloudInitEnabled;
    }

    public EntityModel<Boolean> getSysprepEnabled() {
        return sysprepEnabled;
    }

    public void setSysprepEnabled(EntityModel<Boolean> sysprepEnabled) {
        this.sysprepEnabled = sysprepEnabled;
    }

    private VmInitModel vmInitModel;

    public VmInitModel getVmInitModel() {
        return vmInitModel;
    }

    public void setVmInitModel(VmInitModel vmInitModel) {
        this.vmInitModel = vmInitModel;
    }


    private NotChangableForVmInPoolKeyValueModel customPropertySheet;

    public KeyValueModel getCustomPropertySheet() {
        return customPropertySheet;
    }

    public void setCustomPropertySheet(NotChangableForVmInPoolKeyValueModel customPropertySheet) {
        this.customPropertySheet = customPropertySheet;
    }

    private Map<Version, Map<String, String>> privateCustomPropertiesKeysList;

    public Map<Version, Map<String, String>> getCustomPropertiesKeysList() {
        return privateCustomPropertiesKeysList;
    }

    public void setCustomPropertiesKeysList(Map<Version, Map<String, String>> value) {
        privateCustomPropertiesKeysList = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateIsAutoAssign;

    public EntityModel<Boolean> getIsAutoAssign() {
        return privateIsAutoAssign;
    }

    public void setIsAutoAssign(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateIsAutoAssign = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> hostCpu;

    public EntityModel<Boolean> getHostCpu() {
        return hostCpu;
    }

    public void setHostCpu(NotChangableForVmInPoolEntityModel<Boolean> hostCpu) {
        this.hostCpu = hostCpu;
    }

    private NotChangableForVmInPoolListModel<MigrationSupport> migrationMode;

    public ListModel<MigrationSupport> getMigrationMode() {
        return migrationMode;
    }

    public void setMigrationMode(NotChangableForVmInPoolListModel<MigrationSupport> value) {
        migrationMode = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> overrideMigrationDowntime;

    public EntityModel<Boolean> getOverrideMigrationDowntime() {
        return overrideMigrationDowntime;
    }

    private void setOverrideMigrationDowntime(NotChangableForVmInPoolEntityModel<Boolean> value) {
        overrideMigrationDowntime = value;
    }

    private NotChangableForVmInPoolEntityModel<Integer> migrationDowntime;

    public EntityModel<Integer> getMigrationDowntime() {
        return migrationDowntime;
    }

    private void setMigrationDowntime(NotChangableForVmInPoolEntityModel<Integer> value) {
        migrationDowntime = value;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> privateIsTemplatePublic;

    public EntityModel<Boolean> getIsTemplatePublic() {
        return privateIsTemplatePublic;
    }

    private void setIsTemplatePublic(NotChangableForVmInPoolEntityModel<Boolean> value) {
        privateIsTemplatePublic = value;
    }

    private boolean privateIsFirstRun;

    public boolean getIsFirstRun() {
        return privateIsFirstRun;
    }

    public void setIsFirstRun(boolean value) {
        privateIsFirstRun = value;
    }

    private List<DiskModel> disks;

    public List<DiskModel> getDisks() {
        return disks;
    }

    public void setDisks(List<DiskModel> value) {
        if (disks != value) {
            disks = value;
            onPropertyChanged(new PropertyChangedEventArgs("Disks")); //$NON-NLS-1$
        }
    }

    private DisksAllocationModel disksAllocationModel;

    public DisksAllocationModel getDisksAllocationModel() {
        return disksAllocationModel;
    }

    private void setDisksAllocationModel(DisksAllocationModel value) {
        disksAllocationModel = value;
    }

    private boolean isDisksAvailable;

    public boolean getIsDisksAvailable() {
        return isDisksAvailable;
    }

    public void setIsDisksAvailable(boolean value) {
        isDisksAvailable = value;
        onPropertyChanged(new PropertyChangedEventArgs("IsDisksAvailable")); //$NON-NLS-1$
    }

    private boolean isCustomPropertiesTabAvailable;

    public boolean getIsCustomPropertiesTabAvailable() {
        return isCustomPropertiesTabAvailable;
    }

    public void setIsCustomPropertiesTabAvailable(boolean value) {
        if (isCustomPropertiesTabAvailable != value) {
            isCustomPropertiesTabAvailable = value;
            onPropertyChanged(new PropertyChangedEventArgs("IsCustomPropertiesTabAvailable")); //$NON-NLS-1$
        }
    }

    private final VmModelBehaviorBase behavior;

    public VmModelBehaviorBase getBehavior() {
        return behavior;
    }

    private void setBehavior(VmModelBehaviorBase value) {
    }

    private NotChangableForVmInPoolEntityModel<String> cpuPinning;

    public EntityModel<String> getCpuPinning() {
        return cpuPinning;
    }

    public void setCpuPinning(NotChangableForVmInPoolEntityModel<String> cpuPinning) {
        this.cpuPinning = cpuPinning;
    }

    private NotChangableForVmInPoolEntityModel<Integer> cpuSharesAmount;

    public EntityModel<Integer> getCpuSharesAmount() {
        return cpuSharesAmount;
    }

    public void setCpuSharesAmount(NotChangableForVmInPoolEntityModel<Integer> cpuSharesAmount) {
        this.cpuSharesAmount = cpuSharesAmount;
    }

    private NotChangableForVmInPoolListModel<CpuSharesAmount> cpuSharesAmountSelection;

    public ListModel<CpuSharesAmount> getCpuSharesAmountSelection() {
        return cpuSharesAmountSelection;
    }

    public void setCpuSharesAmountSelection(NotChangableForVmInPoolListModel<CpuSharesAmount> cpuSharesAmountSelection) {
        this.cpuSharesAmountSelection = cpuSharesAmountSelection;
    }

    private ListModel<String> vncKeyboardLayout;

    public ListModel<String> getVncKeyboardLayout() {
        return vncKeyboardLayout;
    }

    public void setVncKeyboardLayout(ListModel<String> vncKeyboardLayout) {
        this.vncKeyboardLayout = vncKeyboardLayout;
    }

    private SerialNumberPolicyModel serialNumberPolicy;

    public SerialNumberPolicyModel getSerialNumberPolicy() {
        return serialNumberPolicy;
    }

    public void setSerialNumberPolicy(SerialNumberPolicyModel value) {
        this.serialNumberPolicy = value;
    }

    private EntityModel<Boolean> bootMenuEnabled;

    public EntityModel<Boolean> getBootMenuEnabled() {
        return bootMenuEnabled;
    }

    public void setBootMenuEnabled(EntityModel<Boolean> bootMenuEnabled) {
        this.bootMenuEnabled = bootMenuEnabled;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> spiceFileTransferEnabled;

    public EntityModel<Boolean> getSpiceFileTransferEnabled() {
        return spiceFileTransferEnabled;
    }

    public void setSpiceFileTransferEnabled(NotChangableForVmInPoolEntityModel<Boolean> spiceFileTransferEnabled) {
        this.spiceFileTransferEnabled = spiceFileTransferEnabled;
    }

    private NotChangableForVmInPoolEntityModel<Boolean> spiceCopyPasteEnabled;

    public EntityModel<Boolean> getSpiceCopyPasteEnabled() {
        return spiceCopyPasteEnabled;
    }

    public void setSpiceCopyPasteEnabled(NotChangableForVmInPoolEntityModel<Boolean> spiceCopyPasteEnabled) {
        this.spiceCopyPasteEnabled = spiceCopyPasteEnabled;
    }

    private NotChangableForVmInPoolListModel<CpuProfile> cpuProfiles;

    public ListModel<CpuProfile> getCpuProfiles() {
        return cpuProfiles;
    }

    public void setCpuProfiles(NotChangableForVmInPoolListModel<CpuProfile> cpuProfiles) {
        this.cpuProfiles = cpuProfiles;
    }

    private EntityModel<Boolean> numaEnabled;

    private NotChangableForVmInPoolListModel<NumaTuneMode> numaTuneMode;

    public ListModel<NumaTuneMode> getNumaTuneMode() {
        return numaTuneMode;
    }

    public void setNumaTuneMode(NotChangableForVmInPoolListModel<NumaTuneMode> numaTuneMode) {
        this.numaTuneMode = numaTuneMode;
    }

    private int initialsNumaNodeCount;

    private NotChangableForVmInPoolEntityModel<Integer> numaNodeCount;

    public EntityModel<Integer> getNumaNodeCount() {
        return numaNodeCount;
    }

    public void setNumaNodeCount(NotChangableForVmInPoolEntityModel<Integer> numaNodeCount) {
        this.numaNodeCount = numaNodeCount;
    }

    private List<VmNumaNode> vmNumaNodes;

    public List<VmNumaNode> getVmNumaNodes() {
        return vmNumaNodes;
    }

    public void setVmNumaNodes(List<VmNumaNode> vmNumaNodes) {
        this.vmNumaNodes = vmNumaNodes;
    }

    private UICommand numaSupportCommand;

    public UICommand getNumaSupportCommand() {
        return numaSupportCommand;
    }

    private void setNumaSupportCommand(UICommand numaSupportCommand) {
        this.numaSupportCommand = numaSupportCommand;
    }

    private boolean numaChanged = false;

    private ListModel<Boolean> autoConverge;

    public ListModel<Boolean> getAutoConverge() {
        return autoConverge;
    }

    public void setAutoConverge(NotChangableForVmInPoolListModel<Boolean> autoConverge) {
        this.autoConverge = autoConverge;
    }

    private ListModel<Boolean> migrateCompressed;

    public ListModel<Boolean> getMigrateCompressed() {
        return migrateCompressed;
    }

    public void setMigrateCompressed(NotChangableForVmInPoolListModel<Boolean> migrateCompressed) {
        this.migrateCompressed = migrateCompressed;
    }

    public UnitVmModel(VmModelBehaviorBase behavior, ListModel<?> parentModel) {
        this.behavior = behavior;
        this.behavior.setModel(this);

        setNicsWithLogicalNetworks(new VnicInstancesModel());
        setAdvancedMode(new EntityModel<Boolean>(false));
        setValid(new EntityModel<Boolean>(true));
        setAttachedToInstanceType(new EntityModel<Boolean>(true));
        setStorageDomain(new NotChangableForVmInPoolListModel<StorageDomain>());
        setName(new NotChangableForVmInPoolEntityModel<String>());
        setNumOfMonitors(new NotChangableForVmInPoolListModel<Integer>());
        setAllowConsoleReconnect(new NotChangableForVmInPoolEntityModel<Boolean>());
        setVmId(new NotChangableForVmInPoolEntityModel<String>());
        setDescription(new NotChangableForVmInPoolEntityModel<String>());
        setComment(new NotChangableForVmInPoolEntityModel<String>());
        setMinAllocatedMemory(new NotChangableForVmInPoolEntityModel<Integer>());
        setUsbPolicy(new NotChangableForVmInPoolListModel<UsbPolicy>());
        setConsoleDisconnectAction(new NotChangableForVmInPoolListModel<ConsoleDisconnectAction>());
        setIsStateless(new NotChangableForVmInPoolEntityModel<Boolean>());
        setIsRunAndPause(new NotChangableForVmInPoolEntityModel<Boolean>());
        setIsSmartcardEnabled(new NotChangableForVmInPoolEntityModel<Boolean>());
        setIsDeleteProtected(new NotChangableForVmInPoolEntityModel<Boolean>());
        setSsoMethodNone(new NotChangableForVmInPoolEntityModel<Boolean>());
        setSsoMethodGuestAgent(new NotChangableForVmInPoolEntityModel<Boolean>());
        setConsoleDeviceEnabled(new NotChangableForVmInPoolEntityModel<Boolean>());
        setCopyPermissions(new NotChangableForVmInPoolEntityModel<Boolean>());

        //rng
        setIsRngEnabled(new NotChangableForVmInPoolEntityModel<Boolean>());
        getIsRngEnabled().getEntityChangedEvent().addListener(this);
        setRngBytes(new NotChangableForVmInPoolEntityModel<Integer>());
        setRngPeriod(new NotChangableForVmInPoolEntityModel<Integer>());
        setRngSourceRandom(new NotChangableForVmInPoolEntityModel<Boolean>());
        setRngSourceHwrng(new NotChangableForVmInPoolEntityModel<Boolean>());

        // by default not available - only for new VM
        getCopyPermissions().setIsAvailable(false);
        getCopyPermissions().setEntity(false);
        setVncKeyboardLayout(new NotChangableForVmInPoolListModel<String>());
        setVmType(new NotChangableForVmInPoolListModel<VmType>());
        getVmType().setItems(Arrays.asList(VmType.Desktop, VmType.Server));
        getVmType().setSelectedItem(VmType.Server);
        getVmType().setIsChangeable(false);
        getVmType().getSelectedItemChangedEvent().addListener(this);

        setCdImage(new NotChangableForVmInPoolListModel<String>());
        getCdImage().setIsChangeable(false);

        setMemoryBalloonDeviceEnabled(new EntityModel<Boolean>());
        getMemoryBalloonDeviceEnabled().setEntity(true);
        getMemoryBalloonDeviceEnabled().setIsAvailable(false);

        setSpiceProxyEnabled(new EntityModel<Boolean>(false));
        setSpiceProxy(new EntityModel<String>());

        setIsSubTemplate(new NotChangableForVmInPoolEntityModel<Boolean>(false));
        getIsSubTemplate().getEntityChangedEvent().addListener(this);
        setTemplateVersionName(new NotChangableForVmInPoolEntityModel<String>());
        setBaseTemplate(new NotChangableForVmInPoolListModel<VmTemplate>());
        getBaseTemplate().getSelectedItemChangedEvent().addListener(this);

        setCdAttached(new NotChangableForVmInPoolEntityModel<Boolean>());
        getCdAttached().getEntityChangedEvent().addListener(new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {

                getCdImage().setIsChangeable(getCdAttached().getEntity());
            }
        });
        getCdAttached().setEntity(false);

        setIsHighlyAvailable(new NotChangableForVmInPoolEntityModel<Boolean>());
        getIsHighlyAvailable().getEntityChangedEvent().addListener(this);
        setIsTemplatePublic(new NotChangableForVmInPoolEntityModel<Boolean>());
        setKernel_parameters(new NotChangableForVmInPoolEntityModel<String>());
        setKernel_path(new NotChangableForVmInPoolEntityModel<String>());
        setInitrd_path(new NotChangableForVmInPoolEntityModel<String>());
        setCustomProperties(new NotChangableForVmInPoolEntityModel<String>());
        setCustomPropertySheet(new NotChangableForVmInPoolKeyValueModel());
        setDisplayType(new NotChangableForVmInPoolListModel<DisplayType>());
        setGraphicsType(new NotChangableForVmInPoolListModel<GraphicsTypes>());
        setSecondBootDevice(new NotChangableForVmInPoolListModel<EntityModel<BootSequence>>());
        setBootMenuEnabled(new NotChangableForVmInPoolEntityModel<Boolean>());
        setPriority(new NotChangableForVmInPoolListModel<EntityModel<Integer>>());
        setVmInitEnabled(new EntityModel<Boolean>(false));
        setCloudInitEnabled(new EntityModel<Boolean>());
        setSpiceFileTransferEnabled(new NotChangableForVmInPoolEntityModel<Boolean>());
        setSpiceCopyPasteEnabled(new NotChangableForVmInPoolEntityModel<Boolean>());
        setSysprepEnabled(new EntityModel<Boolean>());
        getVmInitEnabled().getEntityChangedEvent().addListener(this);
        setVmInitModel(new VmInitModel());
        setTemplateWithVersion(new NotChangableForVmInPoolListModel<TemplateWithVersion>());
        getTemplateWithVersion().getSelectedItemChangedEvent().addListener(this);

        setInstanceTypes(new NotChangableForVmInPoolListModel<InstanceType>());
        setInstanceImages(new InstanceImagesModel(this, parentModel));

        setQuota(new NotChangableForVmInPoolListModel<Quota>());
        getQuota().setIsAvailable(false);

        setDataCenterWithClustersList(new NotChangableForVmInPoolListModel<DataCenterWithCluster>());
        getDataCenterWithClustersList().getSelectedItemChangedEvent().addListener(this);

        setEmulatedMachine(new NotChangableForVmInPoolListModel<String>());

        setCustomCpu(new NotChangableForVmInPoolListModel<String>());

        setTimeZone(new NotChangableForVmInPoolListModel<TimeZoneModel>());
        getTimeZone().getSelectedItemChangedEvent().addListener(this);

        setDefaultHost(new NotChangableForVmInPoolListModel<VDS>());
        getDefaultHost().getSelectedItemChangedEvent().addListener(this);

        setOSType(new NotChangableForVmInPoolListModel<Integer>() {
            @Override
            public void setSelectedItem(Integer value) {
                if (!AsyncDataProvider.getInstance().osNameExists(value)) {
                    DataCenterWithCluster dataCenterWithCluster = getDataCenterWithClustersList().getSelectedItem();
                    VDSGroup cluster = dataCenterWithCluster == null ? null : dataCenterWithCluster.getCluster();
                    if (cluster == null) {
                        return;
                    }
                    super.setSelectedItem(AsyncDataProvider.getInstance().getDefaultOs(cluster.getArchitecture()));
                } else {
                    super.setSelectedItem(value);
                }
            }
        });

        getOSType().getSelectedItemChangedEvent().addListener(this);

        setFirstBootDevice(new NotChangableForVmInPoolListModel<EntityModel<BootSequence>>());
        getFirstBootDevice().getSelectedItemChangedEvent().addListener(this);

        setProvisioning(new NotChangableForVmInPoolEntityModel<Boolean>());
        getProvisioning().getEntityChangedEvent().addListener(this);

        setMemSize(new NotChangableForVmInPoolEntityModel<Integer>());
        getMemSize().getEntityChangedEvent().addListener(this);

        setTotalCPUCores(new NotChangableForVmInPoolEntityModel<String>());
        getTotalCPUCores().getEntityChangedEvent().addListener(this);

        setNumOfSockets(new NotChangableForVmInPoolListModel<Integer>());
        getNumOfSockets().getSelectedItemChangedEvent().addListener(this);

        setCoresPerSocket(new NotChangableForVmInPoolListModel<Integer>());
        getCoresPerSocket().getSelectedItemChangedEvent().addListener(this);

        setSerialNumberPolicy(new SerialNumberPolicyModel());

        setMigrationMode(new NotChangableForVmInPoolListModel<MigrationSupport>());
        getMigrationMode().getSelectedItemChangedEvent().addListener(this);

        setOverrideMigrationDowntime(new NotChangableForVmInPoolEntityModel<Boolean>());
        getOverrideMigrationDowntime().getEntityChangedEvent().addListener(this);

        setMigrationDowntime(new NotChangableForVmInPoolEntityModel<Integer>());
        getMigrationDowntime().getEntityChangedEvent().addListener(this);

        setHostCpu(new NotChangableForVmInPoolEntityModel<Boolean>());
        getHostCpu().getEntityChangedEvent().addListener(this);

        setWatchdogAction(new NotChangableForVmInPoolListModel<VmWatchdogAction>());
        getWatchdogAction().getSelectedItemChangedEvent().addListener(this);
        ArrayList<VmWatchdogAction> watchDogActions = new ArrayList<VmWatchdogAction>();
        for (VmWatchdogAction action : VmWatchdogAction.values()) {
            watchDogActions.add(action);
        }
        getWatchdogAction().setItems(watchDogActions);
        getWatchdogAction().setIsChangeable(false);

        setWatchdogModel(new NotChangableForVmInPoolListModel<VmWatchdogType>());
        getWatchdogModel().getSelectedItemChangedEvent().addListener(this);

        setIsAutoAssign(new NotChangableForVmInPoolEntityModel<Boolean>());
        getIsAutoAssign().getEntityChangedEvent().addListener(this);

        setIsTemplatePublic(new NotChangableForVmInPoolEntityModel<Boolean>());
        getIsTemplatePublic().getEntityChangedEvent().addListener(this);
        setIsCustomPropertiesTabAvailable(true);

        resetTabsValidity();

        // NOTE: This is because currently the auto generated view code tries to register events of
        // pooltype for
        // VM/Template views as this model is shared across VM/Template/Pool models
        setPoolType(new NotChangableForVmInPoolListModel<EntityModel<VmPoolType>>());

        setNumOfDesktops(new NotChangableForVmInPoolEntityModel<Integer>());
        getNumOfDesktops().setEntity(0);
        getNumOfDesktops().setIsAvailable(false);

        setAssignedVms(new NotChangableForVmInPoolEntityModel<Integer>());
        getAssignedVms().setEntity(0);
        getAssignedVms().setIsAvailable(false);
        // Assigned VMs count is always read-only.
        getAssignedVms().setIsChangeable(false);

        setPrestartedVms(new NotChangableForVmInPoolEntityModel<Integer>());
        getPrestartedVms().setEntity(0);
        getPrestartedVms().setIsAvailable(false);

        setMaxAssignedVmsPerUser(new NotChangableForVmInPoolEntityModel<Integer>());
        getMaxAssignedVmsPerUser().setEntity(1);
        getMaxAssignedVmsPerUser().setIsAvailable(false);

        setDisksAllocationModel(new DisksAllocationModel());

        setIsVirtioScsiEnabled(new EntityModel<Boolean>());
        getIsVirtioScsiEnabled().setEntity(false);
        getIsVirtioScsiEnabled().setIsAvailable(false);

        setProvisioningClone_IsSelected(new NotChangableForVmInPoolEntityModel<Boolean>());
        getProvisioningClone_IsSelected().getEntityChangedEvent().addListener(this);

        setProvisioningThin_IsSelected(new NotChangableForVmInPoolEntityModel<Boolean>());
        getProvisioningThin_IsSelected().getEntityChangedEvent().addListener(this);

        setCpuPinning(new NotChangableForVmInPoolEntityModel<String>());
        getCpuPinning().setEntity("");
        getCpuPinning().setIsChangeable(false);

        setCpuSharesAmount(new NotChangableForVmInPoolEntityModel<Integer>());
        getCpuSharesAmount().setIsChangeable(false);

        setCpuSharesAmountSelection(new NotChangableForVmInPoolListModel<CpuSharesAmount>());
        getCpuSharesAmountSelection().setItems(Arrays.asList(CpuSharesAmount.values()));
        getCpuSharesAmountSelection().getSelectedItemChangedEvent().addListener(this);
        getCpuSharesAmountSelection().getSelectedItemChangedEvent().addListener(this);
        getCpuSharesAmountSelection().setSelectedItem(CpuSharesAmount.DISABLED);

        setIsSoundcardEnabled(new NotChangableForVmInPoolEntityModel<Boolean>());
        getIsSoundcardEnabled().setEntity(false);
        getIsSoundcardEnabled().setIsChangeable(false);

        setIsSingleQxlEnabled(new NotChangableForVmInPoolEntityModel<Boolean>());
        getBehavior().enableSinglePCI(false);

        selectSsoMethod(SsoMethod.GUEST_AGENT);

        setEditingEnabled(new EntityModel<Boolean>());
        getEditingEnabled().setEntity(true);

        setCpuProfiles(new NotChangableForVmInPoolListModel<CpuProfile>());
        getCpuProfiles().setIsAvailable(false);

        setNumaTuneMode(new NotChangableForVmInPoolListModel<NumaTuneMode>());
        getNumaTuneMode().setItems(AsyncDataProvider.getInstance().getNumaTuneModeList());
        getNumaTuneMode().setSelectedItem(NumaTuneMode.INTERLEAVE);

        setNumaNodeCount(new NotChangableForVmInPoolEntityModel<Integer>());
        getNumaNodeCount().setEntity(0);
        setNumaEnabled(new EntityModel<Boolean>());
        getNumaEnabled().setMessage(ConstantsManager.getInstance().getConstants().numaDisabledInfoMessage());

        setNumaSupportCommand(new UICommand("NumaSupport", new ICommandTarget() { //$NON-NLS-1$
                    @Override
                    public void executeCommand(UICommand command) {
                        numaSupport();
                    }

                    @Override
                    public void executeCommand(UICommand uiCommand, Object... parameters) {
                        numaSupport();
                    }
                }));

        setAutoConverge(new NotChangableForVmInPoolListModel<Boolean>());
        getAutoConverge().setItems(Arrays.asList(null, true, false));
        setMigrateCompressed(new NotChangableForVmInPoolListModel<Boolean>());
        getMigrateCompressed().setItems(Arrays.asList(null, true, false));
        setIcon(new NotChangableForVmInPoolEntityModel<IconWithOsDefault>());

        setIoThreadsEnabled(new NotChangableForVmInPoolEntityModel<>(false));
        setNumOfIoThreads(new NotChangableForVmInPoolEntityModel<>(0));
        getNumOfIoThreads().setIsAvailable(false);
        getIoThreadsEnabled().getEntityChangedEvent().addListener(this);

        setProviders(new NotChangableForVmInPoolListModel<Provider<OpenstackNetworkProviderProperties>>());
    }

    public void initForemanProviders(final Guid selected) {
        AsyncQuery getProvidersQuery = new AsyncQuery();
        getProvidersQuery.asyncCallback = new INewAsyncCallback() {
            @SuppressWarnings("unchecked")
            @Override
            public void onSuccess(Object model, Object result) {
                List<Provider<OpenstackNetworkProviderProperties>> providers =
                        (List<Provider<OpenstackNetworkProviderProperties>>) result;
                Provider<OpenstackNetworkProviderProperties> noneProvider = createNoneProvider();
                providers.add(0, noneProvider);
                ListModel<Provider<OpenstackNetworkProviderProperties>> providersListModel = getProviders();
                if (selected != null) {
                    //Find the selected provider.
                    for (Provider<OpenstackNetworkProviderProperties> provider: providers) {
                        if (provider.getId().equals(selected)) {
                            providersListModel.setItems(providers, provider);
                            break;
                        }
                    }
                }
                if (providersListModel.getItems() == null || providersListModel.getItems().isEmpty()) {
                    providersListModel.setItems(providers, providers.get(0));
                }
                providersListModel.setIsChangeable(true);
            }

            private Provider<OpenstackNetworkProviderProperties> createNoneProvider() {
                Provider<OpenstackNetworkProviderProperties> noneProvider = new Provider<>();
                noneProvider.setId(Guid.Empty);
                noneProvider.setName(constants.providerNone());
                return noneProvider;
            }
        };
        AsyncDataProvider.getInstance().getAllProvidersByType(getProvidersQuery, ProviderType.FOREMAN);
    }

    public void initialize(SystemTreeItemModel SystemTreeSelectedItem) {
        super.initialize();

        getMemSize().setEntity(256);
        getMinAllocatedMemory().setEntity(256);
        getIsStateless().setEntity(false);
        getIsRunAndPause().setEntity(false);
        getIsSmartcardEnabled().setEntity(false);
        isConsoleDeviceEnabled.setEntity(false);
        getIsHighlyAvailable().setEntity(false);
        getIsAutoAssign().setEntity(true);
        getIsTemplatePublic().setEntity(true);
        getBehavior().enableSinglePCI(false);

        isRngEnabled.setEntity(false);
        rngSourceRandom.setEntity(true);

        getHostCpu().setEntity(false);
        getMigrationMode().setIsChangeable(true);

        getCdImage().setIsChangeable(false);

        initGraphicsAndDisplayListeners();
        initFirstBootDevice();
        initNumOfMonitors();
        initAllowConsoleReconnect();
        initMigrationMode();
        initVncKeyboardLayout();
        initConsoleDisconnectAction();

        behavior.initialize(SystemTreeSelectedItem);
    }

    @Override
    public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
        super.eventRaised(ev, sender, args);

        if (ev.matchesDefinition(ListModel.selectedItemChangedEventDefinition)) {
            if (sender == getVmType()) {
                vmTypeChanged();
            } else if (sender == getDataCenterWithClustersList()) {
                dataCenterWithClusterSelectedItemChanged(sender, args);
                updateDisplayAndGraphics();
                behavior.updateNumOfIoThreads();
                initUsbPolicy();
                behavior.updateEmulatedMachines();
                behavior.updateCustomCpu();
            }
            else if (sender == getTemplateWithVersion()) {
                templateWithVersion_SelectedItemChanged(sender, args);
            }
            else if (sender == getTimeZone()) {
                timeZone_SelectedItemChanged(sender, args);
            }
            else if (sender == getDefaultHost()) {
                defaultHost_SelectedItemChanged(sender, args);
            }
            else if (sender == getOSType()) {
                getBehavior().deactivateInstanceTypeManager(new InstanceTypeManager.ActivatedListener() {
                    @Override
                    public void activated() {
                        if (getBehavior().getInstanceTypeManager() != null && !getBehavior().basedOnCustomInstanceType()) {
                            getBehavior().getInstanceTypeManager().updateFildsAfterOsChanged();
                        }
                    }
                });

                oSType_SelectedItemChanged(sender, args);
                getBehavior().oSType_SelectedItemChanged();
                getVmInitModel().osTypeChanged(getOSType().getSelectedItem());
                updateDisplayAndGraphics();
                getBehavior().updateMemoryBalloon();
                initUsbPolicy();

                getBehavior().activateInstanceTypeManager();
            }
            else if (sender == getFirstBootDevice()) {
                firstBootDevice_SelectedItemChanged(sender, args);
            }
            else if (sender == getDisplayType()) {
                initGraphicsConsoles();
                initUsbPolicy();
            }
            else if (sender == getGraphicsType()) {
                upgradeGraphicsRelatedModels();
                initUsbPolicy();
            }
            else if (sender == getNumOfSockets()) {
                numOfSockets_EntityChanged(sender, args);
            }
            else if (sender == getCoresPerSocket()) {
                coresPerSocket_EntityChanged(sender, args);
            }
            else if (sender == getMigrationMode()) {
                behavior.updateUseHostCpuAvailability();
                behavior.updateCpuPinningVisibility();
                behavior.updateHaAvailability();
                behavior.updateNumaEnabled();
            }
            else if (sender == getCpuSharesAmountSelection()) {
                behavior.updateCpuSharesAmountChangeability();
            }
            else if (sender == getBaseTemplate()) {
                behavior.baseTemplateSelectedItemChanged();
            }
            else if (sender == getWatchdogModel()) {
                watchdogModelSelectedItemChanged(sender, args);
            }
        }
        else if (ev.matchesDefinition(HasEntity.entityChangedEventDefinition)) {
            if (sender == getVmInitEnabled()) {
                vmInitEnabledChanged();
            } else if (sender == getIoThreadsEnabled()) {
                behavior.updateNumOfIoThreads();
            } else if (sender == getMemSize()) {
                memSize_EntityChanged(sender, args);
            }
            else if (sender == getTotalCPUCores()) {
                totalCPUCores_EntityChanged(sender, args);
            }
            else if (sender == getIsAutoAssign()) {
                behavior.updateUseHostCpuAvailability();
                behavior.updateCpuPinningVisibility();
                behavior.updateHaAvailability();
                behavior.updateNumaEnabled();
            }
            else if (sender == getProvisioning()) {
                provisioning_SelectedItemChanged(sender, args);
            }
            else if (sender == getProvisioningThin_IsSelected()) {
                if (getProvisioningThin_IsSelected().getEntity()) {
                    getProvisioning().setEntity(false);
                }
            }
            else if (sender == getProvisioningClone_IsSelected()) {
                if (getProvisioningClone_IsSelected().getEntity()) {
                    getProvisioning().setEntity(true);
                }
            } else if (sender == getIsHighlyAvailable()) {
                behavior.updateMigrationAvailability();
            } else if (sender == getOverrideMigrationDowntime()) {
                overrideMigrationDowntimeChanged();
            }
            else if (sender == getIsSubTemplate()) {
                behavior.isSubTemplateEntityChanged();
            }
            else if (sender == getHostCpu()) {
                if(getHostCpu().getEntity() != null && getHostCpu().getEntity()) {
                    getCustomCpu().setIsChangeable(false);
                    getCustomCpu().setSelectedItem(""); //$NON-NLS-1$
                } else {
                    getCustomCpu().setIsChangeable(true);
                }
            }

        }
    }

    private void vmInitEnabledChanged() {
        if(!getVmInitEnabled().getEntity()) {
            getSysprepEnabled().setEntity(false);
            getCloudInitEnabled().setEntity(false);
        } else {
            getSysprepEnabled().setEntity(getIsWindowsOS());
            // for the "other" also use cloud init
            getCloudInitEnabled().setEntity(!getIsWindowsOS());
        }
    }

    private void vmTypeChanged() {
        behavior.vmTypeChanged(getVmType().getSelectedItem());
    }

    private void watchdogModelSelectedItemChanged(Object sender, EventArgs args) {
        if (getWatchdogModel().getSelectedItem() == null) {
            getWatchdogAction().setIsChangeable(false);
        } else {
            getWatchdogAction().setIsChangeable(true);
        }
    }

    protected void initNumOfMonitors() {
        AsyncDataProvider.getInstance().getNumOfMonitorList(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {

                        UnitVmModel model = (UnitVmModel) target;
                        Integer oldNumOfMonitors = null;
                        if (model.getNumOfMonitors().getSelectedItem() != null) {
                            oldNumOfMonitors = model.getNumOfMonitors().getSelectedItem();
                        }
                        ArrayList<Integer> numOfMonitors = (ArrayList<Integer>) returnValue;
                        model.getNumOfMonitors().setItems(numOfMonitors);
                        if (oldNumOfMonitors != null) {
                            model.getNumOfMonitors().setSelectedItem(oldNumOfMonitors);
                        }

                    }
                }));

    }

    protected void initAllowConsoleReconnect() {
        getAllowConsoleReconnect().setEntity(getVmType().getSelectedItem() == VmType.Server);
    }

    private void initConsoleDisconnectAction() {
        getConsoleDisconnectAction().setItems(Arrays.asList(ConsoleDisconnectAction.values()));
    }

    private void initUsbPolicy() {
        VDSGroup cluster = getSelectedCluster();
        Integer osType = getOSType().getSelectedItem();
        DisplayType displayType = getDisplayType().getSelectedItem();
        GraphicsTypes graphicsTypes = getGraphicsType().getSelectedItem();

        if (osType == null || cluster == null || displayType == null || graphicsTypes == null) {
            return;
        }

        getUsbPolicy().setIsChangeable(true);

        UsbPolicy prevSelectedUsbPolicy = getUsbPolicy().getSelectedItem();

        if (Version.v3_1.compareTo(cluster.getCompatibilityVersion()) > 0) {
            if (AsyncDataProvider.getInstance().isWindowsOsType(osType)) {
                getUsbPolicy().setItems(Arrays.asList(
                        UsbPolicy.DISABLED,
                        UsbPolicy.ENABLED_LEGACY
                ));
            } else {
                getUsbPolicy().setItems(Arrays.asList(UsbPolicy.DISABLED));
                getUsbPolicy().setIsChangeable(false);
            }
        }

        if (Version.v3_1.compareTo(cluster.getCompatibilityVersion()) <= 0) {
            if (AsyncDataProvider.getInstance().isLinuxOsType(osType)) {
                getUsbPolicy().setItems(Arrays.asList(
                        UsbPolicy.DISABLED,
                        UsbPolicy.ENABLED_NATIVE
                ));
            } else {
                getUsbPolicy().setItems(
                        Arrays.asList(
                                UsbPolicy.DISABLED,
                                UsbPolicy.ENABLED_LEGACY,
                                UsbPolicy.ENABLED_NATIVE
                        ));
            }
        }

        if (!graphicsTypes.getBackingGraphicsTypes().contains(GraphicsType.SPICE)) {
            getUsbPolicy().setIsChangeable(false);
        }

        if (getBehavior().basedOnCustomInstanceType()) {
            Collection<UsbPolicy> policies = getUsbPolicy().getItems();
            if (policies.contains(prevSelectedUsbPolicy)) {
                getUsbPolicy().setSelectedItem(prevSelectedUsbPolicy);
            } else if (policies.size() > 0) {
                getUsbPolicy().setSelectedItem(policies.iterator().next());
            }
        }
    }

    private void updateMigrationOptions() {
        DataCenterWithCluster dataCenterWithCluster =
                getDataCenterWithClustersList().getSelectedItem();
        if (dataCenterWithCluster == null) {
            return;
        }

        VDSGroup cluster = dataCenterWithCluster.getCluster();
        Version version = cluster.getCompatibilityVersion();

        Boolean isMigrationSupported =
                AsyncDataProvider.getInstance().isMigrationSupported(cluster.getArchitecture(), version);

        if (isMigrationSupported) {
            getMigrationMode().setItems(Arrays.asList(MigrationSupport.values()));
        } else {
            getMigrationMode().setItems(Arrays.asList(MigrationSupport.PINNED_TO_HOST));
        }

        autoConverge.updateChangeability(ConfigurationValues.AutoConvergenceSupported, version);
        migrateCompressed.updateChangeability(ConfigurationValues.MigrationCompressionSupported, version);
    }

    private void initGraphicsAndDisplayListeners() {
        getDisplayType().getSelectedItemChangedEvent().addListener(this);
        getGraphicsType().getSelectedItemChangedEvent().addListener(this);
    }

    private void updateDisplayAndGraphics() {
        VDSGroup cluster = getSelectedCluster();
        Integer osType = getOSType().getSelectedItem();

        if (cluster == null || osType == null) {
            return;
        }

        List<Pair<GraphicsType, DisplayType>> graphicsAndDisplays = AsyncDataProvider.getInstance().getGraphicsAndDisplays(osType, cluster.getCompatibilityVersion());
        initDisplayModels(graphicsAndDisplays);
    }

    public void initDisplayModels(List<Pair<GraphicsType, DisplayType>> graphicsAndDisplays) {
        // get supported display types
        Set<DisplayType> displayTypes = new LinkedHashSet<>();
        for (Pair<GraphicsType, DisplayType> graphicsTypeDisplayTypePair : graphicsAndDisplays) {
            displayTypes.add(graphicsTypeDisplayTypePair.getSecond());
        }

        // set items and set selected one
        DisplayType selectedDisplayType = getDisplayType().getSelectedItem();
        if (displayTypes.contains(selectedDisplayType)) {
            getDisplayType().setItems(displayTypes, selectedDisplayType);
        } else if (displayTypes.size() > 0) {
            getDisplayType().setItems(displayTypes, displayTypes.iterator().next());
        }
    }

    private void initFirstBootDevice() {
        EntityModel tempVar = new EntityModel();
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().hardDiskTitle());
        tempVar.setEntity(BootSequence.C);
        EntityModel hardDiskOption = tempVar;

        List<EntityModel<BootSequence>> firstBootDeviceItems = new ArrayList<EntityModel<BootSequence>>();
        firstBootDeviceItems.add(hardDiskOption);
        EntityModel tempVar2 = new EntityModel();
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cdromTitle());
        tempVar2.setEntity(BootSequence.D);
        firstBootDeviceItems.add(tempVar2);
        EntityModel tempVar3 = new EntityModel();
        tempVar3.setTitle(ConstantsManager.getInstance().getConstants().networkPXETitle());
        tempVar3.setEntity(BootSequence.N);
        firstBootDeviceItems.add(tempVar3);
        getFirstBootDevice().setItems(firstBootDeviceItems);
        getFirstBootDevice().setSelectedItem(hardDiskOption);
    }

    private void initMigrationMode() {
        getMigrationMode().setItems(Arrays.asList(MigrationSupport.values()));
    }

    private void initVncKeyboardLayout() {
        final List<String> layouts =
                (List<String>) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.VncKeyboardLayoutValidValues);
        final ArrayList<String> vncKeyboardLayoutItems = new ArrayList<String>();
        vncKeyboardLayoutItems.add(null); // null value means the global VncKeyboardLayout from vdc_options will be used
        vncKeyboardLayoutItems.addAll(layouts);
        getVncKeyboardLayout().setItems(vncKeyboardLayoutItems);

        GraphicsTypes graphicsTypes = getGraphicsType().getSelectedItem();
        if (graphicsTypes != null) {
            getVncKeyboardLayout().setIsAvailable(graphicsTypes.getBackingGraphicsTypes().contains(GraphicsType.VNC));
        }
    }

    private void dataCenterWithClusterSelectedItemChanged(Object sender, EventArgs args) {
        behavior.dataCenterWithClusterSelectedItemChanged();

        DataCenterWithCluster dataCenterWithCluster = getDataCenterWithClustersList().getSelectedItem();
        if (dataCenterWithCluster != null && dataCenterWithCluster.getDataCenter() != null) {
            getDisksAllocationModel().setQuotaEnforcementType(dataCenterWithCluster.getDataCenter()
                                                                      .getQuotaEnforcementType());
        }

        updateMigrationOptions();
        handleQxlClusterLevel();

        updateWatchdogModels();
        updateBootMenu();
        getInstanceImages().updateActionsAvailability();

        initGraphicsConsoles();
    }

    private void updateBootMenu() {
        if (getSelectedCluster() != null) {
            Version version = getSelectedCluster().getCompatibilityVersion();
            final boolean supported = AsyncDataProvider.getInstance().isBootMenuSupported(version.toString());
            if (!supported) {
                getBootMenuEnabled().setEntity(false);
                getBootMenuEnabled().setChangeProhibitionReason(ConstantsManager.getInstance().getMessages().bootMenuNotSupported(version.toString(2)));
            }
            getBootMenuEnabled().setIsChangeable(supported);
        }
    }

    public boolean getIsQxlSupported() {
        // Enable Single PCI only on cluster 3.3 and high and on Linux OS
        boolean isLinux = getIsLinuxOS();
        boolean isQxl = getDisplayType().getSelectedItem() == DisplayType.qxl;
        boolean isSpice = getGraphicsType().getSelectedItem() == GraphicsTypes.SPICE;
        boolean clusterSupportsSinglePci = getSelectedCluster() != null &&
        Version.v3_3.compareTo(getSelectedCluster().getCompatibilityVersion()) <= 0;

        return isLinux && isQxl && isSpice && clusterSupportsSinglePci;
    }

    private void handleQxlClusterLevel() {
        getBehavior().enableSinglePCI(getIsQxlSupported());

        if (getSelectedCluster() != null) {
            boolean isQxl = getDisplayType().getSelectedItem() == DisplayType.qxl;
            boolean spiceFileTransferToggle = isQxl
                    && AsyncDataProvider.getInstance().isSpiceFileTransferToggleSupported(getSelectedCluster().getCompatibilityVersion().toString());
            if (!spiceFileTransferToggle) {
                handleQxlChangeProhibitionReason(getSpiceFileTransferEnabled(), getSelectedCluster().getCompatibilityVersion().toString(), isQxl);
            }
            getSpiceFileTransferEnabled().setIsChangeable(spiceFileTransferToggle);

            GraphicsTypes selectedGraphics = getGraphicsType().getSelectedItem();
            boolean spiceCopyPasteToggle = selectedGraphics != null
                    && selectedGraphics.getBackingGraphicsTypes().contains(GraphicsType.SPICE)
                    && AsyncDataProvider.getInstance().isSpiceCopyPasteToggleSupported(getSelectedCluster().getCompatibilityVersion().toString());
            if (!spiceCopyPasteToggle) {
                handleQxlChangeProhibitionReason(getSpiceCopyPasteEnabled(), getSelectedCluster().getCompatibilityVersion().toString(), isQxl);
            }
            getSpiceCopyPasteEnabled().setIsChangeable(spiceCopyPasteToggle);
        }

    }

    private void handleQxlChangeProhibitionReason(EntityModel<Boolean> checkbox, String version, boolean isQxl) {
        if (isQxl) {
            checkbox.setChangeProhibitionReason(ConstantsManager.getInstance().getMessages().optionNotSupportedClusterVersionTooOld(version));
        } else {
            checkbox.setChangeProhibitionReason(ConstantsManager.getInstance().getMessages().optionRequiresSpiceEnabled());
        }
    }

    private void templateWithVersion_SelectedItemChanged(Object sender, EventArgs args) {
        behavior.templateWithVersion_SelectedItemChanged();
        behavior.updateMigrationForLocalSD();
    }

    private void timeZone_SelectedItemChanged(Object sender, EventArgs args) {
    }

    private void defaultHost_SelectedItemChanged(Object sender, EventArgs args) {
        behavior.defaultHost_SelectedItemChanged();
    }

    private void oSType_SelectedItemChanged(Object sender, EventArgs args) {
        Integer osType = getOSType().getSelectedItem();

        setIsWindowsOS(AsyncDataProvider.getInstance().isWindowsOsType(osType));
        setIsLinuxOS(AsyncDataProvider.getInstance().isLinuxOsType(osType));

        getInitrd_path().setIsChangeable(getIsLinuxOS());
        getInitrd_path().setIsAvailable(getIsLinuxOS());

        getKernel_path().setIsChangeable(getIsLinuxOS());
        getKernel_path().setIsAvailable(getIsLinuxOS());

        getKernel_parameters().setIsChangeable(getIsLinuxOS());
        getKernel_parameters().setIsAvailable(getIsLinuxOS());

        getBehavior().updateDefaultTimeZone();

        handleQxlClusterLevel();

        updateWatchdogModels(osType);

        vmInitEnabledChanged();
        getInstanceImages().updateActionsAvailability();

        updateIconAccordingToOs();

        initGraphicsConsoles();
    }

    private void updateIconAccordingToOs() {
        final Integer osId = getOSType().getSelectedItem();
        final String oldIcon = getIcon().getEntity() == null ? null : getIcon().getEntity().getIcon();
        final Guid osDefaultIconId = AsyncDataProvider.getInstance().getOsDefaultIconId(osId, false);
        IconCache.getInstance().getOrFetchIcon(osDefaultIconId, new IconCache.IconCallback() {
            @Override
            public void onSuccess(String currentOsDefaultIcon) {
                String iconToShow = getIconToShow(currentOsDefaultIcon);
                Guid smallIconId = getSmallIconId(iconToShow, oldIcon, currentOsDefaultIcon, osDefaultIconId);
                IconWithOsDefault newIconPair = new IconWithOsDefault(iconToShow, currentOsDefaultIcon, smallIconId);
                getIcon().setEntity(newIconPair);
            }
        });
    }

    private Guid getSmallIconId(String newIcon, String oldIcon, String newOsDefaultIcon, Guid newOsDefaultIconId) {
        if (newIcon.equals(oldIcon)) {
            return getIcon().getEntity().getSmallIconId();
        }
        if (newIcon.equals(newOsDefaultIcon)) {
            return AsyncDataProvider.getInstance().getSmallByLargeOsDefaultIconId(newOsDefaultIconId);
        }
        return null;
    }

    protected String getIconToShow(String currentOsDefaultIcon) {
        if (getIcon().getEntity() == null) {
            return  currentOsDefaultIcon;
        }
        return getIcon().getEntity().isCustom()
                ? getIcon().getEntity().getIcon()
                : currentOsDefaultIcon;
    }

    private void updateWatchdogModels() {
        updateWatchdogModels(getOSType().getSelectedItem());
    }

    private void updateWatchdogModels(Integer osType) {
        VDSGroup cluster = getSelectedCluster();
        if (osType != null && cluster != null && getWatchdogModel() != null) {
            AsyncQuery asyncQuery = new AsyncQuery();
            asyncQuery.asyncCallback = new INewAsyncCallback() {
                @Override
                public void onSuccess(Object model, Object returnValue) {
                    getBehavior().deactivateInstanceTypeManager();

                    updateWatchdogItems((HashSet<VmWatchdogType>) ((VdcQueryReturnValue) returnValue)
                            .getReturnValue());

                    getBehavior().activateInstanceTypeManager();

                }
            };
            AsyncDataProvider.getInstance().getVmWatchdogTypes(osType,
                    cluster.getCompatibilityVersion(), asyncQuery);
        }
    }

    public void updateWatchdogItems(Set<VmWatchdogType> vmWatchdogTypes) {
        List<VmWatchdogType> watchDogModels = new ArrayList<VmWatchdogType>();
        for (VmWatchdogType vmWatchdogType : vmWatchdogTypes) {
            watchDogModels.add(vmWatchdogType);
        }

        watchDogModels.add(0, null);
        VmWatchdogType oldWatchdogSelected = getWatchdogModel().getSelectedItem();
        if (watchDogModels.contains(getWatchdogModel().getSelectedItem())) {
            getWatchdogModel().setItems(watchDogModels, getWatchdogModel().getSelectedItem());
        } else {
            getWatchdogModel().setItems(watchDogModels);
        }

        if (watchDogModels.contains(oldWatchdogSelected)) {
            getWatchdogModel().setSelectedItem(oldWatchdogSelected);
        }
    }

    private void firstBootDevice_SelectedItemChanged(Object sender, EventArgs args) {
        EntityModel<BootSequence> entityModel = getFirstBootDevice().getSelectedItem();
        BootSequence firstDevice = entityModel.getEntity();
        EntityModel<BootSequence> prevItem = null;

        List<EntityModel<BootSequence>> list = new ArrayList<EntityModel<BootSequence>>();
        for (EntityModel<BootSequence> item : getFirstBootDevice().getItems()) {
            if (item.getEntity() != firstDevice) {
                list.add(item);
                if (getSecondBootDevice().getSelectedItem() != null && item.getEntity() == getSecondBootDevice().getSelectedItem().getEntity()) {
                    prevItem = item;
                }
            }
        }

        EntityModel<BootSequence> tempVar = new EntityModel<BootSequence>();
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().noneTitle());
        EntityModel<BootSequence> noneOption = tempVar;

        list.add(0, noneOption);

        getSecondBootDevice().setItems(list);
        if (prevItem != null) {
            getSecondBootDevice().setSelectedItem(prevItem);
        } else {
            getSecondBootDevice().setSelectedItem(noneOption);
        }
    }

    private void provisioning_SelectedItemChanged(Object sender, EventArgs args) {
        behavior.provisioning_SelectedItemChanged();
    }

    private void overrideMigrationDowntimeChanged() {
        Boolean entity = getOverrideMigrationDowntime().getEntity();
        getMigrationDowntime().setIsChangeable(Boolean.TRUE.equals(entity));
    }

    protected void initGraphicsConsoles() {
        VDSGroup cluster = getSelectedCluster();
        Integer osType = getOSType().getSelectedItem();

        if (cluster == null || osType == null) {
            return;
        }

        initGraphicsConsoles(osType, cluster.getCompatibilityVersion());
    }

    protected void initGraphicsConsoles(int osType, Version compatibilityVersion) {
        Set<GraphicsTypes> graphicsTypes = new LinkedHashSet<>();
        List<Pair<GraphicsType, DisplayType>> graphicsAndDisplays = AsyncDataProvider.getInstance().getGraphicsAndDisplays(osType, compatibilityVersion);

        for (Pair<GraphicsType, DisplayType> graphicsAndDisplay : graphicsAndDisplays) {
            if (graphicsAndDisplay.getSecond() == getDisplayType().getSelectedItem()) {
                graphicsTypes.add(GraphicsTypes.fromGraphicsType(graphicsAndDisplay.getFirst()));
            }
        }

        if (graphicsTypes.contains(GraphicsTypes.SPICE) && graphicsTypes.contains(GraphicsTypes.VNC)) {
            graphicsTypes.add(GraphicsTypes.SPICE_AND_VNC);
        }

        GraphicsTypes prevSelected = getGraphicsType().getSelectedItem();

        if (prevSelected != null && graphicsTypes.contains(prevSelected)) {
            getGraphicsType().setItems(graphicsTypes, prevSelected);
        } else {
            getGraphicsType().setItems(graphicsTypes);
        }

        upgradeGraphicsRelatedModels();
    }

    private void upgradeGraphicsRelatedModels() {
        DisplayType display = getDisplayType().getSelectedItem();
        GraphicsTypes graphics = getGraphicsType().getSelectedItem();
        if (display == null || graphics == null) {
            return;
        }

        if (display != DisplayType.qxl || !graphics.getBackingGraphicsTypes().contains(GraphicsType.SPICE)) {
            getUsbPolicy().setSelectedItem(UsbPolicy.DISABLED);
            getIsSmartcardEnabled().setEntity(false);
        }

        handleQxlClusterLevel();
        getUsbPolicy().setIsChangeable(graphics.getBackingGraphicsTypes().contains(GraphicsType.SPICE));
        getIsSmartcardEnabled().setIsChangeable(graphics.getBackingGraphicsTypes().contains(GraphicsType.SPICE));
        getVncKeyboardLayout().setIsAvailable(graphics.getBackingGraphicsTypes().contains(GraphicsType.VNC));
        updateNumOfMonitors();
    }

    private void memSize_EntityChanged(Object sender, EventArgs args) {
        behavior.updateMinAllocatedMemory();
    }

    private void numOfSockets_EntityChanged(Object sender, EventArgs args) {
        behavior.numOfSocketChanged();
    }

    private void totalCPUCores_EntityChanged(Object sender, EventArgs args) {
        // do not listen on changes while the totalCpuCoresChanged is adjusting them
        getNumOfSockets().getSelectedItemChangedEvent().removeListener(this);
        getTotalCPUCores().getEntityChangedEvent().removeListener(this);
        getCoresPerSocket().getSelectedItemChangedEvent().removeListener(this);

        behavior.totalCpuCoresChanged();

        // start listening again
        getTotalCPUCores().getEntityChangedEvent().addListener(this);
        getNumOfSockets().getSelectedItemChangedEvent().addListener(this);
        getCoresPerSocket().getSelectedItemChangedEvent().addListener(this);
    }

    private void coresPerSocket_EntityChanged(Object sender, EventArgs args) {
        behavior.coresPerSocketChanged();
    }

    private void updateNumOfMonitors() {
        if (getDisplayType().getSelectedItem() == DisplayType.qxl) {
            getNumOfMonitors().setIsChangeable(true);
        } else {
            getNumOfMonitors().setSelectedItem(1);
            getNumOfMonitors().setIsChangeable(false);
        }
    }

    public BootSequence getBootSequence() {
        EntityModel<BootSequence> firstSelectedItem = getFirstBootDevice().getSelectedItem();
        EntityModel<BootSequence> secondSelectedItem = getSecondBootDevice().getSelectedItem();

        String firstSelectedString =
                firstSelectedItem.getEntity() == null ? "" : firstSelectedItem.getEntity().toString(); //$NON-NLS-1$
        String secondSelectedString =
                secondSelectedItem.getEntity() == null ? "" : secondSelectedItem.getEntity().toString(); //$NON-NLS-1$

        return BootSequence.valueOf(firstSelectedString + secondSelectedString);
    }

    public void setBootSequence(BootSequence value) {
        ArrayList<BootSequence> items = new ArrayList<BootSequence>();
        for (char a : value.toString().toCharArray()) {
            items.add(BootSequence.valueOf(String.valueOf(a)));
        }

        EntityModel<BootSequence> firstBootDevice = null;
        for (EntityModel<BootSequence> item : getFirstBootDevice().getItems()) {
            if (item.getEntity() == Linq.firstOrDefault(items)) {
                firstBootDevice = item;
            }
        }
        getFirstBootDevice().setSelectedItem(firstBootDevice);

        Iterable<EntityModel<BootSequence>> secondDeviceOptions = getSecondBootDevice().getItems();

        if (items.size() > 1) {
            BootSequence last = items.get(items.size() - 1);
            for (EntityModel<BootSequence> a : secondDeviceOptions) {
                if (a.getEntity() != null && a.getEntity() == last) {
                    getSecondBootDevice().setSelectedItem(a);
                    break;
                }
            }
        }
        else {
            for (EntityModel<BootSequence> a : secondDeviceOptions) {
                if (a.getEntity() == null) {
                    getSecondBootDevice().setSelectedItem(a);
                    break;
                }
            }
        }
    }

    public void setDataCentersAndClusters(UnitVmModel model,
            List<StoragePool> dataCenters,
            List<VDSGroup> clusters,
            Guid selectedCluster) {

        if (model.getBehavior().getSystemTreeSelectedItem() != null
                && model.getBehavior().getSystemTreeSelectedItem().getType() != SystemTreeItemType.System) {
            setupDataCenterWithClustersFromSystemTree(model, dataCenters, clusters, selectedCluster);
        } else {
            setupDataCenterWithClusters(model, dataCenters, clusters, selectedCluster);
        }

    }

    protected void setupDataCenterWithClustersFromSystemTree(UnitVmModel model,
            List<StoragePool> dataCenters,
            List<VDSGroup> clusters,
            Guid selectedCluster) {

        StoragePool dataCenter = getDataCenterAccordingSystemTree(model, dataCenters);

        // the dataCenters are the entities just downloaded from server while the dataCenter can be a cached one from the system tree
        dataCenter = dataCenter == null ? null : findDataCenterById(dataCenters, dataCenter.getId());

        List<VDSGroup> possibleClusters = getClusterAccordingSystemTree(model, clusters);
        if (dataCenter == null || possibleClusters == null) {
            getDataCenterWithClustersList().setIsChangeable(false);
            return;
        }

        List<DataCenterWithCluster> dataCentersWithClusters =
                new ArrayList<DataCenterWithCluster>();

        for (VDSGroup cluster : possibleClusters) {
            if (cluster.getStoragePoolId() != null && cluster.getStoragePoolId().equals(dataCenter.getId())) {
                dataCentersWithClusters.add(new DataCenterWithCluster(dataCenter, cluster));
            }
        }
        selectDataCenterWithCluster(selectedCluster, dataCentersWithClusters);
    }

    protected void setupDataCenterWithClusters(UnitVmModel model,
            List<StoragePool> dataCenters,
            List<VDSGroup> clusters,
            Guid selectedCluster) {

        Map<Guid, List<VDSGroup>> dataCenterToCluster = new HashMap<Guid, List<VDSGroup>>();
        for (VDSGroup cluster : clusters) {
            if (cluster.getStoragePoolId() == null) {
                continue;
            }

            if (!dataCenterToCluster.containsKey(cluster.getStoragePoolId())) {
                dataCenterToCluster.put(cluster.getStoragePoolId(), new ArrayList<VDSGroup>());
            }
            dataCenterToCluster.get(cluster.getStoragePoolId()).add(cluster);
        }

        List<DataCenterWithCluster> dataCentersWithClusters =
                new ArrayList<DataCenterWithCluster>();

        for (StoragePool dataCenter : dataCenters) {
            if (dataCenterToCluster.containsKey(dataCenter.getId())) {
                for (VDSGroup cluster : dataCenterToCluster.get(dataCenter.getId())) {
                    dataCentersWithClusters.add(new DataCenterWithCluster(dataCenter, cluster));
                }
            }
        }
        selectDataCenterWithCluster(selectedCluster, dataCentersWithClusters);
    }

    protected void selectDataCenterWithCluster(Guid selectedCluster, List<DataCenterWithCluster> dataCentersWithClusters) {
        DataCenterWithCluster selectedDataCenterWithCluster =
                (selectedCluster == null) ? Linq.firstOrDefault(dataCentersWithClusters)
                        : Linq.firstOrDefault(dataCentersWithClusters,
                                new Linq.DataCenterWithClusterAccordingClusterPredicate(selectedCluster));
        getDataCenterWithClustersList().setItems(dataCentersWithClusters, selectedDataCenterWithCluster);
    }

    private StoragePool getDataCenterAccordingSystemTree(UnitVmModel model, List<StoragePool> list) {
        if (model.getBehavior().getSystemTreeSelectedItem() != null
                && model.getBehavior().getSystemTreeSelectedItem().getType() != SystemTreeItemType.System) {
            switch (model.getBehavior().getSystemTreeSelectedItem().getType()) {
            case Templates:
            case DataCenter:
                return (StoragePool) model.getBehavior().getSystemTreeSelectedItem().getEntity();
            case Cluster:
            case Cluster_Gluster:
            case VMs:
                VDSGroup cluster = (VDSGroup) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                if (cluster.supportsVirtService()) {
                    return findDataCenterById(list, cluster.getStoragePoolId());
                }
                break;

            case Host:
                VDS host = (VDS) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                return findDataCenterById(list, host.getStoragePoolId());

            case Storage:
                StorageDomain storage = (StorageDomain) model.getBehavior().getSystemTreeSelectedItem().getEntity();
                return findDataCenterById(list, storage.getStoragePoolId());
            }
        }
        return null;
    }

    private StoragePool findDataCenterById(List<StoragePool> list, Guid id) {
        if (id == null) {
            return null;
        }

        for (StoragePool dc : list) {
            if (dc.getId().equals(id)) {
                return dc;
            }
        }

        return null;
    }

    private List<VDSGroup> getClusterAccordingSystemTree(UnitVmModel model, List<VDSGroup> clusters) {
        if (behavior.getSystemTreeSelectedItem() != null
                && behavior.getSystemTreeSelectedItem().getType() != SystemTreeItemType.System) {
            switch (model.getBehavior().getSystemTreeSelectedItem().getType()) {
            case Cluster:
            case VMs:
                VDSGroup cluster = (VDSGroup) behavior.getSystemTreeSelectedItem().getEntity();
                return Arrays.asList(cluster);

            case Host:
                VDS host = (VDS) behavior.getSystemTreeSelectedItem().getEntity();
                for (VDSGroup iterCluster : clusters) {
                    if (iterCluster.getId().equals(host.getVdsGroupId())) {
                        return Arrays.asList(iterCluster);
                    }
                }
                break;
            default:
                return clusters;
            }
        }

        return null;
    }

    public boolean validate() {
        return this.validate(true);
    }

    public boolean validate(boolean templateWithVersionRequired) {
        resetTabsValidity();

        getInstanceTypes().setIsValid(true);
        getInstanceTypes().validateSelectedItem(new IValidation[]{new NotEmptyValidation()});

        getDataCenterWithClustersList().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });

        getOSType().validateSelectedItem(new NotEmptyValidation[] { new NotEmptyValidation() });

        DataCenterWithCluster dataCenterWithCluster = getDataCenterWithClustersList().getSelectedItem();

        StoragePool dataCenter =
                dataCenterWithCluster == null ? null : dataCenterWithCluster.getDataCenter();
        if (dataCenter != null && dataCenter.getQuotaEnforcementType() == QuotaEnforcementTypeEnum.HARD_ENFORCEMENT) {
            getQuota().validateSelectedItem(new IValidation[] { new NotEmptyQuotaValidation() });
        }

        if (getOSType().getIsValid()) {
            validateNaming();

            getVmId().setIsValid(true);
            if (getVmId().getIsAvailable() && !StringHelper.isNullOrEmpty(getVmId().getEntity())) {
                getVmId().validateEntity(new IValidation[] { new GuidValidation() });
            }

            getComment().validateEntity(new IValidation[] { new SpecialAsciiI18NOrNoneValidation() });

            setValidTab(TabName.GENERAL_TAB, isValidTab(TabName.GENERAL_TAB)
                    && getVmId().getIsValid()
                    && getComment().getIsValid());
        }

        if (templateWithVersionRequired) {
            getTemplateWithVersion().validateSelectedItem(new IValidation[]{new NotEmptyValidation()});
        }
        getDisksAllocationModel().validateEntity(new IValidation[]{});

        getCdImage().setIsValid(true);
        if (getCdImage().getIsChangable()) {
            getCdImage().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        }

        if (getIsLinuxOS()) {
            getKernel_path().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });
            getInitrd_path().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });
            getKernel_parameters().validateEntity(new IValidation[] { new NoTrimmingWhitespacesValidation() });

            // initrd path and kernel params require kernel path to be filled
            if (StringHelper.isNullOrEmpty(getKernel_path().getEntity())) {

                if (!StringHelper.isNullOrEmpty(getInitrd_path().getEntity())) {
                    getInitrd_path().getInvalidityReasons().add(constants.initrdPathInvalid());
                    getInitrd_path().setIsValid(false);
                    getKernel_path().getInvalidityReasons().add(constants.initrdPathInvalid());
                    getKernel_path().setIsValid(false);
                }

                if (!StringHelper.isNullOrEmpty(getKernel_parameters().getEntity())) {
                    getKernel_parameters().getInvalidityReasons().add(constants.kernelParamsInvalid());
                    getKernel_parameters().setIsValid(false);
                    getKernel_path().getInvalidityReasons().add(constants.kernelParamsInvalid());
                    getKernel_path().setIsValid(false);
                }
            }
        }

        if (!(getBehavior().isBlankTemplateBehavior())) {
            setValidTab(TabName.GENERAL_TAB, isValidTab(TabName.GENERAL_TAB)
                    && getDataCenterWithClustersList().getIsValid()
                    && getTemplateWithVersion().getIsValid());
        }

        setValidTab(TabName.HOST_TAB, isValidTab(TabName.HOST_TAB) && getMigrationDowntime().getIsValid());

        boolean diskAliasesValid = getDisksAllocationModel().getIsValid();

        setValidTab(TabName.RESOURCE_ALLOCATION_TAB, isValidTab(TabName.RESOURCE_ALLOCATION_TAB)
                && getCpuSharesAmount().getIsValid() && diskAliasesValid);

        setValidTab(TabName.BOOT_OPTIONS_TAB, getCdImage().getIsValid() && getKernel_path().getIsValid());
        boolean vmInitIsValid = getVmInitModel().validate();
        setValidTab(TabName.INITIAL_RUN_TAB, vmInitIsValid);

        getIcon().validateEntity(new IValidation[]{new IconWithOsDefaultValidation()});
        setValidTab(TabName.ICON_TAB, getIcon().getIsValid());

        boolean hwPartValid = validateHwPart();

        boolean isValid = hwPartValid && vmInitIsValid && allTabsValid();
        getValid().setEntity(isValid);
        ValidationCompleteEvent.fire(getEventBus(), this);
        return isValid;
    }

    private boolean validateNaming() {
        getName().validateEntity(
                new IValidation[] {
                        new NotEmptyValidation(),
                        new LengthValidation(getBehavior().getMaxNameLength()),
                        getBehavior().getNameAllowedCharactersIValidation()
                });

        getDescription().validateEntity(
                new IValidation[] {
                        new LengthValidation(DESCRIPTION_MAX_LIMIT),
                        new SpecialAsciiI18NOrNoneValidation()
                });

        final boolean isValid = getName().getIsValid() && getDescription().getIsValid();
        setValidTab(TabName.GENERAL_TAB, isValidTab(TabName.GENERAL_TAB) && isValid);
        return isValid;
    }

    public boolean validateInstanceTypePart() {
        resetTabsValidity();

        final boolean isNamingValid = validateNaming();
        boolean isValid = validateHwPart() && isNamingValid;

        getValid().setEntity(isValid);
        ValidationCompleteEvent.fire(getEventBus(), this);
        return isValid;
    }

    public boolean validateHwPart() {
        getMigrationDowntime().validateEntity(new IValidation[] { new NotNullIntegerValidation(0, Integer.MAX_VALUE) });

        getTotalCPUCores().validateEntity(new IValidation[] {
                new NotEmptyValidation(),
                new IntegerValidation(1, behavior.maxCpus),
                new TotalCpuCoresComposableValidation() });

        if (getIsAutoAssign().getEntity() != null && getIsAutoAssign().getEntity() == false) {
            getDefaultHost().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });
        } else {
            getDefaultHost().setIsValid(true);
        }

        if (getCpuSharesAmount().getIsAvailable()) {
            getCpuSharesAmount().validateEntity(new IValidation[] {new NotEmptyValidation()
                    , new IntegerValidation(0, 262144)});
        }

        boolean customPropertySheetValid = getCustomPropertySheet().validate();
        setValidTab(TabName.CUSTOM_PROPERTIES_TAB, customPropertySheetValid);

        if (getSerialNumberPolicy().getSelectedSerialNumberPolicy() == SerialNumberPolicy.CUSTOM) {
            getSerialNumberPolicy().getCustomSerialNumber().validateEntity(new IValidation[] { new NotEmptyValidation() });
        } else {
            getSerialNumberPolicy().getCustomSerialNumber().setIsValid(true);
        }

        getEmulatedMachine().validateSelectedItem(new IValidation[] { new I18NExtraNameOrNoneValidation(), new LengthValidation(
                BusinessEntitiesDefinitions.VM_EMULATED_MACHINE_SIZE)});
        getCustomCpu().validateSelectedItem(new IValidation[] { new I18NExtraNameOrNoneValidation() , new LengthValidation(BusinessEntitiesDefinitions.VM_CPU_NAME_SIZE)});

        setValidTab(TabName.CONSOLE_TAB, getUsbPolicy().getIsValid() && getNumOfMonitors().getIsValid()
                && getSpiceProxy().getIsValid());
        setValidTab(TabName.HOST_TAB, getMigrationDowntime().getIsValid());

        getRngBytes().validateEntity(new IValidation[]{new IntegerValidation(0, Integer.MAX_VALUE), new RngDevValidation()});
        getRngPeriod().validateEntity(new IValidation[]{new IntegerValidation(0, Integer.MAX_VALUE)});

        setValidTab(TabName.TAB_RNG, getRngBytes().getIsValid() && getRngPeriod().getIsValid());

        // Minimum 'Physical Memory Guaranteed' is 1MB
        validateMemorySize(getMemSize(), Integer.MAX_VALUE, 1);
        if (getMemSize().getIsValid()) {
            validateMemorySize(getMinAllocatedMemory(), getMemSize().getEntity(), 1);
        }

        if (getIoThreadsEnabled().getEntity()) {
            getNumOfIoThreads().validateEntity(new IValidation[] {new NotNullIntegerValidation(1, AsyncDataProvider.getInstance().getMaxIoThreadsPerVm())});
        }

        setValidTab(TabName.RESOURCE_ALLOCATION_TAB, getMinAllocatedMemory().getIsValid() && getNumOfIoThreads().getIsValid());

        setValidTab(TabName.SYSTEM_TAB, getMemSize().getIsValid() &&
                getTotalCPUCores().getIsValid() &&
                getSerialNumberPolicy().getCustomSerialNumber().getIsValid() &&
                getEmulatedMachine().getIsValid() &&
                getCustomCpu().getIsValid());

        /*
         * This should be run at very end of the validation process otherwise general validation can override more
         * strict checks in behaviors
         */
        boolean behaviorValid = behavior.validate();

        boolean isValid = behaviorValid && allTabsValid();
        return isValid;
    }

    private void resetTabsValidity() {
        setValidTab(TabName.HOST_TAB, true);
        setIsCustomPropertiesTabAvailable(true);

        setValidTab(TabName.TAB_RNG, true);
        setValidTab(TabName.CUSTOM_PROPERTIES_TAB, true);
        setValidTab(TabName.BOOT_OPTIONS_TAB, true);
        setValidTab(TabName.RESOURCE_ALLOCATION_TAB, true);
        setValidTab(TabName.CONSOLE_TAB, true);
        setValidTab(TabName.INITIAL_RUN_TAB, true);
        setValidTab(TabName.GENERAL_TAB, true);
        setValidTab(TabName.ICON_TAB, true);
        getValid().setEntity(true);
    }

    private class RngDevValidation implements IValidation {

        @Override
        public ValidationResult validate(Object value) {
            ValidationResult res = new ValidationResult();
            res.setSuccess(!(rngBytes.getEntity() == null && rngPeriod.getEntity() != null));
            res.setReasons(Arrays.asList(ConstantsManager.getInstance().getConstants().rngRateInvalid()));
            return res;
        }
    }

    public SsoMethod extractSelectedSsoMethod() {
        return Boolean.TRUE.equals(getSsoMethodGuestAgent().getEntity())
                ? SsoMethod.GUEST_AGENT
                : SsoMethod.NONE;
    }

    public void selectSsoMethod(SsoMethod ssoMethod) {
        getSsoMethodNone().setEntity(SsoMethod.NONE.equals(ssoMethod));
        getSsoMethodGuestAgent().setEntity(SsoMethod.GUEST_AGENT.equals(ssoMethod));
    }

    class TotalCpuCoresComposableValidation implements IValidation {

        @Override
        public ValidationResult validate(Object value) {
            boolean isOk = behavior.isNumOfSocketsCorrect(Integer.parseInt(getTotalCPUCores().getEntity()));
            ValidationResult res = new ValidationResult();
            res.setSuccess(isOk);
            res.setReasons(Arrays.asList(ConstantsManager.getInstance()
                    .getMessages()
                    .incorrectVCPUNumber()));
            return res;

        }

    }

    private void validateMemorySize(EntityModel<Integer> model, int maxMemSize, int minMemSize) {
        boolean isValid = false;

        int memSize = model.getEntity();

        if (memSize == 0) {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .memSizeBetween(minMemSize, maxMemSize));
        }
        else if (memSize > maxMemSize) {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .maxMemSizeIs(maxMemSize));
        }
        else if (memSize < minMemSize) {
            model.getInvalidityReasons().add(ConstantsManager.getInstance()
                    .getMessages()
                    .minMemSizeIs(minMemSize));
        }
        else {
            isValid = true;
        }

        model.setIsValid(isValid);
    }

    private NotChangableForVmInPoolListModel<EntityModel<VmPoolType>> poolType;

    public ListModel<EntityModel<VmPoolType>> getPoolType() {
        return poolType;
    }

    protected void setPoolType(NotChangableForVmInPoolListModel<EntityModel<VmPoolType>> value) {
        poolType = value;
    }

    private NotChangableForVmInPoolEntityModel<Integer> numOfDesktops;

    public EntityModel<Integer> getNumOfDesktops() {
        return numOfDesktops;
    }

    protected void setNumOfDesktops(NotChangableForVmInPoolEntityModel<Integer> value) {
        numOfDesktops = value;
    }

    private NotChangableForVmInPoolEntityModel<Integer> assignedVms;

    public EntityModel<Integer> getAssignedVms() {
        return assignedVms;
    }

    public void setAssignedVms(NotChangableForVmInPoolEntityModel<Integer> value) {
        assignedVms = value;
    }

    private NotChangableForVmInPoolEntityModel<Integer> prestartedVms;

    public EntityModel<Integer> getPrestartedVms() {
        return prestartedVms;
    }

    protected void setPrestartedVms(NotChangableForVmInPoolEntityModel<Integer> value) {
        prestartedVms = value;
    }

    private String prestartedVmsHint;

    public String getPrestartedVmsHint() {
        return prestartedVmsHint;
    }

    public void setPrestartedVmsHint(String value) {
        if (!ObjectUtils.objectsEqual(prestartedVmsHint, value)) {
            prestartedVmsHint = value;
            onPropertyChanged(new PropertyChangedEventArgs("PrestartedVmsHint")); //$NON-NLS-1$
        }
    }

    private NotChangableForVmInPoolEntityModel<Integer> maxAssignedVmsPerUser;

    public EntityModel<Integer> getMaxAssignedVmsPerUser() {
        return maxAssignedVmsPerUser;
    }

    public void setMaxAssignedVmsPerUser(NotChangableForVmInPoolEntityModel<Integer> maxAssignedVmsPerUser) {
        this.maxAssignedVmsPerUser = maxAssignedVmsPerUser;
    }

    private class NotChangableForVmInPoolListModel<T> extends ListModel<T> {
        @Override
        public ListModel<T> setIsChangeable(boolean value) {
            if (!isVmAttachedToPool()) {
                super.setIsChangeable(value);
            }
            return this;
        }
    }

    private class NotChangableForVmInPoolEntityModel<T> extends EntityModel<T> {
        public NotChangableForVmInPoolEntityModel() {
        }

        public NotChangableForVmInPoolEntityModel(T entity) {
            super(entity);
        }

        @Override
        public EntityModel<T> setIsChangeable(boolean value) {
            if (!isVmAttachedToPool()) {
                super.setIsChangeable(value);
            }
            return this;
        }
    }

    private class NotChangableForVmInPoolKeyValueModel extends KeyValueModel {
        @Override
        public KeyValueModel setIsChangeable(boolean value) {
            if (!isVmAttachedToPool()) {
                super.setIsChangeable(value);
            }
            return this;
        }
    }

    private ListModel<VmWatchdogType> watchdogModel;

    public ListModel<VmWatchdogType> getWatchdogModel() {
        return watchdogModel;
    }

    public void setWatchdogModel(ListModel<VmWatchdogType> watchdogModel) {
        this.watchdogModel = watchdogModel;
    }

    private ListModel<VmWatchdogAction> watchdogAction;

    public ListModel<VmWatchdogAction> getWatchdogAction() {
        return watchdogAction;
    }

    public void setWatchdogAction(ListModel<VmWatchdogAction> watchdogAction) {
        this.watchdogAction = watchdogAction;
    }

    public StoragePool getSelectedDataCenter() {
        DataCenterWithCluster dataCenterWithCluster = getDataCenterWithClustersList().getSelectedItem();
        if (dataCenterWithCluster == null) {
            return null;
        }

        return dataCenterWithCluster.getDataCenter();
    }

    public VDSGroup getSelectedCluster() {
        DataCenterWithCluster dataCenterWithCluster = getDataCenterWithClustersList().getSelectedItem();
        if (dataCenterWithCluster == null) {
            return null;
        }

        return dataCenterWithCluster.getCluster();
    }

    public void disableEditing(String msg) {
        getDefaultCommand().setIsExecutionAllowed(false);
        getEditingEnabled().setMessage(msg);
        getEditingEnabled().setEntity(false);
    }

    public static enum CpuSharesAmount {
        DISABLED(0), LOW(512), MEDIUM(1024), HIGH(2048), CUSTOM(-1);

        private int value;
        private CpuSharesAmount(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    public Integer getSelectedMigrationDowntime() {
        if (Boolean.TRUE.equals(getOverrideMigrationDowntime().getEntity())) {
            return getMigrationDowntime().getEntity();
        } else {
            return null;
        }
    }

    public void setSelectedMigrationDowntime(Integer value) {
        getOverrideMigrationDowntime().setEntity(value != null);
        getMigrationDowntime().setEntity(value);
    }

    public boolean isCreateInstanceOnly() {
        return ((CurrentUserRole) TypeResolver.getInstance().resolve(CurrentUserRole.class)).isCreateInstanceOnly();
    }

    private void numaSupport() {
        setNumaChanged(true);
        getBehavior().numaSupport();
    }

    public EntityModel<Boolean> getNumaEnabled() {
        return numaEnabled;
    }

    public void setNumaEnabled(EntityModel<Boolean> numaEnabled) {
        this.numaEnabled = numaEnabled;
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (NumaSupportModel.SUBMIT_NUMA_SUPPORT.equals(command.getName())) {
            onNumaSupport();
        }
    }

    private void onNumaSupport() {
        if (getWindow() == null) {
            return;
        }
        VmNumaSupportModel model = (VmNumaSupportModel) getWindow();
        setVmNumaNodes(model.getVm().getvNumaNodeList());
    }

    public void setNumaChanged(boolean numaChanged) {
        this.numaChanged = numaChanged;
    }

    public boolean isNumaChanged() {
        return numaChanged || initialsNumaNodeCount != getNumaNodeCount().getEntity();
    }

    public void updateNodeCount(int size) {
        initialsNumaNodeCount = size;
        getNumaNodeCount().setEntity(size);
    }

    private NotChangableForVmInPoolListModel<Provider<OpenstackNetworkProviderProperties>> providers;

    public ListModel<Provider<OpenstackNetworkProviderProperties>> getProviders() {
        return providers;
    }

    protected void setProviders(NotChangableForVmInPoolListModel<Provider<OpenstackNetworkProviderProperties>> value) {
        providers = value;
    }
}
