package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.ServerCpu;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VdsDynamic;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VdsArchitectureHelper {
    private static final Logger log = LoggerFactory.getLogger(VdsArchitectureHelper.class);

    /**
     * Gets the architecture type of the given host using its cpu flags, if not found, return the cluster architecture
     * @param host
     *            The host
     * @return
     *            The host architecture type
     */
    public ArchitectureType getArchitecture(VdsStatic host) {
        VDSGroup cluster = DbFacade.getInstance().getVdsGroupDao().get(host.getVdsGroupId());
        VdsDynamic vdsDynamic = DbFacade.getInstance().getVdsDynamicDao().get(host.getId());
        if (vdsDynamic != null) {
            ServerCpu cpu = CpuFlagsManagerHandler.findMaxServerCpuByFlags(vdsDynamic.getCpuFlags(),
                    cluster.getCompatibilityVersion());
            if (cpu != null && cpu.getArchitecture() != null) {
                return cpu.getArchitecture();
            }
        }
        // take architecture from the cluster if it is null on the host level or host is not yet saved in db
        log.info(
                "Failed to get architecture type from host information for host '{}'. Using cluster '{}'"
                        + " architecture value instead.",
                host.getName(),
                cluster.getName());
        return cluster.getArchitecture();
    }
}
