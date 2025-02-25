package se.kth;

import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.JApiClass;
import se.kth.japicmp_analyzer.ApiChange;
import se.kth.spoon.DirectFailuresScan;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;


public class SpoonMain {

    public static void main(String[] args) throws IOException {

        SpoonMain a = new SpoonMain();

        Options defaultOptions = Options.newDefault();
        defaultOptions.setOutputOnlyBinaryIncompatibleModifications(true);
        defaultOptions.setIgnoreMissingClasses(true);

        JarArchiveComparatorOptions comparatorOptions = JarArchiveComparatorOptions.of(defaultOptions);
        JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

        File jarFilePre = new File("/Users/frank/Documents/Work/PHD/bacardi/projects/0abf7148300f40a1da0538ab060552bca4a2f1d8/jasperreports-6.18.1.jar");
        File jarFileBreaking = new File("/Users/frank/Documents/Work/PHD/bacardi/projects/0abf7148300f40a1da0538ab060552bca4a2f1d8/jasperreports-6.19.1.jar");

        JApiCmpArchive preArchive = new JApiCmpArchive(jarFilePre, "v1");
        JApiCmpArchive breakingArchive = new JApiCmpArchive(jarFileBreaking, "v2");
        List<JApiClass> classs = jarArchiveComparator.compare(preArchive, breakingArchive);

        JarFile jarFile = new JarFile(jarFileBreaking);
        int s = jarFile.size();

        System.out.println("Jar file size: " + s);

        List<String> classNames = new ArrayList<>();
        Enumeration<JarEntry> e = jarFile.entries();
        while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) {
                continue;
            }
            // -6 because of .class
            String className = je.getName().substring(0, je.getName().length() - 6);
            className = className.replace('/', '.');
            classNames.add(className);
        }

//        classNames.forEach(System.out::println);

        String jarPath = "/Users/frank/Documents/Work/PHD/bacardi/projects/0abf7148300f40a1da0538ab060552bca4a2f1d8/biapi";

        MavenLauncher launcher = new MavenLauncher(jarPath, MavenLauncher.SOURCE_TYPE.APP_SOURCE);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setIgnoreSyntaxErrors(true);
        CtModel model = launcher.buildModel();

        Optional<CtElement> elementlist = localizeElementFromStackTraceElement(model);

        Set<CtElement> ee = getAllChildren(Collections.singletonList(elementlist.get()));

        Set<CtElement> executedElements = new HashSet<>();
        Set<ApiChange> api = new HashSet<>();
        DirectFailuresScan scan = new DirectFailuresScan(api);

        for (CtElement element : ee) {
            element.accept(scan);
        }

        System.out.println("ELELELELELE");

//
//        List<CtElement> involvedElements = a.checkChangedInvolved(ee.stream().toList(), classNames);
//        List<CtElement> result = a.removeParents(involvedElements);
//
//        result.forEach(f -> {
//            System.out.println("dsdfsdfdsf");
//            System.out.println(f.getReferencedTypes().stream().toList());
//        });

    }

    private static boolean referencesChangedType(CtElement element, List<String> changedClassnames) {
        List<String> referencedElementsTypes = element.getReferencedTypes().stream()
                .map(CtTypeReference::getQualifiedName)
                .toList();
        return containsAny(changedClassnames, referencedElementsTypes);
    }

    private static boolean containsAny(List<String> first, List<String> second) {
        for (String s : second) {
            if (first.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private List<CtElement> checkChangedInvolved(List<CtElement> elements, List<String> changedClassnames) {
        return removeParents(elements).stream()
                .filter(ctElement -> referencesChangedType(ctElement, changedClassnames))
                .toList();
    }

    private List<CtElement> removeParents(List<CtElement> elements) {
        List<CtElement> parents = elements.stream()
                .filter(ctElement -> ctElement.getParent() != null)
                .map(this::getAllParents)
                .flatMap(List::stream)
                .toList();
        return elements.stream()
                .filter(ctElement -> !parents.contains(ctElement))
                .toList();
    }

    private List<CtElement> getAllParents(CtElement ctElement) {
        if (ctElement.getParent() == null) {
            return List.of();
        } else {
            return Stream.concat(getAllParents(ctElement.getParent()).stream(), Stream.of(ctElement.getParent())).toList();
        }
    }

    private static Optional<CtElement> extractParent(List<CtElement> elements) {
        return elements.stream()
                .filter(ctElement -> !elements.contains(ctElement.getParent()))
                .findFirst();
    }

    public static Optional<CtElement> localizeElementFromStackTraceElement(CtModel model) {
        int lineNumber = 369;
        String fileName = "ReportBuilder.java";
        List<CtElement> result = new LinkedList<>();

        model.getElements(new TypeFilter<>(CtElement.class)).stream()
                .forEach(ctElement -> {
                    if (!ctElement.isImplicit() && ctElement.getPosition().isValidPosition()) {
                        if (ctElement.getPosition().getFile().getName().equals(fileName) &&
                                ctElement.getPosition().getLine() == lineNumber) {
                            result.add(ctElement);
                        }
                    }
                });
        Optional<CtElement> parent = extractParent(result);
        return parent;
    }

    public static Set<CtElement> getAllChildren(List<CtElement> elements) {
        Set<CtElement> executedElements = new HashSet<>();
        CustomScanner scanner = new CustomScanner(executedElements);
        for (CtElement element : elements) {
            element.accept(scanner);
        }
        return scanner.getExecutedElements();
    }

}
