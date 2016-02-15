package org.ovirt.engine.docs.utils.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.ovirt.engine.core.utils.EngineLocalConfig;
import org.ovirt.engine.core.utils.servlet.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet serves the context-sensitve help mapping files (JSON format) to the web UI.
 *
 * Each application has documentation in multiple locales, and each locale may have multiple mapping files.
 *
 * The servlet handles loading and compressing all of that information into one JSON file per application.
 *
 * Roughly:
 *   * Get the directory where the context-sensitive help package is installed
 *   * Detect which locales are installed
 *   * For each locale
 *     * get the conf.d directory
 *     * for each application subdir (webadmin, userportal, etc)
 *       * Look for json mapping files
 *       * concatenate them into a single json file for this locale+application
 *   * concatenate all of the locale+application jsons into one giant json per application
 *
 * This happens for every request of an application's json mapping file, which is once per user login.
 *
 * @see ContextSensitiveHelpManager for the client-side portion of this operation.
 */
public class ContextSensitiveHelpMappingServlet extends HttpServlet {

    private static final long serialVersionUID = -393894763659009626L;
    private static final Logger log = LoggerFactory.getLogger(ContextSensitiveHelpMappingServlet.class);

    private static final String MANUAL_DIR_KEY = "manualDir"; //$NON-NLS-1$
    private static final String CSH_MAPPING_DIR = "csh.conf.d"; //$NON-NLS-1$
    private static final String JSON = ".json"; //$NON-NLS-1$

    // parse xxxxxx from /some/path/xxxxxx.json
    private static Pattern REQUEST_PATTERN = Pattern.compile(".*?(?<key>[^/]*)\\.json"); //$NON-NLS-1$
    private static Pattern LOCALE_PATTERN = Pattern.compile("\\w\\w-\\w\\w"); //$NON-NLS-1$

    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Respond to a GET request for the CSH mapping file. See class Javadoc for the algorithm.
     *
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String application = getApplication(request);
        if (application == null) {
            log.warn("ContextSensitiveHelpMappingServlet could not handle request. URL = " + request.getRequestURI()); //$NON-NLS-1$
            return;
        }

        String manualPath = getManualDir(getServletConfig());
        List<String> locales = getLocales(new File(manualPath));

        ObjectNode appCsh = mapper.createObjectNode();

        for (String locale : locales) {
            File cshConfigDir = new File(manualPath + "/" + locale + "/" + CSH_MAPPING_DIR + "/" + application); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            if (cshConfigDir.exists() && cshConfigDir.canRead()) {
                List<JsonNode> configData = readJsonFiles(cshConfigDir);
                // merge the data from all the files
                if (configData.size() > 0) {
                    JsonNode destination = configData.get(0);
                    for (int i = 1; i < configData.size(); i++) {
                        destination = merge(destination, configData.get(i));
                    }

                    appCsh.put(locale, destination);
                }
            }
            else {
                log.error("couldn't get csh directory: " + cshConfigDir); //$NON-NLS-1$
            }
        }

        response.setContentType("application/json"); //$NON-NLS-1$
        PrintStream printStream = new PrintStream(response.getOutputStream());
        printStream.print(appCsh.toString());
        printStream.flush();
    }

    /**
     * parse xxxxxx from /some/path/xxxxxx.json
     */
    protected String getApplication(HttpServletRequest request) {
        Matcher m = REQUEST_PATTERN.matcher(request.getRequestURI());
        if (m.matches()) {
            return m.group("key"); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Read the json files from the directory.
     *
     * @param configPath directory to read
     * @return List<JsonNode> containing each file read
     */
    protected List<JsonNode> readJsonFiles(File configPath) {
        List<JsonNode> nodes = new ArrayList<JsonNode>();
        List<String> jsonFiles = getJsonFiles(configPath);

        for (String jsonFile : jsonFiles) {

            File file = new File(configPath, jsonFile);
            if (file.exists() && file.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
                    nodes.add(mapper.readTree(reader));
                    log.info("Successfully read CSH mapping file '{}'", file.getAbsolutePath()); //$NON-NLS-1$
                }
                catch (IOException e) {
                    log.error("Exception parsing documentation mapping file '{}': {}", //$NON-NLS-1$
                            file.getAbsolutePath(), e.getMessage());
                    log.error("Exception: ", e); //$NON-NLS-1$
                }
            }
        }
        return nodes;
    }

    /**
     * Return sorted list of names of the json config files in the config dir.
     *
     * @param configDir directory to search
     * @return sorted list of file names
     */
    protected List<String> getJsonFiles(File configDir) {

        List<String> jsonFiles = new ArrayList<>();

        if (!configDir.exists() || !configDir.canRead()) {
            log.error("csh configDir doesn't exist: " + configDir); //$NON-NLS-1$
            return jsonFiles;
        }
        for (File configFile : configDir.listFiles()) {
            if (configFile.isFile() && configFile.canRead() && configFile.getName().endsWith(JSON)) {
                jsonFiles.add(configFile.getName());
            }
        }

        // last file wins
        Collections.sort(jsonFiles);
        return jsonFiles;
    }

    /**
     * Get List of the installed documentation locales.
     *
     * @param manualDir directory to search
     * @return
     */
    protected List<String> getLocales(File manualDir) {

        List<String> locales = new ArrayList<>();

        if (!manualDir.exists() || !manualDir.canRead()) {
            log.info("Context-sensitive help is not installed. Manual directory doesn't exist: " + manualDir); //$NON-NLS-1$
            return locales;
        }
        for (File dir : manualDir.listFiles()) {
            if (dir.isDirectory() && dir.canRead()) {
                String name = dir.getName();
                Matcher m = LOCALE_PATTERN.matcher(name);
                if (m.matches()) {
                    locales.add(name);
                }
            }
        }

        return locales;
    }

    /**
     * Get the configured manual directory from the servlet config.
     */
    protected String getManualDir(ServletConfig config) {
        EngineLocalConfig engineLocalConfig = EngineLocalConfig.getInstance();
        String manualDir = ServletUtils.getAsAbsoluteContext(getServletContext().getContextPath(),
                engineLocalConfig.expandString(
                        config.getInitParameter(MANUAL_DIR_KEY).replaceAll("%\\{", "\\${")) //$NON-NLS-1$ //$NON-NLS-2$
        );
        return manualDir;
    }

    /**
     * Merge json objects. This is used to put json mappings from multiple config files into one object.
     *
     * Note that this method is recursive.
     *
     * @param destination destination json node
     * @param source source json node.
     * @return merged json object
     */
    protected static JsonNode merge(JsonNode destination, JsonNode source) {

        Iterator<String> fieldNames = source.getFieldNames();
        while (fieldNames.hasNext()) {

            String fieldName = fieldNames.next();
            JsonNode jsonNode = destination.get(fieldName);
            // if field is an embedded object, recurse
            if (jsonNode != null && jsonNode.isObject()) {
                merge(jsonNode, source.get(fieldName));
            }
            // else it's a plain field
            else if (destination instanceof ObjectNode) {
                // overwrite field
                JsonNode value = source.get(fieldName);
                ((ObjectNode) destination).put(fieldName, value);
            }
        }

        return destination;
    }
}
