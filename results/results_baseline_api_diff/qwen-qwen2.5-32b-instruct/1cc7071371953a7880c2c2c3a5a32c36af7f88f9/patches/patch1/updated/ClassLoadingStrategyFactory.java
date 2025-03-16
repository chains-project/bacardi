package org.assertj.vavr.api;

import io.vavr.control.Try;
import org.assertj.core.internal.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.assertj.core.internal.bytebuddy.dynamic.loading.ClassLoadingStrategy.UsingReflection;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

class ClassLoadingStrategyFactory {

{
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Method PRIVATE_LOOKUP_IN = Try.of(
        () -> MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class)
    ).getOrElse((Method) null);

    static ClassLoadingStrategy classLoadingStrategy(Class<?> assertClass)
    {
        return UsingReflection.of();
    }
}