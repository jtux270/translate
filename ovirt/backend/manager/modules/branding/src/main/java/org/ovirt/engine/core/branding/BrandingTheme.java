package org.ovirt.engine.core.branding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.ovirt.engine.core.utils.servlet.LocaleFilter;

/**
 * This class represents the components of an available theme. There are
 * several things that are important to a theme:
 * <ul>
 *  <li>The path to the theme</li>
 *  <li>The name of the style sheet associated with the theme</li>
 * </ul>
 */
public class BrandingTheme {

    /**
     * The logger.
     */
    private static final Logger log = Logger.getLogger(BrandingTheme.class);

    /**
     * The key for the messages resource bundle name.
     */
    private static final String MESSAGES_KEY = "messages"; //$NON-NLS-1$

    /**
     * The key for the cascading resources bundle name.
     */
    private static final String RESOURCES_KEY = "resources"; //$NON-NLS-1$

    /**
     * The suffix of properties file name.
     */
    private static final String PROPERTIES_FILE_SUFFIX = ".properties"; //$NON-NLS-1$

    /**
     * The name of the branding properties file.
     */
    private static final String BRANDING_PROPERTIES_NAME = "branding" + PROPERTIES_FILE_SUFFIX; //$NON-NLS-1$

    /**
     * The key to use to read the branding version.
     */
    private static final String VERSION_KEY = "version"; //$NON-NLS-1$

    /**
     * The key used to read the welcome page template.
     */
    private static final String TEMPLATE_KEY = "welcome"; //$NON-NLS-1$

    /**
     * The key used to determine if this template should completely replace the template build from all
     * previously processed themes.
     */
    private static final String REPLACE_TEMPLATE_KEY = "welcome_replace"; //$NON-NLS-1$

    /**
     * Property suffix for cascading resources file.
     */
    private static final String FILE_SUFFIX = ".file"; //$NON-NLS-1$

    /**
     * Property suffix for cascading resources contentType.
     */
    private static final String CONTENT_TYPE_SUFFIX = ".contentType"; //$NON-NLS-1$

    /**
     * Post fix for denoting css files.
     */
    private static final String CSS_POST_FIX = "_css"; //$NON-NLS-1$

    private static final String[] TEMPLATE_REPLACE_VALUES = {"true", "false"}; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * The properties associated with the branding theme.
     */
    private final Properties brandingProperties = new Properties();

    /**
     * The path to the branding directory.
     */
    private final String path;

    /**
     * The actual file path.
     */
    private final String filePath;

    /**
     * The version of the branding theme we are supposed to be.
     */
    private final int supportedBrandingVersion;

    /**
     * Availability flag.
     */
    private boolean available;

    /**
     * Constructor.
     * @param brandingPath The path to the theme
     * @param brandingRootPath The root of the path to the branding theme,
     * @param brandingVersion The version to load, if the version don't match the load will fail.
     */
    public BrandingTheme(final String brandingPath, final File brandingRootPath, final int brandingVersion) {
        path = brandingPath.substring(brandingRootPath.getAbsolutePath().length());
        filePath = brandingPath;
        supportedBrandingVersion = brandingVersion;
    }

    /**
     * Load the branding theme based on the passed in paths.
     * @return {@code true} if successfully loaded, {@code false} otherwise.
     */
    public boolean load() {
        final String propertiesFileName = filePath + "/" + BRANDING_PROPERTIES_NAME; //$NON-NLS-1$
        available = false;

        try (FileInputStream propertiesFile = new FileInputStream(propertiesFileName)) {
            brandingProperties.load(propertiesFile);
            available = supportedBrandingVersion == getVersion(brandingProperties);
            if (!available) {
                log.warn("Unable to load branding theme, mismatched version: " //$NON-NLS-1$
                    + getVersion(brandingProperties) + " wanted version: " + supportedBrandingVersion); //$NON-NLS-1$
            } else {
                available = verifyPropertyValues(brandingProperties);
                if (!available) {
                    log.warn("Unable to load branding theme, property value verification failed"); //$NON-NLS-1$
                }
            }
        } catch (IOException e) {
            // Unable to load properties file, disable theme.
            log.warn("Unable to load properties file for " //$NON-NLS-1$
                    + "theme located here:"//$NON-NLS-1$
                    + propertiesFileName, e);
        }
        return available;
    }

