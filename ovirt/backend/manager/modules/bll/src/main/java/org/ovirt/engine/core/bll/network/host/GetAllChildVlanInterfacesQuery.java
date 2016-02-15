package org.ovirt.engine.core.bll.network.host;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.queries.InterfaceAndIdQueryParameters;
import org.ovirt.engine.core.utils.NetworkUtils;

/**
 * This query get interface and return all it's interface vlans, i.e input: eth2
 * return: eth2.4 eth2.5
 */
public class GetAllChildVlanInterfacesQuery<P extends InterfaceAndIdQueryParameters>
        extends QueriesCommandBase<P> {
    public GetAllChildVlanInterfacesQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        ArrayList<VdsNetworkInterface> retVal = new ArrayList<VdsNetworkInterface>();
        if (!NetworkUtils.isVlan(getParameters().getInterface())) {
            List<VdsNetworkInterface> vdsInterfaces =
                    getDbFacade().getInterfaceDao().getAllInterfacesForVds(getParameters().getId());
            for (int i = 0; i < vdsInterfaces.size(); i++) {
                if (NetworkUtils.isVlan(vdsInterfaces.get(i))) {
                    if (NetworkUtils.interfaceBasedOn(vdsInterfaces.get(i),
                            getParameters().getInterface().getName())) {
                        retVal.add(vdsInterfaces.get(i));
                    }
                }
            }
        }
        getQueryReturnValue().setReturnValue(retVal);
    }
}
