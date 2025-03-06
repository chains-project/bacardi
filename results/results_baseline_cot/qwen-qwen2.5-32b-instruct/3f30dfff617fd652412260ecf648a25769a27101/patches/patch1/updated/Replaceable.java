package de.hilling.junit.cdi.scope;

import org.immutables.value.Value;

import java.lang.annotation.*;

/**
 * Marker interface used internally by the {@link de.hilling.junit.cdi.scope.TestScopeExtension} to
 * mark classes that can be mocked or replaced by other test beans.
 */
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Value.Immutable
public @interface Replaceable {
}