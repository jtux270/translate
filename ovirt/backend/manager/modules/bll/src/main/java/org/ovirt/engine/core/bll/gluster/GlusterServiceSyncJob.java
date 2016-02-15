package org.ovirt.engine.core.bll.gluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterClusterService;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerService;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterService;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServiceStatus;
import org.ovirt.engine.core.common.businessentities.gluster.ServiceType;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.gluster.GlusterFeatureSupported;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterServicesListVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

public class GlusterServiceSyncJob extends GlusterJob {
    private static final Log log = LogFactory.getLog(GlusterServiceSyncJob.class);
    private static final GlusterServiceSyncJob instance = new GlusterServiceSyncJob();
    private final Map<String, GlusterService> serviceNameMap = new HashMap<String, GlusterService>();

    private GlusterServiceSyncJob() {
    }

    public static GlusterServiceSyncJob getInstance() {
        return instance;
    }

    @OnTimerMethodAnnotation("refreshGlusterServices")
    public void refreshGlusterServices() {
        if (getServiceNameMap().isEmpty()) {
            // Lazy loading. Keeping this out of the constructor/getInstance
            // helps in writing the JUnit test as well.
            populateServiceMap();
        }

        List<VDSGroup> clusters = getClusterDao().getAll();

        for (VDSGroup cluster : clusters) {
            if (supportsGlusterServicesFeature(cluster)) {
                try {
                    List<VDS> serversList = getClusterUtils().getAllServers(cluster.getId());

                    // If there are no servers in the cluster, set the status as unknown
                    if (serversList.isEmpty()) {
                        Map<ServiceType, GlusterServiceStatus> statusMap = new HashMap<>();
                        for (ServiceType type : getClusterServiceMap(cluster).keySet()) {
                            statusMap.put(type, GlusterServiceStatus.UNKNOWN);
                        }
                        addOrUpdateClusterServices(cluster, statusMap);
                    } else {
                        List<Callable<Map<String, GlusterServiceStatus>>> taskList = createTaskList(serversList);
                        if (taskList != null && taskList.size() > 0) {
                            refreshClusterServices(cluster, ThreadPoolUtil.invokeAll(taskList));
                        }
                    }
                } catch (Exception e) {
                    log.errorFormat("Error while refreshing service statuses of cluster {0}!",
                            cluster.getName(),
                            e);
                }
            }
        }
    }

    private Map<String, GlusterServiceStatus> updateGlusterServicesStatusForStoppedServer(VDS server) {
        Map<String, GlusterServiceStatus> retMap = new HashMap<String, GlusterServiceStatus>();
        List<GlusterServerService> serviceList =
                getGlusterServerServiceDao().getByServerId(server.getId());
        for (GlusterServerService service : serviceList) {
            retMap.put(service.getServiceName(), GlusterServiceStatus.UNKNOWN);
            service.setStatus(GlusterServiceStatus.UNKNOWN);
            getGlusterServerServiceDao().update(service);
        }
        return retMap;
    }

    private List<Callable<Map<String, GlusterServiceStatus>>> createTaskList(List<VDS> serversList) {
        List<Callable<Map<String, GlusterServiceStatus>>> taskList =
                new ArrayList<Callable<Map<String, GlusterServiceStatus>>>();
        for (final VDS server : serversList) {
            taskList.add(new Callable<Map<String, GlusterServiceStatus>>() {
                /**
                 * Fetches and updates status of all services of the given server, <br>
                 * and returns a map having key = service name and value = service status
                 */
                @Override
                public Map<String, GlusterServiceStatus> call() throws Exception {
                    return refreshServerServices(server);
                }
            });
        }
        return taskList;
    }

    /**
     * Analyses statuses of services from all servers of the cluster, and updates the status of the cluster level
     * service type accordingly.
     *
     * @param cluster
     *            Cluster being processed
     * @param serviceStatusMaps
     *            List of service name to status maps from each (UP) server of the cluster
     */
    private void refreshClusterServices(VDSGroup cluster, List<Map<String, GlusterServiceStatus>> serviceStatusMaps) {
        Map<ServiceType, GlusterServiceStatus> fetchedClusterServiceStatusMap =
                createServiceTypeStatusMap(serviceStatusMaps);
        addOrUpdateClusterServices(cluster, fetchedClusterServiceStatusMap);
    }

