package se.kth.japicmp_analyzer;

import japicmp.cli.JApiCli;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.*;
import japicmp.output.OutputFilter;
import japicmp.util.Optional;
import se.kth.spoon.ApiMetadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public class JApiCmpAnalyze {

    private final ApiMetadata oldApi;
    private final ApiMetadata newApi;

    public JApiCmpAnalyze(ApiMetadata oldAPi, ApiMetadata newApi) {
        this.oldApi = oldAPi;
        this.newApi = newApi;
    }

    public List<JApiClass> getChanges() {
        JarArchiveComparatorOptions comparatorOptions = JarArchiveComparatorOptions.of(getDefaultOptions());
        JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

        JApiCmpArchive preArchive = new JApiCmpArchive(oldApi.getPath().toFile(), oldApi.getName());
        JApiCmpArchive breakingArchive = new JApiCmpArchive(newApi.getPath().toFile(), newApi.getName());
        return jarArchiveComparator.compare(preArchive, breakingArchive);
    }

    public Set<ApiChange> getAllChanges(List<JApiClass> jApiClasses) {

        OutputFilter filter = new OutputFilter(getDefaultOptions());
        filter.filter(jApiClasses);
        Set<ApiChange> libraryChanges = new HashSet<>();

        //list of classes
        jApiClasses.forEach(jApiClass -> {


            //read incompatible changes
//            jApiClass.getCompatibilityChanges().forEach(jApiCompatibilityChange -> {

            //go for each change
            jApiClasses.iterator().forEachRemaining(jApiClass1 -> {

                jApiClass1.getCompatibilityChanges().forEach(j -> {

                    if (jApiClass1.getChangeStatus().equals(JApiChangeStatus.NEW)) {
                        libraryChanges.add(
                                new ApiChange()
                                        .setAction(ApiChangeType.ADD)
                                        .setModifier(jApiClass1.getNewClass().get().getModifiers())
                                        .setElement(jApiClass1.getNewClass().get().getName())
                                        .setCategory(jApiClass1.getCompatibilityChanges())
                                        .setName(jApiClass1.getNewClass().get().getSimpleName())
                                        .setNewVersion(newApi)
                                        .setOldVersion(oldApi)
                                        .setLongName(jApiClass1.getNewClass().get().getName())

                        );
                    } else {
                        libraryChanges.add(
                                new ApiChange()
                                        .setAction(ApiChangeType.REMOVE)
                                        .setModifier(jApiClass1.getOldClass().get().getModifiers())
                                        .setElement(jApiClass1.getOldClass().get().getName())
                                        .setCategory(jApiClass1.getCompatibilityChanges())
                                        .setName(jApiClass1.getOldClass().get().getSimpleName())
                                        .setNewVersion(newApi)
                                        .setOldVersion(oldApi)
                                        .setLongName(jApiClass1.getOldClass().get().getName())
                        );
                    }
                });

                jApiClass1.getConstructors().forEach(jApiConstructor -> {
                    String longName = jApiConstructor.getjApiClass().getFullyQualifiedName() + "." + jApiConstructor.getName();

                    if (jApiConstructor.getChangeStatus().equals(JApiChangeStatus.NEW)
                            || jApiConstructor.getChangeStatus().equals(JApiChangeStatus.MODIFIED)) {
                        libraryChanges.add(
                                new ApiChange()
                                        .setAction(ApiChangeType.ADD)
                                        .setModifier(jApiConstructor.getNewConstructor().get().getModifiers())
                                        .setReturnType(jApiConstructor.getName())
                                        .setElement(jApiConstructor.getNewConstructor().get().getLongName())
                                        .setCategory(jApiConstructor.getCompatibilityChanges())
                                        .setName(jApiConstructor.getName())
                                        .setNewVersion(newApi)
                                        .setOldVersion(oldApi)
                                        .setLongName(longName)
                        );
                    } else if (jApiConstructor.getChangeStatus().equals(JApiChangeStatus.REMOVED)
                            || jApiConstructor.getChangeStatus().equals(JApiChangeStatus.MODIFIED)) {
                        libraryChanges.add(
                                new ApiChange()
                                        .setAction(ApiChangeType.REMOVE)
                                        .setModifier(jApiConstructor.getOldConstructor().get().getModifiers())
                                        .setReturnType(jApiConstructor.getName())
                                        .setElement(jApiConstructor.getOldConstructor().get().getLongName())
                                        .setCategory(jApiConstructor.getCompatibilityChanges())
                                        .setName(jApiConstructor.getName())
                                        .setNewVersion(newApi)
                                        .setOldVersion(oldApi)
                                        .setLongName(longName)
                        );
                    }
                });

                //get methods
                jApiClass1.getMethods().forEach(jApiMethod -> {

                    String longName = jApiMethod.getjApiClass().getFullyQualifiedName() + "." + jApiMethod.getName();
                    if (jApiMethod.getChangeStatus().equals(JApiChangeStatus.NEW)) {
                        libraryChanges.add(
                                new ApiChange()
                                        .setAction(ApiChangeType.ADD)
                                        .setModifier(jApiMethod.getNewMethod().get().getModifiers())
                                        .setReturnType(this.getReturnType(jApiMethod.getNewMethod().get().getSignature()))
                                        .setElement(jApiMethod.getNewMethod().get().getLongName())
                                        .setCategory(jApiMethod.getCompatibilityChanges())
                                        .setName(jApiMethod.getName())
                                        .setNewVersion(newApi)
                                        .setOldVersion(oldApi)
                                        .setLongName(longName)
                        );
                    } else if (jApiMethod.getChangeStatus().equals(JApiChangeStatus.REMOVED)) {
                        libraryChanges.add(
                                new ApiChange()
                                        .setAction(ApiChangeType.REMOVE)
                                        .setModifier(jApiMethod.getOldMethod().get().getModifiers())
                                        .setReturnType(this.getReturnType(jApiMethod.getOldMethod().get().getSignature()))
                                        .setElement(jApiMethod.getOldMethod().get().getLongName())
                                        .setCategory(jApiMethod.getCompatibilityChanges())
                                        .setName(jApiMethod.getName())
                                        .setNewVersion(newApi)
                                        .setOldVersion(oldApi)
                                        .setLongName(longName)
                        );
                    } else if (jApiMethod.getChangeStatus().equals(JApiChangeStatus.MODIFIED)) {
                        // System.out.println("MODIFIED: " + jApiMethod.getOldMethod().get().getLongName());
                        libraryChanges.add(
                                new ApiChange()
                                        .setAction(ApiChangeType.REMOVE)
                                        .setModifier(jApiMethod.getOldMethod().get().getModifiers())
                                        .setReturnType(this.getReturnType(jApiMethod.getOldMethod().get().getSignature()))
                                        .setElement(jApiMethod.getOldMethod().get().getLongName())
                                        .setCategory(jApiMethod.getCompatibilityChanges())
                                        .setName(jApiMethod.getName())
                                        .setNewVersion(newApi)
                                        .setOldVersion(oldApi)
                                        .setLongName(longName)
                        );
                        libraryChanges.add(
                                new ApiChange()
                                        .setAction(ApiChangeType.ADD)
                                        .setModifier(jApiMethod.getNewMethod().get().getModifiers())
                                        .setReturnType(this.getReturnType(jApiMethod.getNewMethod().get().getSignature()))
                                        .setElement(jApiMethod.getNewMethod().get().getLongName())
                                        .setCategory(jApiMethod.getCompatibilityChanges())
                                        .setName(jApiMethod.getName())
                                        .setNewVersion(newApi)
                                        .setOldVersion(oldApi)
                                        .setLongName(longName)
                        );
                    }
                });

            });
//            });
        });

        return libraryChanges;
    }

    private String getReturnType(String fromSignature) {
        int semicolonIndex = fromSignature.indexOf(')');
        String type = fromSignature.substring(semicolonIndex + 1);

        if (type.equals("Z")) {
            return "bool";
        } else if (type.equals("V")) {
            return "void";
        } else if (type.equals("I")) {
            return "int";
        } else if (type.indexOf("L") < 0) {
            return type;
        }


        type = type
                .replace("/", ".")
                .replace(";", "");

        // for generics
        type = type.replace("[", "");

        // Remove the initial L that indicates that return type is a class
        type = type.substring(1);

        return type;
    }

    public static String buildSpoonSignature(JApiMethod m) {
        String returnType = m.getReturnType().getOldReturnType();
        if (returnType.equals("n.a."))
            returnType = "void";
        String type = m.getjApiClass().getFullyQualifiedName();
        String name = m.getName();
        String params = m.getParameters().stream().map(JApiParameter::getType).collect(joining(","));
        return "%s %s#%s(%s)".formatted(returnType, type, name, params);
    }

    private static Options getDefaultOptions() {
        Options defaultOptions = Options.newDefault();
        defaultOptions.setAccessModifier(AccessModifier.PROTECTED);
        defaultOptions.setOutputOnlyModifications(true);
        defaultOptions.setXmlOutputFile(Optional.of("output.xml"));
        defaultOptions.setClassPathMode(JApiCli.ClassPathMode.TWO_SEPARATE_CLASSPATHS);
        defaultOptions.setIgnoreMissingClasses(true);
        defaultOptions.setReportOnlyFilename(true);
        String[] excl = {"(*.)?tests(.*)?", "(*.)?test(.*)?",
                "@org.junit.After", "@org.junit.AfterClass",
                "@org.junit.Before", "@org.junit.BeforeClass",
                "@org.junit.Ignore", "@org.junit.Test",
                "@org.junit.runner.RunWith"};

        for (String e : excl) {
            defaultOptions.addExcludeFromArgument(Optional.of(e), false);
        }
        return defaultOptions;
    }
}
