package se.kth;

import com.google.gson.Gson;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.FileWriter;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

public class MethodReturnLoggerAgent {
    public static final List<Object> returnValues = new ArrayList<>();

    public static void premain(String agentArgs, Instrumentation inst) {
        new AgentBuilder.Default()
                .type(ElementMatchers.named("org.jsoup.nodes.Element"))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) ->
                        builder.method(ElementMatchers.named("prepend"))
                                .intercept(Advice.to(MethodReturnLoggerAdvice.class)))
                .installOn(inst);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveToFile();
        }));
    }

    public static void saveToFile() {
        try (FileWriter writer = new FileWriter("method_returns.json")) {
            new Gson().toJson(returnValues, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MethodReturnLoggerAdvice {
        @Advice.OnMethodExit
        static void logReturnValue(@Advice.Return Object returnValue) {
            synchronized (returnValues) {
                System.out.println("Logging value");
                returnValues.add(returnValue.toString());
            }
        }
    }
}

