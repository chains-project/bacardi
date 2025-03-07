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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.any;

public class VavrAssumptions {

    /**
     * This NamingStrategy takes the original class's name and adds a suffix to distinguish it.
     * The default is ByteBuddy but for debugging purposes, it makes sense to add AssertJ as a name.
     */
    private static final ByteBuddy BYTE_BUDDY = new ByteBuddy().with(TypeValidation.DISABLED);

    private static final Implementation ASSUMPTION = MethodDelegation.to(AssumptionMethodInterceptor.class);

    private static final net.bytebuddy.pool.TypePool.TypeCache<net.bytebuddy.pool.TypePool.Key> CACHE = new net.bytebuddy.pool.TypePool.Default().withCache();

    private static final class AssumptionMethodInterceptor {

        @net.bytebuddy.implementation.bind.annotation.RuntimeType
        public static Object intercept(@This AbstractVavrAssert<?, ?> assertion, @SuperCall Callable<Object> proxy) throws Exception {
            try {
                Object result = proxy.call();
                if (result != assertion && result instanceof AbstractVavrAssert) {
                    final AbstractVavrAssert<?, ?> assumption = asAssumption((AbstractVavrAssert<?, ?>) result);
                    return assumption.withAssertionState(assertion);
                }
                return result;
            } catch (AssertionError e) {
                throw assumptionNotMet(e);
            }
        }
    }

    // ... (rest of the class remains unchanged)

    private static RuntimeException assumptionNotMet(AssertionError assertionError) throws ReflectiveOperationException {
        Class<?> assumptionClass = getAssumptionClass("org.junit.AssumptionViolatedException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, assertionError);

        assumptionClass = getAssumptionClass("org.opentest4j.TestAbortedException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, assertionError);

        assumptionClass = getAssumptionClass("org.testng.SkipException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, assertionError);

        throw new IllegalStateException("Assumptions require JUnit, opentest4j or TestNG on the classpath");
    }

    private static Class<?> getAssumptionClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static <ASSERTION, ACTUAL> ASSERTION asAssumption(Class<ASSERTION> assertionType,
                                                              Class<?>[] constructorTypes,
                                                              Object... constructorParams) {
        try {
            Class<? extends ASSERTION> type = createAssumptionClass(assertionType);
            Constructor<? extends ASSERTION> constructor = type.getConstructor(constructorTypes);
            return constructor.newInstance(constructorParams);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException("Cannot create assumption instance", e);
        }
    }

    private static <ASSERTION> Class<? extends ASSERTION> createAssumptionClass(Class<ASSERTION> assertionType) {
        return BYTE_BUDDY.subclass(assertionType)
                .method(any())
                .intercept(ASSUMPTION)
                .make()
                .load(VavrAssumptions.class.getClassLoader())
                .getLoaded();
    }

    // for method that change the object under test (e.g. extracting)
    private static AbstractVavrAssert<?, ?> asAssumption(AbstractVavrAssert<?, ?> assertion) {
        // @format:off
        Object actual = assertion.actual();
        if (assertion instanceof LazyAssert) return asAssumption(LazyAssert.class, Lazy.class, actual);
        if (assertion instanceof EitherAssert) return asAssumption(EitherAssert.class, Either.class, actual);
        if (assertion instanceof MapAssert) return asAssumption(MapAssert.class, Map.class, actual);
        if (assertion instanceof OptionAssert) return asAssumption(OptionAssert.class, Option.class, actual);
        if (assertion instanceof SeqAssert) return asAssumption(SeqAssert.class, Seq.class, actual);
        if (assertion instanceof TryAssert) return asAssumption(TryAssert.class, Try.class, actual);
        if (assertion instanceof ValidationAssert) return asAssumption(ValidationAssert.class, Validation.class, actual);
        // @format:on
        // should not arrive here
        throw new IllegalArgumentException("Unsupported assumption creation for " + assertion.getClass());
    }

    // ... (rest of the class remains unchanged)
}