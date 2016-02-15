package org.ovirt.engine.core.common.queries;

import java.util.List;

import org.ovirt.engine.core.compat.Guid;

public class IdsQueryParameters extends VdcQueryParametersBase {
    private static final long serialVersionUID = 575294540991590541L;

    private List<Guid> ids;

    public IdsQueryParameters() {
    }

    public List<Guid> getIds() {
        return ids;
    }
    public void setId(List<Guid> vms) {
        this.ids = vms;
    }
}
