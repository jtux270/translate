package org.ovirt.engine.core.vdsbroker;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.dal.VdcCommandBase;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSExceptionBase;

public abstract class VDSCommandBase<P extends VDSParametersBase> extends VdcCommandBase {
    private P _parameters;

    public P getParameters() {
        return _parameters;
    }

    protected VDSReturnValue _returnValue = null;

    public VDSReturnValue getVDSReturnValue() {
        return _returnValue;
    }

    public void setVDSReturnValue(VDSReturnValue value) {
        _returnValue = value;
    }

    @Override
    public Object getReturnValue() {
        return getVDSReturnValue().getReturnValue();
    }

    @Override
    public void setReturnValue(Object value) {
        getVDSReturnValue().setReturnValue(value);
    }

    public VDSCommandBase(P parameters) {
        _parameters = parameters;
    }

    @Override
    public String toString() {
        String addInfo = getAdditionalInformation();
        return String.format("%s(%s %s)", super.toString(),
                (!addInfo.isEmpty() ? addInfo + "," : StringUtils.EMPTY),
                (getParameters() != null ? getParameters().toString() : "null"));
    }

    @Override
    protected void executeCommand() {
        try {
            // creating ReturnValue object since execute can be called more than once (failover)
            // and we want returnValue clean from last run.
            _returnValue = new VDSReturnValue();
            getVDSReturnValue().setSucceeded(true);
            executeVDSCommand();
        } catch (RuntimeException ex) {
            setVdsRuntimeError(ex);
        }
    }

    protected void setVdsRuntimeError(RuntimeException ex) {
        getVDSReturnValue().setSucceeded(false);
        getVDSReturnValue().setExceptionString(ex.toString());
        getVDSReturnValue().setExceptionObject(ex);

        VDSExceptionBase vdsExp = (VDSExceptionBase) ((ex instanceof VDSExceptionBase) ? ex : null);
        // todo: consider adding unknown vds error in case of non
        // VDSExceptionBase exception
        if (vdsExp != null) {
            if (vdsExp.getVdsError() != null) {
                getVDSReturnValue().setVdsError(((VDSExceptionBase) ex).getVdsError());
            } else if (vdsExp.getCause() instanceof VDSExceptionBase) {
                getVDSReturnValue().setVdsError(((VDSExceptionBase) vdsExp.getCause()).getVdsError());
            }
        }

        logException(ex);
    }

    private void logException(RuntimeException ex) {
        log.errorFormat("Command {0} execution failed. Exception: {1}", this, ExceptionUtils.getMessage(ex));
        if (log.isDebugEnabled()) {
            log.debugFormat("Detailed stacktrace:", ex);
        }
    }

    protected String getAdditionalInformation() {
        return StringUtils.EMPTY;
    }

    protected abstract void executeVDSCommand();
}
