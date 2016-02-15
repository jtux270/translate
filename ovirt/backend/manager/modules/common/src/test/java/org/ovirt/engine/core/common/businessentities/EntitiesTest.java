package org.ovirt.engine.core.common.businessentities;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.compat.Guid;

public class EntitiesTest {

    private static final Guid[] GUIDs = {
            new Guid("000000000000-0000-0000-0000-00000001"),
            new Guid("000000000000-0000-0000-0000-00000002"),
            new Guid("000000000000-0000-0000-0000-00000003"),
            new Guid("000000000000-0000-0000-0000-00000004") };

    @Test
    public void businessEntitiesById() {
        List<VmDevice> list = new ArrayList<VmDevice>();

        VmDeviceId id1 = new VmDeviceId(GUIDs[0], GUIDs[1]);
        VmDeviceId id2 = new VmDeviceId(GUIDs[2], GUIDs[3]);

        VmDevice d1 = new VmDevice();
        d1.setId(id1);
        VmDevice d2 = new VmDevice();
        d2.setId(id2);

        list.add(d1);
        list.add(d2);

        Map<VmDeviceId, VmDevice> businessEntitiesById = Entities.businessEntitiesById(list);

        Assert.assertTrue(businessEntitiesById.containsKey(id1));
        Assert.assertTrue(businessEntitiesById.containsKey(id2));

        Assert.assertFalse(businessEntitiesById.containsKey(new VmDeviceId(GUIDs[0], GUIDs[3])));
    }

    @Test
    public void objectNames() {
        List<Network> list = new ArrayList<Network>();
        Network n1 = new Network();
        n1.setName("network1");
        Network n2 = new Network();
        n2.setName("network2");
        list.add(n1);
        list.add(n2);
        Set<String> names = Entities.objectNames(list);
        Assert.assertTrue(names.size() == 2);
        Assert.assertTrue(names.contains("network1"));
        Assert.assertTrue(names.contains("network2"));
        Assert.assertFalse(names.contains("network3"));
        Assert.assertTrue(Entities.objectNames(null).equals(Collections.emptySet()));
        Assert.assertTrue(Entities.objectNames(new ArrayList<Network>()).equals(Collections.emptySet()));
    }

    @Test
    public void collectiontoStringNull() {
        assertEquals("[]", Entities.collectionToString(null, ""));
    }

    @Test
    public void collectiontoStringEmpty() {
        assertEquals("[]", Entities.collectionToString(Collections.emptyList(), ""));
    }

    @Test
    public void collectiontoStringOneElement() {
        String s = "abc";
        assertEquals("[" + s + "]", Entities.collectionToString(Arrays.asList(s), ""));
    }

    @Test
    public void collectiontoStringMultipleElements() {
        String s = "abc";
        assertEquals("[" + s + ",\n" + s + "]", Entities.collectionToString(Arrays.asList(s, s), ""));
    }

    @Test
    public void collectiontoStringMultipleElementsWithPrefix() {
        String s = "abc";
        String p = "   ";
        assertEquals("[" + s + ",\n" + p + s + "]", Entities.collectionToString(Arrays.asList(s, s), p));
    }
}
