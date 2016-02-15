package org.ovirt.engine.ui.common.widget.uicommon.popup.vm;

import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.hiddenField;
import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.simpleField;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractVmPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;

public class VmPopupWidget extends AbstractVmPopupWidget {

    interface ViewIdHandler extends ElementIdHandler<VmPopupWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    public VmPopupWidget(EventBus eventBus) {
        super(eventBus);
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

    @Override
    public void edit(UnitVmModel unitVmModel) {
        super.edit(unitVmModel);

        if (unitVmModel.isVmAttachedToPool()) {
            // this just disables it, does not hides it
            specificHost.setEnabled(false);
        }

    }

    @Override
    protected PopupWidgetConfigMap createWidgetConfiguration() {
        PopupWidgetConfigMap widgetConfiguration = super.createWidgetConfiguration().
                putAll(poolSpecificFields(), hiddenField()).
                putOne(baseTemplateEditor, hiddenField()).
                update(templateVersionNameEditor, hiddenField()).
                update(instanceImagesEditor, simpleField());

        updateOrAddToWidgetConfiguration(widgetConfiguration, detachableWidgets(), UpdateToDetachable.INSTANCE);
        updateOrAddToWidgetConfiguration(widgetConfiguration, adminOnlyWidgets(), UpdateToAdminOnly.INSTANCE);

        return widgetConfiguration;
    }
}
