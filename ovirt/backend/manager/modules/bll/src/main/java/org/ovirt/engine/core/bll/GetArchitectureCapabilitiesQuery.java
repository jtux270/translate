package org.ovirt.engine.core.bll;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.queries.ArchCapabilitiesParameters;
import org.ovirt.engine.core.common.queries.ArchCapabilitiesParameters.ArchCapabilitiesVerb;
import org.ovirt.engine.core.compat.Version;

public class GetArchitectureCapabilitiesQuery<P extends ArchCapabilitiesParameters> extends QueriesCommandBase<P> {

    public GetArchitectureCapabilitiesQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        setReturnValue(getMap(getParameters().getArchCapabilitiesVerb()));
    }

    private static Map<ArchitectureType, Map<Version, Boolean>> getMap(ArchCapabilitiesVerb archCapabilitiesVerb) {
        if (archCapabilitiesVerb == null) {
            return null;
        }

        Map<ArchitectureType, Map<Version, Boolean>> supportMap =
                new EnumMap<ArchitectureType, Map<Version, Boolean>>(ArchitectureType.class);

        for (ArchitectureType arch : ArchitectureType.values()) {
            Map<Version, Boolean> archMap = new HashMap<Version, Boolean>();

            for (Version version : Version.ALL) {
                archMap.put(version, isSupported(archCapabilitiesVerb, arch, version));
            }

            supportMap.put(arch, archMap);
        }

        return supportMap;
    }

    /**
     * Checks if a feature is supported
     *
     * @param archCapabilitiesVerb
     *            The feature
     * @param architecture
     *            The CPU Architecture
     * @param version
     *            The cluster compatibility version
     * @return
     */
    private static boolean isSupported(ArchCapabilitiesVerb archCapabilitiesVerb,
            ArchitectureType architecture,
            Version version) {
        switch (archCapabilitiesVerb) {
        case GetMigrationSupport:
            return FeatureSupported.isMigrationSupported(architecture, version);
        case GetMemorySnapshotSupport:
            return FeatureSupported.isMemorySnapshotSupportedByArchitecture(architecture, version);
        case GetSuspendSupport:
            return FeatureSupported.isSuspendSupportedByArchitecture(architecture, version);
        }
        return false;
    }
}
