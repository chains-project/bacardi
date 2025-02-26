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
import java.util.stream.Collectors;

public class DSN {

    public static String root = "xp-dsn";

    public TestScanner newScanner() throws Exception {
        SpecScanner specScanner = new SpecScanner();
        List<Constraint> l = specScanner.scan();
        Bench.mode = Bench.Mode.REPLAY;
        return new TestScanner(l);
    }

    // ... (rest of the code remains unchanged)

    private static class UnitTestsVisitor extends VoidVisitorAdapter<Void> {

      private final List<Integer> l;

      private final PrettyPrinterConfiguration noComments = new PrettyPrinterConfiguration();

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
}