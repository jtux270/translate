package org.ovirt.engine.core.services;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ovirt.engine.core.bll.interfaces.BackendInternal;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.utils.ejb.BeanProxyType;
import org.ovirt.engine.core.utils.ejb.BeanType;
import org.ovirt.engine.core.utils.ejb.EjbUtils;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

/*
 * This servlet gets a session ID, validates it, checks if the logged in user is administrator, and if so returns its user name
 */
public class GetSessionUser extends HttpServlet {
    private static final long serialVersionUID = -6984391651645165467L;

    private String SESSION_ID_PARAMETER = "sessionID";
    private int SUCCESS_CODE = HttpURLConnection.HTTP_OK;
    private int FAILED_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;

    private static final Log log = LogFactory.getLog(GetSessionUser.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doProcessRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doProcessRequest(request, response);
    }

    private void doProcessRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionID = request.getParameter(SESSION_ID_PARAMETER);
        if (runQuery(request, response, sessionID)) {
            response.setStatus(SUCCESS_CODE);
            log.debug("Validate Session " + sessionID + " succeeded");
        } else {
            response.setStatus(FAILED_CODE);
            log.debug("Validate Session " + sessionID + " failed");
        }
    }

    private boolean runQuery(HttpServletRequest request, HttpServletResponse response, String sessionID) {
        BackendInternal backend = null;
        VdcQueryParametersBase params = null;
        VdcQueryReturnValue queryReturnValue = null;
        boolean returnValue = false;

        backend = (BackendInternal) EjbUtils.findBean(BeanType.BACKEND, BeanProxyType.LOCAL);
        log.debug("Calling ValidateSession query");

        params = new VdcQueryParametersBase();
        params.setSessionId(sessionID);

        queryReturnValue = backend.runInternalQuery(VdcQueryType.ValidateSession, params);

        if (queryReturnValue != null) {
            returnValue = queryReturnValue.getSucceeded();

            if (returnValue) {
                DbUser user = queryReturnValue.getReturnValue();

                // We get the user name only in case the validation succeeded, and the user is an administrator
                if (user.isAdmin()) {
                    log.debug("Getting user name");
                    printUPNToResponse(response, getUPN(user));
                } else {
                    log.error("User is not authorized to perform operation");
                    returnValue = false;
                }
            }
        } else {
            log.error("Got NULL from backend.RunQuery");
        }

        return returnValue;
    }

    private void printUPNToResponse(HttpServletResponse response, String upn) {
        try {
            response.getWriter().print(upn);
        } catch (IOException e) {
            log.error("IO Exception while writing user name: ", e);
        }

    }

    private String getUPN(DbUser user) {
        String retVal = user.getLoginName();
        if (!retVal.contains("@")) {
            retVal = retVal + "@" + user.getDomain();
        }
        return retVal;
    }
}
