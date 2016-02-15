package org.ovirt.engine.ui.uicommonweb.models.templates;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.ImageOperation;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.storage.MoveOrCopyDiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;

public class CopyDiskModel extends MoveOrCopyDiskModel {
    public CopyDiskModel() {
        super();

        setIsSourceStorageDomainAvailable(true);
    }

    @Override
    public void init(ArrayList<DiskImage> disksImages) {
        if (disksImages.size() > 0) {
            setIsAliasChangable(!isTemplateDisk(disksImages.get(0)));
        }

        setDiskImages(disksImages);

        AsyncDataProvider.getInstance().getDiskList(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                CopyDiskModel copyDiskModel = (CopyDiskModel) target;
                ArrayList<Disk> disks = (ArrayList<Disk>) returnValue;

                copyDiskModel.onInitAllDisks(disks);
                copyDiskModel.onInitDisks();
            }
        }));
    }

    @Override
    protected void initStorageDomains() {
        Disk disk = getDisks().get(0).getDisk();
        if (disk.getDiskStorageType() != DiskStorageType.IMAGE) {
            return;
        }

        AsyncDataProvider.getInstance().getStorageDomainList(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                CopyDiskModel copyDiskModel = (CopyDiskModel) target;
                ArrayList<StorageDomain> storageDomains = (ArrayList<StorageDomain>) returnValue;

                copyDiskModel.onInitStorageDomains(storageDomains);
            }
        }), ((DiskImage) disk).getStoragePoolId());
    }

    @Override
    protected VdcActionType getActionType() {
        return VdcActionType.MoveOrCopyDisk;
    }

    @Override
    protected String getWarning(List<String> disks) {
        return messages.cannotCopyDisks(StringHelper.join(", ", disks.toArray())); //$NON-NLS-1$
    }

    @Override
    protected String getNoActiveSourceDomainMessage() {
        return constants.noActiveSourceStorageDomainAvailableMsg();
    }

    @Override
    protected String getNoActiveTargetDomainMessage() {
        return constants.diskExistsOnAllActiveStorageDomainsMsg();
    }

    @Override
    protected MoveOrCopyImageGroupParameters createParameters(Guid sourceStorageDomainGuid,
            Guid destStorageDomainGuid,
            DiskImage disk) {
        MoveOrCopyImageGroupParameters moveOrCopyImageGroupParameters = new MoveOrCopyImageGroupParameters(disk.getImageId(),
                sourceStorageDomainGuid,
                destStorageDomainGuid,
                ImageOperation.Copy);
        moveOrCopyImageGroupParameters.setImageGroupID(disk.getId());
        return moveOrCopyImageGroupParameters;
    }

    @Override
    protected void doExecute() {
        super.doExecute();

        ArrayList<VdcActionParametersBase> parameters = getParameters();
        if (parameters.isEmpty()) {
            cancel();
            return;
        }

        Frontend.getInstance().runMultipleAction(getActionType(), parameters,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {
                        CopyDiskModel localModel = (CopyDiskModel) result.getState();
                        localModel.cancel();
                    }
                }, this);
    }

    @Override
    protected boolean allowedStorageDomain(ArrayList<StorageDomain> sourceActiveStorageDomains, boolean shouldFilterBySourceType, DiskImage diskImage, DiskModel templateDisk, StorageDomain sd) {
        // can not move template to the same storage domain
        boolean isTemplate = isTemplateDisk(diskImage);
        if (isTemplate && sourceActiveStorageDomains.contains(sd)) {
            return false;
        }

        return super.allowedStorageDomain(sourceActiveStorageDomains, shouldFilterBySourceType, diskImage, templateDisk, sd);
    }

    private boolean isTemplateDisk(DiskImage  diskImage) {
        return diskImage.getVmEntityType() != null && diskImage.getVmEntityType().isTemplateType();
    }

}
