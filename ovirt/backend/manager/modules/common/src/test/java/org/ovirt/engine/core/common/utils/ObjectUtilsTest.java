package org.ovirt.engine.core.common.utils;

import java.math.BigDecimal;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ObjectUtilsTest {

    @Test
    public void testObjectsEqual() {
        Integer ten = Integer.valueOf(10);
        assertFalse(ObjectUtils.objectsEqual(ten, Integer.valueOf(20)));
        assertTrue(ObjectUtils.objectsEqual(ten, Integer.valueOf(10)));
        assertTrue(ObjectUtils.objectsEqual(null, null));
        assertFalse(ObjectUtils.objectsEqual(ten, null));
        assertFalse(ObjectUtils.objectsEqual(null, ten));
    }

    @Test
    public void testBigDecimalEqual() {
        assertTrue(ObjectUtils.bigDecimalEqual(new BigDecimal("0"), new BigDecimal("0.0")));
        assertTrue(ObjectUtils.bigDecimalEqual(new BigDecimal("0.0"), new BigDecimal("0.00")));
        assertTrue(ObjectUtils.bigDecimalEqual(new BigDecimal("1"), new BigDecimal("1.0")));
        assertTrue(ObjectUtils.bigDecimalEqual(new BigDecimal("0.1"), new BigDecimal("0.1")));
        assertTrue(ObjectUtils.bigDecimalEqual(null, null));
        assertFalse(ObjectUtils.bigDecimalEqual(null, new BigDecimal("0")));
        assertFalse(ObjectUtils.bigDecimalEqual(new BigDecimal("0"), null));
        assertFalse(ObjectUtils.bigDecimalEqual(new BigDecimal("1"), new BigDecimal("0")));
    }
}
