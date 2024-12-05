package se.kth.spoon;

import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

public class SpoonFullyQualifiedNameExtractor {
    public static String getFullyQualifiedName(CtField<?> ctField) {
        return getFullyQualifiedName(ctField.getDeclaringType()) + "." + ctField.getSimpleName();
    }

    public static String getFullyQualifiedName(CtInvocation<?> ctInvocation) {
        return getFullyQualifiedName(ctInvocation.getExecutable());
    }

    public static String getFullyQualifiedName(CtConstructorCall<?> ctConstructorCall) {
        return getFullyQualifiedName(ctConstructorCall.getExecutable());
    }

    public static String getFullyQualifiedName(CtExecutableReference<?> ctExecutableReference) {
        if (ctExecutableReference.isConstructor()) {
            return ctExecutableReference.getSignature();
        } else {
            CtTypeReference<?> typeReference = ctExecutableReference.getDeclaringType();
            if (typeReference == null) {
                return ctExecutableReference.getSignature();
            }
            return typeReference.getQualifiedName() + "." + ctExecutableReference.getSimpleName();
        }
    }

    public static String getFullyQualifiedName(CtFieldRead<?> ctFieldRead) {
        return getFullyQualifiedName(ctFieldRead.getVariable());
    }

    public static String getFullyQualifiedName(CtFieldWrite<?> ctFieldWrite) {
        return getFullyQualifiedName(ctFieldWrite.getVariable());
    }

    public static String getFullyQualifiedName(CtFieldReference<?> ctFieldReference) {
        return ctFieldReference.getQualifiedName();
    }

    public static String getFullyQualifiedName(CtTypeReference<?> ctTypeReference) {
        return ctTypeReference.getQualifiedName();
    }

    public static String getFullyQualifiedName(CtMethod<?> ctMethod) {
        CtType<?> declaringType = ctMethod.getDeclaringType();
        return declaringType.getQualifiedName() + "." + ctMethod.getSignature();
    }

    public static String getFullyQualifiedName(CtConstructor<?> ctConstructor) {
        return ctConstructor.getSignature();
    }

    public static String getFullyQualifiedName(CtType<?> ctType) {
        return ctType.getQualifiedName();
    }
}
