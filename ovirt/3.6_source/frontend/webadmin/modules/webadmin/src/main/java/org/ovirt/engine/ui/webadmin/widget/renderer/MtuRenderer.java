package org.ovirt.engine.ui.webadmin.widget.renderer;

import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import com.google.gwt.text.shared.AbstractRenderer;

public class MtuRenderer extends AbstractRenderer<Integer> {

    private static int defaultMtu =
            (Integer) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.DefaultMTU);

    private final static ApplicationMessages messages = AssetProvider.getMessages();

    @Override
    public String render(Integer mtu) {
        return mtu == 0 ? messages.defaultMtu(defaultMtu) : mtu.toString();
    }

}
