package org.ovirt.engine.core.utils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.ovirt.engine.core.common.businessentities.Nameable;
import org.ovirt.engine.core.common.errors.EngineMessage;

public class ReplacementUtils {

    protected static final int DEFAULT_MAX_NUMBER_OF_PRINTED_ITEMS = 5;
    protected static final String DEFAULT_SEPARATOR = "," + System.lineSeparator();
    private static final String COUNTER_SUFFIX = "_COUNTER";
    private static final String SET_VARIABLE_VALUE_FORMAT = "${0} {1}";
    private static final String LIST_SUFFIX = "_LIST";
    private static final String ENTITY_SUFFIX  = "_ENTITY";

    private ReplacementUtils() {
    }

    /**
     * Replace a property defined within a message with a bounded number of elements.<br>
     * In addition, if a counter appears in the message, it will be replaced with the elements size:<br>
     * <ul>
     * <li>The elements' size property name is expected to be {propertyName}_COUNTER</li>
     * </ul>
     *
     * @param propertyName
     *            the property name which represents the collection.
     * @param items
     *            the collection of items to be shown in the message.
     * @param separator
     *            the separator that will separate between the elements.
     * @param maxNumberOfPrintedItems
     *            the bound value to limit the number of printed elements.
     * @return a mutable collection contains two elements:<br>
     *         <ul>
     *         <li>The property name and its replacement items.</li>
     *         <li>The property counter name and the items size.</li>
     *         </ul>
     */
    public static Collection<String> replaceWith(String propertyName,
            Collection<?> items,
            String separator,
            int maxNumberOfPrintedItems) {
        Validate.isTrue(maxNumberOfPrintedItems >= 1);
        Validate.isTrue(StringUtils.isNotEmpty(separator));

        int maxNumOfItems = Math.min(maxNumberOfPrintedItems, items.size());
        List<String> printedItems = new ArrayList<>(maxNumOfItems);

        for (Object item : items) {
            if (--maxNumOfItems < 0) {
                break;
            }
            printedItems.add(String.format("\t%s", String.valueOf(item)));
        }

        if (items.size() > maxNumberOfPrintedItems) {
            printedItems.add("\t...");
        }

        ArrayList<String> replacements = new ArrayList<>();
        replacements.add(createSetVariableString(propertyName, StringUtils.join(printedItems, separator)));
        replacements.add(createSetVariableString(propertyName + COUNTER_SUFFIX, items.size()));

        return replacements;
    }

    public static String createSetVariableString(String propertyName, Object value) {
        return MessageFormat.format(SET_VARIABLE_VALUE_FORMAT, propertyName, value);
    }

    /**
     * Replace a property defined within a message with a bounded number of elements.<br>
     * In addition, if a counter appears in the message, it will be replaced with the elements size:<br>
     * <ul>
     * <li>The elements' size property name is expected to be {propertyName}_COUNTER</li>
     * </ul>
     *
     * @param propertyName
     *            the property name which represents the collection.
     * @param items
     *            the collection of items to be shown in the message.
     * @return a mutable collection contains two elements:<br>
     *         <ul>
     *         <li>The property name and its replacement items.</li>
     *         <li>The property counter name and the items size.</li>
     *         </ul>
     */
    public static Collection<String> replaceWith(String propertyName, Collection<?> items) {
        return replaceWith(propertyName, items, DEFAULT_SEPARATOR, DEFAULT_MAX_NUMBER_OF_PRINTED_ITEMS);
    }

    public static Collection<String> replaceAllWith(String propertyName, Collection<?> items) {
        return replaceWith(propertyName, items, DEFAULT_SEPARATOR, items.size());
    }



    /**
     * Replace a property defined within a message with a bounded number of elements of {@link Nameable}.<br>
     * In addition, if a counter appears in the message, it will be replaced with the elements size:<br>
     * <ul>
     * <li>The elements' size property name is expected to be {propertyName}_COUNTER</li>
     * </ul>
     *
     * @param propertyName
     *            the property name which represents the collection
     * @param items
     *            the collection of items to be shown in the message
     * @return a mutable collection contains two elements:<br>
     *         <ul>
     *         <li>The property name and its replacement items.</li>
     *         <li>The property counter name and the items size.</li>
     *         </ul>
     */
    public static <T extends Nameable> Collection<String> replaceWithNameable(String propertyName, Collection<T> items) {
        List<Object> printedItems = new ArrayList<>(items.size());

        for (Nameable itemName : items) {
            printedItems.add(itemName.getName());
        }

        return replaceWith(propertyName, printedItems);
    }

    public static String getVariableName(EngineMessage engineMessage) {
        return engineMessage + ENTITY_SUFFIX;
    }

    public static String getListVariableName(EngineMessage engineMessage) {
        return engineMessage + LIST_SUFFIX;
    }

    public static Collection<String> getListVariableAssignmentString(EngineMessage engineMessage, Collection<?> values) {
        return ReplacementUtils.replaceWith(ReplacementUtils.getListVariableName(engineMessage), values);
    }

    public static Collection<String> getListVariableAssignmentStringUsingAllValues(EngineMessage engineMessage, Collection<?> values) {
        return ReplacementUtils.replaceAllWith(ReplacementUtils.getListVariableName(engineMessage), values);
    }

    //TODO MM: this is older construct which probably will not be needed after dropping of SetupNetworksHelper. When that's done, messages needs to be revisited and 'getVariableAssignmentString' can be used instead.
    public static String getVariableAssignmentStringWithMultipleValues(EngineMessage engineMessage, String value) {
        return createSetVariableString(getListVariableName(engineMessage), value);
    }

    public static String getVariableAssignmentString(EngineMessage engineMessage, String value) {
        return createSetVariableString(getVariableName(engineMessage), value);
    }
}
