package org.ovirt.engine.core.bll.network.host;

import java.util.List;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.VdsCommand;
import org.ovirt.engine.core.common.action.HostSetupNetworksParameters;
import org.ovirt.engine.core.common.action.NetworkAttachmentParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.network.NetworkAttachment;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkAttachmentDao;
import org.ovirt.engine.core.utils.ReplacementUtils;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;

@NonTransactiveCommandAttribute
public class AddNetworkAttachmentCommand<T extends NetworkAttachmentParameters> extends VdsCommand<T> {
    @Inject
    private InterfaceDao interfaceDao;

    @Inject
    private NetworkAttachmentDao networkAttachmentDao;

    @Inject
    private NetworkIdNetworkNameCompleter networkIdNameCompleter;

    private List<VdsNetworkInterface> hostNics;

    public AddNetworkAttachmentCommand(T parameters) {
        super(parameters);
        addValidationGroup(CreateEntity.class);
    }

    @Override
    protected boolean canDoAction() {
        NicNameNicIdCompleter completer = new NicNameNicIdCompleter(getHostInterfaces());
        completer.completeNetworkAttachment(getParameters().getNetworkAttachment());

        this.networkIdNameCompleter.completeNetworkAttachment(getParameters().getNetworkAttachment());

        NetworkAttachment networkAttachment = getParameters().getNetworkAttachment();
        if (networkAttachment == null) {
            return failCanDoAction(EngineMessage.NETWORK_ATTACHMENT_NOT_SPECIFIED);
        }

        if (networkAttachment.getId() != null) {
            return failCanDoAction(EngineMessage.NETWORK_ATTACHMENT_CANNOT_BE_CREATED_WITH_SPECIFIC_ID,
                ReplacementUtils.createSetVariableString("NETWORK_ATTACHMENT_CANNOT_BE_CREATED_WITH_SPECIFIC_ID_ENTITY",
                    networkAttachment.getId()));
        }

        return true;
    }

    @Override
    protected void executeCommand() {
        HostSetupNetworksParameters params = new HostSetupNetworksParameters(getParameters().getVdsId());
        NetworkAttachment networkAttachment = getParameters().getNetworkAttachment();
        params.getNetworkAttachments().add(networkAttachment);

        //storing ids, so we're sure, that they were not mistakenly altered in HostSetupNetworks command.
        Guid networkId = networkAttachment.getNetworkId();
        Guid nicId = networkAttachment.getNicId();

        VdcReturnValueBase returnValue = runInternalAction(VdcActionType.HostSetupNetworks, params);

        if (returnValue.getSucceeded()) {

            Guid createdAttachmentId = resolveCreatedAttachmentId(networkId, nicId);
            getReturnValue().setActionReturnValue(createdAttachmentId);
        } else {
            propagateFailure(returnValue);
        }


        setSucceeded(returnValue.getSucceeded());
    }

    private Guid resolveCreatedAttachmentId(final Guid requiredNetworkId, Guid configuredNicId) {
        VdsNetworkInterface configuredNic = interfaceDao.get(configuredNicId);
        List<NetworkAttachment> attachmentsOnNic = networkAttachmentDao.getAllForNic(configuredNic.getId());

        NetworkAttachment createNetworkAttachment = LinqUtils.firstOrNull(attachmentsOnNic,
                new Predicate<NetworkAttachment>() {

                    @Override
                    public boolean eval(NetworkAttachment networkAttachment) {
                        return networkAttachment.getNetworkId().equals(requiredNetworkId);
                    }
                });

        if (createNetworkAttachment == null) {
            throw new IllegalStateException();
        }

        return createNetworkAttachment.getId();
    }

    private List<VdsNetworkInterface> getHostInterfaces() {
        if (hostNics == null) {
            hostNics = interfaceDao.getAllInterfacesForVds(getVdsId());
        }

        return hostNics;
    }
}
