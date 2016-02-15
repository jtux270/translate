package org.ovirt.engine.core.bll;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.ExportRepoImageParameters;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.businessentities.storage.LunDisk;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VmDao;


/** A test case for {@link org.ovirt.engine.core.bll.ExportRepoImageCommandTest} */
@RunWith(MockitoJUnitRunner.class)
public class ExportRepoImageCommandTest extends ImportExportRepoImageCommandTest {

    @Mock
    protected VmDao vmDao;

    protected ExportRepoImageCommand<ExportRepoImageParameters> cmd;

    protected VM vm;

    @Before
    public void setUp() {
        super.setUp();

        vm = new VM();
        vm.setStatus(VMStatus.Down);

        when(vmDao.getVmsListForDisk(getDiskImageId(), Boolean.FALSE)).thenReturn(Arrays.asList(vm));

        ExportRepoImageParameters exportParameters = new ExportRepoImageParameters(
                getDiskImageGroupId(), getRepoStorageDomainId());

        cmd = spy(new ExportRepoImageCommand<>(exportParameters));

        doReturn(vmDao).when(cmd).getVmDao();
        doReturn(getStorageDomainDao()).when(cmd).getStorageDomainDao();
        doReturn(getStoragePoolDao()).when(cmd).getStoragePoolDao();
        doReturn(getDiskDao()).when(cmd).getDiskDao();
        doReturn(getProviderProxy()).when(cmd).getProviderProxy();
    }

    @Test
    public void testCanDoActionSuccess() {
        CanDoActionTestUtils.runAndAssertCanDoActionSuccess(cmd);
    }

    @Test
    public void testCanDoActionLunDisk() {
        when(getDiskDao().get(getDiskImageGroupId())).thenReturn(new LunDisk());
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(cmd,
                EngineMessage.ACTION_TYPE_FAILED_NOT_SUPPORTED_DISK_STORAGE_TYPE);
    }

    @Test
    public void testCanDoActionImageDoesNotExist() {
        when(getDiskDao().get(getDiskImageGroupId())).thenReturn(null);
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(cmd,
                EngineMessage.ACTION_TYPE_FAILED_DISK_NOT_EXIST);
    }

    @Test
    public void testCanDoActionDomainInMaintenance() {
        getDiskStorageDomain().setStatus(StorageDomainStatus.Maintenance);
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(cmd,
                EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2);
    }

    @Test
    public void testCanDoActionImageHasParent() {
        getDiskImage().setParentId(Guid.newGuid());
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(cmd,
                EngineMessage.ACTION_TYPE_FAILED_DISK_CONFIGURATION_NOT_SUPPORTED);
    }

    @Test
    public void testCanDoActionVmRunning() {
        vm.setStatus(VMStatus.Up);
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(cmd,
                EngineMessage.ACTION_TYPE_FAILED_VM_IS_RUNNING);
    }

    @Test
    public void testCanDoActionImageLocked() {
        getDiskImage().setImageStatus(ImageStatus.LOCKED);
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(cmd,
                EngineMessage.ACTION_TYPE_FAILED_DISKS_LOCKED);
    }
}
