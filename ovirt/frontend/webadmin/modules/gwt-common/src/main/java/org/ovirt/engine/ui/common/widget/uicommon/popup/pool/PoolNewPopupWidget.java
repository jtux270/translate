package org.ovirt.engine.ui.common.widget.uicommon.popup.pool;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.text.shared.Parser;
import java.text.ParseException;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.IntegerEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.ToStringEntityModelRenderer;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractVmPopupWidget;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfigMap;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.hiddenField;
import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.simpleField;

public class PoolNewPopupWidget extends AbstractVmPopupWidget {

    interface ViewIdHandler extends ElementIdHandler<PoolNewPopupWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    public PoolNewPopupWidget(CommonApplicationConstants constants,
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
    public void edit(final UnitVmModel object) {
        super.edit(object);
        initTabAvailabilityListeners(object);

        if (object.getIsNew()) {
            object.getNumOfDesktops().setEntity(1);
            prestartedVmsEditor.setEnabled(false);
        }
    }

    @Override
    protected void createNumOfDesktopEditors() {
        numOfVmsEditor = new IntegerEntityModelTextBoxEditor();
        incraseNumOfVmsEditor = new EntityModelTextBoxOnlyEditor<Integer>(
                new ToStringEntityModelRenderer<Integer>(), new Parser<Integer>() {

            @Override
            public Integer parse(CharSequence text) throws ParseException {
                // forwards to the currently active editor
                return numOfVmsEditor.asEditor().getValue();
            }

        });
    }

    private void initTabAvailabilityListeners(final UnitVmModel pool) {
        // TODO should be handled by the core framework
        pool.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).propertyName;
                if ("IsPoolTabValid".equals(propName)) { //$NON-NLS-1$
                    poolTab.markAsInvalid(null);
                }
            }
        });
    }

    @Override
    protected PopupWidgetConfigMap createWidgetConfiguration() {
        PopupWidgetConfigMap widgetConfiguration = super.createWidgetConfiguration().
                update(highAvailabilityTab, hiddenField()).
                update(spiceProxyEditor, simpleField().visibleInAdvancedModeOnly()).
                update(spiceProxyEnabledCheckboxWithInfoIcon, simpleField().visibleInAdvancedModeOnly()).
                update(spiceProxyOverrideEnabledEditor, simpleField().visibleInAdvancedModeOnly()).
                putOne(isStatelessEditor, hiddenField()).
                putOne(isRunAndPauseEditor, hiddenField()).
                putOne(editPoolEditVmsPanel, hiddenField()).
                putOne(editPoolIncraseNumOfVmsPanel, hiddenField()).
                putOne(logicalNetworksEditorPanel, hiddenField()).
                putOne(editPoolEditMaxAssignedVmsPerUserPanel, hiddenField()).
                update(templateVersionNameEditor, hiddenField()).
                putAll(detachableWidgets(), simpleField().detachable().visibleInAdvancedModeOnly());

        updateOrAddToWidgetConfiguration(widgetConfiguration, detachableWidgets(), UpdateToDetachable.INSTANCE);

        return widgetConfiguration;
    }

}
