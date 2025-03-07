package org.apache.bval.jsr;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.el.ExpressionFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.metadata.ConstraintDescriptor;

import org.apache.bval.constraints.NotEmpty;
import org.apache.bval.jsr.example.Author;
import org.apache.bval.jsr.example.PreferredGuest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

/**
 * MessageResolverImpl Tester.
 */
@RunWith(Parameterized.class)
public class DefaultMessageInterpolatorTest {
    @Parameters(name="{0}")
    public static List<Object[]> generateParameters(){
        return Arrays.asList(new Object[] { "default", null },
            new Object[] { "ri", "org.glassfish.expressly.ExpressionFactoryImpl" },
            new Object[] { "tomcat", "org.apache.el.ExpressionFactory" },
            new Object[] { "invalid", "java.lang.String" });
    }

    @AfterClass
    public static void cleanup() {
        System.clearProperty(ExpressionFactory.class.getName());
    }

    private static Predicate<ConstraintDescriptor<?>> forConstraintType(Class<? extends Annotation> type) {
        return d -> Objects.equals(type, d.getAnnotation().annotationType());
    }

    private String elImpl;
    private String elFactory;
    private DefaultMessageInterpolator interpolator;
    private Validator validator;
    private boolean elAvailable;
    private ClassLoader originalClassLoader;

    public DefaultMessageInterpolatorTest(String elImpl, String elFactory) {
        this.elImpl = elImpl;
        this.elFactory = elFactory;
    }

    @Before
    public void setUp() throws Exception {
        // store and replace CCL to sidestep EL factory caching
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] {}, originalClassLoader));

        try {
            if (elFactory == null) {
                System.clearProperty(ExpressionFactory.class.getName());
            } else {
                Class<?> elFactoryClass = Class.forName(elFactory);
                System.setProperty(ExpressionFactory.class.getName(), elFactory);

                Class<? extends ExpressionFactory> usedImpl =
                        ((DelegateExpressionFactory) ExpressionFactory.newInstance()).getWrapped().getClass();
                assertTrue(elFactoryClass == usedImpl);
            }
            elAvailable = true;
        } catch (Exception e) {
            elAvailable = false;
        }
        interpolator = new DefaultMessageInterpolator();
        interpolator.setLocale(Locale.ENGLISH);
        validator = ApacheValidatorFactory.getDefault().getValidator();
    }

    @After
    public void tearDownEL() {
        assumeThat(elImpl, equalTo("invalid"));
        assertFalse(elAvailable);
        
        ApacheMessageContext context = context("12345678",
            () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
            .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
            .orElseThrow(() -> new AssertionError("expected constraint missing")));

        when(context
            .getConfigurationProperty(ApacheValidatorConfiguration.Properties.CUSTOM_TEMPLATE_EXPRESSION_EVALUATION))
                .thenAnswer(invocation -> Boolean.toString(true));

        assertEquals("${regexp.charAt(4)}", interpolator.interpolate("${regexp.charAt(4)}",
            context));
    }

    @Test
    public void testDisallowCustomTemplateExpressionEvaluationByDefault() {
        assumeTrue(elAvailable);

        assertEquals("${regexp.charAt(4)}", interpolator.interpolate("${regexp.charAt(4)}",
            context("12345678",
                () -> validator.getConstraintsForClass(Person.class).getConstraintsForProperty("idNumber")
                .getConstraintDescriptors().stream().filter(forConstraintType(Pattern.class)).findFirst()
                .orElseThrow(() -> new AssertionError("expected constraint missing"))));
    }

    @SuppressWarnings("unchecked")
    private ApacheMessageContext context(Object validatedValue, Supplier<ConstraintDescriptor<?>> descriptor) {
        final ApacheMessageContext result = Mockito.mock(ApacheMessageContext.class);
        when(result.unwrap(any(Class.class)))
            .thenAnswer(invocation -> invocation.getArgument(0).cast(result));
        when(result.getValidatedValue()).thenReturn(validatedValue);
        when(result.getConstraintDescriptor()).thenAnswer(invocation -> descriptor.get());
        return result;
    }

    public static class Person {

        @Pattern(message = "Id number should match {regexp}", regexp = "....$")
        public String idNumber;

        @Pattern(message = "Other id should match {regexp}", regexp = ".\\n")
        public String otherId;

        @Pattern(message = "Another value should match ${regexp.intern()}", regexp = "....$")
        public String anotherValue;
        
        @Pattern(message = "Mixed message value of length ${validatedValue.length()} should match {regexp}", regexp = "....$")
        public String mixedMessageValue;
    }
}