package org.ovirt.engine.core.utils.ejb;

/**
 * Enum that defines the beans in engine. All beans must have literals dedfined in this enum
 *
 *
 */
public enum BeanType {
    BACKEND, // Backend bean
    SCHEDULER, // SchedulerUtil
    VDS_EVENT_LISTENER,
    LOCK_MANAGER,
    EVENTQUEUE_MANAGER,
    CACHE_CONTAINER;

}
