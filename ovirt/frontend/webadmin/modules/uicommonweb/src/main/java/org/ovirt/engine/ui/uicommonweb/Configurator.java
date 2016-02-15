package org.ovirt.engine.ui.uicommonweb;

import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.frontend.utils.BaseContextPathData;
import org.ovirt.engine.ui.frontend.utils.FrontendUrlUtils;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.vms.ISpice;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.LocaleInfo;

/**
 * Provides configuration values for client side.
 */
public abstract class Configurator {

    private static final String DOCUMENTATION_LIB_PATH = "html/"; //$NON-NLS-1$
    private static final String DOCUMENTATION_ROOT = BaseContextPathData.getInstance().getRelativePath()
            + "docs/manual"; //$NON-NLS-1$
    private static final String HELPTAG_MAPPING_ROOT = BaseContextPathData.getInstance().getRelativePath()
            + "docs/manual/helptag"; //$NON-NLS-1$

    private static String documentationLangPath;

    public static String getDocumentationLangPath() {
        return documentationLangPath;
    }

    public Configurator() {
        // Set default configuration values
        setSpiceFullScreen(false);

        String currentLocale = LocaleInfo.getCurrentLocale().getLocaleName();
        documentationLangPath = currentLocale.replaceAll("_", "-") + "/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        setSpiceVersion(new Version(4, 4));
        setSpiceDefaultUsbPort(32023);
        setSpiceDisableUsbListenPort(0);
        setBackendPort("8700"); //$NON-NLS-1$
        setLogLevel("INFO"); //$NON-NLS-1$
        setPollingTimerInterval(5000);
    }

    protected static final String DEFAULT_USB_FILTER = "-1,-1,-1,-1,0"; //$NON-NLS-1$
    protected String usbFilter = DEFAULT_USB_FILTER;

    private boolean isInitialized;

    /**
     * Gets or sets the value specifying what is the desired Spice version.
     */
    private Version privateSpiceVersion;

    public Version getSpiceVersion() {
        return privateSpiceVersion;
    }

    protected void setSpiceVersion(Version value) {
        privateSpiceVersion = value;
    }

    private boolean privateIsAdmin;

    public boolean getIsAdmin() {
        return privateIsAdmin;
    }

    protected void setIsAdmin(boolean value) {
        privateIsAdmin = value;
    }

    private int privateSpiceDefaultUsbPort;

    public int getSpiceDefaultUsbPort() {
        return privateSpiceDefaultUsbPort;
    }

    protected void setSpiceDefaultUsbPort(int value) {
        privateSpiceDefaultUsbPort = value;
    }

    private int privateSpiceDisableUsbListenPort;

    public int getSpiceDisableUsbListenPort() {
        return privateSpiceDisableUsbListenPort;
    }

    protected void setSpiceDisableUsbListenPort(int value) {
        privateSpiceDisableUsbListenPort = value;
    }

    private boolean privateIsUsbEnabled;

    public boolean getIsUsbEnabled() {
        return privateIsUsbEnabled;
    }

    protected void setIsUsbEnabled(boolean value) {
        privateIsUsbEnabled = value;
    }

    private boolean privateSpiceAdminConsole;

    public boolean getSpiceAdminConsole() {
        return privateSpiceAdminConsole;
    }

    protected void setSpiceAdminConsole(boolean value) {
        privateSpiceAdminConsole = value;
    }

    private boolean privateSpiceFullScreen;

    public boolean getSpiceFullScreen() {
        return privateSpiceFullScreen;
    }

    protected void setSpiceFullScreen(boolean value) {
        privateSpiceFullScreen = value;
    }

    private ValidateServerCertificateEnum privateValidateServerCertificate = ValidateServerCertificateEnum.values()[0];

    public ValidateServerCertificateEnum getValidateServerCertificate() {
        return privateValidateServerCertificate;
    }

    protected void setValidateServerCertificate(ValidateServerCertificateEnum value) {
        privateValidateServerCertificate = value;
    }

    private String privateBackendPort;

    public String getBackendPort() {
        return privateBackendPort;
    }

    protected void setBackendPort(String value) {
        privateBackendPort = value;
    }

    private String privateLogLevel;

    public String getLogLevel() {
        return privateLogLevel;
    }

    protected void setLogLevel(String value) {
        privateLogLevel = value;
    }

    /**
     * Specifies the interval fronend calls backend to check for updated results for registered queries and searches.
     * Values is in milliseconds.
     */
    private int privatePollingTimerInterval;

    public int getPollingTimerInterval() {
        return privatePollingTimerInterval;
    }

    protected void setPollingTimerInterval(int value) {
        privatePollingTimerInterval = value;
    }

    /**
     * Returns the base URL for serving documentation resources.
     * <p>
     * Example: <code>http://www.example.com/docs/</code>
     *
     * @return Documentation base URL, including the trailing slash.
     */
    public String getDocumentationBaseURL() {
        return FrontendUrlUtils.getRootURL() + DOCUMENTATION_ROOT + "/"; //$NON-NLS-1$
    }

