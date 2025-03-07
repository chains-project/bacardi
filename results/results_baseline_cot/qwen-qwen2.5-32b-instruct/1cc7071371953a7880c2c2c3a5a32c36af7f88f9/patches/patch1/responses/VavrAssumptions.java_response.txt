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
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.any;

public class VavrAssumptions {

    /**
     * This NamingStrategy takes the original class's name and adds a suffix to distinguish it.
     */
    private static final ByteBuddy BYTE_BUDDY = new ByteBuddy().with(TypeValidation.DISABLED)
            .with(new net.bytebuddy.implementation.auxiliary.AuxiliaryType.NamingStrategy.SuffixingRandom("Assertj$Assumptions"));

    private static final Implementation ASSUMPTION = net.bytebuddy.implementation.MethodDelegation.to(AssumptionMethodInterceptor.class);

    private static final net.bytebuddy.pool.TypePool.TypeCache<net.bytebuddy.pool.TypeCache.SimpleKey> CACHE = new net.bytebuddy.pool.TypePool.Default().withCache(new net.bytebuddy.pool.TypePool.Default.Cache.Simple<net.bytebuddy.pool.TypeCache.SimpleKey>());

    private static final class AssumptionMethodInterceptor {

        @RuntimeType
        public static Object intercept(@net.bytebuddy.implementation.bind.annotation.This Object thisInstance, @SuperCall Callable<Object> proxy) throws Exception {
            Object result = proxy.call();
            if (result != thisInstance && result instanceof AbstractVavrAssert) {
                final AbstractVavrAssert<?, ?> assumption = asAssumption((AbstractVavrAssert<?, ?>) result);
                return assumption.withAssertionState((AbstractVavrAssert<?, ?>) thisInstance);
            }
            return result;
        }
    }

    /**
     * Creates a new instance of <code>{@link EitherAssert}</code> assumption.
     *
     * @param <LEFT>  type of the left value contained in the {@link Either}.
     * @param <RIGHT> type of the right value contained in the {@link Either}.
     * @param actual  the actual value.
     * @return the created assumption for assertion object.
     */
    @SuppressWarnings("unchecked")
    public static <LEFT, RIGHT> EitherAssert<LEFT, RIGHT> assumeThat(Either<LEFT, RIGHT> actual) {
        return asAssumption(EitherAssert.class, Either.class, actual);
    }

    /**
     * Creates a new instance of <code>{@link LazyAssert}</code> assumption.
     *
     * @param <VALUE> type of the value contained in the {@link Lazy}.
     * @param actual  the actual value.
     * @return the created assumption for assertion object.
     */
    @SuppressWarnings("unchecked")
    public static <VALUE> LazyAssert<VALUE> assumeThat(Lazy<VALUE> actual) {
        return asAssumption(LazyAssert.class, Lazy.class, actual);
    }

    /**
     * Creates a new instance of <code>{@link MapAssert}</code> assumption.
     *
     * @param <K>    the type of keys in the map.
     * @param <V>    the type of values in the map.
     * @param actual the actual value.
     * @return the created assumption for assertion object.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> MapAssert<K, V> assumeThat(Map<K, V> actual) {
        return asAssumption(MapAssert.class, Map.class, actual);
    }

    /**
     * Creates a new instance of <code>{@link MultimapAssert}</code> assumption.
     *
     * @param <K>    the type of keys in the multimap.
     * @param <V>    the type of values in the multimap.
     * @param actual the actual value.
     * @return the created assumption for assertion object.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> MultimapAssert<K, V> assumeThat(Multimap<K, V> actual) {
        return asAssumption(MultimapAssert.class, Multimap.class, actual);
    }

    /**
     * Creates a new instance of <code>{@link OptionAssert}</code> assumption.
     *
     * @param <VALUE> type of the value contained in the {@link Option}.
     * @param actual  the actual value.
     * @return the created assumption for assertion object.
     */
    @SuppressWarnings("unchecked")
    public static <VALUE> OptionAssert<VALUE> assumeThat(Option<VALUE> actual) {
        return asAssumption(OptionAssert.class, Option.class, actual);
    }

