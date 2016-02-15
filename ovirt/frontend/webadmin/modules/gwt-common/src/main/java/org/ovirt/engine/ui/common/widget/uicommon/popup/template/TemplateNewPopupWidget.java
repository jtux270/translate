package org.ovirt.engine.ui.common.widget.uicommon.popup.template;

import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.hiddenField;

import com.google.gwt.event.shared.EventBus;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractVmPopupWidget;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfigMap;

import com.google.gwt.core.client.GWT;

public class TemplateNewPopupWidget extends AbstractVmPopupWidget {

    interface ViewIdHandler extends ElementIdHandler<TemplateNewPopupWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    public TemplateNewPopupWidget(CommonApplicationConstants constants,
            CommonApplicationResources resources,
            CommonApplicationMessages messages,
            CommonApplicationTemplates applicationTemplates,
            EventBus eventBus) {
        super(constants, resources, messages, applicationTemplates, eventBus);
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

    @Override
    protected PopupWidgetConfigMap createWidgetConfiguration() {
        return super.createWidgetConfiguration().
                putOne(logicalNetworksEditorPanel, hiddenField()).
                putAll(poolSpecificFields(), hiddenField()).
                putOne(templateEditor, hiddenField()).
                putOne(instanceTypesEditor, hiddenField()).
                update(resourceAllocationTab, hiddenField());
    }

}
