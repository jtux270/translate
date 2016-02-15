package org.ovirt.engine.core.bll.network.host;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.queries.InterfaceAndIdQueryParameters;
import org.ovirt.engine.core.utils.NetworkUtils;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;

/**
 * This query get vlan interface and return all it's siblings, i.e input: eth2.2
 * return: eth2.4 eth2.5 (without eth2.2)
 */
public class GetAllSiblingVlanInterfacesQuery<P extends InterfaceAndIdQueryParameters>
        extends QueriesCommandBase<P> {
    public GetAllSiblingVlanInterfacesQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        ArrayList<VdsNetworkInterface> retVal = new ArrayList<VdsNetworkInterface>();
        List<VdsNetworkInterface> vdsInterfaces =
                getDbFacade().getInterfaceDao().getAllInterfacesForVds(getParameters().getId());
        VdsNetworkInterface iface = LinqUtils.firstOrNull(vdsInterfaces, new Predicate<VdsNetworkInterface>() {
            @Override
            public boolean eval(VdsNetworkInterface i) {
                return i.getName().equals(getParameters().getInterface().getName());
            }
        });

        if (iface != null && NetworkUtils.isVlan(iface)) {
            for (int i = 0; i < vdsInterfaces.size(); i++) {
                if (NetworkUtils.isVlan(vdsInterfaces.get(i))
                        && !StringUtils.equals(iface.getName(), vdsInterfaces.get(i)
                                .getName())) {
                    if (StringUtils.equals(iface.getBaseInterface(),
                            vdsInterfaces.get(i).getBaseInterface())) {
                        retVal.add(vdsInterfaces.get(i));
                    }
                }
            }
        }
        getQueryReturnValue().setReturnValue(retVal);
    }
}