    /**
     * Returns the base URL for serving helptag mapping files.
     * @return helptag mapping base URL, including the trailing slash.
     */
    public String getHelpTagMappingBaseURL() {
        return FrontendUrlUtils.getRootURL() + HELPTAG_MAPPING_ROOT + "/"; //$NON-NLS-1$
    }

    /**
     * Returns the base URL for serving locale-specific HTML documentation.
     * <p>
     * Example: <code>http://www.example.com/docs/en-US/html/</code>
     *
     * @return Locale-specific HTML documentation base URL, including the trailing slash.
     */
    public String getDocumentationLibURL() {
        return getDocumentationBaseURL() + documentationLangPath + DOCUMENTATION_LIB_PATH;
    }

    /**
     * Returns the base URL for retrieving Spice-related resources.
     */
    public static String getSpiceBaseURL() {
        return FrontendUrlUtils.getRootURL() + BaseContextPathData.getInstance().getRelativePath()
                + "services/files/spice/"; //$NON-NLS-1$
    }

    protected void setUsbFilter(String usbFilter) {
        this.usbFilter = usbFilter;
    }

    public String getUsbFilter() {
        return usbFilter;
    }

    // Fetch file from a specified path
    public void fetchFile(final String filePath, final Event onFetched) {
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, filePath);
        try {
            requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onError(Request request, Throwable exception) {
                    GWT.log("Error while requesting " + filePath, exception); //$NON-NLS-1$
                }

                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == Response.SC_OK) {
                        String result = response.getText();
                        onFetched.raise(this, new FileFetchEventArgs(result));
                    }
                }
            });
        } catch (RequestException e) {
        }
    }

    public boolean isWebSocketProxyDefined() {
        String wsConfig = (String) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.WebSocketProxy);
        return wsConfig != null && !"".equals(wsConfig) && !"Off".equalsIgnoreCase(wsConfig); //$NON-NLS-1$ $NON-NLS-2$
    }

    public static final class FileFetchEventArgs extends EventArgs {
        private String fileContent;

        public String getFileContent() {
            return fileContent;
        }

        public void setFileContent(String fileContent) {
            this.fileContent = fileContent;
        }

        public FileFetchEventArgs(String fileContent) {
            setFileContent(fileContent);
        }
    }

    public void configure(ISpice spice) {
        spice.setDesiredVersion(getSpiceVersion());
        spice.setCurrentVersion(getSpiceVersion());
        spice.setAdminConsole(getSpiceAdminConsole());
        spice.setFullScreen(getSpiceFullScreen());
        spice.setSpiceBaseURL(getSpiceBaseURL());
        spice.setUsbFilter(getUsbFilter());
        updateSpiceUsbAutoShare(spice);

        if (!isInitialized) {
            updateIsUsbEnabled();
            isInitialized = true;
        }
    }

    private void updateIsUsbEnabled() {
        // Get 'EnableUSBAsDefault' value from database
        AsyncDataProvider.isUSBEnabledByDefault(new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object target, Object returnValue) {
                // Update IsUsbEnabled value
                setIsUsbEnabled((Boolean) returnValue);
            }
        }));
    }

    private void updateSpiceUsbAutoShare(final ISpice spice) {
        AsyncDataProvider.getSpiceUsbAutoShare(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        spice.setUsbAutoShare((Boolean) returnValue);
                    }
                }));
    }

    public void updateSpice32Version() {
        fetchFile(getSpiceBaseURL() + "SpiceVersion.txt", getSpiceVersionFileFetchedEvent()); //$NON-NLS-1$
    }

    public void updateSpice64Version() {
        fetchFile(getSpiceBaseURL() + "SpiceVersion_x64.txt", getSpiceVersionFileFetchedEvent()); //$NON-NLS-1$
    }

    protected void updateSpiceVersion() {
        // Update spice version from the text files which are located on the server.
        // If can't update spice version - leave the default value from the Configurator.
        if (clientPlatformType().equalsIgnoreCase("win32")) { //$NON-NLS-1$
            updateSpice32Version();
        } else if (clientPlatformType().equalsIgnoreCase("win64")) { //$NON-NLS-1$
            updateSpice64Version();
        }
    }

    public boolean isClientLinuxFirefox() {
        return clientOsType().equalsIgnoreCase("Linux") && clientBrowserType().equalsIgnoreCase("Firefox"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean isClientWindowsExplorer() {
        return isClientWindows() && clientBrowserType().equalsIgnoreCase("Explorer"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public boolean isClientWindows() {
        return clientOsType().equalsIgnoreCase("Windows"); //$NON-NLS-1$
    }

    // Create a Version object from string
    public Version parseVersion(String versionStr) {
        return new Version(versionStr.replace(',', '.').replace("\n", "")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected abstract Event getSpiceVersionFileFetchedEvent();

    protected abstract String clientOsType();

    protected abstract String clientBrowserType();

    protected abstract String clientPlatformType();

    public abstract Float clientBrowserVersion();

}
