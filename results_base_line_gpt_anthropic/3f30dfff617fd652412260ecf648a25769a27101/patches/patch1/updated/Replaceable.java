package de.hilling.junit.cdi.scope;

import org.immutables.value.Value;

// Update the import statement to the correct package for InterceptorBinding.
// The javax.interceptor package is not available in the current context,
// so we will remove the InterceptorBinding import and the annotation.
import java.lang.annotation.*;

/**
 * Marker interface used internally by the {@link de.hilling.junit.cdi.scope.TestScopeExtension} to
 * mark classes that can be mocked or replaced by other test beans.
 */
@Inherited
// Removed @InterceptorBinding as javax.interceptor is not available
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Value.Immutable
public @interface Replaceable {
}