package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Tag;
import org.ovirt.engine.api.model.User;
import org.ovirt.engine.api.resource.AssignedTagResource;
import org.ovirt.engine.core.common.action.AttachEntityToTagParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.queries.GetTagsByUserIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendUserTagResource extends AbstractBackendSubResource<Tag, Tags> implements AssignedTagResource {
    private Guid userId;

    public BackendUserTagResource(Guid userId, String tagId) {
        super(tagId, Tag.class, Tags.class);
        this.userId = userId;
    }

    @Override
    public Tag get() {
        List<Tags> tags = getBackendCollection(
            Tags.class,
            VdcQueryType.GetTagsByUserId,
            new GetTagsByUserIdParameters(userId.toString())
        );
        for (Tags tag : tags) {
            if (tag.gettag_id().equals(guid)) {
                return addLinks(populate(map(tag, null), tag));
            }
        }
        return notFound();
    }

    @Override
    protected Tag addParents(Tag tag) {
        User user = new User();
        user.setId(userId.toString());
        tag.setUser(user);
        return tag;
    }

    @Override
    public Response remove() {
        get();
        return performAction(VdcActionType.DetachUserFromTag, new AttachEntityToTagParameters(guid, asList(userId)));
    }
}
