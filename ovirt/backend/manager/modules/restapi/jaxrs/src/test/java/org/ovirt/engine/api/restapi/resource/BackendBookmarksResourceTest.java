package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.ovirt.engine.api.model.Bookmark;
import org.ovirt.engine.core.common.action.BookmarksOperationParameters;
import org.ovirt.engine.core.common.action.BookmarksParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendBookmarksResourceTest extends AbstractBackendCollectionResourceTest<Bookmark,
    org.ovirt.engine.core.common.businessentities.Bookmark, BackendBookmarksResource> {

    static final String[] VALUES = {"host.name='blah'", "vms.status='down'", "template.description='something'"};

    public BackendBookmarksResourceTest() {
        super(new BackendBookmarksResource(), null, "");
    }

    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testRemove() throws Exception {
        setUpGetEntityExpectations(GUIDS[0], getEntity(0));
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveBookmark,
                BookmarksParametersBase.class,
                new String[] { "BookmarkId" },
                new Object[] { GUIDS[0] },
                true,
                true));
        verifyRemove(collection.remove(GUIDS[0].toString()));
    }

    @Test
    public void testRemoveNonExistant() throws Exception{
        setUpGetEntityExpectations(NON_EXISTANT_GUID, null);
        control.replay();
        try {
            collection.remove(NON_EXISTANT_GUID.toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(404, wae.getResponse().getStatus());
        }
    }

    @Test
    public void testRemoveCantDo() throws Exception {
        doTestBadRemove(false, true, CANT_DO);
    }

    @Test
    public void testRemoveFailed() throws Exception {
        doTestBadRemove(true, false, FAILURE);
    }

    @Test
    public void testAddBookmark() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpCreationExpectations(VdcActionType.AddBookmark, BookmarksOperationParameters.class,
                new String[] { "Bookmark.bookmark_name", "Bookmark.bookmark_value" },
                new Object[] { NAMES[0], VALUES[0] }, true, true, null, VdcQueryType.GetBookmarkByBookmarkName,
                NameQueryParameters.class, new String[] { "Name" }, new Object[] { NAMES[0] }, getEntity(0));

        Response response = collection.add(getModel(0));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Bookmark);
        verifyModel((Bookmark)response.getEntity(), 0);
    }

    @Test
    public void testAddIncompleteParameters() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(new Bookmark());
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
             verifyIncompleteException(wae, "Bookmark", "add", "name");
        }
    }

    @Test
    public void testAddBookmarkCantDo() throws Exception {
        doTestBadAddBookmark(false, true, CANT_DO);
    }

    @Test
    public void testAddBookmarkFailure() throws Exception {
        doTestBadAddBookmark(true, false, FAILURE);
    }

    /*************************************************************************************
     * Helpers.
     *************************************************************************************/

    private void doTestBadAddBookmark(boolean canDo, boolean success, String detail) throws Exception {
        setUriInfo(setUpActionExpectations(VdcActionType.AddBookmark, BookmarksOperationParameters.class,
                new String[] { "Bookmark.bookmark_name", "Bookmark.bookmark_value" },
                new Object[] { NAMES[0], VALUES[0] }, canDo, success));
        try {
            collection.add(getModel(0));
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    protected void doTestBadRemove(boolean canDo, boolean success, String detail) throws Exception {
        setUpGetEntityExpectations(GUIDS[0], getEntity(0));
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveBookmark, BookmarksParametersBase.class,
                new String[] { "BookmarkId" }, new Object[] { GUIDS[0] }, canDo, success));
        try {
            collection.remove(GUIDS[0].toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    private void setUpGetEntityExpectations(Guid guid,
            org.ovirt.engine.core.common.businessentities.Bookmark entity) throws Exception {
        setUpGetEntityExpectations(VdcQueryType.GetBookmarkByBookmarkId,
                IdQueryParameters.class, new String[] { "Id" }, new Object[] { guid }, entity);
    }

    @Override
    protected List<Bookmark> getCollection() {
        return collection.list().getBookmarks();
    }

    @Override
    protected org.ovirt.engine.core.common.businessentities.Bookmark getEntity(int index) {
        org.ovirt.engine.core.common.businessentities.Bookmark bookmark =
                new org.ovirt.engine.core.common.businessentities.Bookmark();
        bookmark.setbookmark_id(GUIDS[index]);
        bookmark.setbookmark_name(NAMES[index]);
        bookmark.setbookmark_value(VALUES[index]);
        return bookmark;
    }

    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetAllBookmarks,
                                     VdcQueryParametersBase.class,
                                     new String[] { },
                                     new Object[] { },
                                     setUpBookmarks(),
                                     failure);
        control.replay();
    }

    static List<org.ovirt.engine.core.common.businessentities.Bookmark> setUpBookmarks() {
        List<org.ovirt.engine.core.common.businessentities.Bookmark> bookmarks =
                new ArrayList<org.ovirt.engine.core.common.businessentities.Bookmark>();
        for (int i = 0; i < NAMES.length; i++) {
            org.ovirt.engine.core.common.businessentities.Bookmark bookmark =
                    new org.ovirt.engine.core.common.businessentities.Bookmark();
            bookmark.setbookmark_id(GUIDS[i]);
            bookmark.setbookmark_name(NAMES[i]);
            bookmark.setbookmark_value(VALUES[i]);
            bookmarks.add(bookmark);
        }
        return bookmarks;
    }

    @Override
    protected void verifyModel(Bookmark model, int index) {
        assertEquals(GUIDS[index].toString(), model.getId());
        assertEquals(NAMES[index], model.getName());
        assertEquals(VALUES[index], model.getValue());
        verifyLinks(model);
    }

    static Bookmark getModel(int index) {
        Bookmark model = new Bookmark();
        model.setId(GUIDS[index].toString());
        model.setName(NAMES[index]);
        model.setValue(VALUES[index]);
        return model;
    }
}
