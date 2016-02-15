package org.ovirt.engine.core.common.action;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.compat.Guid;

public class ImportVmParameters extends VmOperationParameterBase implements Serializable {
    private static final long serialVersionUID = -6514416097090370831L;

    @Valid
    private VM vm;
    private Guid sourceDomainId;
    private Guid destDomainId;
    private Guid vdsGroupId;
    private Map<Guid, Disk> diskMap;

    private Guid storagePoolId;
    private Map<Guid, Guid> imageToDestinationDomainMap;
    private boolean imagesExistOnTargetStorageDomain;
    private Guid cpuProfileId;
    private boolean copyCollapse;
    private boolean forceOverride;
    private Guid containerId;
    private Guid storageDomainId;
    private boolean importAsNewEntity;

    public ImportVmParameters() {
        sourceDomainId = Guid.Empty;
        destDomainId = Guid.Empty;
    }

    public ImportVmParameters(VM vm, Guid sourceStorageDomainId, Guid destStorageDomainId, Guid storagePoolId,
            Guid vdsGroupId) {
        super(vm.getId());
        containerId = vm.getId();
        sourceDomainId = sourceStorageDomainId;
        destDomainId = destStorageDomainId;
        setVm(vm);
        setStorageDomainId(destStorageDomainId);
        this.setStoragePoolId(storagePoolId);
        setVdsGroupId(vdsGroupId);
    }

    public VM getVm() {
        return vm;
    }

    public void setVm(VM vm) {
        this.vm = vm;
    }

    public Guid getSourceDomainId() {
        return sourceDomainId;
    }

    public Guid getDestDomainId() {
        return destDomainId;
    }

    public Guid getVdsGroupId() {
        return vdsGroupId;
    }

    public void setVdsGroupId(Guid vdsGroupId) {
        this.vdsGroupId = vdsGroupId;
    }

    public void setDestDomainId(Guid destDomainId) {
        this.destDomainId = destDomainId;
    }

    public void setSourceDomainId(Guid sourceDomainId) {
        this.sourceDomainId = sourceDomainId;
    }

    public Map<Guid, Disk> getDiskMap() {
        return diskMap;
    }

    public void setDiskMap(Map<Guid, Disk> diskMap) {
        this.diskMap = diskMap;
    }

    public Guid getStoragePoolId() {
        return storagePoolId;
    }

    public void setStoragePoolId(Guid storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public Guid getStorageDomainId() {
        return storageDomainId;
    }

    public void setStorageDomainId(Guid storageDomainId) {
        this.storageDomainId = storageDomainId;
    }

    public Map<Guid, Guid> getImageToDestinationDomainMap() {
        return imageToDestinationDomainMap;
    }

    public void setImageToDestinationDomainMap(Map<Guid, Guid> imageToDestinationDomainMap) {
        this.imageToDestinationDomainMap = imageToDestinationDomainMap;
    }

    public boolean isImagesExistOnTargetStorageDomain() {
        return imagesExistOnTargetStorageDomain;
    }

    public void setImagesExistOnTargetStorageDomain(boolean imagesExistOnTargetStorageDomain) {
        this.imagesExistOnTargetStorageDomain = imagesExistOnTargetStorageDomain;
    }

    public Guid getCpuProfileId() {
        return cpuProfileId;
    }

    public void setCpuProfileId(Guid cpuProfileId) {
        this.cpuProfileId = cpuProfileId;
    }

    public boolean getCopyCollapse() {
        return copyCollapse;
    }

    public void setCopyCollapse(boolean copyCollapse) {
        this.copyCollapse = copyCollapse;
    }

    public boolean getForceOverride() {
        return forceOverride;
    }

    public void setForceOverride(boolean forceOverride) {
        this.forceOverride = forceOverride;
    }

    public Guid getContainerId() {
        return containerId;
    }

    public void setContainerId(Guid containerId) {
        this.containerId = containerId;
    }

    public boolean isImportAsNewEntity() {
        return importAsNewEntity;
    }

    public void setImportAsNewEntity(boolean importAsNewEntity) {
        this.importAsNewEntity = importAsNewEntity;
    }

}
