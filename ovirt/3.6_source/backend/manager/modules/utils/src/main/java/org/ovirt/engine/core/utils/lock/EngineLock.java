package org.ovirt.engine.core.utils.lock;

import java.util.Map;

import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.ToStringBuilder;

/**
 *The following class is represent a lock which is used in the system
 */
public class EngineLock implements AutoCloseable {

    private Map<String, Pair<String, String>> exclusiveLocks;
    private Map<String, Pair<String, String>> sharedLocks;

    public EngineLock() {

    }

    public EngineLock(Map<String, Pair<String, String>> exclusiveLocks) {
        this.exclusiveLocks = exclusiveLocks;
    }

    public EngineLock(Map<String, Pair<String, String>> exclusiveLocks, Map<String, Pair<String, String>> sharedLocks) {
        this(exclusiveLocks);
        this.sharedLocks = sharedLocks;
    }

    public Map<String, Pair<String, String>> getExclusiveLocks() {
        return exclusiveLocks;
    }

    public void setExclusiveLocks(Map<String, Pair<String, String>> exclusiveLocks) {
        this.exclusiveLocks = exclusiveLocks;
    }

    public Map<String, Pair<String, String>> getSharedLocks() {
        return sharedLocks;
    }

    public void setSharedLocks(Map<String, Pair<String, String>> sharedLocks) {
        this.sharedLocks = sharedLocks;
    }

    @Override
    public String toString() {
        return ToStringBuilder.forInstance(this)
                .append("exclusiveLocks", exclusiveLocks)
                .append("sharedLocks", sharedLocks)
                .build();
    }

    @Override
    public void close() {
        LockManagerFactory.getLockManager().releaseLock(this);
    }

}
