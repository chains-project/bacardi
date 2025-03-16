package com.pholser.junit.quickcheck.internal.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.ParameterTypeContext;
import com.pholser.junit.quickcheck.internal.generator.conventiontestclasses.Convention;
import com.pholser.junit.quickcheck.internal.generator.conventiontestclasses.GeneratesOtherTypes;
import com.pholser.junit.quickcheck.internal.generator.conventiontestclasses.NotAGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class RegisterGeneratorsByConventionTest {
    private GeneratorRepository repo;
    @Mock private SourceOfRandomness random;
    @Mock private GenerationStatus generationStatus;

    @Before
    public void setupRepository() {
        repo = new GeneratorRepository(random);
    }

    @Test
    public void canGenerateValues() {
        try (MockedStatic<ParameterTypeContext> theMock = Mockito.mockStatic(ParameterTypeContext.class)) {
            theMock.when(() -> ParameterTypeContext.forClass(Convention.class)).thenReturn(new ParameterTypeContext() {
                @Override
                public Class<?> getType() {
                    return Convention.class;
                }
            });

            Generator<?> generator =
                repo.generatorFor(ParameterTypeContext.forClass(Convention.class));

            assertNotNull(generator);
            assertThat(
                generator.generate(random, generationStatus),
                instanceOf(Convention.class));
        }
    }

    @Test
    public void classNameFollowsConventionButNotAGenerator() {
        try (MockedStatic<ParameterTypeContext> theMock = Mockito.mockStatic(ParameterTypeContext.class)) {
            theMock.when(() -> ParameterTypeContext.forClass(NotAGenerator.class)).thenReturn(new ParameterTypeContext() {
                @Override
                public Class<?> getType() {
                    return NotAGenerator.class;
                }
            });

            assertThatNoGeneratorCanBeFound(NotAGenerator.class);
        }
    }

    @Test
    public void noValueTypeMatch() {
        try (MockedStatic<ParameterTypeContext> theMock = Mockito.mockStatic(ParameterTypeContext.class)) {
            theMock.when(() -> ParameterTypeContext.forClass(GeneratesOtherTypes.class)).thenReturn(new ParameterTypeContext() {
                @Override
                public Class<?> getType() {
                    return GeneratesOtherTypes.class;
                }
            });

            assertThatNoGeneratorCanBeFound(GeneratesOtherTypes.class);
        }
    }

    @Test
    public void notAClassFollowingNamingConvention() {
        try (MockedStatic<ParameterTypeContext> theMock = Mockito.mockStatic(ParameterTypeContext.class)) {
            theMock.when(() -> ParameterTypeContext.forClass(this.getClass())).thenReturn(new ParameterTypeContext() {
                @Override
                public Class<?> getType() {
                    return this.getClass();
                }
            });

            assertThatNoGeneratorCanBeFound(this.getClass());
        }
    }

    private void assertThatNoGeneratorCanBeFound(Class<?> valueClass) {
        IllegalArgumentException ex =
            assertThrows(
                IllegalArgumentException.class,
                () -> repo.generatorFor(
                    ParameterTypeContext.forClass(valueClass)));

        assertThat(
            ex.getMessage(),
            containsString(
                "Cannot find generator for " + valueClass.getName()));
    }
}