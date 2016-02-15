package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.OvfEntityData;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.UnregisteredOVFDataDAO;
import org.ovirt.engine.core.utils.ovf.OvfReaderException;

/** A test case for the {@link GetUnregisteredVmTemplatesQuery} class. */
public class GetUnregisteredVmTemplatesQueryTest extends AbstractQueryTest<IdQueryParameters, GetUnregisteredVmTemplatesQuery<? extends IdQueryParameters>> {

    Guid storageDomainId = Guid.newGuid();
    VmEntityType entityType = VmEntityType.TEMPLATE;
    Guid newVmTemplateGuid = Guid.newGuid();
    Guid newVmTemplateGuid2 = Guid.newGuid();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockQueryParameters();
        setUpQueryEntities();
    }

    @Test
    public void testExecuteQueryGetAllEntitiesCommand() throws OvfReaderException {
        getQuery().executeQueryCommand();

        @SuppressWarnings("unchecked")
        List<VmTemplate> result = getQuery().getQueryReturnValue().getReturnValue();
        assertEquals("Wrong number of Templates in result", 2, result.size());
    }

    private void mockQueryParameters() {
        // Mock the Query Parameters
        when(getQueryParameters().getId()).thenReturn(storageDomainId);
    }

    private void setUpQueryEntities() throws OvfReaderException {
        // Set up the expected result
        VmTemplate VmTemplateReturnForOvf = new VmTemplate();
        VmTemplateReturnForOvf.setId(newVmTemplateGuid);
        VmTemplateReturnForOvf.setName("Name");
        String ovfData = new String("OVF data for the first Template");
        OvfEntityData ovfEntityData =
                new OvfEntityData(VmTemplateReturnForOvf.getId(),
                        VmTemplateReturnForOvf.getName(),
                        VmEntityType.TEMPLATE,
                        null,
                        null,
                        storageDomainId,
                        ovfData,
                        null);
        List<OvfEntityData> expectedResult = new ArrayList<>();
        List<OvfEntityData> expectedResultQuery1 = new ArrayList<>();
        expectedResultQuery1.add(ovfEntityData);
        expectedResult.add(ovfEntityData);
        VmTemplate VmTemplateReturnForOvf2 = new VmTemplate();
        VmTemplateReturnForOvf2.setId(newVmTemplateGuid2);
        VmTemplateReturnForOvf2.setName("Name2");
        String ovfData2 = new String("OVF data for the second Template");
        OvfEntityData ovfEntityData2 =
                new OvfEntityData(VmTemplateReturnForOvf2.getId(),
                        VmTemplateReturnForOvf2.getName(),
                        VmEntityType.TEMPLATE,
                        null,
                        null,
                        storageDomainId,
                        ovfData2,
                        null);
        expectedResult.add(ovfEntityData2);
        List<OvfEntityData> expectedResultQuery2 = new ArrayList<>();
        expectedResultQuery2.add(ovfEntityData);

        // Mock the DAOs
        UnregisteredOVFDataDAO unregisteredOVFDataDAOMock = mock(UnregisteredOVFDataDAO.class);
        when(getDbFacadeMockInstance().getUnregisteredOVFDataDao()).thenReturn(unregisteredOVFDataDAOMock);
        when(unregisteredOVFDataDAOMock.getAllForStorageDomainByEntityType(storageDomainId, entityType)).thenReturn(expectedResult);
        when(unregisteredOVFDataDAOMock.getByEntityIdAndStorageDomain(newVmTemplateGuid, storageDomainId)).thenReturn(expectedResultQuery1);
        when(unregisteredOVFDataDAOMock.getByEntityIdAndStorageDomain(newVmTemplateGuid2, storageDomainId)).thenReturn(expectedResultQuery2);

        // Mock OVF
        OvfHelper ovfHelperMock = mock(OvfHelper.class);
        when(getQuery().getOvfHelper()).thenReturn(ovfHelperMock);
        when(ovfHelperMock.readVmTemplateFromOvf(ovfData)).thenReturn(VmTemplateReturnForOvf);
        when(ovfHelperMock.readVmTemplateFromOvf(ovfData2)).thenReturn(VmTemplateReturnForOvf2);
    }
}
