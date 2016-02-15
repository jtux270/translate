package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import org.ovirt.engine.api.model.NIC;
import org.ovirt.engine.api.model.Statistic;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.restapi.util.RxTxCalculator;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatistics;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.compat.Guid;


public class NicStatisticalQuery extends AbstractStatisticalQuery<NIC, VmNetworkInterface> {

    private static final Statistic DATA_RX = create("data.current.rx", "Receive data rate",  GAUGE,   BYTES_PER_SECOND, DECIMAL);
    private static final Statistic DATA_TX = create("data.current.tx", "Transmit data rate", GAUGE,   BYTES_PER_SECOND, DECIMAL);
    private static final Statistic ERRS_RX = create("errors.total.rx", "Total transmit errors",     COUNTER, NONE,            INTEGER);
    private static final Statistic ERRS_TX = create("errors.total.tx", "Total transmit errors",        COUNTER, NONE,            INTEGER);

    protected NicStatisticalQuery(NIC parent) {
        this(null, parent);
    }

    protected NicStatisticalQuery(AbstractBackendResource<NIC, VmNetworkInterface>.EntityIdResolver<Guid> resolver, NIC parent) {
        super(NIC.class, parent, resolver);
    }

    @Override
    public List<Statistic> getStatistics(VmNetworkInterface iface) {
        NetworkStatistics s = iface.getStatistics();
        return asList(setDatum(clone(DATA_RX), RxTxCalculator.percent2bytes(iface.getSpeed(), s.getReceiveRate())),
                setDatum(clone(DATA_TX), RxTxCalculator.percent2bytes(iface.getSpeed(), s.getTransmitRate())),
                setDatum(clone(ERRS_RX), s.getReceiveDropRate()),
                setDatum(clone(ERRS_TX), s.getTransmitDropRate()));
    }

    @Override
    public Statistic adopt(Statistic statistic) {
        // clone required because LinkHelper unsets the grandparent
        statistic.setNic(clone(parent));
        return statistic;
    }

    private NIC clone(NIC parent) {
        NIC nic = new NIC();
        nic.setId(parent.getId());
        nic.setVm(new VM());
        nic.getVm().setId(parent.getVm().getId());
        return nic;
    }
}
