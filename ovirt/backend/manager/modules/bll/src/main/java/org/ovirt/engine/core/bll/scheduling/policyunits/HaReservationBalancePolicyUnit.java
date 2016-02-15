package org.ovirt.engine.core.bll.scheduling.policyunits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang.math.NumberUtils;
import org.ovirt.engine.core.bll.scheduling.HaReservationHandling;
import org.ovirt.engine.core.bll.scheduling.PolicyUnitImpl;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

/**
 * This balancing policy, is for use in cases the user selected HA Reservation for its Cluster. The basic methodology
 * is: 1. get the optimal HA VMs for each VM assuming evenly spreaded across the cluster 2. calc the overUtilize as
 * (1)*user configured threshold in percent. 3. randomly choose a VM from a busy host to move to another more available
 * host.
 */
public class HaReservationBalancePolicyUnit extends PolicyUnitImpl {

    private static final Log log = LogFactory.getLog(HaReservationBalancePolicyUnit.class);

    private static final int DEFAULT_OVER_UTILIZATION_VALUE = 200;
    private static final long serialVersionUID = 4926515666890804243L;

    public HaReservationBalancePolicyUnit(PolicyUnit policyUnit) {
        super(policyUnit);
    }

    @Override
    public Pair<List<Guid>, Guid> balance(VDSGroup cluster,
            List<VDS> hosts,
            Map<String, String> parameters,
            ArrayList<String> messages) {

        log.debugFormat("Started HA reservation balancing method for cluster: {0}", cluster.getName());
        if (!cluster.supportsHaReservation()) {
            return null;
        }
        if (hosts == null || hosts.size() < 2) {
            int hostCount = hosts == null ? 0 : hosts.size();
            log.debugFormat("No balancing for cluster {0}, contains only {1} host(s)", cluster.getName(), hostCount);
            return null;
        }

        int haVmsInCluster = 0;

        Map<Guid, List<VM>> hostId2HaVmMapping = HaReservationHandling.mapHaVmToHostByCluster(cluster.getId());
        haVmsInCluster = countHaVmsInCluster(hostId2HaVmMapping);


        int optimalHaDistribution = (int) Math.ceil(((double) haVmsInCluster / hosts.size()));

        int overUtilizationParam = DEFAULT_OVER_UTILIZATION_VALUE;
        if (parameters.get("OverUtilization") != null) {
            overUtilizationParam = NumberUtils.toInt(parameters.get("OverUtilization"));
        } else {
            overUtilizationParam = Config.<Integer> getValue(ConfigValues.OverUtilizationForHaReservation);
        }

        log.debugFormat("optimalHaDistribution value:{0}", optimalHaDistribution);

        int overUtilizationThreshold = (int) Math.ceil(optimalHaDistribution * (overUtilizationParam / 100.0));
        log.debugFormat("overUtilizationThreshold value: {0}", overUtilizationThreshold);

        List<VDS> overUtilizedHosts =
                getHostUtilizedByCondition(hosts, hostId2HaVmMapping, overUtilizationThreshold, Condition.MORE_THAN);
        if (overUtilizedHosts.isEmpty()) {
            log.debugFormat("No over utilized hosts for cluster: {0}", cluster.getName());
            return null;
        }

        List<VDS> underUtilizedHosts =
                getHostUtilizedByCondition(hosts, hostId2HaVmMapping, overUtilizationParam, Condition.LESS_THAN);
        if (underUtilizedHosts.size() == 0) {
            log.debugFormat("No under utilized hosts for cluster: {0}", cluster.getName());
            return null;
        }

        // Get random host from the over utilized hosts
        VDS randomHost = overUtilizedHosts.get(new Random().nextInt(overUtilizedHosts.size()));

        List<VM> migrableVmsOnRandomHost = getMigrableVmsRunningOnVds(randomHost.getId(), hostId2HaVmMapping);
        if (migrableVmsOnRandomHost.isEmpty()) {
            log.debugFormat("No migratable hosts were found for cluster: {0} ", cluster.getName());
            return null;
        }

        // Get random vm to migrate
        VM vm = migrableVmsOnRandomHost.get(new Random().nextInt(migrableVmsOnRandomHost.size()));
        log.infoFormat("VM to be migrated:{0}", vm.getName());

        List<Guid> underUtilizedHostsKeys = new ArrayList<Guid>();
        for (VDS vds : underUtilizedHosts) {
            underUtilizedHostsKeys.add(vds.getId());
        }

        return new Pair<List<Guid>, Guid>(underUtilizedHostsKeys, vm.getId());

    }

    private int countHaVmsInCluster(Map<Guid, List<VM>> hostId2HaVmMapping) {
        int result = 0;
        for (Entry<Guid, List<VM>> entry : hostId2HaVmMapping.entrySet()) {
            result += entry.getValue().size();
        }
        return result;
    }

    private List<VDS> getHostUtilizedByCondition(List<VDS> hosts,
            Map<Guid, List<VM>> hostId2HaVmMapping,
            int UtilizationThreshold, Condition cond) {

        List<VDS> utilizedHosts = new ArrayList<VDS>();

        for (VDS host : hosts) {
            int count = 0;
            List<VM> vms = hostId2HaVmMapping.get(host.getId());
            if (vms != null) {
                count = vms.size();
            }

            if (cond.equals(Condition.LESS_THAN)) {
                if (count < UtilizationThreshold) {
                    utilizedHosts.add(host);
                }
            } else if (cond.equals(Condition.MORE_THAN)) {
                if (count >= UtilizationThreshold) {
                    utilizedHosts.add(host);
                }

            }
        }
        return utilizedHosts;

    }

    private enum Condition {
        LESS_THAN,
        MORE_THAN;
    }

    private List<VM> getMigrableVmsRunningOnVds(final Guid hostId, Map<Guid, List<VM>> hostId2HaVmMapping) {
        List<VM> vms = hostId2HaVmMapping.get(hostId);

        vms = LinqUtils.filter(vms, new Predicate<VM>() {
            @Override
            public boolean eval(VM v) {
                return v.getMigrationSupport() == MigrationSupport.MIGRATABLE;
            }
        });

        return vms;
    }

}