    /**
     * Creates a new instance of <code>{@link SeqAssert}</code> assumption.
     *
     * @param <ELEMENT> type of elements contained in the {@link Seq}.
     * @param actual  the actual value.
     * @return the created assumption for assertion object.
     */
    @SuppressWarnings("unchecked")
    public static <ELEMENT> SeqAssert<ELEMENT> assumeThat(Seq<ELEMENT> actual) {
        return asAssumption(SeqAssert.class, Seq.class, actual);
    }

    /**
     * Creates a new instance of <code>{@link TryAssert}</code> assumption.
     *
     * @param <VALUE> type of the value contained in the {@link io.vavr.control.Try}.
     * @param actual  the actual value.
     * @return the created assumption for assertion object.
     */
    @SuppressWarnings("unchecked")
    public static <VALUE> TryAssert<VALUE> assumeThat(Try<VALUE> actual) {
        return asAssumption(TryAssert.class, Try.class, actual);
    }

    /**
     * Creates a new instance of <code>{@link ValidationAssert}</code> assumption.
     *
     * @param <INVALID> type of the value in the case of the invalid {@link Validation}.
     * @param <VALID>   type of the value in the case of the valid {@link Validation}.
     * @param actual  the actual value.
     * @return the created assumption for assertion object.
     */
    @SuppressWarnings("unchecked")
    public static <INVALID, VALID> ValidationAssert<INVALID, VALID> assumeThat(Validation<INVALID, VALID> actual) {
        return asAssumption(ValidationAssert.class, Validation.class, actual);
    }

    private static <ASSERTION, ACTUAL> ASSERTION asAssumption(Class<ASSERTION> assertionType,
                                                              Class<ACTUAL> actualType,
                                                              Object actual) {
        return asAssumption(assertionType, new Class<?>[]{actualType}, new Object[]{actual});
    }

    private static <ASSERTION> ASSERTION asAssumption(Class<ASSERTION> assertionType,
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

    @SuppressWarnings("unchecked")
    private static <ASSERTION> Class<? extends ASSERTION> createAssumptionClass(Class<ASSERTION> assertClass) {
        net.bytebuddy.pool.TypeCache.SimpleKey cacheKey = new net.bytebuddy.pool.TypeCache.SimpleKey(assertClass);
        return (Class<ASSERTION>) CACHE.findOrInsert(cacheKey, () -> generateAssumptionClass(assertClass));
    }

    private static <ASSERTION> Class<? extends ASSERTION> generateAssumptionClass(Class<ASSERTION> assertionType) {
        return BYTE_BUDDY.subclass(assertionType)
                .method(any())
                .intercept(ASSUMPTION)
                .make()
                .load(VavrAssumptions.class.getClassLoader())
                .getLoaded();
    }

    private static RuntimeException assumptionNotMet(AssertionError e) {
        Class<?> assumptionClass = getAssumptionClass("org.junit.AssumptionViolatedException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, e);

        assumptionClass = getAssumptionClass("org.opentest4j.TestAbortedException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, e);

        assumptionClass = getAssumptionClass("org.testng.SkipException");
        if (assumptionClass != null) return assumptionNotMet(assumptionClass, e);

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
        if (actual instanceof Either) {
            return asAssumption(EitherAssert.class, Either.class, actual);
        } else if (actual instanceof Lazy) {
            return asAssumption(LazyAssert.class, Lazy.class, actual);
        } else if (actual instanceof Map) {
            return asAssumption(MapAssert.class, Map.class, actual);
        } else if (actual instanceof Multimap) {
            return asAssumption(MultimapAssert.class, Multimap.class, actual);
        } else if (actual instanceof Option) {
            return asAssumption(OptionAssert.class, Option.class, actual);
        } else if (actual instanceof Seq) {
            return asAssumption(SeqAssert.class, Seq.class, actual);
        } else if (actual instanceof Try) {
            return asAssumption(TryAssert.class, Try.class, actual);
        } else if (actual instanceof Validation) {
            return asAssumption(ValidationAssert.class, Validation.class, actual);
        }
        throw new IllegalArgumentException("Unsupported assumption creation for " + assertion.getClass());
    }
}