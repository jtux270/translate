package org.ovirt.engine.core.bll.storage;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.CanDoActionTestUtils;
import org.ovirt.engine.core.bll.CommandAssertUtils;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.validator.storage.StorageConnectionValidator;
import org.ovirt.engine.core.common.action.AttachDetachStorageConnectionParameters;
import org.ovirt.engine.core.common.businessentities.BusinessEntitiesDefinitions;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.storage.LUNStorageServerConnectionMap;
import org.ovirt.engine.core.common.businessentities.storage.LUNStorageServerConnectionMapId;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.LunDao;
import org.ovirt.engine.core.dao.StorageServerConnectionDao;
import org.ovirt.engine.core.dao.StorageServerConnectionLunMapDao;
import org.ovirt.engine.core.utils.MockEJBStrategyRule;

@RunWith(MockitoJUnitRunner.class)
public class AttachStorageServerConnectionToStorageDomainCommandTest {
    @ClassRule
    public static MockEJBStrategyRule ejbRule = new MockEJBStrategyRule();

    private AttachStorageConnectionToStorageDomainCommand<AttachDetachStorageConnectionParameters> command = null;

    private StorageConnectionValidator validator = null;

    private StorageDomain domain = null;

    @Mock
    LunDao lunDao;

    @Mock
    StorageServerConnectionDao connectionDao;

    @Mock
    StorageServerConnectionLunMapDao lunMapDao;

    @Before
    public void init() {
        Guid connectionId = Guid.newGuid();
        Guid domainId = Guid.newGuid();
        AttachDetachStorageConnectionParameters parameters = new AttachDetachStorageConnectionParameters();
        parameters.setStorageConnectionId(connectionId.toString());
        parameters.setStorageDomainId(domainId);
        validator = mock(StorageConnectionValidator.class);
        command = spy(new AttachStorageConnectionToStorageDomainCommand<AttachDetachStorageConnectionParameters>(parameters));
        doReturn(validator).when(command).createStorageConnectionValidator();
        doReturn(lunDao).when(command).getLunDao();
        doReturn(connectionDao).when(command).getStorageServerConnectionDao();
        doReturn(lunMapDao).when(command).getStorageServerConnectionLunMapDao();
        domain = new StorageDomain();
        domain.setId(domainId);
        domain.setStorageDomainType(StorageDomainType.Data);
        domain.setStatus(StorageDomainStatus.Maintenance);
        domain.setStorageType(StorageType.ISCSI);
        doReturn(domain).when(command).getStorageDomain();
    }

    @Test
    public void canDoActionSuccess() {
        when(validator.isConnectionExists()).thenReturn(ValidationResult.VALID);
        when(validator.isConnectionForISCSIDomainAttached(domain)).thenReturn(Boolean.FALSE);
        when(validator.isISCSIConnectionAndDomain(domain)).thenReturn(ValidationResult.VALID);
        when(validator.isDomainOfConnectionExistsAndInactive(domain)).thenReturn(ValidationResult.VALID);
        CanDoActionTestUtils.runAndAssertCanDoActionSuccess(command);
    }

    @Test
    public void canDoActionFailure() {
        when(validator.isConnectionExists()).thenReturn(ValidationResult.VALID);
        when(validator.isConnectionForISCSIDomainAttached(domain)).thenReturn(Boolean.TRUE);
        when(validator.isISCSIConnectionAndDomain(domain)).thenReturn(ValidationResult.VALID);
        when(validator.isDomainOfConnectionExistsAndInactive(domain)).thenReturn(ValidationResult.VALID);
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(command, EngineMessage.ACTION_TYPE_FAILED_STORAGE_CONNECTION_FOR_DOMAIN_ALREADY_EXISTS);
    }

    @Test
    public void canDoActionFailureNotExists() {
        ValidationResult result = new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_CONNECTION_NOT_EXIST);
        when(validator.isConnectionExists()).thenReturn(result);
        when(validator.isISCSIConnectionAndDomain(domain)).thenReturn(ValidationResult.VALID);
        when(validator.isDomainOfConnectionExistsAndInactive(domain)).thenReturn(ValidationResult.VALID);
        CanDoActionTestUtils.runAndAssertCanDoActionFailure(command, EngineMessage.ACTION_TYPE_FAILED_STORAGE_CONNECTION_NOT_EXIST);
    }

    @Test
    public void executeCommandNotFirstDummyLun() {
       LUNs dummyLun = new LUNs();
       dummyLun.setLUN_id(BusinessEntitiesDefinitions.DUMMY_LUN_ID_PREFIX + domain.getId());
       when(lunDao.get(dummyLun.getLUN_id())).thenReturn(dummyLun);

       List<StorageServerConnections> connectionsForDomain = new ArrayList<>();
       StorageServerConnections connection = new StorageServerConnections();
       connection.setid(Guid.newGuid().toString());
       connection.setstorage_type(StorageType.ISCSI);
       connection.setiqn("iqn.1.2.3.4.com");
       connection.setconnection("123.345.266.255");
       connectionsForDomain.add(connection);
       when(connectionDao.getAllForDomain(domain.getId())).thenReturn(connectionsForDomain);
       LUNStorageServerConnectionMapId map_id = new LUNStorageServerConnectionMapId(dummyLun.getLUN_id(), connection.getid());
       when(lunMapDao.get(map_id)).thenReturn(null);
       //dummy lun already exists, thus no need to save
       verify(lunDao, never()).save(dummyLun);
       verify(lunMapDao, never()).save(new LUNStorageServerConnectionMap());
       command.executeCommand();
       CommandAssertUtils.checkSucceeded(command, true);
    }

    @Test
    public void executeCommandFirstDummyLun() {
       LUNs dummyLun = new LUNs();
       dummyLun.setLUN_id(BusinessEntitiesDefinitions.DUMMY_LUN_ID_PREFIX + domain.getId());
       when(lunDao.get(dummyLun.getLUN_id())).thenReturn(null);
       doNothing().when(lunDao).save(dummyLun);
       List<StorageServerConnections> connectionsForDomain = new ArrayList<>();
       StorageServerConnections connection = new StorageServerConnections();
       connection.setid(Guid.newGuid().toString());
       connection.setstorage_type(StorageType.ISCSI);
       connection.setiqn("iqn.1.2.3.4.com");
       connection.setconnection("123.345.266.255");
       connectionsForDomain.add(connection);
       when(connectionDao.getAllForDomain(domain.getId())).thenReturn(connectionsForDomain);
       LUNStorageServerConnectionMapId map_id = new LUNStorageServerConnectionMapId(dummyLun.getLUN_id(), connection.getid());
       when(lunMapDao.get(map_id)).thenReturn(null);
       LUNStorageServerConnectionMap map = new LUNStorageServerConnectionMap();
       doNothing().when(lunMapDao).save(map);
       command.executeCommand();
       CommandAssertUtils.checkSucceeded(command, true);
    }

}
