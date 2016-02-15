package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.host.provider.HostProviderProxy;
import org.ovirt.engine.core.common.businessentities.ErrataCounts;
import org.ovirt.engine.core.common.businessentities.Erratum;
import org.ovirt.engine.core.common.businessentities.Erratum.ErrataSeverity;
import org.ovirt.engine.core.common.businessentities.Erratum.ErrataType;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VdsStaticDao;
import org.ovirt.engine.core.dao.provider.ProviderDao;

@RunWith(MockitoJUnitRunner.class)
public class GetErrataCountsForHostQueryTest extends AbstractQueryTest<IdQueryParameters, GetErrataCountsForHostQuery<IdQueryParameters>> {

    @Mock
    private VdsStaticDao vdsStaticDao;

    @Mock
    private ProviderDao providerDao;

    @Mock
    private HostProviderProxy providerProxy;

    @Mock
    private VdsStatic host;

    @Before
    public void setup() {
        when(getDbFacadeMockInstance().getVdsStaticDao()).thenReturn(vdsStaticDao);
        when(getDbFacadeMockInstance().getProviderDao()).thenReturn(providerDao);
    }

    @Test
    public void hostDoesNotExist() {
        when(vdsStaticDao.get(any(Guid.class))).thenReturn(null);
        getQuery().executeQueryCommand();

        assertFalse(getQuery().getQueryReturnValue().getSucceeded());
        assertNull(getQuery().getQueryReturnValue().getReturnValue());
        assertEquals(EngineMessage.ACTION_TYPE_FAILED_HOST_NOT_EXIST.name(), getQuery().getQueryReturnValue()
                .getExceptionString());
    }

    @Test
    public void hostHasNoProvider() {
        when(vdsStaticDao.get(any(Guid.class))).thenReturn(host);
        getQuery().executeQueryCommand();

        assertFalse(getQuery().getQueryReturnValue().getSucceeded());
        assertNull(getQuery().getQueryReturnValue().getReturnValue());
        assertEquals(EngineMessage.NO_FOREMAN_PROVIDER_FOR_HOST.name(), getQuery().getQueryReturnValue()
                .getExceptionString());
    }

    @Test
    public void noAvailableHostErrata() {
        setupToReportErrata(Collections.<Erratum> emptyList());
        getQuery().executeQueryCommand();

        assertNotNull(getQuery().getQueryReturnValue().getReturnValue());
        ErrataCounts returnValue = (ErrataCounts) getQuery().getQueryReturnValue().getReturnValue();
        for (ErrataType type : ErrataType.values()) {
            assertEquals(0, returnValue.getCountByType(type));
        }
    }

    @Test
    public void availableHostErrata() {
        setupToReportErrata(expectedErrata());
        getQuery().executeQueryCommand();

        ErrataCounts counts = (ErrataCounts) getQuery().getQueryReturnValue().getReturnValue();
        assertEquals(11, counts.getTotal());
        assertEquals(5, counts.getCountByType(ErrataType.BUGFIX));
        assertEquals(4, counts.getCountByType(ErrataType.ENHANCEMENT));
        assertEquals(2, counts.getCountByType(ErrataType.SECURITY));
        assertEquals(0, counts.getCountByTypeAndSeverity(ErrataType.SECURITY, ErrataSeverity.MODERATE));
    }

    @SuppressWarnings("unchecked")
    private void setupToReportErrata(List<Erratum> errata) {
        when(host.getHostProviderId()).thenReturn(mock(Guid.class));
        when(vdsStaticDao.get(any(Guid.class))).thenReturn(host);
        when(providerDao.get(any(Guid.class))).thenReturn(mock(Provider.class));
        doReturn(providerProxy).when(getQuery()).getHostProviderProxy(any(Provider.class));
        doReturn(errata).when(providerProxy).getErrataForHost(any(String.class));
    }

    private List<Erratum> expectedErrata() {
        List<Erratum> errata = new ArrayList<>();
        errata.addAll(createErrata(ErrataType.BUGFIX, ErrataSeverity.MODERATE, 2));
        errata.addAll(createErrata(ErrataType.BUGFIX, ErrataSeverity.IMPORTANT, 3));
        errata.addAll(createErrata(ErrataType.ENHANCEMENT, ErrataSeverity.MODERATE, 4));
        errata.addAll(createErrata(ErrataType.SECURITY, ErrataSeverity.CRITICAL, 2));
        return errata;
    }

    private List<Erratum> createErrata(ErrataType type, ErrataSeverity severity, int total) {
        List<Erratum> errata = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            errata.add(createErratum(type, severity));
        }

        return errata;
    }

    private Erratum createErratum(ErrataType type, ErrataSeverity severity) {
        Erratum erratum = new Erratum();
        erratum.setType(type);
        erratum.setSeverity(severity);
        return erratum;
    }
}
