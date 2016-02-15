package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.common.businessentities.MigrationMethod;
import org.ovirt.engine.core.common.utils.ToStringBuilder;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;

public class MigrateVDSCommandParameters extends VdsAndVmIDVDSParametersBase {
    private String srcHost;
    private Guid dstVdsId;
    private String dstHost;
    private MigrationMethod migrationMethod;
    private boolean tunnelMigration;
    private String dstQemu;
    private Version clusterVersion;
    private Integer migrationDowntime;
    private Boolean autoConverge;
    private Boolean migrateCompressed;
    private String consoleAddress;

    public MigrateVDSCommandParameters(Guid vdsId, Guid vmId, String srcHost, Guid dstVdsId,
                                       String dstHost, MigrationMethod migrationMethod, boolean tunnelMigration,
                                       String dstQemu, Version clusterVersion, int migrationDowntime,
                                       Boolean autoConverge, Boolean migrateCompressed, String consoleAddress) {
        super(vdsId, vmId);
        this.srcHost = srcHost;
        this.dstVdsId = dstVdsId;
        this.dstHost = dstHost;
        this.migrationMethod = migrationMethod;
        this.tunnelMigration = tunnelMigration;
        this.dstQemu = dstQemu;
        this.clusterVersion = clusterVersion;
        this.migrationDowntime = migrationDowntime;
        this.autoConverge = autoConverge;
        this.migrateCompressed = migrateCompressed;
        this.consoleAddress = consoleAddress;
    }

    public String getSrcHost() {
        return srcHost;
    }

    public Guid getDstVdsId() {
        return dstVdsId;
    }

    public String getDstHost() {
        return dstHost;
    }

    public MigrationMethod getMigrationMethod() {
        return migrationMethod;
    }

    public boolean isTunnelMigration() {
        return tunnelMigration;
    }

    public String getDstQemu() {
        return dstQemu;
    }

    public int getMigrationDowntime() {
        return migrationDowntime;
    }

    public Boolean getMigrateCompressed() {
        return migrateCompressed;
    }

    public void setMigrateCompressed(Boolean migrateCompressed) {
        this.migrateCompressed = migrateCompressed;
    }

    public Boolean getAutoConverge() {
        return autoConverge;
    }

    public void setAutoConverge(Boolean autoConverge) {
        this.autoConverge = autoConverge;
    }

    public MigrateVDSCommandParameters() {
        migrationMethod = MigrationMethod.OFFLINE;
    }

    public void setClusterVersion(Version clusterVersion) {
        this.clusterVersion = clusterVersion;
    }

    public Version getClusterVersion() {
        return clusterVersion;
    }

    public String getConsoleAddress() {
        return consoleAddress;
    }

    public void setConsoleAddress(String consoleAddress) {
        this.consoleAddress = consoleAddress;
    }

    @Override
    protected ToStringBuilder appendAttributes(ToStringBuilder tsb) {
        return super.appendAttributes(tsb)
                .append("srcHost", getSrcHost())
                .append("dstVdsId", getDstVdsId())
                .append("dstHost", getDstHost())
                .append("migrationMethod", getMigrationMethod())
                .append("tunnelMigration", isTunnelMigration())
                .append("migrationDowntime", getMigrationDowntime())
                .append("autoConverge", getAutoConverge())
                .append("migrateCompressed", getMigrateCompressed())
                .append("consoleAddress", getConsoleAddress());
    }
}
