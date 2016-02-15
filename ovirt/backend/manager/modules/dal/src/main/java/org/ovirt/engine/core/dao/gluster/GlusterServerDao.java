package org.ovirt.engine.core.dao.gluster;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterServer;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DAO;
import org.ovirt.engine.core.dao.GenericDao;

public interface GlusterServerDao extends DAO, GenericDao<GlusterServer, Guid> {

    public GlusterServer getByServerId(Guid serverId);

    public GlusterServer getByGlusterServerUuid(Guid glusterServerUuid);

    public void removeByGlusterServerUuid(Guid glusterServerUuid);
}
