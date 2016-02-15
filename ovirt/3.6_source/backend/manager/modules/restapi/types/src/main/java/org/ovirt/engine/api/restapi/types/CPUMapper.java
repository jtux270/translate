package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.api.model.Architecture;
import org.ovirt.engine.api.model.CPU;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.ServerCpu;

public class CPUMapper {

    @Mapping(from = ServerCpu.class, to = CPU.class)
    public static CPU map(ServerCpu entity,
            CPU template) {
        CPU model = template != null ? template : new CPU();

        model.setName(entity.getCpuName());
        model.setLevel(entity.getLevel());
        model.setArchitecture(map(entity.getArchitecture(), null));

        return model;
    }

    @Mapping(from = Architecture.class, to = ArchitectureType.class)
    public static ArchitectureType map(Architecture model,
            ArchitectureType template) {
        if (model != null) {
            switch (model) {
            case UNDEFINED:
                return ArchitectureType.undefined;
            case X86_64:
                return ArchitectureType.x86_64;
            case PPC64:
                return ArchitectureType.ppc64;
            default:
                return null;
            }
        }
        return null;
    }

    @Mapping(from = ArchitectureType.class, to = String.class)
    public static String map(ArchitectureType model,
            String template) {
        if (model != null) {
            switch (model) {
            case undefined:
                return Architecture.UNDEFINED.value();
            case x86_64:
                return Architecture.X86_64.value();
            case ppc64:
                return Architecture.PPC64.value();
            default:
                return null;
            }
        }
        return null;
    }

}
