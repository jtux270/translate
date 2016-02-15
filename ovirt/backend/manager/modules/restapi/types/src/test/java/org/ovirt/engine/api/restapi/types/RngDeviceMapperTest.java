package org.ovirt.engine.api.restapi.types;

import org.junit.Test;
import org.ovirt.engine.api.model.Rate;
import org.ovirt.engine.api.model.RngDevice;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;


import static org.junit.Assert.assertEquals;


public class RngDeviceMapperTest {

    @Test
    public void testMapFromBackendToRest() throws Exception {
        VmRngDevice entity = new VmRngDevice();
        entity.setBytes(11);
        entity.setPeriod(10);
        entity.setSource(VmRngDevice.Source.RANDOM);

        RngDevice expected = new RngDevice();
        expected.setRate(new Rate());
        expected.getRate().setBytes(11);
        expected.getRate().setPeriod(10);
        expected.setSource(VmRngDevice.Source.RANDOM.toString());

        assertEquals(expected.getRate().getBytes(), RngDeviceMapper.map(entity, null).getRate().getBytes());
        assertEquals(expected.getRate().getPeriod(), RngDeviceMapper.map(entity, null).getRate().getPeriod());
        assertEquals(expected.getSource(), RngDeviceMapper.map(entity, null).getSource());
    }

    @Test
    public void testMapFromRestToBackend() throws Exception {
        RngDevice model = new RngDevice();
        model.setSource(VmRngDevice.Source.HWRNG.toString());
        model.setRate(new Rate());
        model.getRate().setBytes(10);
        model.getRate().setPeriod(11);

        VmRngDevice expected = new VmRngDevice();
        expected.setBytes(10);
        expected.setPeriod(11);
        expected.setSource(VmRngDevice.Source.HWRNG);

        assertEquals(expected, RngDeviceMapper.map(model, null));
    }
}