    /**
     * Verify that all required property values are valid.
     * @param properties The {@code Properties} object to check.
     */
    private boolean verifyPropertyValues(final Properties properties) {
        boolean result = true;
        if (brandingProperties.getProperty(REPLACE_TEMPLATE_KEY) != null &&
            !Arrays.asList(TEMPLATE_REPLACE_VALUES).contains(
                    brandingProperties.getProperty(REPLACE_TEMPLATE_KEY).toLowerCase())) {
            log.warn(REPLACE_TEMPLATE_KEY + " value is not true or false"); //$NON-NLS-1$
            result = false;
        }
        return result;
    }

    /**
     * Getter for the style path.
     * @return A {@code String} containing the path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the version of the branding read.
     * @param properties The {@code Properties} to use to get the version.
     * @return The version of the branding theme read as an {@code integer}, 0 if we are unable to read
     * the version.
     */
    public int getVersion(final Properties properties) {
        int result = 0;
        try {
            result = Integer.valueOf(properties.getProperty(VERSION_KEY));
        } catch (NumberFormatException nfe) {
            // Do nothing, not a valid version, return 0.
        }
        return result;
    }

    /**
     * Get the css resources for this theme for this {@code ApplicationType}.
     * @param applicationName The application name for which to get the css resources
     * @return A {@code List} of filenames
     */
    public List<String> getThemeStylesheets(String applicationName) {
        List<String> ret = null;
        final String cssFiles = brandingProperties.getProperty(applicationName + CSS_POST_FIX);
        if (cssFiles == null) {
            log.warn("Theme '" + this.getPath() + "' has no property defined for key '" + //$NON-NLS-1$ //$NON-NLS-2$
                    applicationName + CSS_POST_FIX + "'"); //$NON-NLS-1$
        }
        else {
            // comma-delimited list
            ret = Arrays.asList(cssFiles.split("\\s*,\\s*")); //$NON-NLS-1$
        }
        return ret;
    }

    /**
     * Get the theme messages resource bundle for the US locale.
     * @return A {@code ResourceBundle} containing messages.
     */
    public List<ResourceBundle> getMessagesBundle() {
        // Default to US Locale.
        return getMessagesBundle(LocaleFilter.DEFAULT_LOCALE);
    }

    /**
     * Get the theme messages resource bundle.
     * @param locale the locale to load the bundle for.
     * @return A {@code ResourceBundle} containing messages.
     */
    public List<ResourceBundle> getMessagesBundle(final Locale locale) {
        return getBundle(MESSAGES_KEY, locale);
    }

    /**
     * Get the theme cascading resources bundle.
     * @return A {@code ResourceBundle} containing resource paths.
     */
    public ResourceBundle getResourcesBundle() {
        List<ResourceBundle> bundleList = getBundle(RESOURCES_KEY, LocaleFilter.DEFAULT_LOCALE);
        if (bundleList.size() >= 1) {
            return bundleList.get(0);
        }
        throw new MissingResourceException("can't load resources bundle", null, null); //$NON-NLS-1$
    }

