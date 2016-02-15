package org.ovirt.engine.ui.webadmin;

import org.ovirt.engine.ui.common.utils.DynamicMessages;

import com.google.gwt.core.client.GWT;

public class ApplicationDynamicMessages extends DynamicMessages {

    private static final ApplicationConstants constants = GWT.create(ApplicationConstants.class);

    public ApplicationDynamicMessages() {
        super();
        addFallback(DynamicMessageKey.APPLICATION_TITLE, constants.applicationTitle());
        addFallback(DynamicMessageKey.VERSION_ABOUT, constants.ovirtVersionAbout());
        addFallback(DynamicMessageKey.COPY_RIGHT_NOTICE, constants.copyRightNotice());
        addFallback(DynamicMessageKey.DOC, constants.engineWebAdminDoc());
        addFallback(DynamicMessageKey.FEEDBACK_LINK_LABEL, constants.feedbackMessage());
        addFallback(DynamicMessageKey.FEEDBACK_LINK_TOOLTIP, constants.feedbackTooltip());
        addFallback(DynamicMessageKey.GUIDE_LINK_LABEL, constants.guideLinkLabel());
        addFallback(DynamicMessageKey.VENDOR_URL, constants.vendorUrl());
    }

}
