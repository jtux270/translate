package org.ovirt.engine.core.dao;

import org.ovirt.engine.core.common.businessentities.EngineSession;

/**
 * <code>EngineSessionDao</code> defines a type which performs CRUD operations on instances of {@link org.ovirt.engine.core.common.businessentities.EngineSession}.
 */
public interface EngineSessionDao extends Dao, SearchDao<EngineSession> {
    /**
     * Retrieves the session with the specified id.
     *
     * @param id the id
     * @return the engine session
     */
    EngineSession get(long id);

    /**
     * Retrieves the session with the specified engine session id.
     *
     * @param id the session id
     * @return the engine session
     */
    public EngineSession getBySessionId(String id);

    /**
     * Saves the specified session
     *
     * @param session the session
     */
    long save(EngineSession session);

    /**
     * Removes the session with the specified id.
     *
     * @param id
     */
    int remove(long id);

    /**
     * Remove all sessions from the table
     */
    int removeAll();

}
