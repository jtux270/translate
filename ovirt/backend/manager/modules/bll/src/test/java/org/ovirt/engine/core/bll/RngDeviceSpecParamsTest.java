package org.ovirt.engine.core.bll;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;

public class RngDeviceSpecParamsTest {

    @Test
    public void testGenerateFullSpecParams() {
        VmRngDevice dev = new VmRngDevice();
        dev.setBytes(12);
        dev.setPeriod(34);
        dev.setSource(VmRngDevice.Source.RANDOM);

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put("bytes", "12");
        expectedParams.put("period", "34");
        expectedParams.put("source", "random");

        Assert.assertEquals(expectedParams, dev.getSpecParams());
    }

    @Test
    public void testGenerateSpecParams() {
        VmRngDevice dev = new VmRngDevice();
        dev.setSource(VmRngDevice.Source.HWRNG);

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put("source", "hwrng");

        Assert.assertEquals(expectedParams, dev.getSpecParams());
    }

}
