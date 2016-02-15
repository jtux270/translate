package org.ovirt.engine.core.dao;

import java.util.Collection;

import org.ovirt.engine.core.common.businessentities.DiskImageDynamic;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;

public interface DiskImageDynamicDAO extends GenericDao<DiskImageDynamic, Guid>, MassOperationsDao<DiskImageDynamic, Guid> {

    public void updateAllDiskImageDynamicWithDiskIdByVmId(Collection<Pair<Guid, DiskImageDynamic>> diskImageDynamic);

}
