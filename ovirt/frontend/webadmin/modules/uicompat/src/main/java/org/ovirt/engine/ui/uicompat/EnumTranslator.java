package org.ovirt.engine.ui.uicompat;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;

public class EnumTranslator<T extends Enum<?>> extends Translator {
    private static final Logger logger = Logger.getLogger(EnumTranslator.class.getName());
    private static final UIConstants constants = GWT.create(UIConstants.class);

    private static final Map<Class<? extends Enum<?>>, EnumTranslator> translatorsMap = new HashMap<Class<? extends Enum<?>>, EnumTranslator>();

    private Enums enums = GWT.create(Enums.class);
    private Class<T> type;

    public EnumTranslator(Class<T> type) {
        this.type = type;
    }

    public static <T extends Enum<?>> Translator create(Class<T> type) {
        EnumTranslator translator = translatorsMap.get(type);
        if (translator == null) {
            translator = new EnumTranslator<T>(type);
            translatorsMap.put(type, translator);
        }

        return translator;
    }

    public static String createAndTranslate(Enum<?> enumObj) {
        String title = constants.notAvailableLabel();

        if (enumObj != null) {
            Translator translator = EnumTranslator.create(enumObj.getDeclaringClass());
            title = enumObj.name();

            try {
                title = translator.get(enumObj);
            } catch (MissingResourceException e) {
                // Silently ignore missing resource
                logger.info("Missing Enum resource: " + e.getLocalizedMessage()); //$NON-NLS-1$
            }
        }

        return title;
    }

    @Override
    public String get(Object key) {
        //FIXME: hack: due to java restriction for method names with chars that are not letters, digits, and underscores, replace . with 0
        if(key == null){
            return null;
        }
        String enumName = type.toString();
        enumName = enumName.substring(enumName.lastIndexOf(".")+1,enumName.length()); //$NON-NLS-1$
        String translatedEnum = enums.getString(enumName + "___" + key.toString()); //$NON-NLS-1$

        return translatedEnum;
    }
}
