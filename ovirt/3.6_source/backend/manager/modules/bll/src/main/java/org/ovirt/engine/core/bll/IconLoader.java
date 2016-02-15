package org.ovirt.engine.core.bll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.bll.utils.IconUtils;
import org.ovirt.engine.core.bll.validator.IconValidator;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmIconDefault;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.queries.VmIconIdSizePair;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VmStaticDao;
import org.ovirt.engine.core.dao.VmTemplateDao;
import org.ovirt.engine.core.utils.EngineLocalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It loads icons from packaging/icons/{small|large} dirs referenced from
 * packaging/conf/osinfo-defaults.properties by os name keys.
 * <p>
 * It
 * <ul>
 *     <li>adds/updates icons to/in vm_icons table</li>
 *     <li>regenerates vm_icon_defaults table</li>
 *     <li>sets default icons according to OS for VMs and Templates in vm_static
 *         table if at least one icon is missing;
 *         this is only useful during the first run and to fix inconsistencies</li>
 * </ul>
 */
public class IconLoader {

    private static final Logger log = LoggerFactory.getLogger(IconLoader.class);
    private static final Path ICONS_DIR = EngineLocalConfig.getInstance().getUsrDir().toPath().resolve("icons");
    private static final Path LARGE_ICON_DIR =ICONS_DIR.resolve("large");
    private static final Path SMALL_ICON_DIR =ICONS_DIR.resolve("small");
    private final Map<Integer, VmIconIdSizePair> osIdToIconIdMap = new HashMap<>();
    private final int DEFAULT_OS_ID = OsRepository.DEFAULT_X86_OS;

    private IconLoader() {
        loadIconsToDatabase();
        ensureDefaultOsIconExists();
        updateVmIconDefaultsTable();
        updateVmStaticTable();
    }

    private void loadIconsToDatabase() {
        final HashMap<Integer, String> osIdToOsNameMap =
                SimpleDependecyInjector.getInstance().get(OsRepository.class).getUniqueOsNames();
        for (Map.Entry<Integer, String> entry : osIdToOsNameMap.entrySet()) {
            final VmIconIdSizePair iconIdPair = ensureIconsInDatabase(entry.getValue());
            if (iconIdPair != null) {
                osIdToIconIdMap.put(entry.getKey(), iconIdPair);
            }
        }
    }

    private void updateVmStaticTable() {
        final VmStaticDao vmStaticDao = DbFacade.getInstance().getVmStaticDao();
        for (VmStatic vmStatic : vmStaticDao.getAllWithoutIcon()) {
            setIconsByOs(vmStatic);
            vmStaticDao.update(vmStatic);
        }

        final VmTemplateDao vmTemplateDao = DbFacade.getInstance().getVmTemplateDao();
        for (VmTemplate vmTemplate : vmTemplateDao.getAllWithoutIcon()) {
            setIconsByOs(vmTemplate);
            vmTemplateDao.update(vmTemplate);
        }

    }

    private void setIconsByOs(VmBase vmBase) {
        final VmIconIdSizePair iconIdPair = getIconIdPairByOsId(vmBase.getOsId());
        vmBase.setSmallIconId(iconIdPair.getSmall());
        vmBase.setLargeIconId(iconIdPair.getLarge());
    }

    private VmIconIdSizePair getIconIdPairByOsId(int osId) {
        final VmIconIdSizePair osDefaultIcons = osIdToIconIdMap.get(osId);
        if (osDefaultIcons != null) {
            return osDefaultIcons;
        }
        return osIdToIconIdMap.get(DEFAULT_OS_ID);
    }

    /**
     * It recreates 'vm_icon_defaults' table based on new configuration.
     */
    private void updateVmIconDefaultsTable() {
        DbFacade.getInstance().getVmIconsDefaultDao().removeAll();
        for (Map.Entry<Integer, VmIconIdSizePair> entry : osIdToIconIdMap.entrySet()) {
            final VmIconDefault osDefautlIconIds = new VmIconDefault(Guid.newGuid(),
                    entry.getKey(),
                    entry.getValue().getSmall(),
                    entry.getValue().getLarge());
            DbFacade.getInstance().getVmIconsDefaultDao().save(osDefautlIconIds);
        }
    }

    private void ensureDefaultOsIconExists() {
        if (osIdToIconIdMap.get(DEFAULT_OS_ID) == null) {
            throw new RuntimeException("Icons for default os not found.");
        }
    }

    public static void load() {
        new IconLoader();
    }

    private static VmIconIdSizePair ensureIconsInDatabase(String osName) {
        final Guid smallIconId = ensureIconInDatabase(SMALL_ICON_DIR, osName);
        final Guid largeIconId = ensureIconInDatabase(LARGE_ICON_DIR, osName);
        if (smallIconId != null && largeIconId != null) {
            return new VmIconIdSizePair(smallIconId, largeIconId);
        }
        return null;
    }

    private static Guid ensureIconInDatabase(Path dir, String osName) {
        try {
            return ensureIconInDatabaseUnchecked(dir, osName);
        } catch (RuntimeException e) {
            log.warn(e.toString());
            return null;
        }
    }
    private static Guid ensureIconInDatabaseUnchecked(Path dir, String osName) {
        final ResolvedIcon resolvedIcon = resolveIconName(dir, osName);
        final String dataUrl = loadIcon(resolvedIcon);
        return DbFacade.getInstance().getVmIconDao().ensureIconInDatabase(dataUrl);
    }

    private static String loadIcon(ResolvedIcon resolvedIcon) {
        try {
            final byte[] bytes = Files.readAllBytes(resolvedIcon.getPath());
            return IconUtils.toDataUrl(bytes, resolvedIcon.getType());
        } catch (IOException e) {
            throw new RuntimeException("Icon " + resolvedIcon.getPath() + "can't be open.");
        }
    }

    private static ResolvedIcon resolveIconName(Path dir, String osName) {
        for (IconValidator.FileType fileType : IconValidator.FileType.values()) {
            for (String extension : fileType.getExtensions()) {
                final Path iconPath = dir.resolve(osName + "." + extension);
                if (Files.exists(iconPath)) {
                    return new ResolvedIcon(iconPath, fileType);
                }
            }
        }
        throw new RuntimeException("Icon for " + osName + " was not found in " + dir);
    }

    private static class ResolvedIcon {
        private final Path path;
        private final IconValidator.FileType type;

        public ResolvedIcon(Path path, IconValidator.FileType type) {
            this.path = path;
            this.type = type;
        }

        public Path getPath() {
            return path;
        }

        public IconValidator.FileType getType() {
            return type;
        }
    }

}
