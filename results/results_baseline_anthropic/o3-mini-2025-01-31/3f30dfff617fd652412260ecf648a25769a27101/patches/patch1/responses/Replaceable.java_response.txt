package de.hilling.junit.cdi.scope;

import org.immutables.value.Value;
import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.*;

@Inherited
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Value.Immutable
public @interface Replaceable {
}