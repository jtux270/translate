package org.ovirt.engine.core.jboss_auth_plugin;

import java.io.IOException;

import org.jboss.as.domain.management.plugin.AuthenticationPlugIn;
import org.jboss.as.domain.management.plugin.AuthorizationPlugIn;
import org.jboss.as.domain.management.plugin.Credential;
import org.jboss.as.domain.management.plugin.PlugInProvider;

public class OvirtAuthPlugInProvider implements PlugInProvider {

    @Override
    public AuthenticationPlugIn<Credential> loadAuthenticationPlugIn(String s) {
        if ("OvirtAuth".equals(s)) {
            return new OvirtAuthPlugIn();
        }
        return null;
    }

    /**
     * Authorization is checked in the backend and is part of the authentication procedure. If a user is authenticated
     * then he is allowed all actions exposed by this management interface
     */
    @Override
    public AuthorizationPlugIn loadAuthorizationPlugIn(String s) {
        return new AuthorizationPlugIn() {
            @Override
            public String[] loadRoles(String s, String s1) throws IOException {
                return new String[0];
            }
        };
    }

}
