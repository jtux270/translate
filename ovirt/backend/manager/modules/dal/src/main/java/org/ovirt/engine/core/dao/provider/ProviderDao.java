package org.ovirt.engine.core.dao.provider;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.GenericDao;
import org.ovirt.engine.core.dao.SearchDAO;

public interface ProviderDao extends GenericDao<Provider<?>, Guid>, SearchDAO<Provider<?>> {

    /**
     * Query for the provider by name.
     *
     * @param name
     *            The name of the provider.
     * @return The provider, or <code>null</code> if not found.
     */
    Provider<?> getByName(String name);

    /**
     * Query for the providers by type.
     *
     * @param providerType
     *            The type of the provider.
     * @return All providers of that type, or <code>null</code> if none exist found.
     */
    List<Provider<?>> getAllByType(ProviderType providerType);
}
