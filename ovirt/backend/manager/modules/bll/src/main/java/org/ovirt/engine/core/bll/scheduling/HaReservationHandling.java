package org.ovirt.engine.core.bll.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

/**
 * A helper class for the scheduling mechanism for checking the HA Reservation status of a Cluster
 */
public class HaReservationHandling {

    private static final Log log = LogFactory.getLog(HaReservationHandling.class);
    /**
     * @param cluster
     *            - Cluster to check
     * @param failedHosts
     *            - a list to return all the hosts that failed the check, must be initialized outside this method
     * @return true: Cluster is HaReservation safe. false: a failover in one of the Clusters Hosts could negatively
     *         impacting performance.
     */
    public boolean checkHaReservationStatusForCluster(VDSGroup cluster, List<VDS> failedHosts) {
        List<VDS> hosts = DbFacade.getInstance().getVdsDao().getAllForVdsGroupWithStatus(cluster.getId(), VDSStatus.Up);

        // No hosts, return true
        if (hosts == null || hosts.isEmpty()) {
            return true;
        }
        // HA Reservation is not possible with less than 2 hosts
        if (hosts.size() < 2) {
            log.debugFormat("Cluster: {0} failed HA reservation check because there is only one host in the cluster",
                    cluster.getName());
            failedHosts.addAll(hosts);
            return false;
        }

        // List of host id and cpu/ram free resources
        // for the outer Pair, first is host id second is a Pair of cpu and ram
        // for the inner Pair, first is cpu second is ram
        List<Pair<Guid, Pair<Integer, Integer>>> hostsUnutilizedResources = getUnutilizedResources(hosts);

        Map<Guid, List<VM>> hostToHaVmsMapping = mapHaVmToHostByCluster(cluster.getId());

        for (VDS host : hosts) {
            if (hostToHaVmsMapping.get(host.getId()) != null) {
                boolean isHaSafe =
                        findReplacementForHost(cluster, host,
                                hostToHaVmsMapping.get(host.getId()),
                                hostsUnutilizedResources);
                if (!isHaSafe) {
                    failedHosts.add(host);
                }
            }
        }

        log.infoFormat("HA reservation status for cluster {0} is {1}", cluster.getName(), failedHosts.isEmpty() ? "OK"
                : "Failed");
        return failedHosts.isEmpty();
    }

    private boolean findReplacementForHost(VDSGroup cluster, VDS host,
            List<VM> vmList,
            List<Pair<Guid, Pair<Integer, Integer>>> hostsUnutilizedResources) {

        Map<Guid, Pair<Integer, Integer>> additionalHostsUtilizedResources =
                new HashMap<Guid, Pair<Integer, Integer>>();

        for (VM vm : vmList) {
            int curVmMemSize = 0;
            if(vm.getUsageMemPercent() != null) {
                curVmMemSize = (int) Math.round(vm.getMemSizeMb() * (vm.getUsageMemPercent() / 100.0));
            }

            int curVmCpuPercent = 0;
            if (vm.getUsageCpuPercent() != null) {
                curVmCpuPercent =
                        vm.getUsageCpuPercent() * vm.getNumOfCpus()
                                / SlaValidator.getEffectiveCpuCores(host, cluster.getCountThreadsAsCores());
            }
            log.debugFormat("VM {0}. CPU usage:{1}%, RAM usage:{2}MB", vm.getName(), curVmCpuPercent, curVmMemSize);

            boolean foundForCurVm = false;
            for (Pair<Guid, Pair<Integer, Integer>> hostData : hostsUnutilizedResources) {
                // Make sure not to run on the same Host as the Host we are testing
                if (hostData.getFirst().equals(host.getId())) {
                    continue;
                }

                // Check Memory and CPU
                if (hostData.getSecond() != null && hostData.getSecond().getSecond() != null
                        && hostData.getSecond().getFirst() != null) {

                    int memoryFree = hostData.getSecond().getSecond();
                    int cpuFree = hostData.getSecond().getFirst();

                    long additionalMemory = 0;
                    int additionalCpu = 0;

                    if (additionalHostsUtilizedResources.get(hostData.getFirst()) != null) {
                        additionalCpu = additionalHostsUtilizedResources.get(hostData.getFirst()).getFirst();
                        additionalMemory = additionalHostsUtilizedResources.get(hostData.getFirst()).getSecond();
                    }

                    if ((memoryFree - additionalMemory) >= curVmMemSize && (cpuFree - additionalCpu) >= curVmCpuPercent) {
                        // Found a place for current vm, add the RAM and CPU size to additionalHostsUtilizedResources
                        Pair<Integer, Integer> cpuRamPair = additionalHostsUtilizedResources.get(hostData.getFirst());
                        if (cpuRamPair != null) {
                            cpuRamPair.setFirst(cpuRamPair.getFirst() + curVmCpuPercent);
                            cpuRamPair.setSecond(cpuRamPair.getSecond() + curVmMemSize);
                        } else {
                            cpuRamPair = new Pair<>(curVmCpuPercent, curVmMemSize);
                            additionalHostsUtilizedResources.put(hostData.getFirst(), cpuRamPair);
                        }

                        foundForCurVm = true;
                        break;
                    }

                }

            }

            if (!foundForCurVm) {
                log.infoFormat("Did not found a replacement host for VM:{0}", vm.getName());
                return false;
            }

        }

        return true;
    }

