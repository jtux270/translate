package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.CdRom;
import org.ovirt.engine.api.resource.SnapshotCdRomResource;

public class BackendSnapshotCdRomResource implements SnapshotCdRomResource {

    protected String cdRomId;
    protected BackendSnapshotCdRomsResource collection;

    public BackendSnapshotCdRomResource(String cdRomId, BackendSnapshotCdRomsResource collection) {
        super();
        this.cdRomId = cdRomId;
        this.collection = collection;
    }

    @Override
    public CdRom get() {
        for (CdRom cdRom : collection.list().getCdRoms()) {
            if (cdRom.getId().equals(cdRomId)) {
                return cdRom;
            }
        }
        throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
    }
}