    private void addOrUpdateClusterServices(VDSGroup cluster,
            Map<ServiceType, GlusterServiceStatus> fetchedClusterServiceStatusMap) {
        Map<ServiceType, GlusterClusterService> existingClusterServiceMap = getClusterServiceMap(cluster);
        for (Entry<ServiceType, GlusterServiceStatus> entry : fetchedClusterServiceStatusMap.entrySet()) {
            ServiceType type = entry.getKey();
            GlusterServiceStatus status = entry.getValue();

            GlusterClusterService existingClusterService = existingClusterServiceMap.get(type);
            if (existingClusterService == null) {
                existingClusterServiceMap.put(type, addClusterServiceToDb(cluster, type, status));
            } else if (existingClusterService.getStatus() != status) {
                updateClusterServiceStatus(existingClusterService, status);
            }
        }
    }

    private Map<ServiceType, GlusterServiceStatus> createServiceTypeStatusMap(List<Map<String, GlusterServiceStatus>> serviceStatusMaps) {
        Map<ServiceType, GlusterServiceStatus> fetchedServiceTypeStatusMap =
                new HashMap<ServiceType, GlusterServiceStatus>();
        for (Entry<String, GlusterServiceStatus> entry : mergeServiceStatusMaps(serviceStatusMaps).entrySet()) {
            String serviceName = entry.getKey();
            GlusterServiceStatus status = entry.getValue();
            ServiceType type = getServiceNameMap().get(serviceName).getServiceType();

            GlusterServiceStatus foundStatus = fetchedServiceTypeStatusMap.get(type);
            if (foundStatus == null) {
                fetchedServiceTypeStatusMap.put(type, status);
            } else if (foundStatus != status) {
                GlusterServiceStatus finalStatus = getFinalStatus(status, foundStatus);
                fetchedServiceTypeStatusMap.put(type, finalStatus);
            }
        }
        return fetchedServiceTypeStatusMap;
    }

    private GlusterServiceStatus getFinalStatus(GlusterServiceStatus firstStatus, GlusterServiceStatus secondStatus) {
        return firstStatus.getCompositeStatus(secondStatus);
    }

    private Map<String, GlusterServiceStatus> mergeServiceStatusMaps(List<Map<String, GlusterServiceStatus>> serviceStatusMaps) {
        Map<String, GlusterServiceStatus> mergedServiceStatusMap = new HashMap<String, GlusterServiceStatus>();
        for (Map<String, GlusterServiceStatus> serviceStatusMap : serviceStatusMaps) {
            for (Entry<String, GlusterServiceStatus> entry : serviceStatusMap.entrySet()) {
                String serviceName = entry.getKey();
                GlusterServiceStatus status = entry.getValue();
                GlusterServiceStatus alreadyFoundStatus = mergedServiceStatusMap.get(serviceName);
                if (alreadyFoundStatus == null) {
                    mergedServiceStatusMap.put(serviceName, status);
                } else if (alreadyFoundStatus != status && alreadyFoundStatus != GlusterServiceStatus.MIXED) {
                    GlusterServiceStatus finalStatus = getFinalStatus(status, alreadyFoundStatus);
                    mergedServiceStatusMap.put(serviceName, finalStatus);
                }
            }
        }
        return mergedServiceStatusMap;
    }

    private Map<ServiceType, GlusterClusterService> getClusterServiceMap(VDSGroup cluster) {
        List<GlusterClusterService> clusterServices = getGlusterClusterServiceDao().getByClusterId(cluster.getId());
        if (clusterServices == null) {
            clusterServices = new ArrayList<GlusterClusterService>();
        }

        Map<ServiceType, GlusterClusterService> clusterServiceMap = new HashMap<ServiceType, GlusterClusterService>();
        for (GlusterClusterService clusterService : clusterServices) {
            clusterServiceMap.put(clusterService.getServiceType(), clusterService);
        }
        return clusterServiceMap;
    }

