package org.ovirt.engine.api.restapi.resource.aaa;

import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.api.model.Group;
import org.ovirt.engine.api.resource.aaa.DomainGroupResource;
import org.ovirt.engine.api.restapi.resource.AbstractBackendSubResource;
import org.ovirt.engine.api.restapi.utils.DirectoryEntryIdUtils;
import org.ovirt.engine.core.aaa.DirectoryGroup;
import org.ovirt.engine.core.common.queries.DirectoryIdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

/**
 * This resource corresponds to a group that exists in some directory accessible by the engine, and that may or may not
 * have been added to the engine and stored in the database. This resource doesn't provide information about the
 * permissions, roles or tags of the group, even if those have been already assigned and stored in the database.
 */
public class BackendDomainGroupResource
        extends AbstractBackendSubResource<Group, DirectoryGroup>
        implements DomainGroupResource {

    private BackendDomainGroupsResource parent;

    public BackendDomainGroupResource(String id, BackendDomainGroupsResource parent) {
        super(DirectoryEntryIdUtils.decode(id), Group.class, DirectoryGroup.class);
        this.parent = parent;
    }

    public BackendDomainGroupsResource getParent() {
        return parent;
    }

    public void setParent(BackendDomainGroupsResource parent) {
        this.parent = parent;
    }

    @Override
    public Group get() {
        String directory = parent.getDirectory().getName();
        DirectoryIdQueryParameters parameters = new DirectoryIdQueryParameters(directory, id);
        return performGet(VdcQueryType.GetDirectoryGroupById, parameters, BaseResource.class);
    }

    @Override
    protected Group doPopulate(Group model, DirectoryGroup entity) {
        return model;
    }

    // We need to override this method because the native identifier of this
    // resource isn't an UUID.
    @Override
    protected Guid asGuidOr404(String id) {
        return null;
    }

}
