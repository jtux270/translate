package org.ovirt.engine.core.bll.host;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSType;

/**
 * {@code UpdateAvailable} represents the ability of its implementing class to examine if updates are available for a
 * the given host
 */
public interface UpdateAvailable {

    /**
     * Checks if a host has an available updates
     *
     * @param host
     *            The examined host
     * @return {@code true} if updates are available, else {@code false}
     */
    boolean isUpdateAvailable(VDS host);

    /**
     * @return the host type
     */
    VDSType getHostType();
}
