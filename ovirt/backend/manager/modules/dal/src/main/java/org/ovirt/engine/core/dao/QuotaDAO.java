package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.compat.Guid;

/**
 * <code>QuotaDAO</code> is an interface for operations implements the calling to quota stored procedures. (@see
 * QuotaDAODbFacadeImpl)
 */
public interface QuotaDAO extends DAO, SearchDAO<Quota> {

    /**
     * Saves the Quota definition.
     *
     * @param quota
     *            the Quota
     */
    public void save(Quota quota);

    /**
     * Saves Quota by Quota Id.
     *
     * @param id
     *            the Quota Id
     */
    public Quota getById(Guid id);

    /**
     * Get total number of quota in the DB
     * @return total number of quota
     */
    public int getQuotaCount();

    /**
     * Get all the full quotas. Including consumption data. This call is very heavy and should be used really and with
     * caution. It was created to support cache initialization
     *
     * @return all quota in DB (including consumption calculation)
     */
    public List<Quota> getAllQuotaIncludingConsumption();

    /**
     * Removes the Quota with the specified id.
     *
     * @param id
     *            the quota id
     */
    public void remove(Guid id);

    /**
     * Update Quota, by re-inserting its sub Quota lists, and update the global quota parameters.
     *
     * @param quota
     *            - The Quota to update.
     */
    public void update(Quota quota);

    /**
     * Get specific limitation for <code>VdsGroup</code>.
     *
     * @param vdsGroupId
     *            - The vds group id, if null returns all the vds group limitations in the storage pool.
     * @param quotaId
     *            - The <code>Quota</code> id
     * @param allowEmpty
     *            - Whether to return empty quotas or not
     * @return List of QuotaStorage
     */
    public List<QuotaVdsGroup> getQuotaVdsGroupByVdsGroupGuid(Guid vdsGroupId, Guid quotaId, boolean allowEmpty);

    /**
     * Get specific limitation for <code>VdsGroup</code>.
     *
     * @param vdsGroupId
     *            - The vds group id, if null returns all the vds group limitations in the storage pool.
     * @param quotaId
     *            - The <code>Quota</code> id
     * @return List of QuotaStorage
     */
    public List<QuotaVdsGroup> getQuotaVdsGroupByVdsGroupGuid(Guid vdsGroupId, Guid quotaId);

    /**
     * Get specific limitation for storage domain.
     *
     * @param storageId
     *            - The storage id, if null returns all the storages limitation in the storage pool.
     * @param quotaId
     *            - The quota id
     * @param allowEmpty
     *            - Whether to return empty quotas or not
     * @return List of QuotaStorage
     */
    public List<QuotaStorage> getQuotaStorageByStorageGuid(Guid storageId, Guid quotaId, boolean allowEmpty);

    /**
     * Get specific limitation for storage domain.
     *
     * @param storageId
     *            - The storage id, if null returns all the storages limitation in the storage pool.
     * @param quotaId
     *            - The quota id
     * @return List of QuotaStorage
     */
    public List<QuotaStorage> getQuotaStorageByStorageGuid(Guid storageId, Guid quotaId);

    /**
     * Get all the QuotaStorage in the system including consumption calculation. This call is very heavy and should be
     * used really and with caution. It was created to support cache initialization
     *
     * @return all QuotaStorage (including consumption calculation)
     */
    public List<QuotaStorage> getAllQuotaStorageIncludingConsumption();

    /**
     * Get <code>Quota</code> by name.
     *
     * @param quotaName
     *            - The quota name to find.
     * @param storagePoolId
     *            - The storage pool id that the quota is being searched in.
     * @return The quota entity that was found.
     */
    public Quota getQuotaByQuotaName(String quotaName);

    /**
     * Get list of <code>Quotas</code> which are consumed by ad element id in storage pool (if not storage pool id not
     * null).
     *
     * @param adElementId
     *            - The user ID or group ID.
     * @param storagePoolId
     *            - The storage pool Id to search the quotas in (If null search all over the setup).
     * @return All quotas for user.
     */
    public List<Quota> getQuotaByAdElementId(Guid adElementId, Guid storagePoolId, boolean recursive);

    /**
     * Get all quota storages which belong to quota with quotaId.
     */
    public List<QuotaStorage> getQuotaStorageByQuotaGuid(Guid quotaId);

    /**
     * Get all quota storages which belong to quota with quotaId.
     * In case no quota storages are returned, a fictitious {@link QuotaStorage} is returned,
     * with an {@link Guid.Empty} Storage Id and a <code>null</code> name.
     */
    public List<QuotaStorage> getQuotaStorageByQuotaGuidWithGeneralDefault(Guid quotaId);

    /**
     * Get all quota vds groups, which belong to quota with quotaId.
     */
    public List<QuotaVdsGroup> getQuotaVdsGroupByQuotaGuid(Guid quotaId);

    /**
     * Get all the QuotaVdsGroup in the system including consumption calculation. This call is very heavy and should be
     * used really and with caution. It was created to support cache initialization
     *
     * @return all QuotaVdsGroup (including consumption calculation)
     */
    public List<QuotaVdsGroup> getAllQuotaVdsGroupIncludingConsumption();

    /**
     * Get all quota Vds groups, which belong to quota with quotaId.
     * In case no quota Vds Groups are returned, a fictitious QuotaVdsGroup is returned,
     * with an {@link Guid.Empty} Vds Id and a <code>null</code> name.
     */
    public List<QuotaVdsGroup> getQuotaVdsGroupByQuotaGuidWithGeneralDefault(Guid quotaId);

    /**
     * Returns all the Quota storages in the storage pool if v_storage_id is null, if v_storage_id is not null then a
     * specific quota storage will be returned.
     */
    public List<Quota> getQuotaByStoragePoolGuid(Guid storagePoolId);

    /**
     * Returns a list of all the quotas that are relevant to the given {@link #storageId} -
     * be it specific quotas (i.e., defined directly on the storage) or
     * general (i.e., defined on the storage pool containing the storage).<br/>
     * <b>Note:</b> The quotas returned are <b>thin</b> objects, containing only the metadata of the quota,
     * not the usage data.
     */
    public List<Quota> getAllRelevantQuotasForStorage(Guid storageId, Guid userID, boolean isFiltered);

    /**
     * Returns a list of all the quotas that are relevant to the given {@link #vdsGroupId} -
     * be it specific quotas (i.e., defined directly on the VDS Group) or
     * general (i.e., defined on the storage pool containing the group).<br/>
     * <b>Note:</b> The quotas returned are <b>thin</b> objects, containing only the metadata of the quota,
     * not the usage data.
     */
    public List<Quota> getAllRelevantQuotasForVdsGroup(Guid vdsGroupId, Guid userID, boolean isFiltered);

    /**
     * Is the Quota in use by any Image or VM (checks only for existence of the quota id in
     * the 'vm_static' and 'images' tables).
     * @param quota -quota to look for
     * @return - True if the Quota is in use by at least ont VM or one Image.
     */
    public boolean isQuotaInUse(Quota quota);

    /**
     * gets all the vm statuses that we are not counting for: Down(0),Suspended(13),ImageIllegal(14),ImageLocked(15) in
     * order DB and server to be in sync.
     * @return array of vm statuses that quota shouldn't be calculated for.
     */
    public List<Integer> getNonCountableQutoaVmStatuses();
}
