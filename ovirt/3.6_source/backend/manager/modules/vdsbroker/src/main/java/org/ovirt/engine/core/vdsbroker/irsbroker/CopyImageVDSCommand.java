package org.ovirt.engine.core.vdsbroker.irsbroker;

import org.ovirt.engine.core.common.asynctasks.AsyncTaskCreationInfo;
import org.ovirt.engine.core.common.asynctasks.AsyncTaskType;
import org.ovirt.engine.core.common.vdscommands.CopyImageVDSCommandParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.storage.StorageDomainHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyImageVDSCommand<P extends CopyImageVDSCommandParameters> extends IrsCreateCommand<P> {

    private static final Logger log = LoggerFactory.getLogger(CopyImageVDSCommand.class);

    public CopyImageVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeIrsBrokerCommand() {
        StorageDomainHelper.checkNumberOfLVsForBlockDomain(getParameters().getDstStorageDomainId());
        /**
         * TODO: EINAV: - Consider 'RunAsync' parameter (pass it to IRS too). - Add 'description' parameter. - we should
         * provide 'volumeUUID'. - TaskID should be provided by IRS.
         */
        setReturnValue(Guid.Empty);

        log.info("-- executeIrsBrokerCommand: calling 'copyImage' with two new parameters: description and UUID. Parameters:");
        log.info("++ sdUUID={}", getParameters().getStorageDomainId());
        log.info("++ spUUID={}", getParameters().getStoragePoolId());
        log.info("++ vmGUID={}", getParameters().getVmId());
        log.info("++ srcImageGUID={}", getParameters().getImageGroupId());
        log.info("++ srcVolUUID={}", getParameters().getImageId());
        log.info("++ dstImageGUID={}", getParameters().getdstImageGroupId());
        log.info("++ dstVolUUID={}", getParameters().getDstImageId());
        log.info("++ descr={}", getParameters().getImageDescription());
        log.info("++ dstSdUUID={}", getParameters().getDstStorageDomainId());

        // NOTE: The 'uuidReturn' variable will contain the taskID and not the
        // created image id!
        uuidReturn = getIrsProxy().copyImage(getParameters().getStorageDomainId().toString(),
                                             getParameters().getStoragePoolId().toString(),
                                             getParameters().getVmId().toString(),
                                             getParameters().getImageGroupId().toString(),
                                             getParameters().getImageId().toString(),
                                             getParameters().getdstImageGroupId().toString(),
                                             getParameters().getDstImageId().toString(),
                                             getParameters().getImageDescription(),
                                             getParameters().getDstStorageDomainId().toString(),
                                             getParameters().getCopyVolumeType().getValue(),
                                             getParameters().getVolumeFormat().getValue(),
                                             getParameters().getPreallocate().getValue(),
                                             String.valueOf(getParameters().getPostZero()).toLowerCase(),
                                             String.valueOf(getParameters().getForce()).toLowerCase());
        proceedProxyReturnValue();

        Guid taskID = new Guid(uuidReturn.uuid);

        createdImageId = getParameters().getDstImageId().toString();
        setReturnValue(taskID);

        getVDSReturnValue().setCreationInfo(
                new AsyncTaskCreationInfo(taskID, AsyncTaskType.copyImage, getParameters().getStoragePoolId()));
    }
}
