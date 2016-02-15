package org.ovirt.engine.extensions.aaa.builtin.kerberosldap;

import org.ovirt.engine.api.extensions.ExtMap;
import org.ovirt.engine.api.extensions.aaa.Authn;
import org.ovirt.engine.core.common.businessentities.aaa.LdapUser;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.extensions.aaa.builtin.kerberosldap.utils.kerberos.AuthenticationResult;

public class LdapAuthenticateUserCommand extends LdapBrokerCommandBase {

    public LdapAuthenticateUserCommand(LdapUserPasswordBaseParameters parameters) {
        super(parameters);
    }

    protected void initCredentials(String domain) {

    }

    @Override
    protected void executeQuery(DirectorySearcher directorySearcher) {
        log.debug("Executing LdapAuthenticateUserCommand");

        LdapUser user = null;
        LdapQueryData queryData = new LdapQueryDataImpl();

        String loginName = getLoginName();
        ExtMap output = new ExtMap().mput(Authn.InvokeKeys.PRINCIPAL, loginName);
        setReturnValue(output);

        if (getLoginName().contains("@")) { // the user name is UPN use 'User
            // Principal Name' search
            queryData.setLdapQueryType(LdapQueryType.getUserByPrincipalName);
            // The domain in the UPN must overwrite the domain field. Discrepancies between the UPN domain and
            // the domain may lead failure in Kerberos queries
            String[] loginNameParts = getLoginName().split("@");
            String principalName = constructPrincipalName(loginNameParts[0], loginNameParts[1]);
            String domain = loginNameParts[1].toLowerCase();
            queryData.setFilterParameters(new Object[] { principalName });
            queryData.setDomain(domain);
            setDomain(domain);
            setAuthenticationDomain(domain);
        } else {
            // the user name is NT format use 'SAM Account Name' search
            setAuthenticationDomain(getDomain());
            queryData.setDomain(getDomain());
            queryData.setLdapQueryType(LdapQueryType.getUserByName);
            queryData.setFilterParameters(new Object[] { getLoginName() });
        }
        Object searchResult = directorySearcher.findOne(queryData);


        if (searchResult == null) {
            log.errorFormat("Failed authenticating user: {0} to domain {1}. Ldap Query Type is {2}",
                    getLoginName(),
                    getAuthenticationDomain(),
                    queryData.getLdapQueryType().name());
            setSucceeded(false);
            Exception ex = directorySearcher.getException();
            handleDirectorySearcherException(output, ex);
        } else {
            user = populateUserData((LdapUser) searchResult, getAuthenticationDomain());
            if (user != null) {
                setReturnValue(output.mput(
                        Authn.InvokeKeys.RESULT,
                        Authn.AuthResult.SUCCESS
                        ).mput(
                                Authn.InvokeKeys.AUTH_RECORD,
                                new ExtMap().mput(
                                        Authn.AuthRecord.PRINCIPAL,
                                        loginName
                                        )));
                setSucceeded(true);
            } else {
                log.errorFormat("Failed authenticating. Domain is {0}. User is {1}. The user doesn't have a UPN",
                        getAuthenticationDomain(),
                        getLoginName());
                output.put(Authn.InvokeKeys.RESULT, Authn.AuthResult.CREDENTIALS_INCORRECT);
            }
        }

        if (!getSucceeded()) {
            output.putIfAbsent(Authn.InvokeKeys.RESULT, Authn.AuthResult.GENERAL_ERROR);
        }
    }

    private void handleDirectorySearcherException(ExtMap output, Exception ex) {
        if (ex instanceof AuthenticationResultException) {
            AuthenticationResultException authResultException = (AuthenticationResultException) ex;
            AuthenticationResult result = authResultException.getResult();
            if (result == null) {
                result = AuthenticationResult.OTHER;
            }
            log.error(result.getDetailedMessage());
            output.put(Authn.InvokeKeys.RESULT, resultsMap.get(result));
        }
    }

    private String constructPrincipalName(String username, String domain) {
        return username + '@' + domain.toUpperCase();
    }

    private static final Log log = LogFactory.getLog(LdapAuthenticateUserCommand.class);
}