    public static Map<Guid, List<VM>> mapVmToHost(List<VM> vms) {
        Map<Guid, List<VM>> hostToHaVmsMapping = new HashMap<>();

        for (VM vm : vms) {
            if (!Guid.isNullOrEmpty(vm.getRunOnVds())) {
                if (!hostToHaVmsMapping.containsKey(vm.getRunOnVds())) {
                    List<VM> vmsOfHost = new ArrayList<VM>();
                    vmsOfHost.add(vm);
                    hostToHaVmsMapping.put(vm.getRunOnVds(), vmsOfHost);
                } else {
                    hostToHaVmsMapping.get(vm.getRunOnVds()).add(vm);
                }
            }
        }
        return hostToHaVmsMapping;
    }

    private List<Pair<Guid, Pair<Integer, Integer>>> getUnutilizedResources(List<VDS> hosts) {
        List<Pair<Guid, Pair<Integer, Integer>>> hostsUnutilizedResources =
                new ArrayList<>();
        for (VDS host : hosts) {
            Pair<Integer, Integer> innerUnutilizedCpuRamPair = new Pair<>();
            int hostFreeCpu = 0;
            if (host.getUsageCpuPercent() != null) {
                hostFreeCpu = 100 - host.getUsageCpuPercent();
            }
            innerUnutilizedCpuRamPair.setFirst(hostFreeCpu);

            // Get available memory for the Host, round down to int
            int hostFreeMem = (int) host.getMaxSchedulingMemory();
            innerUnutilizedCpuRamPair.setSecond(hostFreeMem);

            Pair<Guid, Pair<Integer, Integer>> outerUnutilizedCpuRamPair =
                    new Pair<>(host.getId(), innerUnutilizedCpuRamPair);

            hostsUnutilizedResources.add(outerUnutilizedCpuRamPair);
        }
        return hostsUnutilizedResources;
    }

    @SuppressWarnings("unchecked")
    public static Map<Guid, List<VM>> mapHaVmToHostByCluster(Guid clusterId) {

        List<VM> vms = DbFacade.getInstance().getVmDao().getAllForVdsGroup(clusterId);
        if (vms == null || vms.isEmpty()) {
            log.debugFormat("No VMs available for this cluster with id {0}", clusterId);
            // return empty map
            return Collections.EMPTY_MAP;
        }

        vms = LinqUtils.filter(vms, new Predicate<VM>() {
            @Override
            public boolean eval(VM v) {
                return v.isAutoStartup();
            }
        });

        return mapVmToHost(vms);

    }

}
