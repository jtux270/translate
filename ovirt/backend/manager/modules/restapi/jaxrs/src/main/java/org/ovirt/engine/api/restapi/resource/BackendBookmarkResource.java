package org.ovirt.engine.api.restapi.resource;

import org.ovirt.engine.api.model.Bookmark;
import org.ovirt.engine.api.resource.BookmarkResource;
import org.ovirt.engine.core.common.action.BookmarksOperationParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendBookmarkResource extends AbstractBackendSubResource<Bookmark,
    org.ovirt.engine.core.common.businessentities.Bookmark> implements BookmarkResource {

    protected BackendBookmarkResource(String id) {
        super(id, Bookmark.class, org.ovirt.engine.core.common.businessentities.Bookmark.class);
    }

    @Override
    public Bookmark get() {
        return performGet(VdcQueryType.GetBookmarkByBookmarkId, new IdQueryParameters(guid));
    }

    @Override
    public Bookmark update(Bookmark incoming) {
        return performUpdate(incoming, new QueryIdResolver<Guid>(VdcQueryType.GetBookmarkByBookmarkId,
                IdQueryParameters.class), VdcActionType.UpdateBookmark, new UpdateParametersProvider());
    }

    protected class UpdateParametersProvider implements ParametersProvider<Bookmark,
        org.ovirt.engine.core.common.businessentities.Bookmark> {

        @Override
        public VdcActionParametersBase getParameters(Bookmark incoming,
                org.ovirt.engine.core.common.businessentities.Bookmark entity) {
            return new BookmarksOperationParameters(map(incoming, entity));
        }
    }

    @Override
    protected Bookmark doPopulate(Bookmark model, org.ovirt.engine.core.common.businessentities.Bookmark entity) {
        return model;
    }

}
