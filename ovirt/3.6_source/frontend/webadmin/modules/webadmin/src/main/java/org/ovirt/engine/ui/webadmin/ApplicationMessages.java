package org.ovirt.engine.ui.webadmin;

import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.ui.common.CommonApplicationMessages;

public interface ApplicationMessages extends CommonApplicationMessages {

    @DefaultMessage("A new version is available; an upgrade option will appear once the Host is moved to maintenance mode.")
    String hostHasUpgradeAlert();

    @DefaultMessage("A new version is available. <a>Upgrade</a>")
    String hostInSupportedStatusHasUpgradeAlert();

    @DefaultMessage("This host is in non responding state. Try to Activate it; If the problem persists, switch Host to Maintenance mode and try to reinstall it.")
    String hostHasReinstallAlertNonResponsive();

    @DefaultMessage("Host installation failed. Fix installation issues and try to <a>Re-Install</a>")
    String hostHasReinstallAlertInstallFailed();

    @DefaultMessage("Host is in maintenance mode, you can Activate it by pressing the Activate button. If you wish to upgrade or reinstall it click <a>here</a>.")
    String hostHasReinstallAlertMaintenance();

    @DefaultMessage("The Host network configuration is not saved. <a>Save</a>")
    String hostHasNICsAlert();

    @DefaultMessage("This host is in non responding state and has no power management configured. Please reboot manually.")
    String hostHasManualFenceAlert();

    @DefaultMessage("Power Management is not configured for this Host. <a>Enable Power Management</a>")
    String hostHasNoPowerManagementAlert();

    @DefaultMessage("Host Destination is disabled since you have selected Virtual Machines in several Clusters.")
    String migrateHostDisabledVMsInServerClusters();

    @DefaultMessage("Note that some of the selected VMs are already running on this Host and will not be migrated.")
    String migrateSomeVmsAlreadyRunningOnHost();

    @DefaultMessage("No available Host to migrate to.")
    String migrateNoAvailableHost();

    @DefaultMessage("Please make sure the Host ''{0}'' has been manually shut down or rebooted.")
    String manaulFencePopupMessageLabel(String hostName);

    @DefaultMessage("All references to objects that reside on Storage Domain {0} in the database will be removed. You may need to manually clean the storage in order to reuse it.")
    String storageDestroyPopupMessageLabel(String storageName);

    @DefaultMessage("All references to objects that belong to Data Center {0} in the database will be removed. You may need to manually clean the Storage Domains in order to reuse them.")
    String detaCenterForceRemovePopupMessageLabel(String dcName);

    @DefaultMessage("For Server Load - Allow scheduling of {0}% of physical memory")
    String clusterPopupMemoryOptimizationForServerLabel(String a);

    @DefaultMessage("For Desktop Load - Allow scheduling of {0}% of physical memory")
    String clusterPopupMemoryOptimizationForDesktopLabel(String a);

    @DefaultMessage("Custom Overcommit Threshold - Set to {0}% via API/CLI")
    String clusterPopupMemoryOptimizationCustomLabel(String a);

    @DefaultMessage("The Network will be added to the Data Center {0} as well.")
    String theNetworkWillBeAddedToTheDataCenterAsWell(String dcName);

    @DefaultMessage("(VLAN {0})")
    String vlanNetwork(int vlanId);

    @DefaultMessage("Virtual Machine {0} already exists")
    String sameVmNameExists(String vmName);

    @DefaultMessage("{0} out of unlimited MB")
    String unlimitedMemConsumption(long  mem);

    @DefaultMessage("{0} out of unlimited vCPUs")
    String unlimitedVcpuConsumption(int vcpu);

    @DefaultMessage("{0} out of {1} MB")
    String limitedMemConsumption(long mem, long limit);

    @DefaultMessage("{0} out of {1} vCPUs")
    String limitedVcpuConsumption(int vcpu, int limit);

    @DefaultMessage("{0} out of unlimited GB")
    String unlimitedStorageConsumption(String storage);

    @DefaultMessage("{0} out of {1} GB")
    String limitedStorageConsumption(String storage, double limit);

    @DefaultMessage("Some new hosts are detected in the cluster. You can <a>Import</a> them to engine or <a>Detach</a> them from the cluster.")
    String clusterHasNewGlusterHosts();

    @DefaultMessage("{0} VMs running, out of which {1} migrating")
    String vmsWithTotalMigrations(String vmCount, String vmMigrating);

    @DefaultMessage("{0} VMs running, {1} are migrating to host and {2} are migrating from host")
    String vmsWithInOutMigrations(String vmCount, String incomingMigrations, String outgoingMigrations);

    @DefaultMessage("Rebalance {0}")
    String rebalanceStatusMessage(JobExecutionStatus jobStatus);

    @DefaultMessage("{0}. Moving the display network will drop VM console connectivity until they are restarted.")
    String moveDisplayNetworkWarning(String networkOperationMessage);

    @DefaultMessage("Default ({0})")
    String defaultMtu(int mtu);

    @DefaultMessage("Free {0}\nUsed {1} of total available {2}")
    String glusterCapacityInfo(String freeSize, String usedSize, String totalSize);

    @DefaultMessage("Volume data from {0} of cluster {1} is replicated to this volume.")
    String geoRepSlaveVolumeToolTip(String mastervolName, String clusterName);

    @DefaultMessage("0 - {0}")
    String maxVfs(int maxVfs);

    @DefaultMessage("{0} ({1})")
    String nameId(String name, String id);

    @DefaultMessage("{0}% ({1} GB)")
    String percentWithValueInGB(int percent, int value);

    @DefaultMessage("Stripe Size {0} KB is recommended for {1} device")
    String stripSizeInfoForGlusterBricks(int stripeSize, String raidType);

    @DefaultMessage("(Choose storage devices of RAID type: {0})")
    String getStorageDeviceSelectionInfo(String raidType);
}
