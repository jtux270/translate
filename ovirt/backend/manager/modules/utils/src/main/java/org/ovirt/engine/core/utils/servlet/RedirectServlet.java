package org.ovirt.engine.core.utils.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.utils.EngineLocalConfig;

/**
 * This servlet redirects a request to another URL which is defined as an init param. The key of the init param is
 * url.
 * <br />
 * For instance: <br />
 * &lt;init-param&gt; <br />
 * &nbsp;&nbsp;&lt;param-name&gt;url&lt;/param-name&gt;<br />
 * &nbsp;&nbsp;&lt;param-value&gt;/ovirt-engine/&lt;/param-value&gt;<br />
 * &lt;/init-param&gt; <br />
 * The url param value can contain property expressions for expanding Engine property values in
 * form of %{PROP_NAME}.
 */
public class RedirectServlet extends HttpServlet {
    private static final long serialVersionUID = -1794616863361241804L;

    private static final String URL = "url";
    private static final String URL404 = "url404";

    private String url;
    private String url404;
    private static final String url404Default = EngineLocalConfig.getInstance().getEngineURI() + "/404.html";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // we use %{x} convention to avoid conflict with jboss properties
        url = EngineLocalConfig.getInstance().expandString(config.getInitParameter(URL).replaceAll("%\\{", "\\${"));
        String url404ConfigValue = config.getInitParameter(URL404);
        if (StringUtils.isNotEmpty(url404ConfigValue)) {
            url404 = EngineLocalConfig.getInstance().expandString(url404ConfigValue.replaceAll("%\\{", "\\${"));
        } else {
            url404 = url404Default;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String redirectUrl = url;
        if (StringUtils.isNotEmpty(redirectUrl)) {
            String queryString = request.getQueryString();
            if (StringUtils.isNotEmpty(queryString)) {
                if (redirectUrl.indexOf("?") == -1) {
                    redirectUrl += "?";
                } else {
                    redirectUrl += "&";
                }
                redirectUrl += queryString;
            }
        } else {
            redirectUrl = url404;
        }
        response.sendRedirect(redirectUrl);
    }
}
