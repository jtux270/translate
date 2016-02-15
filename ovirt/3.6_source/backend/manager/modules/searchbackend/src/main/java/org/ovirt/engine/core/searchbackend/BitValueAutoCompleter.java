package org.ovirt.engine.core.searchbackend;

import java.util.HashMap;

public class BitValueAutoCompleter extends BaseAutoCompleter implements IConditionValueAutoCompleter {
    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";
    private final HashMap<String, Integer> bitValues = new HashMap<String, Integer>();

    public BitValueAutoCompleter() {
        bitValues.put(TRUE, 1);
        verbs.add(TRUE);
        bitValues.put(FALSE, 0);
        verbs.add(FALSE);
        buildCompletions();
    }

    public String convertFieldEnumValueToActualValue(String fieldValue) {
        String retval = "";
        if (bitValues.containsKey(fieldValue.toUpperCase())) {
            retval = bitValues.get(fieldValue.toUpperCase()).toString();
        }
        return retval;
    }
}
