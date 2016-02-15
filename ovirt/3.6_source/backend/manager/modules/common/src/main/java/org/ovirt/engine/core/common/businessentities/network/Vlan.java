package org.ovirt.engine.core.common.businessentities.network;

import org.ovirt.engine.core.common.utils.ToStringBuilder;


public class Vlan extends VdsNetworkInterface {

    private static final long serialVersionUID = -2458958954004227402L;

    public Vlan() {
    }

    public Vlan(int vlanId, String baseInterface) {
        setVlanId(vlanId);
        setBaseInterface(baseInterface);
        setName(baseInterface + "." + vlanId);
    }

    @Override
    protected ToStringBuilder appendAttributes(ToStringBuilder tsb) {
        return super.appendAttributes(tsb)
                .append("baseInterface", getBaseInterface())
                .append("vlanId", getVlanId());
    }
}
