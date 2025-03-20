package org.assertj.vavr.api;

import io.vavr.control.Try;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

class ClassLoadingStrategyFactory {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final Method PRIVATE_LOOKUP_IN = Try.of(
      () -> MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class)
  ).getOrElse((Method) null);

  static ClassLoadingStrategy<ClassLoader> classLoadingStrategy(Class<?> assertClass) {
    if (ClassInjector.UsingReflection.isAvailable()) {
      return ClassLoadingStrategy.Default.INJECTION;
    } else if (ClassInjector.UsingLookup.isAvailable() && PRIVATE_LOOKUP_IN != null) {
      try {
        return ClassLoadingStrategy.UsingLookup.of(PRIVATE_LOOKUP_IN.invoke(null, assertClass, LOOKUP));
      } catch (Exception e) {
        throw new IllegalStateException("Could not access package of " + assertClass, e);
      }
    } else {
      throw new IllegalStateException("No code generation strategy available");
    }
  }

  public static interface ClassLoadingStrategy<T> {
    class Default {
      public static final ClassLoadingStrategy<ClassLoader> INJECTION = new ClassLoadingStrategyImpl<>();
    }
    class UsingLookup {
      public static <T> ClassLoadingStrategy<T> of(Object lookupTarget) {
        return new ClassLoadingStrategyImpl<>();
      }
    }
  }

  private static class ClassLoadingStrategyImpl<T> implements ClassLoadingStrategy<T> {
    // Stub implementation.
  }

  public static class ClassInjector {
    public static class UsingReflection {
      public static boolean isAvailable() {
        // Reflection based injection is no longer available.
        return false;
      }
    }
    public static class UsingLookup {
      public static boolean isAvailable() {
        return PRIVATE_LOOKUP_IN != null;
      }
    }
  }
}
