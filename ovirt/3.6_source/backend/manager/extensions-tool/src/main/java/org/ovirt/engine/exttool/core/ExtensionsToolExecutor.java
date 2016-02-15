package org.ovirt.engine.exttool.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.ovirt.engine.api.extensions.Base;
import org.ovirt.engine.api.extensions.ExtMap;
import org.ovirt.engine.core.extensions.mgr.ExtensionsManager;
import org.ovirt.engine.core.uutils.cli.parser.ArgumentsParser;
import org.slf4j.LoggerFactory;

public class ExtensionsToolExecutor {

    private static String PROGRAM_NAME = System.getProperty("org.ovirt.engine.exttool.core.programName");
    private static String PACKAGE_NAME = System.getProperty("org.ovirt.engine.exttool.core.packageName");
    private static String PACKAGE_VERSION = System.getProperty("org.ovirt.engine.exttool.core.packageVersion");
    private static String PACKAGE_DISPLAY_NAME = System.getProperty("org.ovirt.engine.exttool.core.packageDisplayName");
    private static String ENGINE_ETC = System.getProperty("org.ovirt.engine.exttool.core.engineEtc");

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ExtensionsToolExecutor.class);
    private static final Logger OVIRT_LOGGER = Logger.getLogger("org.ovirt");

    private static final ExtMap context = new ExtMap();

    public static void main(String... args) {
        int exitStatus = 1;
        List<String> cmdArgs = new ArrayList<>(Arrays.asList(args));

        try {
            final Map<String, String> contextSubstitutions = new HashMap<>();
            contextSubstitutions.put("@ENGINE_ETC@", ENGINE_ETC);
            contextSubstitutions.put("@PROGRAM_NAME@", PROGRAM_NAME);
            context.put(ModuleService.ContextKeys.CLI_PARSER_SUBSTITUTIONS, contextSubstitutions);

            setupLogger();
            ArgumentsParser parser;

            Map<String, ModuleService> moduleServices = loadModules(ModuleService.class);
            context.put(ModuleService.ContextKeys.MODULES, moduleServices);
            final Map<String, String> substitutions = new HashMap<>(contextSubstitutions);
            substitutions.put("@MODULE_LIST@", getModules(moduleServices));

            try (InputStream stream = ExtensionsToolExecutor.class.getResourceAsStream("arguments.properties")) {
                parser = new ArgumentsParser(stream, "core");
                parser.getSubstitutions().putAll(substitutions);
            }
            parser.parse(cmdArgs);
            Map<String, Object> argMap = parser.getParsedArgs();
            setupLogger(argMap);

            if((Boolean)argMap.get("help")) {
                System.out.format("Usage: %s", parser.getUsage());
                throw new ExitException("Help", 0);
            } else if((Boolean)argMap.get("version")) {
                System.out.format("%s-%s (%s)%n", PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_DISPLAY_NAME);
                throw new ExitException("Version", 0);
            }
            if(!parser.getErrors().isEmpty()) {
                for(Throwable t : parser.getErrors()) {
                    log.error(t.getMessage());
                    log.debug(t.getMessage(), t);
                }
                throw new ExitException("Parsing error", 1);
            }

            if (cmdArgs.size() < 1) {
                log.error("Please provide module.");
                throw new ExitException("Module not provided", 1);
            }
            String module = cmdArgs.get(0);
            ModuleService moduleService = moduleServices.get(module);
            if (moduleService == null) {
                log.error("No such '{}' module exists.", module);
                throw new ExitException(1);
            }
            moduleService.parseArguments(cmdArgs);
            log.info("========================================================================");
            log.info("============================ Initialization ============================");
            log.info("========================================================================");
            ExtensionsManager extensionsManager = new ExtensionsManager();
            extensionsManager.getGlobalContext().put(
                Base.GlobalContextKeys.APPLICATION_NAME,
                Base.ApplicationNames.OVIRT_ENGINE_EXTENSIONS_TOOL
            );
            context.put(ModuleService.ContextKeys.EXTENSION_MANAGER, extensionsManager);
            loadExtensions(extensionsManager, moduleService, argMap);
            log.info("========================================================================");
            log.info("============================== Execution ===============================");
            log.info("========================================================================");
            moduleService.run();
            exitStatus = 0;
        } catch(ExitException e) {
            log.debug(e.getMessage(), e);
            exitStatus = e.getExitCode();
        } catch (Throwable t) {
            log.error(t.getMessage() != null ? t.getMessage() : t.getClass().getName());
            log.debug("Exception:", t);
        }
        log.debug("Exiting with status '{}'", exitStatus);
        System.exit(exitStatus);
    }

    private static void loadExtensions(
        ExtensionsManager extensionsManager,
        ModuleService moduleService,
        Map<String, Object> argMap
    ) {
        List<File> files = (List<File>)argMap.get("extension-file");
        if(files == null) {
            files = listFiles(
                ((String) argMap.get("extensions-dir")),
                "properties"
            );
        }

        List<String> loadedExtensions = new LinkedList<>();
        for(File f : files) {
            log.debug("Loading extension file '{}'", f.getName());
            try {
                loadedExtensions.add(extensionsManager.load(f));
            } catch (Exception ex) {
                log.error("Extension '{}' load failed (ignored): {}", f.getName(), ex.getMessage());
                log.debug("Exception:", ex);
            }
        }

        for(String extension : loadedExtensions) {
            try {
                extensionsManager.initialize(extension);
                log.debug("Extension '{}' initialized", extension);
            } catch (Exception ex) {
                log.error("Extension '{}' initialization failed (ignored): {}", extension, ex.getMessage());
                log.debug("Exception:", ex);
            }
        }
        extensionsManager.dump();
    }

    private static Map<String, ModuleService> loadModules(Class cls) {
        Map<String, ModuleService> modules = new HashMap<>();
        if(cls != null) {
            ServiceLoader<ModuleService> loader = ServiceLoader.load(cls);
            for (ModuleService module : loader) {
                modules.put(module.getName(), module);
                module.setContext(context);
            }
        }
        log.debug("Loaded modules: {}", modules.keySet());

        return modules;
    }

    private static void setupLogger() {
        String logLevel = System.getenv("OVIRT_LOGGING_LEVEL");
        OVIRT_LOGGER.setLevel(
            logLevel != null ? Level.parse(logLevel) : Level.INFO
        );
    }

    private static void setupLogger(Map<String, Object> args) throws IOException {
        Logger log = Logger.getLogger("");
        String logfile = (String)args.get("log-file");
        if(logfile != null) {
            FileHandler fh = new FileHandler(logfile);
            fh.setFormatter(new SimpleFormatter());
            log.addHandler(fh);
        }

        OVIRT_LOGGER.setLevel((Level)args.get("log-level"));
    }

    private static List<File> listFiles(String directory, String suffix) {
        File dir = new File(directory);
        List<File> propFiles = new ArrayList<>();
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File file : dirFiles) {
                if (file.getName().endsWith(suffix)) {
                    propFiles.add(file);
                }
            }
        }

        return propFiles;
    }

    private static String getModules(Map<String, ModuleService> modules) {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, ModuleService> entry : new TreeMap<>(modules).entrySet()) {
            sb.append(
                String.format("  %-10s - %s%n", entry.getKey(), entry.getValue().getDescription())
            );
        }
        return sb.toString();
    }

}
