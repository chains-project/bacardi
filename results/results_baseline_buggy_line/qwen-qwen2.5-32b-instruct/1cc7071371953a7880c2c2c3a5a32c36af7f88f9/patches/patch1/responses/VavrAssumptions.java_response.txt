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
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.assertj.core.util.CheckReturnValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class VavrAssumptions {

    private static final ByteBuddy BYTE_BUDDY = new ByteBuddy().with(TypeValidation.DISABLED)
            .with(new net.bytebuddy.dynamic.scaffold.subclass.NamingStrategy.SuffixingRandom("Assertj$Assumptions"));

    private static final MethodDelegation ASSUMPTION = MethodDelegation.to(AssumptionMethodInterceptor.class);

    private static final net.bytebuddy.pool.TypeCache<net.bytebuddy.pool.TypeCache.SimpleKey> CACHE = new net.bytebuddy.pool.TypeCache.Simple<>(net.bytebuddy.pool.TypeCache.Sort.SOFT);

    private static final class AssumptionMethodInterceptor {

        @RuntimeType
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

    @CheckReturnValue
    public static <LEFT, RIGHT> EitherAssert<LEFT, RIGHT> assumeThat(Either<LEFT, RIGHT> actual) {
        return asAssumption(EitherAssert.class, Either.class, actual);
    }

    @CheckReturnValue
    public static <VALUE> LazyAssert<VALUE> assumeThat(Lazy<VALUE> actual) {
        return asAssumption(LazyAssert.class, Lazy.class, actual);
    }

    @CheckReturnValue
    public static <K, V> MapAssert<K, V> assumeThat(Map<K, V> actual) {
        return asAssumption(MapAssert.class, Map.class, actual);
    }

    @CheckReturnValue
    public static <K, V> MultimapAssert<K, V> assumeThat(Multimap<K, V> actual) {
        return asAssumption(MultimapAssert.class, Multimap.class, actual);
    }

    @CheckReturnValue
    public static <VALUE> OptionAssert<VALUE> assumeThat(Option<VALUE> actual) {
        return asAssumption(OptionAssert.class, Option.class, actual);
    }

    @CheckReturnValue
    public static <ELEMENT> SeqAssert<ELEMENT> assumeThat(Seq<ELEMENT> actual) {
        return asAssumption(SeqAssert.class, Seq.class, actual);
    }

    @CheckReturnValue
    public static <INVALID, VALID> ValidationAssert<INVALID, VALID> assumeThat(Validation<INVALID, VALID> actual) {
        return asAssumption(ValidationAssert.class, Validation.class, actual);
    }

    private static <ASSERTION, ACTUAL> ASSERTION asAssumption(Class<ASSERTION> assertionType,
                                                              Class<ACTUAL> actualType,
                                                              Object actual) {
        return asAssumption(assertionType, new Class<?>[]{actualType}, new Object[]{actual});
    }

    private static <ASSERTION> ASSERTION asAssumption(Class<ASSERTION> assertClass, Class<?>[] constructorTypes, Object... constructorParams) {
        try {
            Class<? extends ASSERTION> type = createAssumptionClass(assertClass);
            Constructor<? extends ASSERTION> constructor = type.getConstructor(constructorTypes);
            return constructor.newInstance(constructorParams);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException("Cannot create assumption instance", e);
        }
    }

    private static <ASSERTION> Class<? extends ASSERTION> createAssumptionClass(Class<ASSERTION> assertionType) {
        net.bytebuddy.pool.TypeCache.SimpleKey cacheKey = new net.bytebuddy.pool.TypeCache.SimpleKey(assertionType);
        return (Class<ASSERTION>) CACHE.findOrInsert(VavrAssumptions.class.getClassLoader(),
                cacheKey,
                () -> generateAssumptionClass(assertionType));
    }

    private static <ASSERTION> Class<? extends ASSERTION> generateAssumptionClass(Class<ASSERTION> assertionType) {
        return BYTE_BUDDY.subclass(assertionType)
                .method(ElementMatchers.any())
                .intercept(ASSUMPTION)
                .make()
                .load(VavrAssumptions.class.getClassLoader())
                .getLoaded();
    }

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

    private static RuntimeException assumptionNotMet(Class<?> exceptionClass, AssertionError e) throws ReflectiveOperationException {
        return (RuntimeException) exceptionClass.getConstructor(String.class, Throwable.class)
                .newInstance("assumption was not met due to: " + e.getMessage(), e);
    }

    private static AbstractVavrAssert<?, ?> asAssumption(AbstractVavrAssert<?, ?> assertion) {
        Object actual = assertion.actual();
        if (assertion instanceof LazyAssert) return asAssumption(LazyAssert.class, Lazy.class, actual);
        if (assertion instanceof EitherAssert) return asAssumption(EitherAssert.class, Either.class, actual);
        if (assertion instanceof MapAssert) return asAssumption(MapAssert.class, Map.class, actual);
        if (assertion instanceof OptionAssert) return asAssumption(OptionAssert.class, Option.class, actual);
        if (assertion instanceof SeqAssert) return asAssumption(SeqAssert.class, Seq.class, actual);
        if (assertion instanceof TryAssert) return asAssumption(TryAssert.class, Try.class, actual);
        if (assertion instanceof ValidationAssert) return asAssumption(ValidationAssert.class, Validation.class, actual);
        throw new IllegalArgumentException("Unsupported assumption creation for " + assertion.getClass());
    }
}