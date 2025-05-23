package com.mycila.maven.plugin.license.dependencies;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeListGenerator;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeListGeneratorException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MavenProjectLicenses implements LicenseMap, LicenseMessage {

  private Set<MavenProject> projects;
  private DependencyGraphBuilder graph;
  private ProjectBuilder projectBuilder;
  private ProjectBuildingRequest buildingRequest;
  private ArtifactFilter filter;
  private Log log;

  public MavenProjectLicenses(final Set<MavenProject> projects, final DependencyGraphBuilder graph,
                              final ProjectBuilder projectBuilder, final ProjectBuildingRequest buildingRequest,
                              final ArtifactFilter filter, final Log log) {
    this.setProjects(projects);
    this.setBuildingRequest(buildingRequest);
    this.setGraph(graph);
    this.setFilter(filter);
    this.setProjectBuilder(projectBuilder);
    this.setLog(log);

    log.info(String.format("%s %s", INFO_LICENSE_IMPL, this.getClass()));
  }

  public MavenProjectLicenses(final MavenSession session, MavenProject project, final DependencyGraphBuilder graph,
                              final ProjectBuilder projectBuilder, final List<String> scopes, final Log log) {
    this(Collections.singleton(project), graph, projectBuilder, getBuildingRequestWithDefaults(session),
        new CumulativeScopeArtifactFilter(scopes), log);
  }

  private static ProjectBuildingRequest getBuildingRequestWithDefaults(final MavenSession session) {
    ProjectBuildingRequest request;
    if (session == null) {
      request = new DefaultProjectBuildingRequest();
    } else {
      request = session.getProjectBuildingRequest();
    }
    return request;
  }

  protected Set<License> getLicensesFromArtifact(final Artifact artifact) {
    Set<License> licenses = new HashSet<>();
    try {
      MavenProject project = getProjectBuilder().build(artifact, getBuildingRequest()).getProject();
      licenses.addAll(project.getLicenses());
    } catch (ProjectBuildingException ex) {
      getLog().warn(String.format("Could not get project from dependency's artifact: %s", artifact.getFile()));
    }

    return licenses;
  }

  protected Map<License, Set<Artifact>> getLicenseMapFromArtifacts(final Set<Artifact> dependencies) {
    final ConcurrentMap<License, Set<Artifact>> map = new ConcurrentHashMap<>();

    dependencies.parallelStream().forEach(artifact -> getLicensesFromArtifact(artifact).forEach(license -> {
      map.putIfAbsent(license, new HashSet<>());
      Set<Artifact> artifacts = map.get(license);
      artifacts.add(artifact);
      map.put(license, artifacts);
    }));

    return map;
  }

  @Override
  public Map<License, Set<Artifact>> getLicenseMap() {
    return getLicenseMapFromArtifacts(getDependencies());
  }

  private Set<Artifact> getDependencies() {
    final Set<Artifact> artifacts = new HashSet<>();
    final Set<DependencyNode> dependencies = new HashSet<>();

    getLog().debug(String.format("Building dependency graphs for %d projects", getProjects().size()));
    getProjects().parallelStream().forEach(project -> {
      try {
        DependencyNodeListGenerator generator = new DependencyNodeListGenerator();
        dependencies.addAll(generator.generateDependencyNodeList(getGraph().buildDependencyGraph(buildingRequest, getFilter())));
      } catch (DependencyGraphBuilderException | DependencyNodeListGeneratorException ex) {
        getLog().warn(String.format("Could not get children from project %s, it's dependencies will not be checked!",
            project.getId()));
      }
    });

    dependencies.parallelStream().forEach(d -> artifacts.add(d.getArtifact()));
    getLog().info(String.format("%s: %d", INFO_DEPS_DISCOVERED, dependencies.size()));

    return artifacts;
  }

  protected Set<MavenProject> getProjects() {
    return projects;
  }

  private void setProjects(final Set<MavenProject> projects) {
    this.projects = Optional.ofNullable(projects).orElse(new HashSet<>());
  }

  private DependencyGraphBuilder getGraph() {
    return graph;
  }

  private void setGraph(DependencyGraphBuilder graph) {
    this.graph = Optional.ofNullable(graph).orElse(new DependencyGraphBuilder() {
      @Override
      public DependencyNode buildDependencyGraph(ProjectBuildingRequest request, ArtifactFilter filter) throws DependencyGraphBuilderException {
        // Dummy implementation to avoid compilation error
        return null;
      }
    });
  }

  private ProjectBuilder getProjectBuilder() {
    return projectBuilder;
  }

  private void setProjectBuilder(ProjectBuilder projectBuilder) {
    this.projectBuilder = Optional.ofNullable(projectBuilder).orElse(new DefaultProjectBuilder());
  }

  private ArtifactFilter getFilter() {
    return filter;
  }

  private void setFilter(ArtifactFilter filter) {
    this.filter = filter;
  }

  private Log getLog() {
    return log;
  }

  private void setLog(Log log) {
    this.log = log;
  }

  private ProjectBuildingRequest getBuildingRequest() {
    return buildingRequest;
  }

  protected void setBuildingRequest(final ProjectBuildingRequest buildingRequest) {
    this.buildingRequest = Optional.ofNullable(buildingRequest).orElse(new DefaultProjectBuildingRequest());
  }
}