    /**
     * Refreshes statuses of services on given server, and returns a map of service name to it's status
     *
     * @param server
     *            The server whose services statuses are to be refreshed
     * @return map of service name to it's status
     */
    @SuppressWarnings({ "unchecked", "serial" })
    private Map<String, GlusterServiceStatus> refreshServerServices(final VDS server) {
        Map<String, GlusterServiceStatus> serviceStatusMap = new HashMap<String, GlusterServiceStatus>();
        if (server.getStatus() != VDSStatus.Up) {
            // Update the status of all the services of stopped server in single transaction
            TransactionSupport.executeInScope(TransactionScopeOption.Required,
                    new TransactionMethod<Map<String, GlusterServiceStatus>>() {
                        @Override
                        public Map<String, GlusterServiceStatus> runInTransaction() {
                            return updateGlusterServicesStatusForStoppedServer(server);
                        }
                    });
        } else {
            acquireLock(server.getId());
            try {
                Map<Guid, GlusterServerService> existingServicesMap = getExistingServicesMap(server);
                List<GlusterServerService> servicesToUpdate = new ArrayList<GlusterServerService>();

                VDSReturnValue returnValue = runVdsCommand(VDSCommandType.GlusterServicesList,
                        new GlusterServicesListVDSParameters(server.getId(), getServiceNameMap().keySet()));
                if (!returnValue.getSucceeded()) {
                    log.errorFormat("Couldn't fetch services statuses from server {0}, error: {1}! " +
                            "Updating statuses of all services on this server to UNKNOWN.",
                            server.getHostName(),
                            returnValue.getVdsError().getMessage());
                    logUtil.logServerMessage(server, AuditLogType.GLUSTER_SERVICES_LIST_FAILED);
                    return updateStatusToUnknown(existingServicesMap.values());
                }

                for (final GlusterServerService fetchedService : (List<GlusterServerService>) returnValue.getReturnValue()) {
                    serviceStatusMap.put(fetchedService.getServiceName(), fetchedService.getStatus());
                    GlusterServerService existingService = existingServicesMap.get(fetchedService.getServiceId());
                    if (existingService == null) {
                        insertServerService(server, fetchedService);
                    } else {
                        final GlusterServiceStatus oldStatus = existingService.getStatus();
                        final GlusterServiceStatus newStatus = fetchedService.getStatus();
                        if (oldStatus != newStatus) {
                            log.infoFormat("Status of service {0} on server {1} changed from {2} to {3}. Updating in engine now.",
                                    fetchedService.getServiceName(),
                                    server.getHostName(),
                                    oldStatus.name(),
                                    newStatus.name());
                            logUtil.logAuditMessage(server.getVdsGroupId(),
                                    null,
                                    server,
                                    AuditLogType.GLUSTER_SERVER_SERVICE_STATUS_CHANGED,
                                    new HashMap<String, String>() {
                                        {
                                            put(GlusterConstants.SERVICE_NAME, fetchedService.getServiceName());
                                            put(GlusterConstants.OLD_STATUS, oldStatus.getStatusMsg());
                                            put(GlusterConstants.NEW_STATUS, newStatus.getStatusMsg());
                                        }
                                    });
                            existingService.setStatus(fetchedService.getStatus());
                            servicesToUpdate.add(existingService);
                        }
                    }
                }
                if (servicesToUpdate.size() > 0) {
                    getGlusterServerServiceDao().updateAll(servicesToUpdate);
                }
            } finally {
                releaseLock(server.getId());
            }
        }

        return serviceStatusMap;
    }

    private Map<String, GlusterServiceStatus> updateStatusToUnknown(Collection<GlusterServerService> existingServices) {
        Map<String, GlusterServiceStatus> serviceStatusMap = new HashMap<String, GlusterServiceStatus>();

        for (GlusterServerService existingService : existingServices) {
            existingService.setStatus(GlusterServiceStatus.UNKNOWN);
            serviceStatusMap.put(existingService.getServiceName(), existingService.getStatus());
        }

        getGlusterServerServiceDao().updateAll(existingServices);
        return serviceStatusMap;
    }

