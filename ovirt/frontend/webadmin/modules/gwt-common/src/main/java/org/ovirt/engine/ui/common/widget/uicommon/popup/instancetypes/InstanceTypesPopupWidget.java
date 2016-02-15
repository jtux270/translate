package org.ovirt.engine.ui.common.widget.uicommon.popup.instancetypes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractVmPopupWidget;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfigMap;
import static org.ovirt.engine.ui.common.widget.uicommon.popup.vm.PopupWidgetConfig.hiddenField;

public class InstanceTypesPopupWidget extends AbstractVmPopupWidget {

    interface ViewIdHandler extends ElementIdHandler<InstanceTypesPopupWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    public InstanceTypesPopupWidget(CommonApplicationConstants constants,
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
    protected void initialize() {
        super.initialize();

        mainTabPanel.setHeaderVisible(false);
    }

    @Override
    protected PopupWidgetConfigMap createWidgetConfiguration() {
        return super.createWidgetConfiguration().
                putAll(poolSpecificFields(), hiddenField()).
                putOne(isDeleteProtectedEditor, hiddenField()).
                putOne(isStatelessEditor, hiddenField()).
                putOne(isRunAndPauseEditor, hiddenField()).
                putOne(commentEditor, hiddenField()).
                putOne(vmTypeEditor, hiddenField()).
                putOne(oSTypeEditor, hiddenField()).
                putOne(templateEditor, hiddenField()).
                putOne(initialRunTab, hiddenField()).
                putOne(expander, hiddenField()).
                putOne(allowConsoleReconnectEditor, hiddenField()).
                putOne(cdAttachedEditor, hiddenField()).
                putOne(cdImageEditor, hiddenField()).
                putOne(refreshButton, hiddenField()).
                putOne(timeZoneEditor, hiddenField()).
                putOne(generalLabel, hiddenField()).
                putOne(quotaEditor, hiddenField()).
                putOne(cpuAllocationPanel, hiddenField()).
                putOne(vncKeyboardLayoutEditor, hiddenField()).
                putOne(storageAllocationPanel, hiddenField()).
                putOne(customPropertiesTab, hiddenField()).
                putOne(ssoMethodLabel, hiddenField()).
                putOne(ssoMethodNone, hiddenField()).
                putOne(ssoMethodGuestAgent, hiddenField()).
                putOne(hostCpuEditor, hiddenField()).
                putOne(templateVersionNameEditor, hiddenField()).
                putOne(bootMenuEnabledEditor, hiddenField()).
                putOne(serialNumberPolicyEditor, hiddenField()).
                putOne(timeZoneEditorWithInfo, hiddenField()).
                putOne(startRunningOnPanel, hiddenField()).
                putOne(spiceCopyPasteEnabledEditor, hiddenField()).
                putOne(spiceFileTransferEnabledEditor, hiddenField());
    }
}
