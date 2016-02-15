package org.ovirt.engine.ui.uicommonweb.models.vms;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VolumeType;
import org.ovirt.engine.core.common.businessentities.profiles.DiskProfile;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;

public class DiskModel extends Model
{
    private Disk disk;
    private VM vm;

    private EntityModel<Integer> size;
    private EntityModel<String> alias;
    private EntityModel<String> description;
    private EntityModel<String> sourceStorageDomainName;

    private ListModel<VolumeType> volumeType;
    private ListModel<DiskInterface> diskInterface;
    private ListModel<StorageDomain> sourceStorageDomain;
    private ListModel<StorageDomain> storageDomain;
    private ListModel<DiskProfile> diskProfile;
    private ListModel<Quota> quota;

    private boolean pluggedToRunningVm;

    public Disk getDisk() {
        return disk;
    }

    public void setDisk(Disk disk) {
        this.disk = disk;
    }

    public VM getVm() {
        return vm;
    }

    public void setVm(VM vm) {
        this.vm = vm;
    }

    public EntityModel<Integer> getSize() {
        return size;
    }

    public void setSize(EntityModel<Integer> size) {
        this.size = size;
    }

    public EntityModel<String> getAlias() {
        return alias;
    }

    public void setAlias(EntityModel<String> alias) {
        this.alias = alias;
    }

    public EntityModel<String> getDescription() {
        return description;
    }

    public void setDescription(EntityModel<String> description) {
        this.description = description;
    }

    public EntityModel<String> getSourceStorageDomainName() {
        return sourceStorageDomainName;
    }

    public void setSourceStorageDomainName(EntityModel<String> sourceStorageDomainName) {
        this.sourceStorageDomainName = sourceStorageDomainName;
    }

    public ListModel<VolumeType> getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(ListModel<VolumeType> volumeType) {
        this.volumeType = volumeType;
    }

    public ListModel<DiskInterface> getDiskInterface() {
        return diskInterface;
    }

    public void setDiskInterface(ListModel<DiskInterface> diskInterface) {
        this.diskInterface = diskInterface;
    }

    public ListModel<StorageDomain> getSourceStorageDomain() {
        return sourceStorageDomain;
    }

    public void setSourceStorageDomain(ListModel<StorageDomain> sourceStorageDomain) {
        this.sourceStorageDomain = sourceStorageDomain;
    }

    public ListModel<StorageDomain> getStorageDomain() {
        return storageDomain;
    }

    public void setStorageDomain(ListModel<StorageDomain> storageDomain) {
        this.storageDomain = storageDomain;
    }

    public ListModel<DiskProfile> getDiskProfile() {
        return diskProfile;
    }

    public void setDiskProfile(ListModel<DiskProfile> diskProfile) {
        this.diskProfile = diskProfile;
    }

    public ListModel<Quota> getQuota() {
        return quota;
    }

    public void setQuota(ListModel<Quota> quota) {
        this.quota = quota;
    }

    public boolean isPluggedToRunningVm() {
        return pluggedToRunningVm;
    }

    public void setPluggedToRunningVm(boolean pluggedToRunningVm) {
        this.pluggedToRunningVm = pluggedToRunningVm;
    }

    public DiskModel() {
        setSize(new EntityModel<Integer>());
        setAlias(new EntityModel<String>());
        setDescription(new EntityModel<String>());
        setSourceStorageDomainName(new EntityModel<String>());
        setSourceStorageDomain(new ListModel<StorageDomain>());
        setDiskInterface(new ListModel<DiskInterface>());
        setStorageDomain(new ListModel<StorageDomain>());
        setDiskProfile(new ListModel<DiskProfile>());

        setQuota(new ListModel<Quota>());
        getQuota().setIsAvailable(false);

        setVolumeType(new ListModel<VolumeType>());
        getVolumeType().setItems(AsyncDataProvider.getVolumeTypeList());
    }
}
