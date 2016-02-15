package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.constants.StorageConstants;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.StorageDomainStaticDao;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
public class AddExistingBlockStorageDomainCommandTest {

    private AddExistingBlockStorageDomainCommand<StorageDomainManagementParameter> command;
    private StorageDomainManagementParameter parameters;

    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule(
            mockConfig(ConfigValues.HostedEngineStorageDomainName, StorageConstants.HOSTED_ENGINE_STORAGE_DOMAIN_NAME),
            mockConfig(ConfigValues.WarningLowSpaceIndicator, 10),
            mockConfig(ConfigValues.CriticalSpaceActionBlocker, 5)
    );

    @Mock
    private StorageDomainStaticDao storageDomainStaticDao;

    @Before
    public void setUp() {
        parameters = new StorageDomainManagementParameter(getStorageDomain());
        parameters.setVdsId(Guid.newGuid());
        command = spy(new AddExistingBlockStorageDomainCommand<>(parameters));
        doReturn(storageDomainStaticDao).when(command).getStorageDomainStaticDao();

        doNothing().when(command).addStorageDomainInDb();
        doNothing().when(command).updateStorageDomainDynamicFromIrs();
        doNothing().when(command).saveLUNsInDB(anyListOf(LUNs.class));
    }

    @Test
    public void testAddExistingBlockDomainSuccessfully() {
        doReturn(getLUNs()).when(command).getLUNsFromVgInfo(parameters.getStorageDomain().getStorage());
        command.executeCommand();
        assertTrue(command.getReturnValue().getSucceeded());
    }

    @Test
    public void testAddExistingBlockDomainWhenVgInfoReturnsEmptyLunList() {
        doReturn(Collections.emptyList()).when(command).getLUNsFromVgInfo(parameters.getStorageDomain().getStorage());
        assertFalse("Could not connect to Storage Domain", command.canAddDomain());
        assertTrue("Import block Storage Domain should have failed due to empty Lun list returned from VGInfo ",
                command.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(EngineMessage.ACTION_TYPE_FAILED_PROBLEM_WITH_CANDIDATE_INFO.toString()));
    }

    @Test
    public void testAlreadyExistStorageDomain() {
        when(command.getStorageDomainStaticDao().get(any(Guid.class))).thenReturn(getStorageDomain());
        assertFalse("Storage Domain already exists", command.canAddDomain());
        assertTrue("Import block Storage Domain should have failed due to already existing Storage Domain",
                command.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_ALREADY_EXIST.toString()));
    }

    @Test
    public void testAddHostedEngineStorageSucceeds() {
        doReturn(getLUNs()).when(command).getLUNsFromVgInfo(parameters.getStorageDomain().getStorage());
        doReturn(Collections.emptyList()).when(command).getAllLuns();

        parameters.getStorageDomain().setStorageName(StorageConstants.HOSTED_ENGINE_STORAGE_DOMAIN_NAME);
        assertTrue(command.canAddDomain());
    }

    private static StorageDomainStatic getStorageDomain() {
        StorageDomainStatic storageDomain = new StorageDomainStatic();
        storageDomain.setStorage(Guid.newGuid().toString());
        return storageDomain;
    }

    private static List<LUNs> getLUNs() {
        LUNs lun = new LUNs();
        lun.setId(Guid.newGuid().toString());
        return Collections.singletonList(lun);
    }
}
