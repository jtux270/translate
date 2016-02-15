package org.ovirt.engine.ui.common.widget.label;

import org.ovirt.engine.core.common.businessentities.MigrateOnErrorOptions;
import org.ovirt.engine.ui.common.CommonApplicationConstants;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.user.client.ui.ValueLabel;

public class ResiliencePolicyLabel extends ValueLabel<MigrateOnErrorOptions> {

    public ResiliencePolicyLabel(final CommonApplicationConstants constants) {
        super(new AbstractRenderer<MigrateOnErrorOptions>() {
            @Override
            public String render(MigrateOnErrorOptions migrateOnErrorOptions) {
                switch (migrateOnErrorOptions) {
                case YES:
                    return constants.yes();
                case NO:
                    return constants.no();
                case HA_ONLY:
                    return constants.highPriorityOnly();
                default:
                    return null;
                }
            }
        });
    }
}
