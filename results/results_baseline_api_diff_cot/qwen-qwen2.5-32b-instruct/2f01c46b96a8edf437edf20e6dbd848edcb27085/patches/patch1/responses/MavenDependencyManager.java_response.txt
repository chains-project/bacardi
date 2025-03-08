package se.kth.depclean.wrapper;

import static com.google.common.collect.ImmutableSet.of;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import se.kth.depclean.core.AbstractDebloater;
import se.kth.depclean.core.analysis.graph.DependencyGraph;
import se.kth.depclean.core.analysis.model.ProjectDependencyAnalysis;
import se.kth.depclean.core.analysis.src.ImportsAnalyzer;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;
import se.kth.depclean.core.wrapper.LogWrapper;
import se.kth.depclean.graph.MavenDependencyGraph;
import se.kth.depclean.util.MavenDebloater;
import se.kth.depclean.util.MavenInvoker;
import se.kth.depclean.util.json.ParsedDependencies;

/**
 * Maven's implementation of the dependency manager wrapper.
 */
@AllArgsConstructor
public class MavenDependencyManager implements DependencyManagerWrapper {

  private static final String DIRECTORY_TO_COPY_DEPENDENCIES = "dependency";

  private final Log logger;
  private final MavenProject project;
  private final MavenSession session;
  private final DependencyGraphBuilder dependencyGraphBuilder;
  private final Model model;

  // ... (rest of the class remains unchanged)

  @Override
  public Set<String> collectUsedClassesFromProcessors() {
    getLog().debug("# collectUsedClassesFromProcessors()");
    return Optional.ofNullable(project.getBuild())
        .map(build -> build.getPlugins())
        .flatMap(plugins -> plugins.stream()
            .filter(plugin -> "org.bsc.maven".equals(plugin.getGroupId()) && "maven-processor-plugin".equals(plugin.getArtifactId()))
            .findFirst())
        .flatMap(plugin -> plugin.getExecutions().stream()
            .filter(execution -> "process".equals(execution.getId()))
            .findFirst())
        .flatMap(execution -> Optional.ofNullable(execution.getConfiguration()))
        .map(config -> (PluginExecution) config)
        .map(PluginExecution::getGoals)
        .map(goals -> goals.stream()
            .map(goal -> goal.getConfiguration())
            .flatMap(config -> config.getChildren().stream())
            .filter(child -> "processors".equals(child.getName()))
            .flatMap(processors -> processors.getChildren().stream())
            .map(Xpp3Dom::getValue)
            .collect(Collectors.toSet()))
        .orElse(of());
  }

  // ... (rest of the class remains unchanged)
}