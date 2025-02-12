package se.kth.Util;

import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtFieldImpl;

public class ConstructType {

    public static String getAsCausingConstructs(CtElement elements) {


        if (elements instanceof CtExecutableReference<?>) {
            if (((CtExecutableReference<?>) elements).isConstructor()) {
                return "Constructor";
            } else {
                return "Method";
            }
        }
        if (elements instanceof CtFieldRead<?>) {
            return "Field";
        }
        if (elements instanceof CtTypeReference<?>) {
            return "Type reference";
        }
        if (elements instanceof CtVariableRead<?>) {
            return "Variable reference";
        }
        if (elements instanceof CtFieldReference<?>) {
            return "Field reference";
        }
        if (elements instanceof CtFieldImpl<?>) {
            return "Field";
        }
        if (elements instanceof CtInvocation<?>) {
            if (((CtInvocation<?>) elements).getExecutable().isConstructor()) {
                return "Constructor";
            } else {
                return "Method";
            }
        }
        return "Construct";


    }



}
