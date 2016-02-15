package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.compat.Guid;

/**
 * <code>VdsStaticDAO</code> defines a type that performs CRUD operations on instances of {@link VDS}.
 *
 *
 */
public interface VdsStaticDAO extends GenericDao<VdsStatic, Guid> {
    /**
     * Retrieves the instance for the given host name.
     *
     * @param hostname
     *            the host name
     * @return the instance
     */
    VdsStatic getByHostName(String hostname);

    /**
     * Retrieves the instance for the given vds name.
     * @param vdsName
     *            the vds name
     * @return the instance
     */
    VdsStatic getByVdsName(String vdsName);

    /**
     * Finds all instances with the given ip address.
     * @param address
     *            the ip address
     * @return the list of instances
     */
    List<VdsStatic> getAllWithIpAddress(String address);

    /**
     * Retrieves all instances associated with the specified VDS group.
     *
     * @param vdsGroup
     *            the group id
     * @return the list of instances
     */
    List<VdsStatic> getAllForVdsGroup(Guid vdsGroup);
 }
