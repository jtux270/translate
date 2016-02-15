package org.ovirt.engine.core.common.action;

import javax.validation.Valid;

import org.ovirt.engine.core.common.businessentities.VmTemplate;

public class UpdateVmTemplateParameters extends VmTemplateParametersBase {
    private static final long serialVersionUID = 7250355162926369307L;
    @Valid
    private VmTemplate _vmTemplate;
    /*
     * This parameter is used to decide if to create or remove sound device
     * if it is null then the current configuration will remain
     */
    private Boolean soundDeviceEnabled;

    /*
     * This parameter is used to decide if to create or remove console device
     * if it is null then the current configuration will remain
     */
    private Boolean consoleEnabled;

    public UpdateVmTemplateParameters(VmTemplate vmTemplate) {
        _vmTemplate = vmTemplate;
    }

    public VmTemplate getVmTemplateData() {
        return _vmTemplate;
    }

    public UpdateVmTemplateParameters() {
    }

    public Boolean isSoundDeviceEnabled() {
        return soundDeviceEnabled;
    }

    public void setSoundDeviceEnabled(boolean soundDeviceEnabled) {
        this.soundDeviceEnabled = soundDeviceEnabled;
    }

    public Boolean isConsoleEnabled() {
        return consoleEnabled;
    }

    public void setConsoleEnabled(Boolean consoleEnabled) {
        this.consoleEnabled = consoleEnabled;
    }

}
