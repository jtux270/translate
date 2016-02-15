package org.ovirt.engine.core.dao;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.CommandAssociatedEntity;
import org.ovirt.engine.core.common.businessentities.CommandEntity;
import org.ovirt.engine.core.compat.CommandStatus;
import org.ovirt.engine.core.compat.Guid;

public class CommandEntityDaoTest extends BaseGenericDaoTestCase<Guid, CommandEntity, CommandEntityDao> {

    @Override
    protected CommandEntity generateNewEntity() {

        CommandEntity commandEntity = new CommandEntity();
        commandEntity.setCommandType(VdcActionType.AddBond);
        commandEntity.setCreatedAt(new Date(System.currentTimeMillis()));
        commandEntity.setId(Guid.newGuid());
        commandEntity.setCommandStatus(CommandStatus.ACTIVE);
        VdcActionParametersBase params = new VdcActionParametersBase();
        commandEntity.setCommandParameters(params);
        return commandEntity;
    }

    @Override
    protected void updateExistingEntity() {
        // TODO Auto-generated method stub

    }

    @Override
    protected Guid getExistingEntityId() {
        return FixturesTool.EXISTING_COMMAND_ENTITY_ID;
    }

    @Override
    protected CommandEntityDao prepareDao() {
        return dbFacade.getCommandEntityDao();
    }

    @Override
    protected Guid generateNonExistingId() {
        return Guid.newGuid();
    }

    @Override
    protected int getEneitiesTotalCount() {
        return 3;
    }

    @Test
    public void testRemove() {
        CommandEntity cmd = dbFacade.getCommandEntityDao().get(getExistingEntityId());
        assertNotNull(cmd);
        dbFacade.getCommandEntityDao().remove(getExistingEntityId());
        CommandEntity cmdAfterRemoval = dbFacade.getCommandEntityDao().get(getExistingEntityId());
        assertNull(cmdAfterRemoval);
    }

    @Test
    public void testGetAll() {
        List<CommandEntity> cmds = dbFacade.getCommandEntityDao().getAll();
        assertNotNull(cmds);
        assertTrue(cmds.size() > 0);
    }

    @Test
    public void testGetCommandIdsByEntity() {
        Guid storageId = Guid.newGuid();
        CommandEntity cmdEntity1 = generateNewEntity();
        dao.save(cmdEntity1);
        Set<CommandAssociatedEntity> cocoCmdEntities1 = new HashSet<>();
        cocoCmdEntities1.add(new CommandAssociatedEntity(cmdEntity1.getId(), VdcObjectType.Storage, storageId));
        cocoCmdEntities1.add(new CommandAssociatedEntity(cmdEntity1.getId(), VdcObjectType.Disk, Guid.newGuid()));
        dao.insertCommandAssociatedEntities(cocoCmdEntities1);

        CommandEntity cmdEntity2 = generateNewEntity();
        dao.save(cmdEntity2);
        Set<CommandAssociatedEntity> cocoCmdEntities2 = new HashSet<>();
        cocoCmdEntities2.add(new CommandAssociatedEntity(cmdEntity2.getId(), VdcObjectType.Storage, storageId));
        cocoCmdEntities2.add(new CommandAssociatedEntity(cmdEntity2.getId(), VdcObjectType.Disk, Guid.newGuid()));
        dao.insertCommandAssociatedEntities(cocoCmdEntities2);

        List<Guid> cmIds = dao.getCommandIdsByEntity(storageId);
        assertNotNull(cmIds);
        assertThat(cmIds, hasSize(2));
        assertThat(cmIds, hasItems(cmdEntity1.getId(), cmdEntity2.getId()));
    }

    @Test
    public void testGetAllInsertAsyncTaskEntitities() {
        CommandEntity cmdEntity = generateNewEntity();
        dao.save(cmdEntity);
        Set<CommandAssociatedEntity> cocoCmdEntities = new HashSet<>();
        cocoCmdEntities.add(new CommandAssociatedEntity(cmdEntity.getId(), VdcObjectType.Storage, Guid.newGuid()));
        cocoCmdEntities.add(new CommandAssociatedEntity(cmdEntity.getId(), VdcObjectType.Disk, Guid.newGuid()));
        dao.insertCommandAssociatedEntities(cocoCmdEntities);
        List<CommandAssociatedEntity> entities = dao.getAllCommandAssociatedEntities(cmdEntity.getId());
        assertNotNull(entities);
        assertEquals(2, entities.size());
        assertThat(entities, hasItems(cocoCmdEntities.toArray(new CommandAssociatedEntity[cocoCmdEntities.size()])));
    }
}
