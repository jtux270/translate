package org.ovirt.engine.core.vdsbroker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class VdsMonitor {

    private final BlockingQueue<Boolean> queue;

    public VdsMonitor() {
        queue = new SynchronousQueue<Boolean>();
    }

    /**
     * A synchronous queue for the enclosing VDS
     *
     * @return
     */
    public BlockingQueue<Boolean> getQueue() {
        return queue;
    }
}
