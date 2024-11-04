package se.kth.extractor;

import lombok.Getter;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.CtScanner;

import java.util.Set;

@Getter
public class CustomScanner extends CtScanner {

    private final Set<CtElement> executedElements;

    public CustomScanner(Set<CtElement> ctElements) {
        this.executedElements = ctElements;
    }

    public void collectExecutedElements(CtElement ctElement) {
        if (ctElement == null || this.executedElements.contains(ctElement)) {
            return;
        }
        this.executedElements.add(ctElement);

        ctElement.accept(this);
    }

    @Override
    public <T> void visitCtInvocation(CtInvocation<T> invocation) {
        this.collectExecutedElements(invocation.getExecutable());
        super.visitCtInvocation(invocation);
    }

    @Override
    public <T> void visitCtConstructorCall(CtConstructorCall<T> constructorCall) {
        this.collectExecutedElements(constructorCall.getExecutable());
        super.visitCtConstructorCall(constructorCall);
    }

    @Override
    public void visitCtIf(CtIf ifElement) {
        this.collectExecutedElements(ifElement.getThenStatement());
        if (ifElement.getElseStatement() != null) {
            this.collectExecutedElements(ifElement.getElseStatement());
        }
        super.visitCtIf(ifElement);
    }

    @Override
    public void visitCtWhile(CtWhile whileLoop) {
        this.collectExecutedElements(whileLoop.getLoopingExpression());
        this.collectExecutedElements(whileLoop.getBody());
        super.visitCtWhile(whileLoop);
    }

    @Override
    public <R> void visitCtBlock(CtBlock<R> block) {
        for (CtStatement statement : block.getStatements()) {
            this.collectExecutedElements(statement);
        }
        super.visitCtBlock(block);
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> method) {
        this.collectExecutedElements(method.getBody());
        super.visitCtMethod(method);
    }

    @Override
    public <T> void visitCtExecutableReference(CtExecutableReference<T> reference) {
        this.collectExecutedElements(reference.getDeclaration());
        super.visitCtExecutableReference(reference);
    }

}
