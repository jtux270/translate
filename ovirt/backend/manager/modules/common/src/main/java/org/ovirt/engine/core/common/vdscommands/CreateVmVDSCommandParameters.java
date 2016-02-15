package org.ovirt.engine.core.common.vdscommands;

import org.ovirt.engine.core.common.action.SysPrepParams;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.compat.Guid;

public class CreateVmVDSCommandParameters extends VdsAndVmIDVDSParametersBase {

    private VM vm;
    private SysPrepParams sysPrepParams;

    public CreateVmVDSCommandParameters() {
    }

    public CreateVmVDSCommandParameters(Guid vdsId, VM vm) {
        super(vdsId, vm.getId());
        this.vm = vm;
    }

    public VM getVm() {
        return vm;
    }

    @Override
    public String toString() {
        return String.format("%s, vm=%s", super.toString(), getVm());
    }

    public SysPrepParams getSysPrepParams() {
        return sysPrepParams;
    }

    public void setSysPrepParams(SysPrepParams sysPrepParams) {
        this.sysPrepParams = sysPrepParams;
    }
}
