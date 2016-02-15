package org.ovirt.engine.core.bll.context;

import java.util.Collection;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.BusinessEntityWithStatus;

/**
 * An implementation of COmpensation Context that does nothing - will be used by commands that do not implement
 * compensation. This class is stateless, therefore it is possible to use a single instance of it
 */
public class NoOpCompensationContext implements CompensationContext {

    private static final NoOpCompensationContext instance = new NoOpCompensationContext();

    private NoOpCompensationContext() {
    }

    @Override
    public void snapshotEntity(BusinessEntity<?> entity) {
    }

    @Override
    public void snapshotEntityUpdated(BusinessEntity<?> entity) {
    }

    @Override
    public void snapshotNewEntity(BusinessEntity<?> entity) {
    }

    @Override
    public <T extends Enum<?>> void  snapshotEntityStatus(BusinessEntityWithStatus<?, T> entity, T status) {
    }

    @Override
    public <T extends Enum<?>> void snapshotEntityStatus(BusinessEntityWithStatus<?, T> entity) {
    }

    @Override
    public void stateChanged() {
    }

    @Override
    public void resetCompensation() {
    }

    public static CompensationContext getInstance() {
        return instance;
    }

    @Override
    public void snapshotEntities(Collection<? extends BusinessEntity<?>> entities) {
    }

    @Override
    public void snapshotNewEntities(Collection<? extends BusinessEntity<?>> entities) {
    }
}
