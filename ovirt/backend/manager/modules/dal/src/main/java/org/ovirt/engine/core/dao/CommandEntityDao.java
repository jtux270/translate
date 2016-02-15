package org.ovirt.engine.core.dao;

import java.util.Date;

import org.ovirt.engine.core.common.businessentities.CommandEntity;
import org.ovirt.engine.core.compat.CommandStatus;
import org.ovirt.engine.core.compat.Guid;

public interface CommandEntityDao extends GenericDao<CommandEntity, Guid> {

    void saveOrUpdate(CommandEntity commandEntity);

    void updateStatus(Guid command, CommandStatus status);

    void updateExecuted(Guid id);

    void updateNotified(Guid id);

    void removeAllBeforeDate(Date cutoff);

}
