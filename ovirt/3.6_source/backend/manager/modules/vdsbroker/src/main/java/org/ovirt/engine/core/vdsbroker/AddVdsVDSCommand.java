package org.ovirt.engine.core.vdsbroker;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.vdscommands.AddVdsVDSCommandParameters;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddVdsVDSCommand<P extends AddVdsVDSCommandParameters> extends VdsIdVDSCommandBase<P> {

    private static final Logger log = LoggerFactory.getLogger(AddVdsVDSCommand.class);

    public AddVdsVDSCommand(P parameters) {
        super(parameters, true);
    }

    @Override
    protected void executeVdsIdCommand() {
        log.info("AddVds - entered , starting logic to add VDS '{}'", getVdsId());
        VDS vds = DbFacade.getInstance().getVdsDao().get(getVdsId());
        log.info("AddVds - VDS '{}' was added, will try to add it to the resource manager",
                getVdsId());
        ResourceManager.getInstance().AddVds(vds, false);
    }
}
