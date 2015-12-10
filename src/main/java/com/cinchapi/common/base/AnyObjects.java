/*
 * Copyright (c) 2015 Cinchapi Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cinchapi.common.base;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Helper functions
 * <em>(that are not already contained in {@link java.util.Objects
 * Objects} or other common utility classes for the same purpose)</em> that
 * operate on any {@link Object}.
 * 
 * @author Jeff Nelson
 */
@SuppressWarnings("rawtypes")
public final class AnyObjects {

    /**
     * The custom {@link EmptyDefinition empty definitions} that are provided
     * for classes.
     */
    private static final Map<Class<?>, EmptyDefinition<?>> defs = new HashMap<Class<?>, EmptyDefinition<?>>();
    static {
        registerEmptyDefinition(String.class, new EmptyDefinition<String>() {

            @Override
            public boolean metBy(String object) {
                return object.isEmpty();
            }

        });
        registerEmptyDefinition(Collection.class,
                new EmptyDefinition<Collection>() {

                    @Override
                    public boolean metBy(Collection object) {
                        return object.isEmpty();
                    }

                });
        registerEmptyDefinition(Map.class, new EmptyDefinition<Map>() {

            @Override
            public boolean metBy(Map object) {
                return object.isEmpty();
            }

        });
    }

    /**
     * Register a custom {@link EmptyDefinition} for all objects that are either
     * a member of or {@link Class#isAssignableFrom(Class) assignable from}
     * {@code clazz}.
     * 
     * <p>
     * <strong>NOTE:</strong> This method is not thread safe, so avoid
     * dynamically registering definitions from multiple threads on-demand. This
     * function is designed to be used at a defined time during the
     * application's lifecycle (i.e. during bootstrap).
     * </p>
     * 
     * @param clazz the {@link Class} to register
     * @param def the {@link EmptyDefinition} to associate with {@code clazz}
     */
    public static <T> void registerEmptyDefinition(Class<T> clazz,
            EmptyDefinition<T> def) {
        defs.put(clazz, def);
    }

    /**
     * Return {@code true} if {@code value} is {@code null} or <em>empty</em>
     * based on the semantics of the class.
     * <p>
     * Use the {@link #registerEmptyDefinition(Class, EmptyDefinition)} method
     * to provide a custom definition of emptiness for user defined classes.
     * </p>
     * 
     * @param value the value to check for {@code null} or emptiness
     * @return {@code true} if
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean isNullOrEmpty(T value) {
        if(value == null) {
            return true;
        }
        else {
            Class<T> clazz = (Class<T>) value.getClass();
            EmptyDefinition<T> def = (EmptyDefinition<T>) defs.get(clazz);
            if(def != null) {
                return def.metBy(value);
            }
            else {
                for (Entry<Class<?>, EmptyDefinition<?>> entry : defs
                        .entrySet()) {
                    Class<?> clz = entry.getKey();
                    if(clz.isAssignableFrom(clazz)) {
                        def = (EmptyDefinition<T>) entry.getValue();
                        return def.metBy(value);
                    }
                }
                return false;
            }
        }
    }

    public static <T> T checkNotNullOrEmpty(T value, Object message) {
        if(!isNullOrEmpty(value)) {
            return value;
        }
        else {
            throw message == null ? new IllegalArgumentException()
                    : new IllegalArgumentException(message.toString());
        }
    }

    public static <T> T checkNotNullOrEmpty(T value) {
        return checkNotNullOrEmpty(value, null);
    }

    private AnyObjects() {/* noop */}

}
