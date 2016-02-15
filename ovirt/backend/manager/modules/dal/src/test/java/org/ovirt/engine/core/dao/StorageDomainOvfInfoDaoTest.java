package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.StorageDomainOvfInfo;
import org.ovirt.engine.core.common.businessentities.StorageDomainOvfInfoStatus;
import org.ovirt.engine.core.compat.Guid;

public class StorageDomainOvfInfoDaoTest extends BaseGenericDaoTestCase<Guid, StorageDomainOvfInfo, StorageDomainOvfInfoDao> {
    @Override
    protected StorageDomainOvfInfo generateNewEntity() {
        return new StorageDomainOvfInfo(FixturesTool.STORAGE_DOAMIN_NFS_MASTER, null, FixturesTool.DISK_ID_2, StorageDomainOvfInfoStatus.OUTDATED, null);
    }

    @Override
    protected void updateExistingEntity() {
        existingEntity.setStatus(StorageDomainOvfInfoStatus.DISABLED);
        existingEntity.setLastUpdated(new Date(System.currentTimeMillis()));
    }

    @Override
    protected Guid getExistingEntityId() {
        return FixturesTool.DISK_ID;
    }

    @Override
    protected StorageDomainOvfInfoDao prepareDao() {
        return dbFacade.getStorageDomainOvfInfoDao();
    }

    @Override
    protected Guid generateNonExistingId() {
        return Guid.newGuid();
    }

    @Override
    protected int getEneitiesTotalCount() {
        //unneeded here
        return 0;
    }

    @Override
    public void testGetAll() {
        //unneeded here
    }

    @Override
    public void testGet() {
        //unneeded here
    }

    @Test
    public void updateOvfUpdatedInfoFewDomains() {
        StorageDomainOvfInfo ovfInfo = dao.get(getExistingEntityId());
        StorageDomainOvfInfo ovfInfo1 = dao.getAllForDomain(FixturesTool.STORAGE_DOAMIN_NFS2_2).get(0);
        assertFalse("domain shouldn't be ovf updated prior to test",
                ovfInfo1.getStatus() == StorageDomainOvfInfoStatus.UPDATED);
        assertFalse("domain shouldn't be ovf updated prior to test",
                ovfInfo.getStatus() == StorageDomainOvfInfoStatus.UPDATED);
        dao.updateOvfUpdatedInfo(Arrays.asList(ovfInfo.getStorageDomainId(), ovfInfo1.getStorageDomainId()),
                StorageDomainOvfInfoStatus.UPDATED,
                StorageDomainOvfInfoStatus.DISABLED);
        ovfInfo = dao.get(ovfInfo.getOvfDiskId());
        ovfInfo1 = dao.get(ovfInfo1.getOvfDiskId());
        assertTrue(ovfInfo.getStatus() == StorageDomainOvfInfoStatus.UPDATED);
        assertTrue(ovfInfo1.getStatus() == StorageDomainOvfInfoStatus.UPDATED);
    }

    @Test
    public void updateOvfUpdatedInfoWithRelevantExceptStatus() {
        StorageDomainOvfInfo ovfInfo = dao.get(getExistingEntityId());
        ovfInfo.setStatus(StorageDomainOvfInfoStatus.DISABLED);
        dao.update(ovfInfo);
        StorageDomainOvfInfo ovfInfo1 = dao.getAllForDomain(FixturesTool.STORAGE_DOAMIN_NFS2_2).get(0);
        assertFalse("domain shouldn't be ovf updated prior to test",
                ovfInfo1.getStatus() == StorageDomainOvfInfoStatus.UPDATED);
        dao.updateOvfUpdatedInfo(Arrays.asList(ovfInfo.getStorageDomainId(), ovfInfo1.getStorageDomainId()),
                StorageDomainOvfInfoStatus.UPDATED,
                StorageDomainOvfInfoStatus.DISABLED);
        ovfInfo = dao.get(ovfInfo.getOvfDiskId());
        ovfInfo1 = dao.get(ovfInfo1.getOvfDiskId());
        assertTrue(ovfInfo.getStatus() == StorageDomainOvfInfoStatus.DISABLED);
        assertTrue(ovfInfo1.getStatus() == StorageDomainOvfInfoStatus.UPDATED);
    }

    @Test
    public void loadStorageDomainIdsForOvfIds() {
        StorageDomainOvfInfo ovfInfo = dao.get(getExistingEntityId());
        assertTrue("domain shouldn't have stored ovfs prior to test", ovfInfo.getStoredOvfIds().isEmpty());
        Guid ovfId1 = Guid.newGuid();
        Guid ovfId2 = Guid.newGuid();
        List<Guid> ovfIds = Arrays.asList(ovfId1, ovfId2);

        ovfInfo.setStoredOvfIds(ovfIds);

        dao.update(ovfInfo);

        ovfInfo = dao.get(ovfInfo.getOvfDiskId());
        assertEquals(ovfIds.size(), ovfInfo.getStoredOvfIds().size());
        assertTrue(ovfInfo.getStoredOvfIds().containsAll(ovfIds));
    }
}
