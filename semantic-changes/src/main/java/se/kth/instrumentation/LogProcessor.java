package se.kth.instrumentation;

import spoon.SpoonModelBuilder;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.factory.FactoryImpl;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;

public class LogProcessor extends AbstractProcessor<CtExecutable> {

    private final FactoryImpl factory;

    public LogProcessor() {
        this.factory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment());

    }

    @Override
    public void process(CtExecutable element) {
        CtCodeSnippetStatement snippet = this.factory.Core().createCodeSnippetStatement();

        // Snippet which contains the log.
        final String value = String.format("System.out.println(\"Enter in the method %s from class %s\");",
                element.getSimpleName(),
                element.getParent(CtClass.class).getSimpleName());
        snippet.setValue(value);

        // Inserts the snippet at the beginning of the method body.
        if (element.getBody() != null) {
            element.getBody().insertBegin(snippet);
        }
    }
}
