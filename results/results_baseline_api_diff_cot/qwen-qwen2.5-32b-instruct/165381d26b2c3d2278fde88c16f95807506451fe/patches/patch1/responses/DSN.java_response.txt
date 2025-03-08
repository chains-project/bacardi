/*
 * Copyright  2020 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.safeplace;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration; // Updated import
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

/**
 * @author Fabien Hermenier
 */
public class DSN {

    public static String root = "xp-dsn";

    public TestScanner newScanner() throws Exception {
        SpecScanner specScanner = new SpecScanner();
        List<Constraint> l = specScanner.scan();
        System.out.println(l.stream().map(Constraint::pretty).collect(Collectors.joining("\n")));

        return new TestScanner(l);
    }

    //@Test
    public void fuzzingSizing() throws Exception {
        TestScanner sc = newScanner();

        Path path = Paths.get(root, "fuzz.csv");
        Files.deleteIfExists(path);

        for (int p = 100; p <= 1000; p+=100) {
            for (int s = 2; s <= 20; s+=2) {
                System.out.println("--- Population: " + p + " scale: " + s + " ---");
                Bench.report = new CSVReport(path, Integer.toString(p));
                Bench.population = p;
                Bench.scale = s;
                sc.testGroups("sides").forEach(x -> System.out.println(x.go().toString()));
            }
        }
    }

    //@Test
    public void fuzzingScalability() throws Exception {
        TestScanner sc = newScanner();
        Bench.population = 500;
        Bench.scale = 10;
        Path p = Paths.get(root, "verifier_stable.csv");
        Files.deleteIfExists(p);
        boolean first = true;
        for (Verifier v : new Verifier[]{new SpecVerifier(), new CheckerVerifier()}) {
            if (first) {
                Bench.mode = Bench.Mode.SAVE;
                first = !first;
            } else {
                Bench.mode = Bench.Mode.REPLAY;
            }
            System.out.println("--- Verifier: " + v.getClass() + " ---");
            Bench.report = new CSVReport(p, v.id());
            sc.test(Bench.class).forEach(x -> {
                x.verifyWith(v);
                System.out.println(x.go().toString());
            });
        }
    }

    //@Test
    public void specLength() throws Exception {
        SpecScanner sc = new SpecScanner();
        List<Constraint> l = sc.scan();
        System.out.println(l.stream().map(Constraint::pretty).collect(Collectors.joining("\n")));

        Path path = Paths.get(root, "verifier_stable.csv");
        Files.deleteIfExists(path);
        boolean first = true;
        for (Verifier v : new Verifier[]{new SpecVerifier(), new CheckerVerifier()}) {
            if (first) {
                Bench.mode = Bench.Mode.SAVE;
                first = !first;
            } else {
                Bench.mode = Bench.Mode.REPLAY;
            }
            System.out.println("--- Verifier: " + v.getClass() + " ---");
            Bench.report = new CSVReport(path, v.id());
            sc.test(Bench.class).forEach(x -> {
                x.verifyWith(v);
                System.out.println(x.go().toString());
            });
        }
    }

    //@Test
    public void specVsCheckers() throws Exception {
        TestScanner sc = newScanner();
        Bench.population = 500;
        Bench.scale = 10;
        Path path = Paths.get(root, "verifier_stable.csv");
        Files.deleteIfExists(path);
        boolean first = true;
        for (Verifier v : new Verifier[]{new SpecVerifier(), new CheckerVerifier()}) {
            if (first) {
                Bench.mode = Bench.Mode.SAVE;
                first = !first;
            } else {
                Bench.mode = Bench.Mode.REPLAY;
            }
            System.out.println("--- Verifier: " + v.getClass() + " ---");
            Bench.report = new CSVReport(path, v.id());
            sc.test(Bench.class).forEach(x -> {
                x.verifyWith(v);
                System.out.println(x.go().toString());
            });
        }
    }

    //@Test
    public void fuzzingSizing() throws Exception {
        TestScanner sc = newScanner();

        Path path = Paths.get(root, "fuzz.csv");
        Files.deleteIfExists(path);

        for (int p = 100; p <= 1000; p+=100) {
            for (int s = 2; s <= 20; s+=2) {
                System.out.println("--- Population: " + p + " scale: " + s + " ---");
                Bench.report = new CSVReport(path, Integer.toString(p));
                Bench.population = p;
                Bench.scale = s;
                sc.testGroups("sides").forEach(x -> System.out.println(x.go().toString()));
            }
        }
    }

    //@Test
    public void fuzzingScalability() throws Exception {
        TestScanner sc = newScanner();
        Bench.population = 500;
        Bench.scale = 10;
        Path p = Paths.get(root, "verifier_stable.csv");
        Files.deleteIfExists(p);
        boolean first = true;
        for (Verifier v : new Verifier[]{new SpecVerifier(), new CheckerVerifier()}) {
            if (first) {
                Bench.mode = Bench.Mode.SAVE;
                first = !first;
            } else {
                Bench.mode = Bench.Mode.REPLAY;
            }
            System.out.println("--- Verifier: " + v.getClass() + " ---");
            Bench.report = new CSVReport(p, v.id());
            sc.test(Bench.class).forEach(x -> {
                x.verifyWith(v);
                System.out.println(x.go().toString());
            });
        }
    }

    //@Test
    public void specLength() throws Exception {
        SpecScanner sc = newSpecScanner();
        List<Constraint> l = sc.scan();
        System.out.println(l.stream().map(Constraint::pretty).collect(Collectors.joining("\n")));

        Path path = Paths.get(root, "verifier_stable.csv");
        Files.deleteIfExists(path);
        boolean first = true;
        for (Verifier v : new Verifier[]{new SpecVerifier(), new CheckerVerifier()}) {
            if (first) {
                Bench.mode = Bench.Mode.SAVE;
                first = !first;
            } else {
                Bench.mode = Bench.Mode.REPLAY;
            }
            System.out.println("--- Verifier: " + v.getClass() + " ---");
            Bench.report = new CSVReport(path, v.id());
            sc.test(Bench.class).forEach(x -> {
                x.verifyWith(v);
                System.out.println(x.go().toString());
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

        private final PrettyPrinterConfiguration noComments = new PrettyPrinterConfiguration(); // Updated instantiation

        UnitTestsVisitor(List<Integer> numbers) {
            this.l = numbers;
            noComments.setPrintComments(false); // Updated method call
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