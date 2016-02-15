package org.ovirt.engine.core.common.action;

import java.io.Serializable;
import org.ovirt.engine.core.compat.Guid;

public class LogoutUserParameters extends VdcActionParametersBase implements Serializable {
    private static final long serialVersionUID = -8545136602971701926L;
    private Guid _userId;

    public LogoutUserParameters(Guid userId) {
        _userId = userId;
    }

    public Guid getUserId() {
        return _userId;
    }

    public LogoutUserParameters() {
        _userId = Guid.Empty;
    }
}
