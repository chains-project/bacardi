package org.simplify4u.plugins.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.repository.RepositorySystem;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

/**
 * Utilities specific for org.apache.maven.plugins:maven-compiler-plugin.
 */
public final class MavenCompilerUtils {

    private static final String GROUPID = "org.apache.maven.plugins";
    private static final String ARTIFACTID = "maven-compiler-plugin";

    private static final String PACKAGING = "jar";

    private MavenCompilerUtils() {
        // No need to instantiate utility class.
    }

    /**
     * Check if provided plugin is org.apache.maven.plugins:maven-compiler-plugin.
     *
     * @param plugin any plugin instance
     * @return Returns true iff plugin is maven-compiler-plugin.
     */
    public static boolean checkCompilerPlugin(Plugin plugin) {
        return GROUPID.equals(plugin.getGroupId()) && ARTIFACTID.equals(plugin.getArtifactId());
    }

    /**
     * Extract annotation processors for maven-compiler-plugin configuration.
     *
     * @param system maven repository system
     * @param plugin maven-compiler-plugin plugin
     * @return Returns set of maven artifacts configured as annotation processors.
     */
    public static Set<Artifact> extractAnnotationProcessors(RepositorySystem system, Plugin plugin) {
        requireNonNull(system);
        if (!checkCompilerPlugin(plugin)) {
            throw new IllegalArgumentException("Plugin is not '" + GROUPID + ":" + ARTIFACTID + "'.");
        }
        final Object config = plugin.getConfiguration();
        if (config == null) {
            return emptySet();
        }
        if ("Xpp3Dom".equals(config.getClass().getSimpleName())) {
            try {
                Method getChildrenMethod = config.getClass().getMethod("getChildren", String.class);
                Object annotationProcessorPathsObj = getChildrenMethod.invoke(config, "annotationProcessorPaths");
                Object[] annotationProcessorPaths = (annotationProcessorPathsObj instanceof Object[])
                        ? (Object[]) annotationProcessorPathsObj : new Object[0];
                return Arrays.stream(annotationProcessorPaths)
                        .flatMap(aggregate -> {
                            try {
                                Method getChildren = aggregate.getClass().getMethod("getChildren", String.class);
                                Object childrenObj = getChildren.invoke(aggregate, "path");
                                Object[] children = (childrenObj instanceof Object[]) ? (Object[]) childrenObj : new Object[0];
                                return Arrays.stream(children);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .map(processor -> {
                            String groupId = extractChildValueReflectively(processor, "groupId");
                            String artifactId = extractChildValueReflectively(processor, "artifactId");
                            String version = extractChildValueReflectively(processor, "version");
                            return system.createArtifact(groupId, artifactId, version, PACKAGING);
                        })
                        .filter(a -> !a.getGroupId().isEmpty())
                        .filter(a -> !a.getArtifactId().isEmpty())
                        .filter(a -> !a.getVersion().isEmpty())
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                throw new UnsupportedOperationException("Please report that an unsupported type of configuration container" +
                        " was encountered: " + config.getClass(), e);
            }
        }
        throw new UnsupportedOperationException("Please report that an unsupported type of configuration container" +
                " was encountered: " + config.getClass());
    }

    /**
     * Extract child value if child is present, or return empty string if absent.
     *
     * @param node the parent node
     * @param name the child node name
     * @return Returns child value if child node present or otherwise empty string.
     */
    private static String extractChildValueReflectively(Object node, String name) {
        try {
            Method getChildMethod = node.getClass().getMethod("getChild", String.class);
            Object child = getChildMethod.invoke(node, name);
            if (child == null) {
                return "";
            }
            Method getValueMethod = child.getClass().getMethod("getValue");
            Object value = getValueMethod.invoke(child);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}