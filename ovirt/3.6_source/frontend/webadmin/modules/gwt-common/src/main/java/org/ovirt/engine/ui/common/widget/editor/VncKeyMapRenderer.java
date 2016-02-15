package org.ovirt.engine.ui.common.widget.editor;

import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import com.google.gwt.text.shared.AbstractRenderer;

public class VncKeyMapRenderer extends AbstractRenderer<String> {

    final String globalLayout;
    private final static CommonApplicationMessages messages = AssetProvider.getMessages();

    public VncKeyMapRenderer() {
        globalLayout = (String) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.VncKeyboardLayout);
    }

    @Override
    public String render(String object) {
        if (object == null) {
            return messages.globalVncKeyboardLayoutCaption(globalLayout);
        } else {
            return object;
        }
    };
}
