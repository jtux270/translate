package org.ovirt.engine.extensions.aaa.builtin.kerberosldap;

import java.util.EnumMap;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.compat.Guid;

public class SearchQueryFotmatter implements LdapQueryFormatter<LdapQueryExecution> {

    private final EnumMap<SearchLangageLDAPTokens, String> tokensToLDAPKeys;
    private LdapFilterSearchEnginePreProcessor preProcessor;
    private LdapIdEncoder ldapIdEncoder;

    public SearchQueryFotmatter(EnumMap<SearchLangageLDAPTokens, String> tokensToLDAPKeys,
            LdapFilterSearchEnginePreProcessor p, LdapIdEncoder ldapIdEncoder) {
        this.tokensToLDAPKeys = tokensToLDAPKeys;
        this.preProcessor = p;
        this.ldapIdEncoder = ldapIdEncoder;
    }

    public SearchQueryFotmatter(EnumMap<SearchLangageLDAPTokens, String> tokensToLDAPKeys,
            LdapFilterSearchEnginePreProcessor p) {
        this(tokensToLDAPKeys, p, new DefaultIdEncoder());
    }

    /**
     * Replace the keywords generated by the SyntaxChecker class with the provider-type specific LDAP query. The
     * replacement is basically identifying Dollar sign, "$" as a prefix for the token and then fetching the token value
     * from the map. e.g. an search expression like this (&($LDAP_USER_ACCOUNT)($GIVENNAME=John)) should look
     * $LDAP_USER_ACCOUNT in
     */
    @Override
    public LdapQueryExecution format(LdapQueryMetadata queryMetadata) {

        String filter = (String) queryMetadata.getQueryData().getFilterParameters()[0];
        filter = preProcessor.preProcess(filter);
        for (Entry<SearchLangageLDAPTokens, String> tokenEntry : tokensToLDAPKeys.entrySet()) {
            filter = StringUtils.replace(filter, tokenEntry.getKey().name(), tokenEntry.getValue());
        }
        String userIdToken = tokensToLDAPKeys.get(SearchLangageLDAPTokens.$USER_ID);
        filter = encodeGuids(filter, userIdToken);

        String baseDN = queryMetadata.getBaseDN();

        return new LdapQueryExecution(filter,
                        filter, // The display filter in that case is like the filter
                baseDN,
                        queryMetadata.getContextMapper(),
                        queryMetadata.getSearchScope(),
                        queryMetadata.getReturningAttributes(),
                        queryMetadata.getQueryData().getDomain());
    }

    private String encodeGuids(String filter, String userIdToken) {
        String result = filter;
        int userIdTokenIndex = filter.indexOf(userIdToken);
        if (userIdTokenIndex != -1) {
            int guidIndex = filter.indexOf('=', userIdTokenIndex) + 1;
            int bracketIndex = filter.indexOf(')', userIdTokenIndex);
            result =
                    filter.substring(0, guidIndex) +
                            ldapIdEncoder.encodedId(new Guid(filter.substring(guidIndex, bracketIndex))) +
                            encodeGuids(filter.substring(bracketIndex), userIdToken);
        }
        return result;
    }

}

