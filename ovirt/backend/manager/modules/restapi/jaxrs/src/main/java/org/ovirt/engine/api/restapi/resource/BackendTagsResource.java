package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Tag;
import org.ovirt.engine.api.resource.TagResource;
import org.ovirt.engine.api.resource.TagsResource;
import org.ovirt.engine.core.common.action.TagsActionParametersBase;
import org.ovirt.engine.core.common.action.TagsOperationParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendTagsResource
    extends AbstractBackendCollectionResource<Tag, Tags>
    implements TagsResource {

    public BackendTagsResource() {
        super(Tag.class, Tags.class);
    }

    @Override
    public org.ovirt.engine.api.model.Tags list() {
        List<Tags> tags = getTags();
        tags.add(getRootTag());
        return mapCollection(tags);
    }

    @Override
    @SingleEntityResource
    public TagResource getTagSubResource(String id) {
        return inject(new BackendTagResource(id, this));
    }

    @Override
    public Response add(Tag tag) {
        validateParameters(tag, "name");

        if (isSetParentName(tag)) {
            tag.getParent().getTag().setId(getParentId(tag));
        }

        return performCreate(VdcActionType.AddTag,
                               new TagsOperationParameters(map(tag)),
                               new TagNameResolver(tag.getName()));
    }

    @Override
    public Response performRemove(String id) {
        return performAction(VdcActionType.RemoveTag, new TagsActionParametersBase(asGuid(id)));
    }

    protected List<Tags> getTags() {
        return getBackendCollection(VdcQueryType.GetAllTags, new VdcQueryParametersBase());
    }

    protected Tags getRootTag() {
        return getEntity(Tags.class, VdcQueryType.GetRootTag, new VdcQueryParametersBase(), "root");
    }

    protected org.ovirt.engine.api.model.Tags mapCollection(List<Tags> entities) {
        org.ovirt.engine.api.model.Tags collection = new org.ovirt.engine.api.model.Tags();
        for (Tags entity : entities) {
            collection.getTags().add(addLinks(map(entity)));
        }
        return collection;
    }

    boolean isSetParentName(Tag tag) {
        return tag.isSetParent() && tag.getParent().isSetTag() && tag.getParent().getTag().isSetName();
    }

    String getParentId(Tag tag) {
        return lookupTagByName(tag.getParent().getTag().getName()).gettag_id().toString();
    }

    protected Tags lookupTagByName(String name) {
        return getEntity(Tags.class, VdcQueryType.GetTagByTagName, new NameQueryParameters(name), name);
    }

    protected class TagNameResolver extends EntityIdResolver<Guid> {

        private String name;

        TagNameResolver(String name) {
            this.name = name;
        }

        @Override
        public Tags lookupEntity(Guid id) throws BackendFailureException {
            assert (id == null); // AddTag returns nothing, lookup name instead
            return lookupTagByName(name);
        }
    }

    @Override
    protected Tag doPopulate(Tag model, Tags entity) {
        return model;
    }
}