    /**
     * Load the Java resource bundle associated with the passed in Locale and name.
     * @param name The name of the {@code ResourceBundle} file.
     * @param locale The locale to load.
     * @return A {@code ResourceBundle} containing the resources.
     */
    private List<ResourceBundle> getBundle(String name, Locale locale) {
        List<ResourceBundle> result = new ArrayList<ResourceBundle>();
        String lastProcessedBundle = null;
        try {
            File themeDirectory = new File(filePath);
            URLClassLoader urlLoader = new URLClassLoader(
                    new URL[] {
                            themeDirectory.toURI().toURL() });
            final String messageFileNames = brandingProperties.getProperty(name);
            if (messageFileNames != null) {
                //The values can be a comma separated list of file names, split them and load each of them.
                for (String fileName: messageFileNames.split(",")) {
                    fileName = lastProcessedBundle = fileName.trim();
                    String bundleName = fileName.lastIndexOf(PROPERTIES_FILE_SUFFIX) != -1
                            ? fileName.substring(0, fileName.lastIndexOf(PROPERTIES_FILE_SUFFIX))
                            : messageFileNames;
                    result.add(ResourceBundle.getBundle(bundleName, locale, urlLoader));
                }
            } else {
                log.warn("Theme " + this.getPath() + " has no property defined for key " //$NON-NLS-1$ //$NON-NLS-2$
                        + name);
            }
        } catch (IOException e) {
            // Unable to load messages resource bundle.
            log.warn("Unable to read resources resource " //$NON-NLS-1$
                    + "bundle, returning null", e); //$NON-NLS-1$
        } catch (MissingResourceException mre) {
            log.warn("Theme " + this.getPath() + " is missing ResourceBundle " //$NON-NLS-1$ //$NON-NLS-2$
                    + lastProcessedBundle);
        }

        return result;
    }

    public boolean shouldReplaceWelcomePageSectionTemplate() {
        return Boolean.valueOf(brandingProperties.getProperty(REPLACE_TEMPLATE_KEY));
    }

    /**
     * Return the raw welcome template as a string.
     * @return The raw template string.
     */
    public String getWelcomePageSectionTemplate() {
        String result = "";
        try {
            final String templateFileName = filePath + "/" + brandingProperties.getProperty(TEMPLATE_KEY); //$NON-NLS-1$
            result = readWelcomeTemplateFile(templateFileName);
        } catch (IOException ioe) {
            log.error("Unable to load welcome template", ioe); //$NON-NLS-1$
        } catch (NullPointerException e) {
            log.error("Unable to locate welcome template key in branding properties", e); //$NON-NLS-1$
        }
        return result;
    }

    /**
     * Read the welcome page template file. The template is standard HTML format, but with one difference.
     * If a line starts with '#' it is considered a comment and will not end up in the output.
     * @param fileName The name of the file to read.
     * @return The contents of the file as a string.
     * @throws IOException if unable to read the template file.
     */
    private String readWelcomeTemplateFile(final String fileName) throws IOException {
        StringBuilder templateBuilder = new StringBuilder();

        try (
            InputStream in = new FileInputStream(fileName);
            Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
            BufferedReader bufferedReader= new BufferedReader(reader);
        ){
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                if (!currentLine.startsWith("#")) { // # is comment.
                    templateBuilder.append(currentLine);
                    templateBuilder.append("\n"); //$NON-NLS-1$
                }
            }
        }

        return templateBuilder.toString();
    }

    /**
     * Look for the resource in this theme, and return the path to it if it exists.
     * Return null if the resource isn't found in the theme.
     * @param resourceName The name of the resource.
     * @return resource path, or null if no resource found in this theme
     */
    public CascadingResource getCascadingResource(String resourceName) {
        if (resourceName == null) {
            return null;
        }

        try {
            String resourceFile = getResourcesBundle().getString(resourceName + FILE_SUFFIX);

            File file = new File(filePath + "/" + resourceFile); //$NON-NLS-1$
            if (file.exists() && file.canRead()) {
                // ok, good, we have a file. was a contentType specified?
                String contentType = null;
                try {
                    contentType = getResourcesBundle().getString(resourceName + CONTENT_TYPE_SUFFIX);
                } catch (MissingResourceException mre) {
                    // no-op -- contentType is optional, no big deal
                }
                return new CascadingResource(file, contentType);
            }
        } catch (MissingResourceException mre) {
            // no-op -- this theme just doesn't have this resource. will try the next lowest theme.
        }

        return null;
    }

    @Override
    public String toString() {
        return "Path to theme: " + getPath() + ", User portal css: " //$NON-NLS-1$ //$NON-NLS-2$
        + getThemeStylesheets("userportal") + ", Web admin css: " //$NON-NLS-1$ //$NON-NLS-2$
        + getThemeStylesheets("webadmin") + ", Welcome page css: " //$NON-NLS-1$ //$NON-NLS-2$
        + getThemeStylesheets("welcome"); //$NON-NLS-1$
    }

}
