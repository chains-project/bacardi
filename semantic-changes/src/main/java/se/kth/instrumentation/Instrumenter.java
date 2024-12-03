package se.kth.instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.instrumentation.model.TargetMethod;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtParameterImpl;

import java.util.List;

public class Instrumenter {

    private final static Logger logger = LoggerFactory.getLogger(Instrumenter.class);

    private final CtModel model;

    public Instrumenter(CtModel model) {
        this.model = model;
    }

    public void instrumentForMethod(TargetMethod targetMethod) {
        CtMethod first = this.locateTargetMethod(targetMethod);
        System.out.println(first);
    }

    private CtMethod locateTargetMethod(TargetMethod targetMethod) {
        List<CtMethod> allMethods = model.getElements(new TypeFilter<>(CtMethod.class));
        List<CtMethod> matchingMethods = allMethods.stream()
                .filter(ctMethod -> methodsEquals(ctMethod, targetMethod))
                .toList();
        if (matchingMethods.size() > 1) {
            logger.warn("Found multiple matching methods for {}, proceeding with first match", targetMethod);
        }
        return matchingMethods.getFirst();
    }

    private boolean methodsEquals(CtMethod ctMethod, TargetMethod targetMethod) {
        return ctMethod.getSimpleName().equals(targetMethod.getName())
                && ((CtClassImpl) ctMethod.getParent()).getQualifiedName().equals(targetMethod.getParentName())
                && ctMethod.getParameters().size() == targetMethod.getParameters().size()
                && ctMethod.getParameters().stream().map(param -> ((CtParameterImpl) param).getType().getQualifiedName()).toList().equals(targetMethod.getParameters());
    }
}
