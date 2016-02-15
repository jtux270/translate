package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.action.SysPrepParams;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigUtil;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.utils.FileUtil;
import org.ovirt.engine.core.utils.collections.DomainsPasswordMap;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public final class SysprepHandler {
    private static final Map<String, String> userPerDomain = new HashMap<String, String>();
    private static Map<String, String> passwordPerDomain = new HashMap<String, String>();
    public static final Map<String, Integer> timeZoneIndex = new HashMap<String, Integer>();
    private static OsRepository osRepository = SimpleDependecyInjector.getInstance().get(OsRepository.class);

    private static final Log log = LogFactory.getLog(SysprepHandler.class);

    static {
        initTimeZones();
        fillUsersMap();
        fillPasswordsMap();
    }

    /**
     * TODO: This code is the exact code as in UsersDomainCacheManagerService, until we have a suitable location that
     * them both can use. Note that every change in one will probably require the same change in the other
     */
    private static void fillUsersMap() {
        String userPerDomainEntry = Config.<String> getValue(ConfigValues.AdUserName);
        if (!userPerDomainEntry.isEmpty()) {
            String[] domainUserPairs = userPerDomainEntry.split(",");

            for (String domainUserPair : domainUserPairs) {
                String[] parts = domainUserPair.split(":");
                String domain = parts[0].trim().toLowerCase();
                String userName = parts[1].trim();

                userPerDomain.put(domain, userName);
            }
        }
    }

    /**
     * TODO: This code is the exact code as in UsersDomainCacheManagerService, until we have a suitable location that
     * them both can use. Note that every change in one will probably require the same change in the other
     */
    private static void fillPasswordsMap() {
        passwordPerDomain = Config.<DomainsPasswordMap> getValue(ConfigValues.AdUserPassword);
    }

    public static String getSysPrep(VM vm, SysPrepParams sysPrepParams) {
        String sysPrepContent = "";
        if (vm.getVmInit() != null && !StringUtils.isEmpty(vm.getVmInit().getCustomScript())) {
            sysPrepContent = vm.getVmInit().getCustomScript();
        } else {
            sysPrepContent = LoadFile(osRepository.getSysprepPath(vm.getVmOsId(), null));
        }
        sysPrepContent = replace(sysPrepContent, "$ProductKey$", osRepository.getProductKey(vm.getVmOsId(), null));

        String domain = (vm.getVmInit() != null && vm.getVmInit().getDomain() != null) ?
                vm.getVmInit().getDomain() : "";
        String hostName = (vm.getVmInit() != null && vm.getVmInit().getHostname() != null) ?
                vm.getVmInit().getHostname() : vm.getName();

        if (sysPrepContent.length() > 0) {

            sysPrepContent = populateSysPrepDomainProperties(sysPrepContent, domain, sysPrepParams);
            sysPrepContent = replace(sysPrepContent, "$ComputerName$", hostName != null ? hostName : "");
            String timeZone = getTimeZone(vm);
            sysPrepContent = replace(sysPrepContent, "$TimeZone$", timeZone);

            String inputLocale = Config.<String> getValue(ConfigValues.DefaultSysprepLocale);
            String uiLanguage = Config.<String> getValue(ConfigValues.DefaultSysprepLocale);
            String systemLocale = Config.<String> getValue(ConfigValues.DefaultSysprepLocale);
            String userLocale = Config.<String> getValue(ConfigValues.DefaultSysprepLocale);
            String activeDirectoryOU = "";
            String adminPassword = Config.<String> getValue(ConfigValues.LocalAdminPassword);
            String orgName = Config.<String> getValue(ConfigValues.OrganizationName);

            if (vm.getVmInit() != null) {
                if (!StringUtils.isEmpty(vm.getVmInit().getInputLocale())) {
                    inputLocale = vm.getVmInit().getInputLocale();
                }
                if (!StringUtils.isEmpty(vm.getVmInit().getUiLanguage())) {
                    uiLanguage = vm.getVmInit().getUiLanguage();
                }
                if (!StringUtils.isEmpty(vm.getVmInit().getSystemLocale())) {
                    systemLocale = vm.getVmInit().getSystemLocale();
                }
                if (!StringUtils.isEmpty(vm.getVmInit().getUserLocale())) {
                    userLocale = vm.getVmInit().getUserLocale();
                }
                if (!StringUtils.isEmpty(vm.getVmInit().getActiveDirectoryOU())) {
                    activeDirectoryOU = vm.getVmInit().getActiveDirectoryOU();
                }
                if (!StringUtils.isEmpty(vm.getVmInit().getRootPassword())) {
                    adminPassword = vm.getVmInit().getRootPassword();
                }
                if (!StringUtils.isEmpty(vm.getVmInit().getOrgName())) {
                    orgName = vm.getVmInit().getOrgName();
                }
            }

            sysPrepContent = replace(sysPrepContent, "$SetupUiLanguageUiLanguage$", uiLanguage);
            sysPrepContent = replace(sysPrepContent, "$InputLocale$", inputLocale);
            sysPrepContent = replace(sysPrepContent, "$UILanguage$", uiLanguage);
            sysPrepContent = replace(sysPrepContent, "$SystemLocale$", systemLocale);
            sysPrepContent = replace(sysPrepContent, "$UserLocale$", userLocale);
            sysPrepContent = replace(sysPrepContent, "$MachineObjectOU$", activeDirectoryOU);
            sysPrepContent = replace(sysPrepContent, "$OrgName$", orgName);
            sysPrepContent = replace(sysPrepContent, "$AdminPassword$", adminPassword);
        }

        return sysPrepContent;
    }

    private static String populateSysPrepDomainProperties(String sysPrepContent,
            String domain,
            SysPrepParams sysPrepParams) {

        String domainName;
        String adminUserName;
        String adminPassword;

        if (sysPrepParams == null || StringUtils.isEmpty(sysPrepParams.getSysPrepDomainName())) {
            domainName = useDefaultIfNull("domain", domain, "", true);
        } else {
            domainName = sysPrepParams.getSysPrepDomainName();
        }

        if (sysPrepParams == null || sysPrepParams.getSysPrepUserName() == null
                || sysPrepParams.getSysPrepPassword() == null) {
            adminUserName = useDefaultIfNull("user", userPerDomain.get(domainName.toLowerCase()),
                    Config.<String> getValue(ConfigValues.SysPrepDefaultUser), true);

            adminPassword = useDefaultIfNull("password", passwordPerDomain.get(domainName.toLowerCase()),
                    Config.<String> getValue(ConfigValues.SysPrepDefaultPassword), false);
        } else {
            adminUserName = sysPrepParams.getSysPrepUserName();
            adminPassword = sysPrepParams.getSysPrepPassword();
        }

        // Get values from SysPrepParams - alternative for username,password and domain.
        sysPrepContent = replace(sysPrepContent, "$JoinDomain$", domainName);
        sysPrepContent = replace(sysPrepContent, "$DomainAdmin$", adminUserName);
        sysPrepContent = replace(sysPrepContent, "$DomainAdminPassword$", adminPassword);

        return sysPrepContent;
    }

    static String replace(String sysPrepContent, String pattern, String value) {
        return sysPrepContent.replaceAll(Pattern.quote(pattern), Matcher.quoteReplacement(value));
    }

    private static String useDefaultIfNull(String key, String value, String defaultValue,
            boolean printDefaultValue) {
        if (value == null && printDefaultValue) {
            log.warnFormat("Could not find value for key '{0}'. Going to use default value of: '{1}'",
                    key, defaultValue);
        }
        return value != null ? value : defaultValue;
    }

    private static String getTimeZone(VM vm) {
        String timeZone = null;
        // Can be empty if the VM was imported.
        if (vm.getVmInit() != null && StringUtils.isNotEmpty(vm.getVmInit().getTimeZone())) {
            timeZone = vm.getVmInit().getTimeZone();
        } else {
            timeZone = Config.<String> getValue(ConfigValues.DefaultWindowsTimeZone);
        }

        if (osRepository.isTimezoneValueInteger(vm.getStaticData(). getOsId(), null)) {
            // send correct time zone as sysprep expect to get it (a wierd number)
            return getTimezoneIndexByKey(timeZone);
        }

        return timeZone;
    }

    private static String getSysprepDir() {
        return Config.<String> getValue(ConfigValues.DataDir) + File.separator + "sysprep";
    }

    private static String LoadFile(String fileName) {
        String content = "";
        fileName = ConfigUtil.resolvePath(getSysprepDir(), fileName);
        if (new File(fileName).exists()) {
            try {
                content = FileUtil.readAllText(fileName);
            } catch (Exception e) {
                log.error("Failed to read sysprep template: " + fileName, e);
            }
        } else {
            log.error("Sysprep template: " + fileName + " not found");
        }
        return content;
    }

    // exclude 13 and 158 - not in the sysprep documentation!
    // {"Arabic Standard Time", 158},
    // {"Jerusalem Standard Time", 135},
    // {"Mexico Standard Time 2", 13},
    // {"Malay Peninsula Standard Time", 215},

    // TimeZone reference from Microsoft:
    // http://msdn.microsoft.com/en-us/library/ms912391(v=winembedded.11).aspx
    private static void initTimeZones() {
        timeZoneIndex.put("(GMT+04:30) Afghanistan Standard Time", 175);
        timeZoneIndex.put("(GMT-09:00) Alaskan Standard Time", 3);
        timeZoneIndex.put("(GMT+03:00) Arab Standard Time", 150);
        timeZoneIndex.put("(GMT+04:00) Arabian Standard Time", 165);
        timeZoneIndex.put("(GMT+03:00) Arabic Standard Time", 158);
        timeZoneIndex.put("(GMT-04:00) Atlantic Standard Time", 50);
        /** Before September 2007 Venezuela observed AST */
        timeZoneIndex.put("(GMT-04:30) Venezuelan Standard Time", 50);
        // timeZoneIndex.put("(GMT+04:00) Azerbaijan Standard Time", xxx);
        timeZoneIndex.put("(GMT-10:00) Azores Standard Time", 80);
        timeZoneIndex.put("(GMT-06:00) Canada Central Standard Time", 25);
        timeZoneIndex.put("(GMT-01:00) Cape Verde Standard Time", 83);
        timeZoneIndex.put("(GMT+04:00) Caucasus Standard Time", 170);
        timeZoneIndex.put("(GMT+09:30) Cen. Australia Standard Time", 250);
        timeZoneIndex.put("(GMT-06:00) Central America Standard Time", 33);
        timeZoneIndex.put("(GMT+06:00) Central Asia Standard Time", 195);
        // timeZoneIndex.put("(GMT-04:00) Central Brazilian Standard Time ", xxx);
        timeZoneIndex.put("(GMT+01:00) Central Europe Standard Time", 95);
        timeZoneIndex.put("(GMT+01:00) Central European Standard Time", 100);
        timeZoneIndex.put("(GMT+11:00) Central Pacific Standard Time", 280);
        timeZoneIndex.put("(GMT-06:00) Central Standard Time", 20);
        timeZoneIndex.put("(GMT-06:00) Central Standard Time (Mexico)", 30);
        timeZoneIndex.put("(GMT+08:00) China Standard Time", 210);
        timeZoneIndex.put("(GMT-12:00) Dateline Standard Time", 0);
        timeZoneIndex.put("(GMT+03:00) E. Africa Standard Time", 155);
        timeZoneIndex.put("(GMT+10:00) E. Australia Standard Time", 260);
        timeZoneIndex.put("(GMT+02:00) E. Europe Standard Time", 115);
        timeZoneIndex.put("(GMT-03:00) E. South America Standard Time", 65);
        timeZoneIndex.put("(GMT-05:00) Eastern Standard Time", 35);
        timeZoneIndex.put("(GMT+02:00) Egypt Standard Time", 120);
        timeZoneIndex.put("(GMT+05:00) Ekaterinburg Standard Time", 180);
        timeZoneIndex.put("(GMT+12:00) Fiji Standard Time", 285);
        timeZoneIndex.put("(GMT+02:00) FLE Standard Time", 125);
        timeZoneIndex.put("(GMT+04:00) Georgian Standard Time", 70);
        timeZoneIndex.put("(GMT) GMT Standard Time", 85);
        timeZoneIndex.put("(GMT-03:00) Greenland Standard Time", 73);
        timeZoneIndex.put("(GMT) Greenwich Standard Time", 90);
        timeZoneIndex.put("(GMT+02:00) GTB Standard Time", 130);
        timeZoneIndex.put("(GMT-10:00) Hawaiian Standard Time", 2);
        timeZoneIndex.put("(GMT+05:30) India Standard Time", 190);
        timeZoneIndex.put("(GMT+03:00) Iran Standard Time", 160);
        timeZoneIndex.put("(GMT+02:00) Israel Standard Time", 135);
        timeZoneIndex.put("(GMT+09:00) Korea Standard Time", 230);
        timeZoneIndex.put("(GMT-02:00) Mid-Atlantic Standard Time", 75);
        timeZoneIndex.put("(GMT-07:00) Mountain Standard Time", 10);
        timeZoneIndex.put("(GMT+06:30) Myanmar Standard Time", 203);
        timeZoneIndex.put("(GMT+06:00) N. Central Asia Standard Time", 201);
        timeZoneIndex.put("(GMT+05:45) Nepal Standard Time", 193);
        timeZoneIndex.put("(GMT+12:00) New Zealand Standard Time", 290);
        timeZoneIndex.put("(GMT-03:30) Newfoundland Standard Time", 60);
        timeZoneIndex.put("(GMT+08:00) North Asia East Standard Time", 227);
        timeZoneIndex.put("(GMT+07:00) North Asia Standard Time", 207);
        timeZoneIndex.put("(GMT+04:00) Pacific SA Standard Time", 56);
        timeZoneIndex.put("(GMT-08:00) Pacific Standard Time", 4);
        timeZoneIndex.put("(GMT+01:00) Romance Standard Time", 105);
        timeZoneIndex.put("(GMT+03:00) Russian Standard Time", 145);
        timeZoneIndex.put("(GMT-03:00) SA Eastern Standard Time", 70);
        timeZoneIndex.put("(GMT-05:00) SA Pacific Standard Time", 45);
        timeZoneIndex.put("(GMT-04:00) SA Western Standard Time", 55);
        timeZoneIndex.put("(GMT-11:00) Samoa Standard Time", 1);
        timeZoneIndex.put("(GMT+07:00) SE Asia Standard Time", 205);
        timeZoneIndex.put("(GMT+08:00) Singapore Standard Time", 215);
        timeZoneIndex.put("(GMT+02:00) South Africa Standard Time", 140);
        timeZoneIndex.put("(GMT+06:00) Sri Lanka Standard Time", 200);
        timeZoneIndex.put("(GMT+08:00) Taipei Standard Time", 220);
        timeZoneIndex.put("(GMT+10:00) Tasmania Standard Time", 265);
        timeZoneIndex.put("(GMT+09:00) Tokyo Standard Time", 235);
        timeZoneIndex.put("(GMT+13:00) Tonga Standard Time", 300);
        timeZoneIndex.put("(GMT-05:00) US Eastern Standard Time", 40);
        timeZoneIndex.put("(GMT-07:00) US Mountain Standard Time", 15);
        timeZoneIndex.put("(GMT+10:00) Vladivostok Standard Time", 270);
        timeZoneIndex.put("(GMT+08:00) W. Australia Standard Time", 225);
        timeZoneIndex.put("(GMT+01:00) W. Central Africa Standard Time", 113);
        timeZoneIndex.put("(GMT+01:00) W. Europe Standard Time", 110);
        timeZoneIndex.put("(GMT+05:00) West Asia Standard Time", 185);
        timeZoneIndex.put("(GMT+10:00) West Pacific Standard Time", 275);
        timeZoneIndex.put("(GMT+09:00) Yakutsk Standard Time", 240);
    }

    // we use:
    // key = "Afghanistan Standard Time"
    // value = "(GMT+04:30) Afghanistan Standard Time"
    public static String getTimezoneKey(String value) {
        return value.substring(value.indexOf(' ') + 1);
    }

    // we get "Afghanistan Standard Time" we return "175"
    // the "Afghanistan Standard Time" is the vm Key that we get from the method getTimezoneKey()
    // "175" is the timezone keys that xp/2003 excpect to get, vista/7/2008 gets "Afghanistan Standard Time"
    public static String getTimezoneIndexByKey(String key) {
        for (Map.Entry<String, Integer> timeZoneEntry : timeZoneIndex.entrySet()) {
            if (getTimezoneKey(timeZoneEntry.getKey()).equals(key)) {
                return timeZoneEntry.getValue().toString();
            }
        }
        log.errorFormat("getTimezoneIndexByKey: cannot find timezone key '{0}'", key);
        return key;
    }

}
