package org.ovirt.engine.ui.common.widget.uicommon.vm;

import org.ovirt.engine.core.common.businessentities.OsType;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.common.uicommon.model.ModelProvider;
import org.ovirt.engine.ui.common.widget.form.FormItem;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundFormWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGuestInfoModel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;

public class VmGuestInfoModelForm extends AbstractModelBoundFormWidget<VmGuestInfoModel> {

    interface Driver extends SimpleBeanEditorDriver<VmGuestInfoModel, VmGuestInfoModelForm> {
    }

    private final Driver driver = GWT.create(Driver.class);

    TextBoxLabel guestUserName = new TextBoxLabel();
    TextBoxLabel guestOsArch = new TextBoxLabel();
    TextBoxLabel guestOsType = new TextBoxLabel();
    TextBoxLabel guestOsNamedVersion = new TextBoxLabel();
    TextBoxLabel guestOsKernelVersion = new TextBoxLabel();
    TextBoxLabel guestOsTimezone = new TextBoxLabel();
    TextBoxLabel consoleUserName = new TextBoxLabel();
    TextBoxLabel clientIp = new TextBoxLabel();

    private final static CommonApplicationConstants constants = AssetProvider.getConstants();

    public VmGuestInfoModelForm(ModelProvider<VmGuestInfoModel> modelProvider) {
        super(modelProvider, 3, 4);
        driver.initialize(this);

        // First row - OS Info
        formBuilder.addFormItem(new FormItem(constants.guestOsType(), guestOsType, 0, 0)
            .withDefaultValue(constants.unknown(), new FormItem.DefaultValueCondition() {
                @Override
                public boolean showDefaultValue() {
                    return getModel().getGuestOsType().equals(OsType.Other.toString());
                }
            }));
        formBuilder.addFormItem(new FormItem(constants.guestOsArchitecture(), guestOsArch, 1, 0)
            .withDefaultValue(constants.unknown(), new FormItem.DefaultValueCondition() {
                @Override
                public boolean showDefaultValue() {
                    return getModel().getGuestOsType().equals(OsType.Other.toString());
                }
            }));
        formBuilder.addFormItem(new FormItem(constants.guestOperatingSystem(), guestOsNamedVersion, 2, 0)
            .withDefaultValue(constants.unknown(), new FormItem.DefaultValueCondition() {
                @Override
                public boolean showDefaultValue() {
                    return getModel().getGuestOsType().equals(OsType.Other.toString());
                }
            }));
        // The kernel version is only reported and displayed for Linux based systems
        formBuilder.addFormItem(new FormItem(constants.guestOsKernelInfo(), guestOsKernelVersion, 3, 0) {
            @Override
            public boolean getIsAvailable() {
                return getModel().getGuestOsType().equals(OsType.Linux.toString());
            }
        });

        // Second row - Timezone Info
        formBuilder.addFormItem(new FormItem(constants.guestOsTimezone(), guestOsTimezone, 0, 1)
            .withDefaultValue(constants.unknown(), new FormItem.DefaultValueCondition() {
                @Override
                public boolean showDefaultValue() {
                    return getModel().getGuestOsType().equals(OsType.Other.toString());
                }
            }));

        // Third row - Logged In User & Console Info
        formBuilder.addFormItem(new FormItem(constants.loggedInUserVm(), guestUserName, 0, 2));
        formBuilder.addFormItem(new FormItem(constants.consoleConnectedUserVm(), consoleUserName, 1, 2));
        formBuilder.addFormItem(new FormItem(constants.consoleConnectedClientIp(), clientIp, 2, 2));
    }

    @Override
    protected void doEdit(VmGuestInfoModel model) {
        driver.edit(model);
    }

}
