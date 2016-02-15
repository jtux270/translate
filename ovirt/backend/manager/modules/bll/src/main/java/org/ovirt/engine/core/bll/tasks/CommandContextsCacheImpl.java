package org.ovirt.engine.core.bll.tasks;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.bll.job.ExecutionContext;
import org.ovirt.engine.core.bll.job.JobRepositoryFactory;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandContextsCache;
import org.ovirt.engine.core.common.businessentities.CommandEntity;
import org.ovirt.engine.core.compat.Guid;

public class CommandContextsCacheImpl implements CommandContextsCache {

    private static final String COMMAND_CONTEXT_MAP_NAME = "commandContextMap";
    private final CommandsCache commandsCache;
    private CacheWrapper<Guid, CommandContext> contextsMap;
    private volatile boolean cacheInitialized;
    private Object LOCK = new Object();

    public CommandContextsCacheImpl(final CommandsCache commandsCache) {
        this.commandsCache = commandsCache;
        contextsMap = CacheProviderFactory.<Guid, CommandContext> getCacheWrapper(COMMAND_CONTEXT_MAP_NAME);
    }

    private void initializeCache() {
        if (!cacheInitialized) {
            synchronized(LOCK) {
                if (!cacheInitialized) {
                    for (Guid cmdId : commandsCache.keySet()) {
                        contextsMap.put(cmdId, buildCommandContext(commandsCache.get(cmdId)));
                    }
                    cacheInitialized = true;
                }
            }
        }
    }

    private CommandContext buildCommandContext(CommandEntity cmdEntity) {
        ExecutionContext executionContext = new ExecutionContext();
        if (!Guid.isNullOrEmpty(cmdEntity.getJobId())) {
            executionContext.setJob(JobRepositoryFactory.getJobRepository().getJobWithSteps(cmdEntity.getJobId()));
        } else if (!Guid.isNullOrEmpty(cmdEntity.getStepId())) {
            executionContext.setStep(JobRepositoryFactory.getJobRepository().getStep(cmdEntity.getStepId()));
        }
        return new CommandContext(new EngineContext()).withExecutionContext(executionContext);
    }

    @Override
    public CommandContext get(Guid commandId) {
        initializeCache();
        return contextsMap.get(commandId);
    }

    @Override
    public void remove(final Guid commandId) {
        contextsMap.remove(commandId);
    }

    @Override
    public void put(final Guid cmdId, final CommandContext context) {
        contextsMap.put(cmdId, context);
    }

}
