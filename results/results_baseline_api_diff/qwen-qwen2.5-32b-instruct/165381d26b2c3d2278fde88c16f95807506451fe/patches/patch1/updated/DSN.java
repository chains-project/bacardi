package org.btrplace.safeplace;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.btrplace.safeplace.spec.Constraint;
import org.btrplace.safeplace.spec.SpecScanner;
import org.btrplace.safeplace.testing.Bench;
import org.btrplace.safeplace.testing.Result;
import org.btrplace.safeplace.testing.TestCampaign;
import org.btrplace.safeplace.testing.TestScanner;
import org.btrplace.safeplace.testing.fuzzer.Restriction;
import org.btrplace.safeplace.testing.reporting.CSVReport;
import org.btrplace.safeplace.testing.reporting.StoredReport;
import org.btrplace.safeplace.testing.verification.Verifier;
import org.btrplace.safeplace.testing.verification.btrplace.CheckerVerifier;
import org.btrplace.safeplace.testing.verification.spec.SpecVerifier;
import org.testng.Assert;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DSN {

    public static String root = "xp-dsn";

    public TestScanner newScanner() throws Exception {
        SpecScanner specScanner = new SpecScanner();
        List<Constraint> l = specScanner.scan();
        Bench.mode = Bench.Mode.REPLAY;
        return new TestScanner(l);
    }

    //@Test
    public void fuzzingSizing() throws Exception {
        TestScanner sc = newScanner();

        Path path = Paths.get(root,"fuzz.csv");
        Files.deleteIfExists(path);

        for (int p = 100; p <= 1000; p+=100) {
            for (int s = 2; s <= 20; s+=2) {
                System.out.println("--- Population: " + p + " scale: " + s + " ---");
                Bench.report = new CSVReport(path, Integer.toString(p));
                System.out.println(sc.test(Bench.class).stream().mapToInt(TestCampaign::go).sum());
            }
        }
    }

    //@Test
    public void fuzzingScalability() throws Exception {
        TestScanner sc = newScanner();

        Path p = Paths.get(root, "testing-speed-notrans.csv");
        Files.deleteIfExists(p);

        for (int i = 1; i <= 30; i+=2) {
            System.out.println("--- scaling factor " + i + "; transitions= " + Bench.transitions +" ---");
            Bench.transitions = false;
            Bench.population = 100;
            Bench.scale = i;
            Bench.report = new CSVReport(p, "");
            sc.test(Bench.class).forEach(c -> System.out.println(c.go().toString()));
        }

        p = Paths.get(root, "testing-speed-trans.csv");
        Files.deleteIfExists(p);

        for (int i = 1; i <= 30; i+=2) {
            System.out.println("--- scaling factor " + i + "; transitions= " + Bench.transitions +" ---");
            Bench.transitions = true;
            Bench.population = 100;
            Bench.scale = i;
            Bench.report = new CSVReport(p, "");
            sc.test(Bench.class).forEach(c -> System.out.println(c.go().toString()));
        }
    }

    //@Test
    public void specLength() throws Exception {
        TestScanner sc = newScanner();
        Bench.mode = Bench.Mode.REPLAY;
        Bench.population = 1000;
        Bench.scale = 5;

        sc.test(Bench.class).forEach(x -> {
            x.reportTo(new StoredReport(Paths.get("xp-dsn", "errors.txt"), r -> !r.result().equals(Result.SUCCESS)));
            System.out.println(x.go());
        });
    }

    //@Test
    public void specVsCheckers() throws Exception {
        TestScanner sc = newScanner();
        Bench.population = 500;
        Bench.scale = 10;
        Path path = Paths.get(root, "mode_stable.csv");
        Files.deleteIfExists(path);
        boolean first = true;
        for (boolean repair : new boolean[]{false, true}) {
            if (first) {
                Bench.mode = Bench.Mode.SAVE;
                first = !first;
            } else {
                Bench.mode = Bench.Mode.REPLAY;
            }
            System.out.println("--- Repair: " + repair + "; replay= " + first + " ---");
            Bench.report = new CSVReport(path, repair ? "enabled" : "disabled");
            sc.test(Bench.class).forEach(x -> {
                x.schedulerParams().doRepair(true);
                System.out.println(x.go());
            });
        }
    }

    private static class FunctionVisitor extends VoidVisitorAdapter<Void> {

        private final List<Integer> l;

        FunctionVisitor(List<Integer> numbers) {
            this.l = numbers;
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            if (n.getNameAsString().equals("eval")) {
                n.getRange().ifPresent(r -> l.add(r.end.line - r.begin.line));
            }
            super.visit(n, arg);
        }
    }

    private static class UnitTestsVisitor extends VoidVisitorAdapter<Void> {

        private final List<Integer> l;

        private final PrettyPrinterConfiguration noComments = new PrettyPrinterConfiguration().setPrintComments(false);

        UnitTestsVisitor(List<Integer> numbers) {
            this.l = numbers;
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            System.out.println(n.getNameAsString());
            if (n.toString(noComments).contains("solve")) {
                n.getRange().ifPresent(r -> l.add(r.end.line - r.begin.line));
            }
            super.visit(n, arg);
        }
    }

    private static class SafeplaceTestsVisitor extends VoidVisitorAdapter<Void> {

        private final List<Integer> l;

        SafeplaceTestsVisitor(List<Integer> numbers) {
            this.l = numbers;
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            for (AnnotationExpr a : n.getAnnotations()) {
                if (!a.getNameAsString().equals("CstrTest")) {
                    return;
                }
            }
            System.out.println(n.getName());
            n.getRange().ifPresent(r -> l.add(r.end.line - r.begin.line));
            super.visit(n, arg);
        }
    }
}