package org.ovirt.engine.core.vdsbroker.vdsbroker;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.errors.VDSError;
import org.ovirt.engine.core.common.vdscommands.VdsIdVDSCommandParametersBase;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.vdsbroker.ResourceManager;
import org.ovirt.engine.core.vdsbroker.VdsManager;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcRunTimeException;

public abstract class VdsBrokerCommand<P extends VdsIdVDSCommandParametersBase> extends BrokerCommandBase<P> {
    private final IVdsServer vdsBroker;
    private VdsStatic vdsStatic;
    private VDS vds;

    @Inject
    private AuditLogDirector auditLogDirector;

    @Inject
    Event<VDSNetworkException> networkError;
    /**
     * Construct the command using the parameters and the {@link VDS} which is loaded from the DB.
     *
     * @param parameters
     *            The parameters of the command.
     */
    public VdsBrokerCommand(P parameters) {
        super(parameters);
        vdsBroker = initializeVdsBroker(parameters.getVdsId());
    }

    /**
     * Construct the command using the parameters and the {@link VDS} which is passed.
     *
     * @param parameters
     *            The parameters of the command.
     * @param vds
     *            The host to use in the command.
     */
    protected VdsBrokerCommand(P parameters, VDS vds) {
        super(parameters);
        this.vdsBroker = initializeVdsBroker(parameters.getVdsId());
        this.vds = vds;
        this.vdsStatic = vds.getStaticData();
    }

    protected IVdsServer initializeVdsBroker(Guid vdsId) {
        VdsManager vdsmanager = ResourceManager.getInstance().GetVdsManager(vdsId);
        if (vdsmanager == null) {
            throw new EngineException(EngineError.RESOURCE_MANAGER_VDS_NOT_FOUND,
                    String.format("Vds with id: %1$s was not found", vdsId));
        }

        setVdsAndVdsStatic(vdsmanager.getCopyVds());
        return vdsmanager.getVdsProxy();
    }

    protected IVdsServer getBroker() {
        return vdsBroker;
    }

    @Override
    protected VDSExceptionBase createDefaultConcreteException(String errorMessage) {
        return new VDSErrorException(errorMessage);
    }

    @Override
    protected String getAdditionalInformation() {
        if (getAndSetVdsStatic() != null) {
            return String.format("HostName = %1$s", getAndSetVdsStatic().getName());
        } else {
            return super.getAdditionalInformation();
        }
    }

    protected VdsStatic getAndSetVdsStatic() {
        if (vdsStatic == null) {
            vdsStatic = getDbFacade().getVdsStaticDao().get(getParameters().getVdsId());
        }
        return vdsStatic;
    }

    protected VDS getVds() {
        return vds;
    }

    protected void setVdsAndVdsStatic(VDS vds) {
        this.vds = vds;
        this.vdsStatic = vds.getStaticData();
    }

    protected DbFacade getDbFacade() {
        return DbFacade.getInstance();
    }

    @Override
    protected void executeVDSCommand() {
        try {
            executeVdsBrokerCommand();
        } catch (VDSNetworkException ex) {
            printReturnValue();
            updateNetworkException(ex, ex.getMessage());
            networkError.fire(ex);
            throw ex;
        } catch (VDSExceptionBase ex) {
            printReturnValue();
            throw ex;
        } catch (XmlRpcRunTimeException ex) {
            VDSNetworkException networkException = createNetworkException(ex);
            printReturnValue();
            networkError.fire(networkException);
            throw networkException;
        }

        // TODO: look for invalid certificates error handling
        catch (RuntimeException e) {
            printReturnValue();
            if (getAndSetVdsStatic() == null) {
                log.error("Failed in '{}' method, for vds id: '{}': {}",
                        getCommandName(), getParameters().getVdsId(), e.getMessage());
            } else {
                log.error("Failed in '{}' method, for vds: '{}'; host: '{}': {}",
                        getCommandName(), getAndSetVdsStatic().getName(), getAndSetVdsStatic().getHostName(),
                        e.getMessage());
            }
            throw e;
        }

    }

    private void updateNetworkException(VDSNetworkException ex, String message) {
        VDSError error = ex.getVdsError();
        if (error == null) {
            error = new VDSError(EngineError.VDS_NETWORK_ERROR, message);
            ex.setVdsError(error);
        }

        error.setVdsId(getVds().getId());
    }

    protected VDSNetworkException createNetworkException(Exception ex) {
        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        VDSNetworkException networkException;
        String message;
        if (rootCause != null) {
            networkException = new VDSNetworkException(rootCause);
            message = rootCause.toString();
        } else {
            networkException = new VDSNetworkException(ex);
            message = ex.getMessage();
        }

        updateNetworkException(networkException, message);
        return networkException;
    }

    @Override
    protected void logToAudit(){
        if (isPolicyResetMessage(getReturnStatus().message)) {
            return;
        }
        AuditLogableBase auditLogableBase = new AuditLogableBase(vds.getId());
        auditLogableBase.setVds(vds);
        auditLogableBase.addCustomValue("message", getReturnStatus().message);

        auditLogDirector.log(auditLogableBase, AuditLogType.VDS_BROKER_COMMAND_FAILURE);
    }

    protected boolean isPolicyResetMessage(String message) {
        return "Policy reset".equals(message);
    }

    protected abstract void executeVdsBrokerCommand();
}
