package org.ovirt.engine.core.bll;

import org.ovirt.engine.core.bll.context.CommandContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.common.action.RemoveAllVmImagesParameters;
import org.ovirt.engine.core.common.action.RemoveImageParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

/**
 * This command removes all Vm images and all created snapshots both from Irs
 * and Db.
 */
@InternalCommandAttribute
@NonTransactiveCommandAttribute
public class RemoveAllVmImagesCommand<T extends RemoveAllVmImagesParameters> extends VmCommand<T> {

    public RemoveAllVmImagesCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected void executeVmCommand() {
        Set<Guid> mImagesToBeRemoved = new HashSet<Guid>();
        List<DiskImage> images = getParameters().images;
        if (images == null) {
            images =
                    ImagesHandler.filterImageDisks(DbFacade.getInstance().getDiskDao().getAllForVm(getVmId()),
                            true,
                            false,
                            true);
        }
        for (DiskImage image : images) {
            if (Boolean.TRUE.equals(image.getActive())) {
                mImagesToBeRemoved.add(image.getImageId());
            }
        }

        Collection<DiskImage> failedRemoving = new LinkedList<>();
        for (final DiskImage image : images) {
            if (mImagesToBeRemoved.contains(image.getImageId())) {
                VdcReturnValueBase vdcReturnValue =
                        runInternalActionWithTasksContext(
                                VdcActionType.RemoveImage,
                                buildRemoveImageParameters(image)
                        );

                if (vdcReturnValue.getSucceeded()) {
                    getReturnValue().getInternalVdsmTaskIdList().addAll(vdcReturnValue.getInternalVdsmTaskIdList());
                } else {
                    StorageDomain domain = getStorageDomainDAO().get(image.getStorageIds().get(0));
                    failedRemoving.add(image);
                    log.errorFormat("Can't remove image id: {0} for VM id: {1} from domain id: {2} due to: {3}.",
                            image.getImageId(),
                            getParameters().getVmId(),
                            image.getStorageIds().get(0),
                            vdcReturnValue.getFault().getMessage());

                    if (domain.getStorageDomainType() == StorageDomainType.Data) {
                        log.infoFormat("Image id: {0} will be set at illegal state with no snapshot id.", image.getImageId());
                        TransactionSupport.executeInScope(TransactionScopeOption.Required,
                                new TransactionMethod<Object>() {
                                    @Override
                                    public Object runInTransaction() {
                                        // If VDSM task didn't succeed to initiate a task we change the disk to at illegal
                                        // state.
                                        updateDiskImagesToIllegal(image);
                                        return true;
                                    }
                                });
                    } else {
                        log.infoFormat("Image id: {0} is not on a data domain and will not be marked as illegal.", image.getImageId());
                    }
                }
            }
        }

        setActionReturnValue(failedRemoving);
        setSucceeded(true);
    }

    private RemoveImageParameters buildRemoveImageParameters(DiskImage image) {
        RemoveImageParameters result = new RemoveImageParameters(image.getImageId());
        result.setParentCommand(getParameters().getParentCommand());
        result.setParentParameters(getParameters().getParentParameters());
        result.setDiskImage(image);
        result.setEntityInfo(getParameters().getEntityInfo());
        result.setForceDelete(getParameters().getForceDelete());
        result.setShouldLockImage(false);
        return result;
    }

    /**
     * Update all disks images of specific disk image to illegal state, and set the vm snapshot id to null, since now
     * they are not connected to any VM.
     *
     * @param diskImage - The disk to update.
     */
    private void updateDiskImagesToIllegal(DiskImage diskImage) {
        List<DiskImage> snapshotDisks =
                getDbFacade().getDiskImageDao().getAllSnapshotsForImageGroup(diskImage.getId());
        for (DiskImage diskSnapshot : snapshotDisks) {
            diskSnapshot.setVmSnapshotId(null);
            diskSnapshot.setImageStatus(ImageStatus.ILLEGAL);
            getDbFacade().getImageDao().update(diskSnapshot.getImage());
        }
    }

    @Override
    protected void endVmCommand() {
        setSucceeded(true);
    }
}
