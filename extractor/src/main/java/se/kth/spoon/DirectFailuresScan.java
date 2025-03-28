package se.kth.spoon;

import lombok.Getter;
import se.kth.japicmp_analyzer.ApiChange;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class DirectFailuresScan extends CtScanner {

    private final Set<CtElement> executedElements;
    private final Set<ApiChange> apiChanges;
    private final Set<ApiChange> matchedApiChanges = new HashSet<>();
    private final Map<CtElement, Set<ApiChange>> matchedApiChangesMap = new ConcurrentHashMap<>();

    public DirectFailuresScan(Set<ApiChange> apiChanges) {
        this.executedElements = new HashSet<>();
        this.apiChanges = apiChanges;
    }

    public void collectExecutedElements(CtElement ctElement) {
        if (ctElement == null || this.executedElements.contains(ctElement)) {
            return;
        }
        this.executedElements.add(ctElement);
        ctElement.accept(this);
    }

    public void addCtElementToApiChangeMap(CtElement ctElement, ApiChange apiChange) {
        if (matchedApiChangesMap.containsKey(ctElement)) {
            matchedApiChangesMap.get(ctElement).add(apiChange);
        } else {
            Set<ApiChange> apiChanges = new HashSet<>();
            apiChanges.add(apiChange);
            matchedApiChangesMap.put(ctElement, apiChanges);
        }
    }

    @Override
    public <T> void visitCtInvocation(CtInvocation<T> invocation) {
        String fullyQualifiedName = SpoonFullyQualifiedNameExtractor.getFullyQualifiedName(invocation);
        apiChanges.stream().filter(apiChange ->
                apiChange.getLongName().equals(fullyQualifiedName)
                        || apiChange.getElement().equals(fullyQualifiedName)).forEach(apiChange -> {
            matchedApiChanges.add(apiChange);
            this.collectExecutedElements(invocation.getExecutable());
            addCtElementToApiChangeMap(invocation.getExecutable(), apiChange);
        });
        super.visitCtInvocation(invocation);
    }

    @Override
    public <T> void visitCtConstructorCall(CtConstructorCall<T> constructorCall) {
        String fullyQualifiedName = SpoonFullyQualifiedNameExtractor.getFullyQualifiedName(constructorCall);

        apiChanges.stream().filter(apiChange -> apiChange.getLongName().equals(fullyQualifiedName)
                || apiChange.getElement().equals(fullyQualifiedName)).forEach(apiChange -> {
            matchedApiChanges.add(apiChange);
            this.collectExecutedElements(constructorCall.getExecutable());
            addCtElementToApiChangeMap(constructorCall, apiChange);
        });
        super.visitCtConstructorCall(constructorCall);
    }

    @Override
    public void visitCtIf(CtIf ifElement) {
//        this.collectExecutedElements(ifElement.getThenStatement());
        if (ifElement.getElseStatement() != null) {
//            this.collectExecutedElements(ifElement.getElseStatement());
        }
        super.visitCtIf(ifElement);
    }

    @Override
    public void visitCtWhile(CtWhile whileLoop) {
//        this.collectExecutedElements(whileLoop.getLoopingExpression());
//        this.collectExecutedElements(whileLoop.getBody());
        super.visitCtWhile(whileLoop);
    }

    @Override
    public <R> void visitCtBlock(CtBlock<R> block) {
//        for (CtStatement statement : block.getStatements()) {
//            this.collectExecutedElements(statement);
//        }
        super.visitCtBlock(block);
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> method) {
        String cTElementQualifiedName = SpoonFullyQualifiedNameExtractor.getFullyQualifiedName(method);
        apiChanges.stream().filter(apiChange -> apiChange.getLongName().equals(cTElementQualifiedName)).
                forEach(apiChange -> {
                    matchedApiChanges.add(apiChange);
                    this.collectExecutedElements(method);
                    addCtElementToApiChangeMap(method, apiChange);

                });
        super.visitCtMethod(method);
    }

    @Override
    public <T> void visitCtExecutableReference(CtExecutableReference<T> reference) {
        String fullyQualifiedName = SpoonFullyQualifiedNameExtractor.getFullyQualifiedName(reference);
        apiChanges.stream().filter(apiChange -> apiChange.getLongName().equals(fullyQualifiedName)).forEach(apiChange -> {
            matchedApiChanges.add(apiChange);
            this.collectExecutedElements(reference.getDeclaration());
            addCtElementToApiChangeMap(reference, apiChange);
        });
        super.visitCtExecutableReference(reference);
    }

    @Override
    public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
//        this.collectExecutedElements(reference.getDeclaration());
        String fullyQualifiedName = SpoonFullyQualifiedNameExtractor.getFullyQualifiedName(reference);
        apiChanges.stream().filter(apiChange -> apiChange.getLongName().equals(fullyQualifiedName)).forEach(apiChange -> {
            matchedApiChanges.add(apiChange);
            this.collectExecutedElements(reference.getDeclaration());
            addCtElementToApiChangeMap(reference, apiChange);
        });
        super.visitCtTypeReference(reference);
    }

    @Override
    public <A extends Annotation> void visitCtAnnotation(CtAnnotation<A> annotation) {
//        this.collectExecutedElements(annotation.getAnnotationType());
        super.visitCtAnnotation(annotation);
    }

    @Override
    public <T> void visitCtFieldReference(CtFieldReference<T> reference) {
//        this.collectExecutedElements(reference.getDeclaringType());
    }

    @Override
    public <T> void visitCtAnnotationMethod(CtAnnotationMethod<T> annotationMethod) {

        super.visitCtAnnotationMethod(annotationMethod);
    }

    @Override
    public <T> void visitCtAnnotationFieldAccess(CtAnnotationFieldAccess<T> annotationFieldAccess) {

        super.visitCtAnnotationFieldAccess(annotationFieldAccess);
    }

    @Override
    public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
        super.visitCtAnnotationType(annotationType);
    }

    @Override
    public void visitCtTry(CtTry tryBlock) {
//        this.collectExecutedElements(tryBlock.getBody());
        super.visitCtTry(tryBlock);
    }

    @Override
    public void visitCtCatch(CtCatch catchBlock) {
//        this.collectExecutedElements(catchBlock.getBody());
        super.visitCtCatch(catchBlock);
    }

    @Override
    public void visitCtTryWithResource(CtTryWithResource tryWithResource) {
        // this.collectExecutedElements(tryWithResource.getBody());
        super.visitCtTryWithResource(tryWithResource);
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
        ctClass.getFields().stream()
                .filter(CtModifiable::isStatic)
                .forEach(this::collectExecutedElements);
        ctClass.getAnnotations()
                .forEach(this::collectExecutedElements);
        super.visitCtClass(ctClass);
    }

    @Override
    public <T, A extends T> void visitCtAssignment(CtAssignment<T, A> assignment) {
        // this.collectExecutedElements(assignment.getAssigned());
        // this.collectExecutedElements(assignment.getAssignment());
        super.visitCtAssignment(assignment);
    }

    @Override
    public <R> void visitCtReturn(CtReturn<R> returnStatement) {
        // this.collectExecutedElements(returnStatement.getReturnedExpression());
        super.visitCtReturn(returnStatement);
    }

    @Override
    public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
        // this.collectExecutedElements(fieldRead.getVariable());
        super.visitCtFieldRead(fieldRead);
    }

}
