package org.ovirt.engine.core.common.queries;

import java.io.Serializable;

public class VdcQueryParametersBase implements Serializable {
    private static final long serialVersionUID = -6766170283465888549L;

    /**
     * The identifier of session which should be set by sender via Rest Api or by front end
     */
    private String sessionId;
    /**
     * The identifier of session which should be set by web client of front end
     */
    private String httpSessionId;

    /**
     * The boolean flag which provides if the session should be refreshed
     */
    private boolean refresh;

    /**
     * The boolean flag which specifies if the query should be filtered
     * (e.g., according to user permissions as opposed to the default, which is running as admin)
     */
    private boolean isFiltered;

    public VdcQueryParametersBase() {
        refresh = true;
    }

    public VdcQueryParametersBase(String sessionId) {
        this();
        this.sessionId = sessionId;

    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getHttpSessionId() {
        return httpSessionId;
    }

    public void setHttpSessionId(String httpSessionId) {
        this.httpSessionId = httpSessionId;
    }

    public boolean getRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    public VdcQueryParametersBase withoutRefresh() {
        setRefresh(false);
        return this;
    }

    public boolean isFiltered() {
        return isFiltered;
    }

    public void setFiltered(boolean isFiltered) {
        this.isFiltered = isFiltered;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(25);
        builder.append("refresh: ");
        builder.append(refresh);
        builder.append(", filtered: ");
        builder.append(isFiltered);
        return builder.toString();
    }
}
