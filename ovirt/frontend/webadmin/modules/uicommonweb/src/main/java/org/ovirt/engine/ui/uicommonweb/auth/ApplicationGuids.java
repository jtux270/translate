package org.ovirt.engine.ui.uicommonweb.auth;

import org.ovirt.engine.core.compat.Guid;

/**
 * This enumerates a set of well known values from the roles and ad_groups table
 */
public enum ApplicationGuids {

    // roles
    engineUser(new Guid("00000000-0000-0000-0001-000000000001")), //$NON-NLS-1$
    superUser(new Guid("00000000-0000-0000-0000-000000000001")), //$NON-NLS-1$
    userTemplateBasedVM(new Guid("def00009-0000-0000-0000-def000000009")), //$NON-NLS-1$
    quotaConsumer(new Guid("def0000a-0000-0000-0000-def00000000a")), //$NON-NLS-1$
    dataCenterAdmin(new Guid("def00002-0000-0000-0000-def000000002")), //$NON-NLS-1$
    vnicProfileUser(new Guid("DEF0000A-0000-0000-0000-DEF000000010")), //$NON-NLS-1$

    // ad_groups
    everyone(new Guid("eee00000-0000-0000-0000-123456789eee")); //$NON-NLS-1$

    private Guid guid;

    private ApplicationGuids(Guid guid) {
        this.guid = guid;
    }

    public Guid asGuid() {
        return guid;
    }
}
