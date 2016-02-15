package org.ovirt.engine.api.restapi.types;

import org.junit.Test;

import org.ovirt.engine.api.model.DataCenter;
import org.ovirt.engine.api.model.DataCenterStatus;
import org.ovirt.engine.api.restapi.model.StorageFormat;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.StoragePool;

public class DataCenterMapperTest extends
        AbstractInvertibleMappingTest<DataCenter, StoragePool, StoragePool> {

    public DataCenterMapperTest() {
        super(DataCenter.class, StoragePool.class, StoragePool.class);
    }

    @Override
    protected DataCenter postPopulate(DataCenter model) {
        model.setStorageFormat(MappingTestHelper.shuffle(StorageFormat.class).value());
        return model;
    }

    @Override
    protected void verify(DataCenter model, DataCenter transform) {
        assertNotNull(transform);
        assertEquals(model.getName(), transform.getName());
        assertEquals(model.getId(), transform.getId());
        assertEquals(model.getDescription(), transform.getDescription());
        assertEquals(model.getComment(), transform.getComment());
        assertEquals(model.isLocal(), transform.isLocal());
        assertEquals(model.getStorageFormat(), transform.getStorageFormat());
    }

    @Test
    //this test was added to support 'status' field, which has only a one-way mapping (from Backend entity to REST entity).
    //The generic test does a round-trip, which would fail when there's only one-way mapping.
    public void testFromBackendToRest() {
        testStatusMapping(StoragePoolStatus.Contend, DataCenterStatus.CONTEND);
        testStatusMapping(StoragePoolStatus.Maintenance, DataCenterStatus.MAINTENANCE);
        testStatusMapping(StoragePoolStatus.NotOperational, DataCenterStatus.NOT_OPERATIONAL);
        testStatusMapping(StoragePoolStatus.NonResponsive, DataCenterStatus.PROBLEMATIC);
        testStatusMapping(StoragePoolStatus.Uninitialized, DataCenterStatus.UNINITIALIZED);
        testStatusMapping(StoragePoolStatus.Up, DataCenterStatus.UP);
    }

    private void testStatusMapping(StoragePoolStatus storagePoolStatus, DataCenterStatus dataCenterStatus) {
        StoragePool storagePool = new StoragePool();
        storagePool.setStatus(storagePoolStatus);
        DataCenter dataCenter = DataCenterMapper.map(storagePool, null);
        assertEquals(dataCenter.getStatus().getState(), dataCenterStatus.value());
    }
}
