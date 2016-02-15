package org.ovirt.engine.core.vdsbroker.vdsbroker.predicates;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.utils.linq.Predicate;

public final class DisplayInterfaceEqualityPredicate implements Predicate<VdsNetworkInterface> {
    private final VdsNetworkInterface iface;

    public DisplayInterfaceEqualityPredicate(VdsNetworkInterface iface) {
        this.iface = iface;
    }

    @Override
    public boolean eval(VdsNetworkInterface otherIface) {
        if (iface == otherIface) {
            return true;
        }
        if (otherIface == null) {
            return false;
        }
        // at this stage both of the objects are not null
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(iface.getName(), otherIface.getName());
        eb.append(iface.getAddress(), otherIface.getAddress());
        return eb.isEquals();
    }
}
