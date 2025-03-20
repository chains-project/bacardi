/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2017-2022 the original author or authors.
 */
package org.assertj.vavr.api;

import io.vavr.Lazy;
import io.vavr.collection.Map;
import io.vavr.collection.Multimap;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.assertj.core.util.CheckReturnValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import static org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.any;
import static org.assertj.core.util.Arrays.array;
import static org.assertj.vavr.api.ClassLoadingStrategyFactory.classLoadingStrategy;

public class VavrAssumptions {

    private static final Class<?> BYTE_BUDDY_CLASS = loadClass("org.assertj.core.internal.bytebuddy.ByteBuddy");
    private static final Class<?> TYPE_CACHE_CLASS = loadClass("org.assertj.core.internal.bytebuddy.TypeCache");
    private static final Class<?> SIMPLE_KEY_CLASS = loadClass("org.assertj.core.internal.bytebuddy.TypeCache$SimpleKey");
    private static final Class<?> AUXILIARY_TYPE_CLASS = loadClass("org.assertj.core.internal.bytebuddy.implementation.auxiliary.AuxiliaryType");
    private static final Class<?> METHOD_DELEGATION_CLASS = loadClass("org.assertj.core.internal.bytebuddy.implementation.MethodDelegation");
    private static final Class<?> RUNTIME_TYPE_CLASS = loadClass("org.assertj.core.internal.bytebuddy.implementation.bind.annotation.RuntimeType");
    private static final Class<?> THIS_CLASS = loadClass("org.assertj.core.internal.bytebuddy.implementation.bind.annotation.This");
    private static final Class<?> SUPER_CALL_CLASS = loadClass("org.assertj.core.internal.bytebuddy.implementation.bind.annotation.SuperCall");
    private static final Class<?> TYPE_VALIDATION_CLASS = loadClass("org.assertj.core.internal.bytebuddy.dynamic.scaffold.TypeValidation");

    private static final Object BYTE_BUDDY = createInstance(BYTE_BUDDY_CLASS);
    private static final Object CACHE = createInstance(TYPE_CACHE_CLASS);
    private static final Object ASSUMPTION = createMethodDelegation();

    private static Object createMethodDelegation() {
        try {
            return METHOD_DELEGATION_CLASS.getMethod("to", Class.class).invoke(null, AssumptionMethodInterceptor.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create method delegation instance", e);
        }
    }

    private static Object createInstance(Class<?> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create instance of " + clazz.getName(), e);
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }

    // ... (rest of the code remains unchanged)

}
