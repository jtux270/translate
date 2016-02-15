package org.ovirt.engine.core.bll.scheduling;

import static org.mockito.Mockito.mock;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dao.scheduling.PolicyUnitDao;
import org.ovirt.engine.core.vdsbroker.ResourceManager;

@Singleton
public class CommonTestMocks {

    @Produces
    private BackendInternal backendInternal = mock(BackendInternal.class);
    @Produces
    private DbFacade dbFacade = mock(DbFacade.class);
    @Produces
    private AuditLogDirector auditLogDirector = mock(AuditLogDirector.class);
    @Produces
    private ResourceManager resourceManager = mock(ResourceManager.class);
    @Produces
    private PolicyUnitDao policyUnitDao = mock(PolicyUnitDao.class);

}
