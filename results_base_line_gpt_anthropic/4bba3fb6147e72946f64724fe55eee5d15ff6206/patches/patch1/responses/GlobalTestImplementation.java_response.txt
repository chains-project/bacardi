package de.hilling.junit.cdi.annotations;

import de.hilling.junit.cdi.scope.TestScoped;
import org.immutables.value.Value;

// Import from the correct package for Priority
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.CDI;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to mark Alternatives that should globally replace
 * production implementations.
 * <p>
 *     These services cannot be disabled or enabled on a per test basis
 *     because the container is only started once.
 * </p>
 */
@Alternative
@TestScoped
@Stereotype
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
// AnnotationLiteral is now used to define priorities in newer CDI versions
public @interface GlobalTestImplementation {
}