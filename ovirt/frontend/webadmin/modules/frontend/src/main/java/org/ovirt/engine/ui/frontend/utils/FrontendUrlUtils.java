package org.ovirt.engine.ui.frontend.utils;

import com.google.gwt.core.client.GWT;

public class FrontendUrlUtils {

    /**
     * Returns the root URL of the server from where the application was served, based on
     * {@linkplain GWT#getModuleBaseURL GWT module base URL}.
     * <p>
     * For example, if the application was served from <code>http://www.example.com/foo/bar</code>, it will return
     * <code>http://www.example.com/</code>.
     *
     * @return Root URL of the server, including the trailing slash.
     */
    public static String getRootURL() {
        String moduleURL = GWT.getModuleBaseURL();
        String separator = "://"; //$NON-NLS-1$
        int index = moduleURL.indexOf(separator);
        index = moduleURL.indexOf("/", index + separator.length()); //$NON-NLS-1$
        String result = (index != -1) ? moduleURL.substring(0, index + 1) : moduleURL;
        if (!result.endsWith("/")) { //$NON-NLS-1$
            result += "/"; //$NON-NLS-1$
        }
        return result;
    }

    /**
     * Returns the current page URL without hash fragment and query part, based on current window location.
     * <p>
     * For example, if the current window location is <code>http://www.example.com/page?query#hash</code>, it will
     * return <code>http://www.example.com/page</code>.
     *
     * @return Current page URL without hash fragment and query part.
     */
    public static native String getCurrentPageURL() /*-{
        var url = $wnd.location.href;

        // Pull off hash fragment part
        var i = url.indexOf('#');
        url = (i != -1) ? url.substring(0, i) : url;

        // Pull off query part
        i = url.indexOf('?');
        url = (i != -1) ? url.substring(0, i) : url;

        return url;
    }-*/;

}
