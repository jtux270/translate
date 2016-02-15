package org.ovirt.engine.core.common.interfaces;

import java.util.List;
import java.util.Locale;

public interface ErrorTranslator {
    /**
     * Translates and resolves errors from error types. error messages contains errors and variables. Variable used in
     * messages. Variable definition must be in format: $variableName variableValue. Variable usage must be in format
     * #variableName Note: Unfound message keys will be beautified!
     *
     * @param errorMsg
     *            messages to be translated
     * @return
     */
    List<String> TranslateErrorText(List<String> errorMsg);

    /**
     * Translates and resolves errors from error types. error messages contains errors and variables. Variable used in
     * messages. Variable definition must be in format: $variableName variableValue. Variable usage must be in format
     * #variableName Note: Unfound message keys will be beautified!
     *
     * @param errorMsg
     *            messages to be translated
     * @param locale
     *            the locale to translate into
     * @return
     */
    List<String> TranslateErrorText(List<String> errorMsg, Locale locale);

    /**
     * returns true if the specified strMessage is in the format: "$variable-name variable-value", false otherwise.
     *
     * @param strMessage
     *            the string that may be a dynamic variable.
     * @return true if input is dynamic variable, false otherwise.
     */
    boolean IsDynamicVariable(String strMessage);

    /**
     * Translates a single error message.
     *
     * @param errorMsg
     *            the message to be translated
     * @param changeIfNotFound
     *            If true: if message key is not found in the resource, return a beautified key. If false, returned
     *            unfound key as is.
     * @return
     */
    String TranslateErrorTextSingle(String errorMsg, boolean changeIfNotFound);

    /**
     * Translates a single error message. Note: if message key not found, a beautified message will return!
     *
     * @param errorMsg
     *            the message to translate
     * @return the translated message or a beautifed message key
     */
    String TranslateErrorTextSingle(String errorMsg);

    /**
     * Translates a single error message. Note: if message key not found, a beautified message will return!
     *
     * @param errorMsg
     *            the message to translate
     * @param locale
     *            the locale to translate into
     * @return the translated message or a beautifed message key
     */
    String TranslateErrorTextSingle(String errorMsg, Locale locale);

    /**
     * Replacing variables ('${...}') within translatedMessages with their values ('$key value') that are also within
     * translatedMessages.
     *
     * @param translatedMessages
     * @return
     */
    List<String> ResolveMessages(List<String> translatedMessages);

}
