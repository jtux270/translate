package org.ovirt.engine.core.vdsbroker.irsbroker;

import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusOnlyReturnForXmlRpc;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StorageDomainListReturnForXmlRpc;

public class IrsServerWrapper implements IIrsServer {

    private IrsServerConnector irsServer;
    private HttpClient httpClient;

    public IrsServerWrapper(IrsServerConnector innerImplementor, HttpClient httpClient) {
        this.irsServer = innerImplementor;
        this.httpClient = httpClient;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public OneUuidReturnForXmlRpc createVolume(String sdUUID, String spUUID, String imgGUID, String size,
            int volFormat, int volType, int diskType, String volUUID, String descr, String srcImgGUID, String srcVolUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.createVolume(sdUUID, spUUID, imgGUID, size, volFormat,
                volType, diskType, volUUID, descr, srcImgGUID, srcVolUUID);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc copyImage(String sdUUID, String spUUID, String vmGUID, String srcImgGUID,
            String srcVolUUID, String dstImgGUID, String dstVolUUID, String descr, String dstSdUUID, int volType,
            int volFormat, int preallocate, String postZero, String force) {
        Map<String, Object> xmlRpcReturnValue = irsServer.copyImage(sdUUID, spUUID, vmGUID, srcImgGUID, srcVolUUID,
                dstImgGUID, dstVolUUID, descr, dstSdUUID, volType, volFormat, preallocate, postZero, force);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc downloadImage(Map methodInfo, String spUUID, String sdUUID, String dstImgGUID, String dstVolUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.downloadImage(methodInfo, spUUID, sdUUID, dstImgGUID, dstVolUUID);
        return new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
    }

    @Override
    public OneUuidReturnForXmlRpc uploadImage(Map methodInfo, String spUUID, String sdUUID, String srcImgGUID, String srcVolUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.uploadImage(methodInfo, spUUID, sdUUID, srcImgGUID, srcVolUUID);
        return new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
    }

    @Override
    public OneUuidReturnForXmlRpc mergeSnapshots(String sdUUID, String spUUID, String vmGUID, String imgGUID,
            String ancestorUUID, String successorUUID, String postZero) {
        Map<String, Object> xmlRpcReturnValue = irsServer.mergeSnapshots(sdUUID, spUUID, vmGUID, imgGUID, ancestorUUID,
                successorUUID, postZero);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public VolumeListReturnForXmlRpc reconcileVolumeChain(String spUUID, String sdUUID, String imgGUID,
            String leafVolUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.reconcileVolumeChain(spUUID, sdUUID, imgGUID,
                leafVolUUID);
        VolumeListReturnForXmlRpc wrapper = new VolumeListReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc deleteVolume(String sdUUID, String spUUID, String imgGUID, String[] volUUID,
            String postZero, String force) {
        Map<String, Object> xmlRpcReturnValue = irsServer.deleteVolume(sdUUID, spUUID, imgGUID, volUUID, postZero,
                force);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneImageInfoReturnForXmlRpc getVolumeInfo(String sdUUID, String spUUID, String imgGUID, String volUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.getVolumeInfo(sdUUID, spUUID, imgGUID, volUUID);
        OneImageInfoReturnForXmlRpc wrapper = new OneImageInfoReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc setVolumeDescription(String sdUUID, String spUUID, String imgGUID, String volUUID, String description) {
        Map<String, Object> xmlRpcReturnValue = irsServer.setVolumeDescription(sdUUID, spUUID, imgGUID, volUUID, description);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public IrsStatsAndStatusXmlRpc getIrsStats() {
        Map<String, Object> xmlRpcReturnValue = irsServer.getStats();
        IrsStatsAndStatusXmlRpc wrapper = new IrsStatsAndStatusXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc importCandidate(String sdUUID, String vmGUID, String templateGUID,
            String templateVolGUID, String path, String type, String force) {
        Map<String, Object> xmlRpcReturnValue = irsServer.importCandidate(sdUUID, vmGUID, templateGUID,
                templateVolGUID, path, type, force);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public FileStatsReturnForXmlRpc getIsoList(String spUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.getIsoList(spUUID);
        FileStatsReturnForXmlRpc wrapper = new FileStatsReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public FileStatsReturnForXmlRpc getFloppyList(String spUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.getFloppyList(spUUID);
        FileStatsReturnForXmlRpc wrapper = new FileStatsReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public FileStatsReturnForXmlRpc getFileStats(String sdUUID, String pattern, boolean caseSensitive) {
        Map<String, Object> xmlRpcReturnValue = irsServer.getFileStats(sdUUID, pattern, caseSensitive);
        FileStatsReturnForXmlRpc wrapper = new FileStatsReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StorageStatusReturnForXmlRpc activateStorageDomain(String sdUUID, String spUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.activateStorageDomain(sdUUID, spUUID);
        StorageStatusReturnForXmlRpc wrapper = new StorageStatusReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc deactivateStorageDomain(String sdUUID, String spUUID, String msdUUID,
            int masterVersion) {
        Map<String, Object> xmlRpcReturnValue = irsServer.deactivateStorageDomain(sdUUID, spUUID, msdUUID,
                masterVersion);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc detachStorageDomain(String sdUUID, String spUUID, String msdUUID, int masterVersion) {
        Map<String, Object> xmlRpcReturnValue = irsServer.detachStorageDomain(sdUUID, spUUID, msdUUID, masterVersion);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc forcedDetachStorageDomain(String sdUUID, String spUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.forcedDetachStorageDomain(sdUUID, spUUID);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc attachStorageDomain(String sdUUID, String spUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.attachStorageDomain(sdUUID, spUUID);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc setStorageDomainDescription(String sdUUID, String description) {
        Map<String, Object> xmlRpcReturnValue = irsServer.setStorageDomainDescription(sdUUID, description);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StorageDomainListReturnForXmlRpc reconstructMaster(String spUUID, int hostSpmId, String msdUUID,
            String masterVersion) {
        Map<String, Object> xmlRpcReturnValue = irsServer.reconstructMaster(spUUID, hostSpmId, msdUUID, masterVersion);
        StorageDomainListReturnForXmlRpc wrapper = new StorageDomainListReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc extendStorageDomain(String sdUUID, String spUUID, String[] devlist) {
        Map<String, Object> xmlRpcReturnValue = irsServer.extendStorageDomain(sdUUID, spUUID, devlist);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc extendStorageDomain(String sdUUID, String spUUID, String[] devlist, boolean force) {
        Map<String, Object> xmlRpcReturnValue = irsServer.extendStorageDomain(sdUUID, spUUID, devlist, force);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc setStoragePoolDescription(String spUUID, String description) {
        Map<String, Object> xmlRpcReturnValue = irsServer.setStoragePoolDescription(spUUID, description);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StoragePoolInfoReturnForXmlRpc getStoragePoolInfo(String spUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.getStoragePoolInfo(spUUID);
        StoragePoolInfoReturnForXmlRpc wrapper = new StoragePoolInfoReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc destroyStoragePool(String spUUID, int hostSpmId, String SCSIKey) {
        Map<String, Object> xmlRpcReturnValue = irsServer.destroyStoragePool(spUUID, hostSpmId, SCSIKey);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc deleteImage(String sdUUID, String spUUID, String imgGUID, String postZero,
            String force) {
        Map<String, Object> xmlRpcReturnValue = irsServer.deleteImage(sdUUID, spUUID, imgGUID, postZero, force);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc moveImage(String spUUID, String srcDomUUID, String dstDomUUID, String imgGUID,
            String vmGUID, int op, String postZero, String force) {
        Map<String, Object> xmlRpcReturnValue = irsServer.moveImage(spUUID, srcDomUUID, dstDomUUID, imgGUID, vmGUID,
                op, postZero, force);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc cloneImageStructure(String spUUID, String srcDomUUID, String imgGUID, String dstDomUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.cloneImageStructure(spUUID, srcDomUUID, imgGUID, dstDomUUID);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc syncImageData(String spUUID, String srcDomUUID, String imgGUID, String dstDomUUID, String syncType) {
        Map<String, Object> xmlRpcReturnValue = irsServer.syncImageData(spUUID, srcDomUUID, imgGUID, dstDomUUID, syncType);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc setMaxHosts(int maxHosts) {
        Map<String, Object> xmlRpcReturnValue = irsServer.setMaxHosts(maxHosts);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc updateVM(String spUUID, Map[] vms) {
        Map<String, Object> xmlRpcReturnValue = irsServer.updateVM(spUUID, vms);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc removeVM(String spUUID, String vmGUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.removeVM(spUUID, vmGUID);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc updateVMInImportExport(String spUUID, Map[] vms, String StorageDomainId) {
        Map<String, Object> xmlRpcReturnValue = irsServer.updateVM(spUUID, vms, StorageDomainId);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc removeVM(String spUUID, String vmGUID, String storageDomainId) {
        Map<String, Object> xmlRpcReturnValue = irsServer.removeVM(spUUID, vmGUID, storageDomainId);
        StatusOnlyReturnForXmlRpc wrapper = new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public GetVmsInfoReturnForXmlRpc getVmsInfo(String storagePoolId, String storageDomainId, String[] VMIDList) {
        Map<String, Object> xmlRpcReturnValue = irsServer.getVmsInfo(storagePoolId, storageDomainId, VMIDList);
        GetVmsInfoReturnForXmlRpc wrapper = new GetVmsInfoReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public StatusOnlyReturnForXmlRpc upgradeStoragePool(String storagePoolId, String targetVersion) {
        Map<String, Object> xmlRpcReturnValue = irsServer.upgradeStoragePool(storagePoolId, targetVersion);
        return new StatusOnlyReturnForXmlRpc(xmlRpcReturnValue);
    }

    @Override
    public ImagesListReturnForXmlRpc getImagesList(String sdUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.getImagesList(sdUUID);
        ImagesListReturnForXmlRpc wrapper = new ImagesListReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public UUIDListReturnForXmlRpc getVolumesList(String sdUUID, String spUUID, String imgUUID) {
        Map<String, Object> xmlRpcReturnValue = irsServer.getVolumesList(sdUUID, spUUID, imgUUID);
        UUIDListReturnForXmlRpc wrapper = new UUIDListReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }

    @Override
    public OneUuidReturnForXmlRpc extendVolumeSize(String spUUID,
                                                   String sdUUID,
                                                   String imageUUID,
                                                   String volumeUUID,
                                                   String newSize) {
        Map<String, Object> xmlRpcReturnValue = irsServer.extendVolumeSize(spUUID, sdUUID, imageUUID,
                volumeUUID, newSize);
        OneUuidReturnForXmlRpc wrapper = new OneUuidReturnForXmlRpc(xmlRpcReturnValue);
        return wrapper;
    }
}
