package org.ovirt.engine.api.restapi.types;

import org.junit.Test;
import org.ovirt.engine.api.model.Bonding;
import org.ovirt.engine.api.model.HostNIC;
import org.ovirt.engine.api.model.QoS;
import org.ovirt.engine.api.model.QosType;
import org.ovirt.engine.api.model.Slaves;
import org.ovirt.engine.core.common.businessentities.network.Bond;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.RandomUtils;

public class HostNicMapperTest extends AbstractInvertibleMappingTest<HostNIC, VdsNetworkInterface, VdsNetworkInterface> {

    public HostNicMapperTest() {
        super(HostNIC.class, VdsNetworkInterface.class, VdsNetworkInterface.class);
    }

    @Override
    protected void verify(HostNIC model, HostNIC transform) {
        assertNotNull(transform);
        assertEquals(model.getName(), transform.getName());
        assertEquals(model.getId(), transform.getId());
        assertNotNull(transform.getNetwork());
        assertEquals(model.getNetwork().getName(), transform.getNetwork().getName());
        assertNotNull(transform.getIp());
        assertEquals(model.getIp().getAddress(), transform.getIp().getAddress());
        assertEquals(model.getIp().getNetmask(), transform.getIp().getNetmask());
        assertEquals(model.getIp().getGateway(), transform.getIp().getGateway());
        assertNotNull(transform.getMac());
        assertEquals(model.getMac().getAddress(), transform.getMac().getAddress());
        assertNotNull(model.getBonding());
        assertEquals(model.getBonding().getOptions().getOptions().size(), transform.getBonding()
                .getOptions()
                .getOptions()
                .size());
        for (int i = 0; i < model.getBonding().getOptions().getOptions().size(); i++) {
            assertEquals(model.getBonding().getOptions().getOptions().get(i).getName(), transform.getBonding()
                    .getOptions()
                    .getOptions()
                    .get(i)
                    .getName());
            assertEquals(model.getBonding().getOptions().getOptions().get(i).getValue(), transform.getBonding()
                    .getOptions()
                    .getOptions()
                    .get(i)
                    .getValue());
        }

        assertNotNull(model.getProperties());
    }

    @Test
    public void testCustomNetworkConfigurationMapped() throws Exception {
        VdsNetworkInterface entity = new VdsNetworkInterface();
        HostNIC model = HostNicMapper.map(entity, null);
        assertFalse(model.isSetCustomConfiguration());

        entity.setNetworkImplementationDetails(new VdsNetworkInterface.NetworkImplementationDetails(false, true));
        model = HostNicMapper.map(entity, null);
        assertEquals(entity.getNetworkImplementationDetails().isInSync(), !model.isCustomConfiguration());

        entity.setNetworkImplementationDetails(new VdsNetworkInterface.NetworkImplementationDetails(true, true));
        model = HostNicMapper.map(entity, null);
        assertEquals(entity.getNetworkImplementationDetails().isInSync(), !model.isCustomConfiguration());
    }

    @Test
    public void testBondMapping() {
        HostNIC model = new HostNIC();
        model.setId(Guid.newGuid().toString());
        model.setName(RandomUtils.instance().nextString(10));
        model.setBonding(new Bonding());
        model.getBonding().setSlaves(new Slaves());
        HostNIC slaveA = new HostNIC();
        slaveA.setName(RandomUtils.instance().nextString(10));
        model.getBonding().getSlaves().getSlaves().add(slaveA);
        Bond entity = HostNicMapper.map(model, null);
        assertNotNull(entity);
        assertEquals(model.getId(), entity.getId().toString());
        assertEquals(model.getName(), entity.getName());
        assertEquals(model.getBonding().getSlaves().getSlaves().size(), entity.getSlaves().size());
        for (HostNIC slave : model.getBonding().getSlaves().getSlaves()) {
            assertTrue(entity.getSlaves().contains(slave.getName()));
        }
    }

    @Override
    protected HostNIC postPopulate(HostNIC model) {
        HostNIC hostNIC = super.postPopulate(model);
        QoS qos = hostNIC.getQos();
        qos.setType(QosType.HOSTNETWORK.name());
        qos.setName(null);
        qos.setDataCenter(null);
        return hostNIC;
    }
}
