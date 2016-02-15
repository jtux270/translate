package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.bll.BaseImagesCommand;
import org.ovirt.engine.core.bll.ImagesHandler;
import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.common.action.ImagesContainterParametersBase;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImageDynamic;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.businessentities.storage.ImageStorageDomainMap;
import org.ovirt.engine.core.common.businessentities.storage.VolumeClassification;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@InternalCommandAttribute
public class CloneSingleCinderDiskCommand<T extends ImagesContainterParametersBase> extends BaseImagesCommand<T> {

    private CinderDisk disk;

    public CloneSingleCinderDiskCommand(T parameters) {
        this(parameters, null);
    }

    public CloneSingleCinderDiskCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
        setStorageDomainId(parameters.getStorageDomainId());
    }

    @Override
    protected void executeCommand() {
        lockImage();
        CinderDisk cinderDisk = getDisk();
        cinderDisk.setDiskAlias(getParameters().getDiskAlias());
        String volumeId = getNewVolumeCinderDisk(cinderDisk);
        cinderDisk.setId(Guid.createGuidFromString(volumeId));
        cinderDisk.setImageId(Guid.createGuidFromString(volumeId));
        cinderDisk.setImageStatus(ImageStatus.LOCKED);
        cinderDisk.setVolumeType(VolumeType.Sparse);
        cinderDisk.setVolumeClassification(VolumeClassification.Volume);
        cinderDisk.setVmSnapshotId(getParameters().getVmSnapshotId());

        // If we clone a disk from snapshot, update the volume with the appropriate parameters.
        if (!cinderDisk.getActive()) {
            cinderDisk.setActive(true);
            cinderDisk.setParentId(Guid.Empty);
        }
        addCinderDiskTemplateToDB(cinderDisk);

        getReturnValue().setActionReturnValue(cinderDisk.getId());
        getParameters().setDestinationImageId(Guid.createGuidFromString(volumeId));
        getParameters().setContainerId(Guid.createGuidFromString(volumeId));
        persistCommand(getParameters().getParentCommand(), true);
        setSucceeded(true);
    }

    private String getNewVolumeCinderDisk(CinderDisk cinderDisk) {
        String volumeId;
        if (cinderDisk.getActive()) {
            volumeId = getCinderBroker().cloneDisk(cinderDisk);
        } else {
            volumeId = getCinderBroker().cloneVolumeFromSnapshot(cinderDisk, cinderDisk.getImageId());
        }
        return volumeId;
    }

    protected void addCinderDiskTemplateToDB(final CinderDisk cinderDisk) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                getBaseDiskDao().save(cinderDisk);
                getImageDao().save(cinderDisk.getImage());
                DiskImageDynamic diskDynamic = new DiskImageDynamic();
                diskDynamic.setId(cinderDisk.getImageId());
                getDiskImageDynamicDao().save(diskDynamic);
                ImageStorageDomainMap image_storage_domain_map = new ImageStorageDomainMap(cinderDisk.getImageId(),
                        cinderDisk.getStorageIds().get(0), cinderDisk.getQuotaId(), cinderDisk.getDiskProfileId());
                getDbFacade().getImageStorageDomainMapDao().save(image_storage_domain_map);
                return null;
            }
        });
    }

    protected CinderDisk getDisk() {
        if (disk == null) {
            disk = (CinderDisk) getDiskImageDao().getSnapshotById(getImageId());
        }
        return disk;
    }

    @Override
    protected void lockImage() {
        ImagesHandler.updateImageStatus(getParameters().getImageId(), ImageStatus.LOCKED);
    }

    @Override
    public CommandCallback getCallback() {
        return new CloneSingleCinderDiskCommandCallback();
    }

    @Override
    protected void endSuccessfully() {
        setSucceeded(true);
    }

    @Override
    protected void endWithFailure() {
        setSucceeded(true);
    }
}
