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

    public static String getConstruct(String apiUse) {


        String a = switch (apiUse) {
            case "METHOD_ADDED_TO_INTERFACE",
                 "METHOD_REMOVED",
                 "METHOD_NOW_FINAL",
                 "METHOD_NOW_ABSTRACT",
                 "METHOD_RETURN_TYPE_CHANGED",
                 "METHOD_REMOVED_IN_SUPERCLASS":
                yield "Method";
            case "FIELD_REMOVED",
                 "FIELD_NOW_FINAL",
                 "FIELD_NO_LONGER_STATIC",
                 "FIELD_NOW_STATIC",
                 "FIELD_LESS_ACCESSIBLE",
                 "ANNOTATION_DEPRECATED_ADDED",
                 "FIELD_TYPE_CHANGED",
                 "FIELD_STATIC_AND_OVERRIDES_STATIC",
                 "FIELD_GENERICS_CHANGED":
                yield "Field";
            case "CLASS_LESS_ACCESSIBLE",
                 "CLASS_NOW_ABSTRACT",
                 "CLASS_NOW_FINAL",
                 "CLASS_NOW_CHECKED_EXCEPTION",
                 "CLASS_REMOVED",
                 "SUPERCLASS_ADDED",
                 "SUPERCLASS_REMOVED",
                 "CLASS_TYPE_CHANGED",
                 "CLASS_GENERIC_TEMPLATE_CHANGED",
                 "ANNOTATION_REMOVED",
                 "ANNOTATION_ADDED",
                 "TYPE_DEPENDENCY",
                 "CLASS_GENERIC_TEMPLATE_GENERICS_CHANGED":
                yield "Class";

            case "INTERFACE_REMOVED",
                 "INTERFACE_ADDED",
                 "METHOD_ABSTRACT_ADDED_TO_CLASS",
                 "METHOD_NEW_DEFAULT":
                yield "Interface";
            case "CONSTRUCTOR_REMOVED":
                yield "Constructor";    // Constructor

            default: {
//                System.out.println("Error: " + apiUse);
                yield apiUse;
            }
        };

        return a;
    }

    public static String getType(CtElement elements, String apiUse) {

        String constructType = getAsCausingConstructs(elements);
        String construct = getConstruct(apiUse);


        return null;
    }
}
