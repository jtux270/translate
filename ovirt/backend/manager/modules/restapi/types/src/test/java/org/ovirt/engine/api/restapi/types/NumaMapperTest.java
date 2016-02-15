package org.ovirt.engine.api.restapi.types;

import java.util.List;

import org.ovirt.engine.api.model.Core;
import org.ovirt.engine.api.model.NumaNodePin;
import org.ovirt.engine.api.model.VirtualNumaNode;
import org.ovirt.engine.core.common.businessentities.VmNumaNode;

public class NumaMapperTest extends AbstractInvertibleMappingTest<VirtualNumaNode, VmNumaNode, VmNumaNode> {

    public NumaMapperTest() {
        super(VirtualNumaNode.class, VmNumaNode.class, VmNumaNode.class);
    }

    @Override
    protected void verify(VirtualNumaNode model, VirtualNumaNode transform) {
        assertNotNull(transform);
        assertEquals(model.getId(), transform.getId());
        assertEquals(model.getIndex(), transform.getIndex());
        assertEquals(model.getMemory(), transform.getMemory());
        List<Core> cpus1 = model.getCpu().getCores().getCore();
        List<Core> cpus2 = transform.getCpu().getCores().getCore();
        assertEquals(cpus1.size(), cpus2.size());
        for (int i = 0; i < cpus1.size(); i++) {
            assertEquals(cpus1.get(i).getIndex(), cpus2.get(i).getIndex());
        }
        List<NumaNodePin> pins1 = model.getNumaNodePins().getNumaNodePin();
        List<NumaNodePin> pins2 = transform.getNumaNodePins().getNumaNodePin();
        assertEquals(pins1.size(), pins2.size());
        for (int i = 0; i < pins1.size(); i++) {
            assertEquals(pins1.get(i).getIndex(), pins2.get(i).getIndex());
        }
    }
}
