package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.api.model.QoS;
import org.ovirt.engine.api.model.QosType;

public class NetworkQosMapperTest extends QosMapperTest {

    @Override
    protected void verify(QoS model, QoS transform) {
        super.verify(model, transform);

        // network limits:
        assertEquals(model.getInboundAverage(), transform.getInboundAverage());
        assertEquals(model.getInboundPeak(), transform.getInboundPeak());
        assertEquals(model.getInboundBurst(), transform.getInboundBurst());
        assertEquals(model.getOutboundAverage(), transform.getOutboundAverage());
        assertEquals(model.getOutboundPeak(), transform.getOutboundPeak());
        assertEquals(model.getOutboundBurst(), transform.getOutboundBurst());
    }

    @Override
    protected QoS postPopulate(QoS model) {
        model = super.postPopulate(model);
        model.setType(QosType.NETWORK.name().toLowerCase());
        return model;

    }
}
