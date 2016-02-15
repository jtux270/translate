package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Ignore;
import org.junit.Test;
import org.ovirt.engine.api.model.Tag;
import org.ovirt.engine.api.model.TagParent;
import org.ovirt.engine.api.restapi.resource.BaseBackendResource.WebFaultException;
import org.ovirt.engine.core.common.action.TagsActionParametersBase;
import org.ovirt.engine.core.common.action.TagsOperationParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendTagsResourceTest
    extends AbstractBackendCollectionResourceTest<Tag, Tags, BackendTagsResource> {

    static int PARENT_IDX = NAMES.length-1;
    static Guid PARENT_GUID = GUIDS[PARENT_IDX];

    public BackendTagsResourceTest() {
        super(new BackendTagsResource(), null, "");
    }

    @Test
    @Ignore
    @Override
    public void testQuery() throws Exception {
    }

    @Test
    public void testList_LimitResults() throws Exception {
        UriInfo uriInfo = setUpUriExpectationsWithMax(false);
        setUpQueryExpectations("");
        collection.setUriInfo(uriInfo);
        List<Tag> results = getCollection();
        assertNotNull(collection);
        assertEquals(3, results.size());
    }

    @Test(expected = WebFaultException.class)
    public void testList_LimitResults_BadFormat() throws Exception {
        UriInfo uriInfo = setUpUriExpectationsWithMax(true);
        setUpEntityQueryExpectations(VdcQueryType.GetAllTags,
                                     VdcQueryParametersBase.class,
                                     new String[] { },
                                     new Object[] { },
                                     setUpTags(),
                                     null);
        control.replay();
        collection.setUriInfo(uriInfo);
        getCollection();
        fail("Expected WebFaultException");
    }

    @Test
    public void testRemove() throws Exception {
        setUpGetEntityExcpectations();
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveTag,
                                           TagsActionParametersBase.class,
                                           new String[] { "TagId" },
                                           new Object[] { GUIDS[0] },
                                           true,
                                           true));
        verifyRemove(collection.remove(GUIDS[0].toString()));
    }

    @Test
    public void testRemoveNonExistant() throws Exception{
        setUpGetEntityExpectations(VdcQueryType.GetTagByTagId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { NON_EXISTANT_GUID },
                null);
        control.replay();
        try {
            collection.remove(NON_EXISTANT_GUID.toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(404, wae.getResponse().getStatus());
        }
    }

    private void setUpGetEntityExcpectations() throws Exception {
        setUpGetEntityExpectations(VdcQueryType.GetTagByTagId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[0] },
                getEntity(0));
    }

    @Test
    public void testRemoveCantDo() throws Exception {
        doTestBadRemove(false, true, CANT_DO);
    }

    @Test
    public void testRemoveFailed() throws Exception {
        doTestBadRemove(true, false, FAILURE);
    }

    protected void doTestBadRemove(boolean canDo, boolean success, String detail) throws Exception {
        setUpGetEntityExcpectations();
        setUriInfo(setUpActionExpectations(VdcActionType.RemoveTag,
                                           TagsActionParametersBase.class,
                                           new String[] { "TagId" },
                                           new Object[] { GUIDS[0] },
                                           canDo,
                                           success));
        try {
            collection.remove(GUIDS[0].toString());
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testAddTag() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpCreationExpectations(VdcActionType.AddTag,
                                  TagsOperationParameters.class,
                                  new String[] { "Tag.tag_name", "Tag.parent_id" },
                                  new Object[] { NAMES[0], PARENT_GUID },
                                  true,
                                  true,
                                  null,
                                  VdcQueryType.GetTagByTagName,
                                  NameQueryParameters.class,
                                  new String[] { "Name" },
                                  new Object[] { NAMES[0] },
                                  getEntity(0));

        Response response = collection.add(getModel(0));
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Tag);
        verifyModel((Tag)response.getEntity(), 0);
    }

    @Test
    public void testAddTagNamedParent() throws Exception {
        setUriInfo(setUpBasicUriExpectations());

        setUpEntityQueryExpectations(VdcQueryType.GetTagByTagName,
                                     NameQueryParameters.class,
                                     new String[] { "Name" },
                                     new Object[] { NAMES[PARENT_IDX] },
                                     getEntity(PARENT_IDX));

        setUpCreationExpectations(VdcActionType.AddTag,
                                  TagsOperationParameters.class,
                                  new String[] { "Tag.tag_name", "Tag.parent_id" },
                                  new Object[] { NAMES[0], PARENT_GUID },
                                  true,
                                  true,
                                  null,
                                  VdcQueryType.GetTagByTagName,
                                  NameQueryParameters.class,
                                  new String[] { "Name" },
                                  new Object[] { NAMES[0] },
                                  getEntity(0));

        Tag model = getModel(0);
        model.getParent().getTag().setId(null);
        model.getParent().getTag().setName(NAMES[PARENT_IDX]);

        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Tag);
        verifyModel((Tag)response.getEntity(), 0);
    }

    @Test
    public void testAddTagNoParent() throws Exception {
        setUriInfo(setUpBasicUriExpectations());

        Tags entity = getEntity(0);
        entity.setparent_id(Guid.Empty);

        setUpCreationExpectations(VdcActionType.AddTag,
                                  TagsOperationParameters.class,
                                  new String[] { "Tag.tag_name", "Tag.parent_id" },
                                  new Object[] { NAMES[0], Guid.Empty },
                                  true,
                                  true,
                                  null,
                                  VdcQueryType.GetTagByTagName,
                                  NameQueryParameters.class,
                                  new String[] { "Name" },
                                  new Object[] { NAMES[0] },
                                  entity);

        Tag model = getModel(0);
        model.setParent(null);
        Response response = collection.add(model);
        assertEquals(201, response.getStatus());
        assertTrue(response.getEntity() instanceof Tag);
        verifyModel((Tag)response.getEntity(), 0, Guid.Empty.toString());
    }

    @Test
    public void testAddIncompleteParameters() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        control.replay();
        try {
            collection.add(new Tag());
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
             verifyIncompleteException(wae, "Tag", "add", "name");
        }
    }

    @Test
    public void testAddTagCantDo() throws Exception {
        doTestBadAddTag(false, true, CANT_DO);
    }

    @Test
    public void testAddTagFailure() throws Exception {
        doTestBadAddTag(true, false, FAILURE);
    }

    private void doTestBadAddTag(boolean canDo, boolean success, String detail) throws Exception {
        setUriInfo(setUpActionExpectations(VdcActionType.AddTag,
                                           TagsOperationParameters.class,
                                           new String[] { "Tag.tag_name", "Tag.parent_id" },
                                           new Object[] { NAMES[0], PARENT_GUID },
                                           canDo,
                                           success));
        try {
            collection.add(getModel(0));
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        assertEquals("", query);

        setUpEntityQueryExpectations(VdcQueryType.GetAllTags,
                                     VdcQueryParametersBase.class,
                                     new String[] { },
                                     new Object[] { },
                                     setUpTags(),
                                     failure);

        if (failure == null) {
            setUpEntityQueryExpectations(VdcQueryType.GetRootTag,
                                         VdcQueryParametersBase.class,
                                         new String[] { },
                                         new Object[] { },
                                         setUpRootTag());
        }

        control.replay();
    }

    @Override
    protected Tags getEntity(int index) {
        return new Tags(DESCRIPTIONS[index], PARENT_GUID, false, GUIDS[index], NAMES[index]);
    }

    static Tags setUpRootTag() {
        return new Tags("root", null, true, Guid.Empty, "root");
    }

    static List<Tags> setUpTags() {
        List<Tags> tags = new ArrayList<Tags>();
        for (int i = 0; i < NAMES.length; i++) {
            tags.add(new Tags(DESCRIPTIONS[i], PARENT_GUID, false, GUIDS[i], NAMES[i]));
        }
        return tags;
    }

    @Override
    protected List<Tag> getCollection() {
        return collection.list().getTags();
    }

    static Tag getModel(int index) {
        return getModel(index, true);
    }

    static Tag getModel(int index, boolean includeParent) {
        Tag model = new Tag();
        model.setId(GUIDS[index].toString());
        model.setName(NAMES[index]);
        model.setDescription(DESCRIPTIONS[index]);
        if (includeParent) {
            model.setParent(new TagParent());
            model.getParent().setTag(new Tag());
            model.getParent().getTag().setId(PARENT_GUID.toString());
        }
        return model;
    }

    @Override
    protected void verifyCollection(List<Tag> collection) throws Exception {
        assertNotNull(collection);
        assertEquals(NAMES.length + 1, collection.size());
        verifyRoot(collection.get(NAMES.length));
        collection.remove(NAMES.length);
        super.verifyCollection(collection);
    }

    @Override
    protected void verifyModel(Tag model, int index) {
        verifyModel(model, index, PARENT_GUID.toString());
    }

    protected void verifyModel(Tag model, int index, String parentId) {
        super.verifyModel(model, index);
        verifyParent(model, parentId);
    }

    static void verifyParent(Tag model, String parentId) {
        assertNotNull(model.getParent());
        assertNotNull(model.getParent().getTag());
        assertEquals(parentId, model.getParent().getTag().getId());
    }

    protected void verifyRoot(Tag root) {
        assertEquals(Guid.Empty.toString(), root.getId());
        assertEquals("root", root.getName());
        assertEquals("root", root.getDescription());
        assertNull(root.getParent());
        verifyLinks(root);
    }

    @SuppressWarnings("unchecked")
    protected UriInfo setUpUriExpectationsWithMax(boolean badFormat) {
        UriInfo uriInfo = control.createMock(UriInfo.class);
        expect(uriInfo.getBaseUri()).andReturn(URI.create(URI_BASE)).anyTimes();
        List<PathSegment> psl = new ArrayList<PathSegment>();

        PathSegment ps = control.createMock(PathSegment.class);
        MultivaluedMap<String, String> matrixParams = control.createMock(MultivaluedMap.class);

        expect(matrixParams.isEmpty()).andReturn(false).anyTimes();
        expect(matrixParams.containsKey("max")).andReturn(true).anyTimes();

        List<String> matrixParamsList = control.createMock(List.class);
        expect(matrixParams.get("max")).andReturn(matrixParamsList).anyTimes();

        expect(ps.getMatrixParameters()).andReturn(matrixParams).anyTimes();
        expect(matrixParamsList.size()).andReturn(1).anyTimes();
        expect(matrixParamsList.get(0)).andReturn(badFormat ? "bla3" : "2").anyTimes();

        psl.add(ps);

        expect(uriInfo.getPathSegments()).andReturn(psl).anyTimes();
        return uriInfo;
    }
}
