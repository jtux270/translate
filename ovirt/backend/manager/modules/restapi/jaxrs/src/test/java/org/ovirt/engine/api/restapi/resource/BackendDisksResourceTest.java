package org.ovirt.engine.api.restapi.resource;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.DiskFormat;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.api.model.StorageDomains;
import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatus;
import org.ovirt.engine.core.common.businessentities.AsyncTaskStatusEnum;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.PropagateErrors;
import org.ovirt.engine.core.common.businessentities.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendDisksResourceTest extends AbstractBackendCollectionResourceTest<Disk, org.ovirt.engine.core.common.businessentities.Disk, BackendDisksResource> {

    public BackendDisksResourceTest() {
        super(new BackendDisksResource(), SearchType.Disk, "Disks : ");
    }

    @Override
    protected List<Disk> getCollection() {
        return collection.list().getDisks();
    }

    @Override
    protected org.ovirt.engine.core.common.businessentities.Disk getEntity(int index) {
        DiskImage entity = new DiskImage();
        entity.setId(GUIDS[index]);
        entity.setvolumeFormat(VolumeFormat.RAW);
        entity.setDiskInterface(DiskInterface.VirtIO);
        entity.setImageStatus(ImageStatus.OK);
        entity.setVolumeType(VolumeType.Sparse);
        entity.setBoot(false);
        entity.setShareable(false);
        entity.setPropagateErrors(PropagateErrors.On);
        return setUpStatisticalEntityExpectations(entity);    }

    static org.ovirt.engine.core.common.businessentities.Disk setUpStatisticalEntityExpectations(DiskImage entity) {
        entity.setReadRate(1);
        entity.setWriteRate(2);
        entity.setReadLatency(3.0);
        entity.setWriteLatency(4.0);
        entity.setFlushLatency(5.0);
        return entity;
    }

    @Test
    public void testAdd() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpHttpHeaderExpectations("Expect", "201-created");
        setUpEntityQueryExpectations(VdcQueryType.GetDiskByDiskId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                getEntity(0));
        Disk model = getModel(0);
        setUpCreationExpectations(VdcActionType.AddDisk,
                AddDiskParameters.class,
                new String[] {"StorageDomainId"},
                new Object[] {GUIDS[2]},
                true,
                true,
                GUIDS[0],
                asList(GUIDS[3]),
                asList(new AsyncTaskStatus(AsyncTaskStatusEnum.finished)),
                VdcQueryType.GetDiskByDiskId,
                IdQueryParameters.class,
                new String[] {"Id"},
                new Object[] {GUIDS[0]},
                getEntity(0));
        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Disk);
        verifyModel((Disk)response.getEntity(), 0);
        assertNull(((Disk)response.getEntity()).getCreationStatus());
    }

    @Test
    public void testAddIdentifyStorageDomainByName() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpHttpHeaderExpectations("Expect", "201-created");
        setUpEntityQueryExpectations(VdcQueryType.GetDiskByDiskId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                getEntity(0));
        Disk model = getModel(0);
        model.getStorageDomains().getStorageDomains().get(0).setId(null);
        model.getStorageDomains().getStorageDomains().get(0).setName("Storage_Domain_1");
        setUpEntityQueryExpectations(VdcQueryType.GetAllStorageDomains,
                VdcQueryParametersBase.class,
                new String[] {},
                new Object[] {},
                getStorageDomains());
        setUpCreationExpectations(VdcActionType.AddDisk,
                AddDiskParameters.class,
                new String[] {},
                new Object[] {},
                true,
                true,
                GUIDS[0],
                asList(GUIDS[3]),
                asList(new AsyncTaskStatus(AsyncTaskStatusEnum.finished)),
                VdcQueryType.GetDiskByDiskId,
                IdQueryParameters.class,
                new String[] {"Id"},
                new Object[] {GUIDS[0]},
                getEntity(0));
        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Disk);
        verifyModel((Disk)response.getEntity(), 0);
        assertNull(((Disk)response.getEntity()).getCreationStatus());
    }

    private Object getStorageDomains() {
        List<org.ovirt.engine.core.common.businessentities.StorageDomain> sds = new LinkedList<org.ovirt.engine.core.common.businessentities.StorageDomain>();
        org.ovirt.engine.core.common.businessentities.StorageDomain sd = new org.ovirt.engine.core.common.businessentities.StorageDomain();
        sd.setStorageName("Storage_Domain_1");
        sd.setId(GUIDS[2]);
        sds.add(sd);
        return sds;
    }

    static Disk getModel(int index) {
        Disk model = new Disk();
        model.setSize(1024 * 1024L);
        model.setFormat(DiskFormat.COW.value());
        model.setInterface(org.ovirt.engine.api.model.DiskInterface.IDE.value());
        model.setSparse(true);
        model.setBootable(false);
        model.setShareable(false);
        model.setPropagateErrors(true);
        model.setStorageDomains(new StorageDomains());
        model.getStorageDomains().getStorageDomains().add(new StorageDomain());
        model.getStorageDomains().getStorageDomains().get(0).setId(GUIDS[2].toString());
        return model;
    }

    @Override
    protected void verifyModel(Disk model, int index) {
        verifyModelSpecific(model, index);
        verifyLinks(model);
    }

    static void verifyModelSpecific(Disk model, int index) {
        assertEquals(GUIDS[index].toString(), model.getId());
        assertFalse(model.isSetVm());
        assertTrue(model.isSparse());
        assertTrue(!model.isBootable());
        assertTrue(model.isPropagateErrors());
    }
}
