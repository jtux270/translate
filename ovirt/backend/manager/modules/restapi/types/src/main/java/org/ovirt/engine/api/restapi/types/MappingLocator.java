/*
* Copyright (c) 2014 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ovirt.engine.api.restapi.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.api.common.util.PackageExplorer;
import org.ovirt.engine.api.restapi.utils.MalformedIdException;
import org.ovirt.engine.api.restapi.utils.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers and manages type mappers.
 */
public class MappingLocator {
    /**
     * The logger used by this class.
     */
    private static final Logger log = LoggerFactory.getLogger(MappingLocator.class);

    private String discoverPackageName;
    private Map<ClassPairKey, Mapper<?, ?>> mappers;

    /**
     * Normal constructor used when injected
     */
    public MappingLocator() {
        mappers = new HashMap<>();
    }

    /**
     * Constructor intended only for testing.
     *
     * @param discoverPackageName
     *            package to look under
     */
    MappingLocator(String discoverPackageName) {
        this.discoverPackageName = discoverPackageName;
        mappers = new HashMap<>();
    }

    /**
     * Discover mappers and populate internal registry. The classloading
     * environment is scanned for classes contained under the
     * org.ovirt.engine.api.restapi.types package and exposing methods decorated
     * with the @Mapping annotation.
     */
    public void populate() {
        String packageName = discoverPackageName != null? discoverPackageName: this.getClass().getPackage().getName();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<String> classNames = PackageExplorer.discoverClasses(packageName);
        for (String className : classNames) {
            try {
                Class<?> mapperClass = classLoader.loadClass(className);
                for (Method method : mapperClass.getMethods()) {
                    Mapping mapping = method.getAnnotation(Mapping.class);
                    if (mapping != null) {
                        mappers.put(new ClassPairKey(mapping.from(), mapping.to()),
                            new MethodInvokerMapper(method, mapping.to()));
                    }
                }
            }
            catch (ClassNotFoundException exception) {
                log.error(
                    "Error while trying to load mapper class \"{}\".",
                    className,
                    exception
                );
            }
        }
    }

    /**
     * Get an appropriate mapper mediating between the required types.
     *
     * @param <F>
     *            the from type
     * @param <T>
     *            the to type
     * @param from
     *            the from class
     * @param to
     *            the to class
     * @return a mapped instance of the to type
     */
    @SuppressWarnings("unchecked")
    public <F, T> Mapper<F, T> getMapper(Class<F> from, Class<T> to) {
        return (Mapper<F, T>) mappers.get(new ClassPairKey(from, to));
    }

    private static class ClassPairKey {
        private Class<?> from, to;

        private ClassPairKey(Class<?> from, Class<?> to) {
            this.from = from;
            this.to = to;
        }

        public int hashCode() {
            return to.hashCode() + from.hashCode();
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other instanceof ClassPairKey) {
                ClassPairKey key = (ClassPairKey) other;
                return to == key.to && from == key.from;
            }
            return false;
        }

        public String toString() {
            return "map from: " + from + " to: " + to;
        }
    }

    private static class MethodInvokerMapper implements Mapper<Object, Object> {
        private Method method;
        private Class<?> to;

        private MethodInvokerMapper(Method method, Class<?> to) {
            this.method = method;
            this.to = to;
        }

        @Override
        public Object map(Object from, Object template) {
            try {
                // REVISIT support non-static mapping methods also
                return to.cast(method.invoke(null, from, template));
            } catch (InvocationTargetException ite) {
              if (ite.getTargetException() instanceof MalformedIdException) {
                   throw (MalformedIdException) ite.getTargetException();
              } else {
                  throw new MappingException(ite);
              }
            } catch (IllegalAccessException e) {
                throw new MappingException(e);
            }
        }

        public String toString() {
            return "map to: " + to + " via " + method;
        }
    }
}
