package org.ovirt.engine.core.bll.aaa;

import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.time.DateUtils;
import org.ovirt.engine.api.extensions.ExtMap;
import org.ovirt.engine.api.extensions.Base;
import org.ovirt.engine.api.extensions.aaa.Acct;
import org.ovirt.engine.api.extensions.aaa.Authn;
import org.ovirt.engine.core.aaa.AcctUtils;
import org.ovirt.engine.core.aaa.AuthenticationProfile;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;

public class SessionDataContainer {

    private static class SessionInfo {
        private ConcurrentMap<String, Object> contentOfSession = new ConcurrentHashMap<>();

    }

    private ConcurrentMap<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    private static final String USER_PARAMETER_NAME = "user";
    private static final String PASSWORD_PARAMETER_NAME = "password";
    private static final String AUTHN_PARAMETER_NAME = "authn";
    private static final String PROFILE_PARAMETER_NAME = "profile";
    private static final String HARD_LIMIT_PARAMETER_NAME = "hard_limit";
    private static final String SOFT_LIMIT_PARAMETER_NAME = "soft_limit";

    private static final String AUTH_RECORD_PARAMETER_NAME = "auth_record";
    private static final String PRINCIPAL_RECORD_PARAMETER_NAME = "principal_record";

    private static SessionDataContainer dataProviderInstance = new SessionDataContainer();

    private SessionDataContainer() {
    }

    public static SessionDataContainer getInstance() {
        return dataProviderInstance;
    }


    /**
     * Get data by session and internal key
     *
     * @param sessionId
     *            - id of session
     * @param key
     *            - the internal key
     * @param refresh
     *            - if perform refresh of session
     * @return
     */
    public final Object getData(String sessionId, String key, boolean refresh) {
        if (sessionId == null) {
            return null;
        }
        SessionInfo sessionInfo = getSessionInfo(sessionId);
        Object value = null;
        if (sessionInfo != null) {
            if (refresh) {
                refresh(sessionInfo);

            }
            value = sessionInfo.contentOfSession.get(key);
        }
        return value;
    }

