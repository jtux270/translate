package org.ovirt.engine.core.bll.scheduling.external;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.utils.customprop.SimpleCustomPropertiesUtil;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;


public class ExternalSchedulerDiscoveryResult {
    private static final Log log = LogFactory.getLog(ExternalSchedulerDiscoveryResult.class);
    private static final String FILTERS = "filters";
    private static final String SCORES = "scores";
    private static final String BALANCE = "balance";
    private List<ExternalSchedulerDiscoveryUnit> filters;
    private List<ExternalSchedulerDiscoveryUnit> scores;
    private List<ExternalSchedulerDiscoveryUnit> balance;

    ExternalSchedulerDiscoveryResult() {
        filters = new LinkedList<ExternalSchedulerDiscoveryUnit>();
        scores = new LinkedList<ExternalSchedulerDiscoveryUnit>();
        balance = new LinkedList<ExternalSchedulerDiscoveryUnit>();
    }

    public boolean populate(Object xmlRpcRawResult) {
        try {
        if (!(xmlRpcRawResult instanceof HashMap)) {
            log.error("External scheduler error, malformed discover results");
            return false;
        }
        @SuppressWarnings("unchecked")
        HashMap<String, HashMap<String, Object[]>> castedResult = (HashMap<String, HashMap<String, Object[]>>) xmlRpcRawResult;

        // keys will be filter, score and balance
        for (Map.Entry<String, HashMap<String, Object[]>> entry : castedResult.entrySet()) {
            String type = entry.getKey();
            HashMap<String, Object[]> typeMap = entry.getValue();
                List<ExternalSchedulerDiscoveryUnit> currentList = getRelevantList(type);
                if (currentList == null) {
                    log.error("External scheduler error, got unknown type");
                    return false;
                }
                // list of module names as keys and [description, regex] as value
                for (Map.Entry<String, Object[]> module: typeMap.entrySet()) {
                    String moduleName = module.getKey();
                    Object[] singleModule = module.getValue();
                    // check custom properties format.
                    String customPropertiesRegex = singleModule[1].toString();
                    if (!StringUtils.isEmpty(customPropertiesRegex) && SimpleCustomPropertiesUtil.getInstance()
                            .syntaxErrorInProperties(customPropertiesRegex)) {
                        log.error("module " + moduleName + " will not be loaded, wrong custom properties format ("
                                + customPropertiesRegex + ")");
                        continue;
                    }
                    ExternalSchedulerDiscoveryUnit currentUnit = new ExternalSchedulerDiscoveryUnit(moduleName,
                            singleModule[0].toString(),
                            customPropertiesRegex);
                    currentList.add(currentUnit);
                }
        }
        return true;
        } catch (Exception e) {
            log.error("External scheduler error, exception why parsing discovery results", e);
            return false;
        }
    }

    private List<ExternalSchedulerDiscoveryUnit> getRelevantList(String type) {
        switch (type) {
        case FILTERS:
            return filters;
        case SCORES:
            return scores;
        case BALANCE:
            return balance;
        default:
            return null;
        }
    }

    List<ExternalSchedulerDiscoveryUnit> getFilters() {
        return filters;
    }

    void setFilters(List<ExternalSchedulerDiscoveryUnit> filters) {
        this.filters = filters;
    }

    List<ExternalSchedulerDiscoveryUnit> getScores() {
        return scores;
    }

    void setScores(List<ExternalSchedulerDiscoveryUnit> scores) {
        this.scores = scores;
    }

    List<ExternalSchedulerDiscoveryUnit> getBalance() {
        return balance;
    }

    void setBalance(List<ExternalSchedulerDiscoveryUnit> balance) {
        this.balance = balance;
    }
}
