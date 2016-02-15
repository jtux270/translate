package org.ovirt.engine.api.restapi.resource;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.Test;
import org.ovirt.engine.api.model.Tag;
import org.ovirt.engine.core.common.action.AttachEntityToTagParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Tags;
import org.ovirt.engine.core.common.queries.GetTagsByTemplateIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendTemplateTagResourceTest
    extends AbstractBackendSubResourceTest<Tag, Tags, BackendTemplateTagResource> {

    private static final Guid TEMPLATE_ID = GUIDS[0];
    private static final Guid TAG_ID = GUIDS[1];

    public BackendTemplateTagResourceTest() {
        super(new BackendTemplateTagResource(TEMPLATE_ID, TAG_ID.toString()));
    }

    @Test
    public void testRemove() throws Exception {
        setUpGetTagsExpectations(true);
        setUriInfo(
            setUpActionExpectations(
                VdcActionType.DetachTemplateFromTag,
                AttachEntityToTagParameters.class,
                new String[] { "TagId", "EntitiesId" },
                new Object[] { TAG_ID, asList(TEMPLATE_ID) },
                true,
                true
            )
        );
        verifyRemove(resource.remove());
    }

    @Test
    public void testRemoveCantDo() throws Exception {
        doTestBadRemove(false, true, CANT_DO);
    }

    @Test
    public void testRemoveFailed() throws Exception {
        doTestBadRemove(true, false, FAILURE);
    }

    private void doTestBadRemove(boolean canDo, boolean success, String detail) throws Exception {
        setUpGetTagsExpectations(true);
        setUriInfo(
            setUpActionExpectations(
                VdcActionType.DetachTemplateFromTag,
                AttachEntityToTagParameters.class,
                new String[] { "TagId", "EntitiesId" },
                new Object[] { TAG_ID, asList(TEMPLATE_ID) },
                canDo,
                success));
        try {
            resource.remove();
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    @Test
    public void testRemoveNonExistant() throws Exception{
        setUpGetTagsExpectations(false);
        control.replay();
        try {
            resource.remove();
            fail("expected WebApplicationException");
        }
        catch (WebApplicationException wae) {
            assertNotNull(wae.getResponse());
            assertEquals(wae.getResponse().getStatus(), 404);
        }
    }

    private void setUpGetTagsExpectations(boolean succeed) throws Exception {
        setUpGetEntityExpectations(
            VdcQueryType.GetTagsByTemplateId,
            GetTagsByTemplateIdParameters.class,
            new String[] { "TemplateId" },
            new Object[] { TEMPLATE_ID.toString() },
            succeed? setUpTagsExpectations(): Collections.emptyList()
        );
    }

    private List<Tags> setUpTagsExpectations() {
        List<Tags> tags = new ArrayList<>();
        for (int i = 0; i < GUIDS.length; i++) {
            Tags tag = setUpTagExpectations(GUIDS[i]);
            tags.add(tag);
        }
        return tags;
    }

    private Tags setUpTagExpectations(Guid tagId) {
        Tags mock = control.createMock(Tags.class);
        expect(mock.gettag_id()).andReturn(tagId).anyTimes();
        expect(mock.getparent_id()).andReturn(TEMPLATE_ID).anyTimes();
        return mock;
    }
}
