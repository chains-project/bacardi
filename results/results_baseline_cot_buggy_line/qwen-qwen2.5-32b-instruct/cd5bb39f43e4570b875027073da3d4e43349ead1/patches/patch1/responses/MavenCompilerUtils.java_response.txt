package org.simplify4u.plugins.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.XmlStreamWriter;

import java.io.StringReader;
import java.io.StringWriter;
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
        if (config instanceof String) {
            try {
                MavenXpp3Reader reader = new MavenXpp3Reader();
                Plugin parsedPlugin = reader.read(new XmlStreamReader(new StringReader((String) config)));
                return stream(parsedPlugin.getConfiguration().getChildren("annotationProcessorPaths"))
                        .flatMap(aggregate -> stream(aggregate.getChildren("path")))
                        .map(processor -> system.createArtifact(
                                extractChildValue(processor, "groupId"),
                                extractChildValue(processor, "artifactId"),
                                extractChildValue(processor, "version"),
                                PACKAGING))
                        .filter(a -> !a.getGroupId().isEmpty())
                        .filter(a -> !a.getArtifactId().isEmpty())
                        .filter(a -> !a.getVersion().isEmpty())
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse plugin configuration", e);
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
    private static String extractChildValue(org.apache.maven.model.Configuration node, String name) {
        final org.apache.maven.model.Configuration child = node.getChild(name);
        return child == null ? "" : child.getValue();
    }
}