    private Map<Guid, GlusterServerService> getExistingServicesMap(VDS server) {
        List<GlusterServerService> existingServices = getGlusterServerServiceDao().getByServerId(server.getId());
        Map<Guid, GlusterServerService> existingServicesMap = new HashMap<Guid, GlusterServerService>();
        if (existingServices != null) {
            for (GlusterServerService service : existingServices) {
                existingServicesMap.put(service.getServiceId(), service);
            }
        }
        return existingServicesMap;
    }

    @SuppressWarnings("serial")
    private void insertServerService(VDS server, final GlusterServerService fetchedService) {
        fetchedService.setId(Guid.newGuid());
        getGlusterServerServiceDao().save(fetchedService);
        log.infoFormat("Service {0} was not mapped to server {1}. Mapped it now.",
                fetchedService.getServiceName(),
                server.getHostName());
        logUtil.logAuditMessage(server.getVdsGroupId(),
                null,
                server,
                AuditLogType.GLUSTER_SERVICE_ADDED_TO_SERVER,
                new HashMap<String, String>() {
                    {
                        put(GlusterConstants.SERVICE_NAME, fetchedService.getServiceName());
                    }
                });
    }

    @SuppressWarnings("serial")
    private void updateClusterServiceStatus(final GlusterClusterService clusterService,
            final GlusterServiceStatus newStatus) {
        final GlusterServiceStatus oldStatus = clusterService.getStatus();
        clusterService.setStatus(newStatus);
        getGlusterClusterServiceDao().update(clusterService);
        log.infoFormat("Status of service type {0} changed on cluster {1} from {2} to {3}.",
                clusterService.getServiceType().name(),
                clusterService.getClusterId(),
                oldStatus,
                newStatus);
        logUtil.logAuditMessage(clusterService.getClusterId(),
                null,
                null,
                AuditLogType.GLUSTER_CLUSTER_SERVICE_STATUS_CHANGED,
                new HashMap<String, String>() {
                    {
                        put(GlusterConstants.SERVICE_TYPE, clusterService.getServiceType().name());
                        put(GlusterConstants.OLD_STATUS, oldStatus.getStatusMsg());
                        put(GlusterConstants.NEW_STATUS, newStatus.getStatusMsg());
                    }
                });
    }

    @SuppressWarnings("serial")
    private GlusterClusterService addClusterServiceToDb(VDSGroup cluster,
            final ServiceType serviceType,
            final GlusterServiceStatus status) {
        GlusterClusterService clusterService = new GlusterClusterService();
        clusterService.setClusterId(cluster.getId());
        clusterService.setServiceType(serviceType);
        clusterService.setStatus(status);

        getGlusterClusterServiceDao().save(clusterService);

        log.infoFormat("Service type {0} not mapped to cluster {1}. Mapped it now.",
                serviceType,
                cluster.getName());
        logUtil.logAuditMessage(clusterService.getClusterId(),
                null,
                null,
                AuditLogType.GLUSTER_CLUSTER_SERVICE_STATUS_ADDED,
                new HashMap<String, String>() {
                    {
                        put(GlusterConstants.SERVICE_TYPE, serviceType.name());
                        put(GlusterConstants.NEW_STATUS, status.getStatusMsg());
                    }
                });

        return clusterService;
    }

    private boolean supportsGlusterServicesFeature(VDSGroup cluster) {
        return cluster.supportsGlusterService()
                && GlusterFeatureSupported.glusterServices(cluster.getcompatibility_version());
    }

    private void populateServiceMap() {
        List<GlusterService> services = getGlusterServiceDao().getAll();
        for (GlusterService service : services) {
            getServiceNameMap().put(service.getServiceName(), service);
        }
    }

    protected Map<String, GlusterService> getServiceNameMap() {
        return serviceNameMap;
    }
}
