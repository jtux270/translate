package org.ovirt.engine.ui.uicommonweb.models.gluster;

import org.ovirt.engine.core.common.businessentities.gluster.BrickProperties;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;

public class BrickPropertiesModel extends Model {

    private EntityModel<GlusterStatus> status;
    private EntityModel<Integer> port;
    private EntityModel<Integer> pid;
    private EntityModel<Double> totalSize;
    private EntityModel<Double> freeSize;
    private EntityModel<String> device;
    private EntityModel<Integer> blockSize;
    private EntityModel<String> mountOptions;
    private EntityModel<String> fileSystem;

    public BrickPropertiesModel() {
        setStatus(new EntityModel<GlusterStatus>());
        setPort(new EntityModel<Integer>());
        setPid(new EntityModel<Integer>());
        setTotalSize(new EntityModel<Double>());
        setFreeSize(new EntityModel<Double>());
        setDevice(new EntityModel<String>());
        setBlockSize(new EntityModel<Integer>());
        setMountOptions(new EntityModel<String>());
        setFileSystem(new EntityModel<String>());
    }

    public void setProperties(BrickProperties brickProperties) {
        getStatus().setEntity(brickProperties.getStatus());
        getPort().setEntity(brickProperties.getPort());
        getPid().setEntity(brickProperties.getPid());
        getTotalSize().setEntity(brickProperties.getTotalSize());
        getFreeSize().setEntity(brickProperties.getFreeSize());
        getDevice().setEntity(brickProperties.getDevice());
        getBlockSize().setEntity(brickProperties.getBlockSize());
        getMountOptions().setEntity(brickProperties.getMntOptions());
        getFileSystem().setEntity(brickProperties.getFsName());
    }

    public EntityModel<GlusterStatus> getStatus() {
        return status;
    }

    public void setStatus(EntityModel<GlusterStatus> status) {
        this.status = status;
    }

    public EntityModel<Integer> getPort() {
        return port;
    }

    public void setPort(EntityModel<Integer> port) {
        this.port = port;
    }

    public EntityModel<Integer> getPid() {
        return pid;
    }

    public void setPid(EntityModel<Integer> pid) {
        this.pid = pid;
    }

    public EntityModel<Double> getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(EntityModel<Double> totalSize) {
        this.totalSize = totalSize;
    }

    public EntityModel<Double> getFreeSize() {
        return freeSize;
    }

    public void setFreeSize(EntityModel<Double> freeSize) {
        this.freeSize = freeSize;
    }

    public EntityModel<String> getDevice() {
        return device;
    }

    public void setDevice(EntityModel<String> device) {
        this.device = device;
    }

    public EntityModel<Integer> getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(EntityModel<Integer> blockSize) {
        this.blockSize = blockSize;
    }

    public EntityModel<String> getMountOptions() {
        return mountOptions;
    }

    public void setMountOptions(EntityModel<String> mountOptions) {
        this.mountOptions = mountOptions;
    }

    public EntityModel<String> getFileSystem() {
        return fileSystem;
    }

    public void setFileSystem(EntityModel<String> fileSystem) {
        this.fileSystem = fileSystem;
    }
}
