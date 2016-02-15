package org.ovirt.engine.api.restapi.resource;


public class BackendHostNetworkAttachmentResourceTest
        extends AbstractBackendNetworkAttachmentResourceTest<BackendHostNetworkAttachmentsResource, BackendHostNetworkAttachmentResource> {

    public BackendHostNetworkAttachmentResourceTest() {
        super(new BackendHostNetworkAttachmentResource(GUIDS[0].toString(),
                new BackendHostNetworkAttachmentsResource(hostId)));
    }

    @Override
    protected void createReourceWithBadGuid() {
        new BackendHostNetworkAttachmentResource("foo", new BackendHostNetworkAttachmentsResource(hostId));
    }
}