    public final void setData(String sessionId, String key, Object value) {
        SessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            sessionInfo = new SessionInfo();
            SessionInfo oldSessionInfo = sessionInfoMap.putIfAbsent(sessionId, sessionInfo);
            if (oldSessionInfo != null) {
               sessionInfo = oldSessionInfo;
            }
         }
        sessionInfo.contentOfSession.put(key, value);
    }

    private SessionInfo getSessionInfo(String sessionId) {
        return sessionInfoMap.get(sessionId);
    }


    /**
     * Remove the cached data of current session
     *
     * @param sessionId
     *            - id of current session
     */
    public final void removeSessionOnLogout(String sessionId) {
        removeSessionImpl(sessionId, Acct.ReportReason.PRINCIPAL_LOGOUT, "Prinicial %1$s has performed logout", getUserName(sessionId));
    }

    /**
     * Will run the process of cleaning expired sessions.
     */
    @OnTimerMethodAnnotation("cleanExpiredUsersSessions")
    public final void cleanExpiredUsersSessions() {
        Date now = new Date();
        Iterator<Entry<String, SessionInfo>>  iter = sessionInfoMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, SessionInfo> entry = iter.next();
            ConcurrentMap<String, Object> sessionMap = entry.getValue().contentOfSession;
            Date hardLimit = (Date) sessionMap.get(HARD_LIMIT_PARAMETER_NAME);
            Date softLimit = (Date) sessionMap.get(SOFT_LIMIT_PARAMETER_NAME);
            if ((hardLimit != null && hardLimit.before(now)) || (softLimit != null && softLimit.before(now))) {
                removeSessionImpl(entry.getKey(), Acct.ReportReason.PRINCIPAL_SESSION_EXPIRED, "Session has expired for principal %1$s", getUserName(entry.getKey()));
            }
        }
    }


    /**
     * Sets the user for the given session Id
     * @param sessionId The session to set
     * @param user The user to set
     */
    public final void setUser(String sessionId, DbUser user) {
        setData(sessionId, USER_PARAMETER_NAME, user);
    }

    public final void setHardLimit(String sessionId, Date hardLimit) {
        setData(sessionId, HARD_LIMIT_PARAMETER_NAME, hardLimit);
    }

    public final void setSoftLimit(String sessionId, Date softLimit) {
        setData(sessionId, SOFT_LIMIT_PARAMETER_NAME, softLimit);
    }

    /**
     * @param sessionId The session to get the user for
     * @param refresh Whether refreshing the session is needed
     * @return The user set for the given {@link #session}
     */
    public DbUser getUser(String sessionId, boolean refresh) {
        return (DbUser) getData(sessionId, USER_PARAMETER_NAME, refresh);
    }

    /**
     * Sets the password of the user for the current session.
     *
     * @param user the password of the user
     */
    public void setPassword(String sessionId, String password) {
        setData(sessionId, PASSWORD_PARAMETER_NAME, password);
    }

    /**
     * Returns the password of the current user stored in the session.
     *
     * @return an array of characters containing the password or
     *     <code>null</code> if the password is not available
     */
    public String getPassword(String sessionId) {
        return (String) getData(sessionId, PASSWORD_PARAMETER_NAME, false);
    }



    public void refresh(String sessionId) {
        refresh(getSessionInfo(sessionId));
    }

    public void setProfile(String sessionId, AuthenticationProfile profile) {
        setData(sessionId, PROFILE_PARAMETER_NAME, profile);
    }

    public AuthenticationProfile getProfile(String sessionId) {
        return (AuthenticationProfile) getData(sessionId, PROFILE_PARAMETER_NAME, false);
    }

    public String getPrincipalName(String sessionId) {
        String principal = null;
        ExtMap authRecord = getAuthRecord(sessionId);
        if (authRecord != null) {
            principal = authRecord.<String>get(Authn.AuthRecord.PRINCIPAL);
        }
        return principal;
    }

    public String getUserName(String sessionId) {
        return String.format(
                "%s@%s",
                getPrincipalName(sessionId),
                getProfile(sessionId) != null ? getProfile(sessionId).getName() : "N/A");
    }

    public void setAuthRecord(String engineSessionId, ExtMap authRecord) {
        setData(engineSessionId, AUTH_RECORD_PARAMETER_NAME, authRecord);
    }

    public ExtMap getAuthRecord(String engineSessionId) {
        return (ExtMap) getData(engineSessionId, AUTH_RECORD_PARAMETER_NAME, false);
    }

    public void setPrincipalRecord(String engineSessionId, ExtMap principalRecord) {
        setData(engineSessionId, PRINCIPAL_RECORD_PARAMETER_NAME, principalRecord);
    }

    public ExtMap getPrincipalRecord(String engineSessionId) {
        return (ExtMap) getData(engineSessionId, PRINCIPAL_RECORD_PARAMETER_NAME, false);
    }

    private void refresh(SessionInfo sessionInfo) {
        int softLimitValue = Config.<Integer> getValue(ConfigValues.UserSessionTimeOutInterval);
        if (softLimitValue > 0) {
            sessionInfo.contentOfSession.put(SOFT_LIMIT_PARAMETER_NAME,
                    DateUtils.addMinutes(new Date(), softLimitValue));
        }
    }

    public boolean isSessionExists(String sessionId) {
        return sessionInfoMap.containsKey(sessionId);
    }

    private void removeSessionImpl(String sessionId, int reason, String message, Object... msgArgs) {
        /*
         * So we won't need to add profile to tests
         */
        String authzName = null;
        if (getProfile(sessionId) != null) {
            authzName = getProfile(sessionId).getAuthz().getContext().<String>get(Base.ContextKeys.INSTANCE_NAME);
        }

        AcctUtils.reportRecords(reason,
                authzName,
                getPrincipalName(sessionId),
                (ExtMap) getData(sessionId, AUTH_RECORD_PARAMETER_NAME, false),
                (ExtMap) getData(sessionId, PRINCIPAL_RECORD_PARAMETER_NAME, false),
                message,
                msgArgs
                );
        sessionInfoMap.remove(sessionId);
    }
}
