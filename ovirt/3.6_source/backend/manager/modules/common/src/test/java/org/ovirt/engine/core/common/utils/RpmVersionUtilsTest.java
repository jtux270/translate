package org.ovirt.engine.core.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RpmVersionUtilsTest {

    @Test
    public void testCompareRpmRelease() {
        assertEquals(-1, RpmVersionUtils.compareRpmParts("2.3.10.4.fc18.x86_64", "2.3.10.4.fc19.x86_64"));
        assertEquals(1, RpmVersionUtils.compareRpmParts("20130821.fc18.x86_64", "20130820.fc18.x86_64"));
        assertEquals(-1, RpmVersionUtils.compareRpmParts("20130820.0.fc18.x86_64", "20130820.1.fc18.x86_64"));
        assertEquals(1, RpmVersionUtils.compareRpmParts("20130820.0.fc18.x86_64", "20130820.fc18.x86_64"));
        assertEquals(1, RpmVersionUtils.compareRpmParts("20130820.1.3.fc18.x86_64", "20130820.1.fc18.x86_64"));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalRpmSplitNoDashes() {
        assertEquals(-1, RpmVersionUtils.splitRpmToParts("abcdef"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalRpmSplitNoEnoughDashes() {
        assertEquals(-1, RpmVersionUtils.splitRpmToParts("abcdef-123"));
    }

    @Test
    public void testISplitRpm() {

        String[] parts = RpmVersionUtils.splitRpmToParts("ovirt-iso-node-2.6.0-20130820.fc18.x86_64");
        assertEquals("ovirt-iso-node", parts[0]);
        assertEquals("2.6.0", parts[1]);
        assertEquals("20130820.fc18.x86_64", parts[2]);
    }

    @Test
    public void testFillCompsArray() {
        StringBuilder[] result = RpmVersionUtils.fillCompsArray("20130820.1.3.fc18.x86_64");
        String[] expected = {"20130820", "1", "3", "fc", "18", "x", "86", "64"};
        int counter = 0;
        while (true) {
            assertEquals(expected[counter], result[counter].toString());
            counter++;
            if (counter == result.length || result[counter] == null) {
                break;
            }
        }
    }

}
