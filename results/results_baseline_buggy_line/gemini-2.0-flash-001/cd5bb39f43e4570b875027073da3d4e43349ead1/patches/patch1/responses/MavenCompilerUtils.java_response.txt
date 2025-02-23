/*
 * Copyright 2019 Danny van Heumen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.simplify4u.plugins.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.repository.RepositorySystem;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.HashSet;
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
        if (config instanceof org.codehaus.plexus.util.xml.Xpp3Dom) {
            org.codehaus.plexus.util.xml.Xpp3Dom xpp3DomConfig = (org.codehaus.plexus.util.xml.Xpp3Dom) config;
            org.codehaus.plexus.util.xml.Xpp3Dom[] annotationProcessorPaths = xpp3DomConfig.getChildren("annotationProcessorPaths");
            Set<Artifact> artifacts = new HashSet<>();
            for (org.codehaus.plexus.util.xml.Xpp3Dom aggregate : annotationProcessorPaths) {
                org.codehaus.plexus.util.xml.Xpp3Dom[] paths = aggregate.getChildren("path");
                for (org.codehaus.plexus.util.xml.Xpp3Dom processor : paths) {
                    String groupId = extractChildValue(processor, "groupId");
                    String artifactId = extractChildValue(processor, "artifactId");
                    String version = extractChildValue(processor, "version");

                    if (!groupId.isEmpty() && !artifactId.isEmpty() && !version.isEmpty()) {
                        artifacts.add(system.createArtifact(groupId, artifactId, version, PACKAGING));
                    }
                }
            }
            return artifacts;
        }
        // It is expected that this will never occur due to all Configuration instances of all plugins being provided as
        // XML document. If this happens to occur on very old plugin versions, we can safely add the type support and
        // simply return an empty set.
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
    private static String extractChildValue(org.codehaus.plexus.util.xml.Xpp3Dom node, String name) {
        final org.codehaus.plexus.util.xml.Xpp3Dom child = node.getChild(name);
        return child == null ? "" : child.getValue();
    }
}