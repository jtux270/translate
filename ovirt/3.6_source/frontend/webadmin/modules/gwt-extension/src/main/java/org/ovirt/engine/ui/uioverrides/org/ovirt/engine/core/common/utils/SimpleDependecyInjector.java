package org.ovirt.engine.core.common.utils;

import java.util.Map;
import java.util.HashMap;

/**
 * This class is needed to de-couple the searchbackend dependencies without introducing any DI frameworks or convulated
 * reference passing. Its a lightweight instance factory to solve situation where UI searchbackend needs diferent
 * instances than Backend.
 * <p/>
 * Sense searchbackend code is shared by UI and backend there is no way to reach other projects, which are specific to
 * each-other, such as dal or uicommonweb.
 * <p/>
 * The first use-case is that searchbackend relay on Enums as value completion. If we want a completion object which
 * have values populated by a code which is only accessible by both UI and backend we better have the object dependency
 * managed outside, by the SyntaxChecker initiator.
 *
 * @see SyntaxChecker
 */
public class SimpleDependecyInjector {

    private static final SimpleDependecyInjector instance = new SimpleDependecyInjector();

    private Map<String, Object> map = new HashMap<String, Object>();

    public static SimpleDependecyInjector getInstance() {
        return instance;
    }

    private SimpleDependecyInjector() {
        // hide ctr
    }

    /**
     * save an instance to the injector. this instance can be later fetched by the {@code SimpleDependecyInjector#get}
     * method. note: only one instance binded to a Class type
     *
     * @param type
     *            the instance to be kept
     * @param <T>
     */
    public <T> void bind(T type) {
        map.put(type.getClass().getName(), type);
    }

    /**
     * bind an instance to a specific named type. This is needed in case bind(T) have concrete types <br>
     * over interface which have method. note: only one instance is bounded to a Class type
     * @param typeName
     *            the name which this class will be mapped too
     * @param type
     *            the instance to be kept
     * @param <T>
     */
    public <T> void bind(String typeName, T type) {
        map.put(typeName, type);
    }

    /**
     * get the instance associated with the Class type note: only one instance binded to a Class type
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T get(Class<T> clazz) {
        return (T) map.get(clazz.getName());
    }

}
