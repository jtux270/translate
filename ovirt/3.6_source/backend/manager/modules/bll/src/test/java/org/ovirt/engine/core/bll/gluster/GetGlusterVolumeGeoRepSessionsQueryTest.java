package org.ovirt.engine.core.bll.gluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.ovirt.engine.core.bll.AbstractQueryTest;
import org.ovirt.engine.core.common.businessentities.gluster.GeoRepSessionStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSession;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSessionDetails;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.gluster.GlusterGeoRepDao;



public class GetGlusterVolumeGeoRepSessionsQueryTest extends AbstractQueryTest<IdQueryParameters, GetGlusterVolumeGeoRepSessionsQuery<IdQueryParameters>>{

    private Guid masterVolumeId = Guid.newGuid();
    private Guid sessionId = Guid.newGuid();
    private Guid slaveVolumeId = Guid.newGuid();
    private Guid slaveNodeUuid = Guid.newGuid();

    private GlusterGeoRepDao geoRepDao;

    private List<GlusterGeoRepSession> getMockGeoRepSessions() {
        List<GlusterGeoRepSession> sessions = new ArrayList<GlusterGeoRepSession>();

        GlusterGeoRepSession session = new GlusterGeoRepSession();
        session.setId(sessionId);
        session.setMasterVolumeId(masterVolumeId);
        session.setSessionKey("");
        session.setSlaveHostName("slave-host-1");
        session.setSlaveVolumeId(slaveVolumeId);
        session.setSlaveNodeUuid(slaveNodeUuid);
        session.setSlaveVolumeName("");
        session.setStatus(GeoRepSessionStatus.ACTIVE);

        sessions.add(session);

        return sessions;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpMock();
    }

    private void setUpMock() {
        geoRepDao = mock(GlusterGeoRepDao.class);
        doReturn(geoRepDao).when(getQuery()).getGeoRepDao();
        doReturn(masterVolumeId).when(getQueryParameters()).getId();
        doReturn(getMockGeoRepSessions()).when(geoRepDao).getGeoRepSessions(masterVolumeId);
        doReturn(new ArrayList<GlusterGeoRepSessionDetails>()).when(geoRepDao).getGeoRepSessionDetails(sessionId);
    }

    @Test
    public void testQueryForStatus() {
        getQuery().executeQueryCommand();

        List<GlusterGeoRepSession> expected = getMockGeoRepSessions();

        List<GlusterGeoRepSession> actual = getQuery().getQueryReturnValue().getReturnValue();

        assertEquals(actual, expected);
    }
}
