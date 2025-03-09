package se.kth.depclean.wrapper;

import static com.google.common.collect.ImmutableSet.of;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

  /**
   * Creates the manager.
   *
   * @param logger                 the logger
   * @param project                the maven project
   * @param session                the maven session
   * @param dependencyGraphBuilder a tool to build the dependency graph
   */
  public MavenDependencyManager(Log logger, MavenProject project, MavenSession session,
      DependencyGraphBuilder dependencyGraphBuilder) {
    this.logger = logger;
    this.project = project;
    this.session = session;
    this.dependencyGraphBuilder = dependencyGraphBuilder;
    this.model = buildModel(project);
  }

  @Override
  public LogWrapper getLog() {
    return new LogWrapper() {
      @Override
      public void info(String message) {
        logger.info(message);
      }

      @Override
      public void error(String message) {
        logger.error(message);
      }

      @Override
      public void debug(String message) {
        logger.debug(message);
      }
    };
  }

  @Override
  public boolean isMaven() {
    return true;
  }

  @Override
  public boolean isPackagingPom() {
    return project.getPackaging().equals("pom");
  }

  @Override
  @SneakyThrows
  public DependencyGraph dependencyGraph() {
    ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
    buildingRequest.setProject(project);
    DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null);
    return new MavenDependencyGraph(project, model, rootNode);
  }

  @Override
  public Set<Path> getOutputDirectories() {
    return Set.of(Paths.get(project.getBuild().getOutputDirectory()));
  }

  @Override
  public Set<Path> getTestOutputDirectories() {
    return Set.of(Paths.get(project.getBuild().getTestOutputDirectory()));
  }

  private Model buildModel(MavenProject project) {
    File pomFile = new File(project.getBasedir().getAbsolutePath() + File.separator + "pom.xml");

    /* Build Maven model to manipulate the pom */
    final Model model;
    FileReader reader;
    MavenXpp3Reader mavenReader = new MavenXpp3Reader();
    try {
      reader = new FileReader(pomFile);
      model = mavenReader.read(reader);
      model.setPomFile(pomFile);
    } catch (Exception ex) {
      getLog().error("Unable to build the maven project.");
      throw new RuntimeException(ex);
    }
    return model;
  }

  /**
   * Maven processors are defined like this.
   * <pre>{@code
   *       <plugin>
   *         <groupId>org.bsc.maven</groupId>
   *         <artifactId>maven-processor-plugin</artifactId>
   *         <executions>
   *           <execution>
   *             <id>process</id>
   *             [...]
   *             <configuration>
   *               <processors>
   *                 <processor>XXXProcessor</processor>
   *               </processors>
   *             </configuration>
   *           </execution>
   *         </executions>
   *       </plugin>
   * }</pre>
   */
  @Override
  public Set<String> collectUsedClassesFromProcessors() {
    getLog().debug("# collectUsedClassesFromProcessors()");
    return Optional.ofNullable(project.getPlugin("org.bsc.maven:maven-processor-plugin"))
        .map(plugin -> plugin.getExecutionsAsMap().get("process"))
        .map(PluginExecution::getConfiguration)
        .map(config -> {
          if (config instanceof org.w3c.dom.Element) {
            return (org.w3c.dom.Element) config;
          } else {
            try {
              DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
              DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
              org.w3c.dom.Document doc = dBuilder.newDocument();
              Element rootElement = doc.createElement("configuration");
              doc.appendChild(rootElement);
              return rootElement;
            } catch (Exception e) {
              logger.error("Error creating document builder: " + e.getMessage());
              return null;
            }
          }
        })
        .map(config -> config.getElementsByTagName("processors").item(0))
        .map(processorsNode -> {
          if (processorsNode != null && processorsNode.hasChildNodes()) {
            NodeList processorNodes = processorsNode.getChildNodes();
            Set<String> processors = new HashSet<>();
            for (int i = 0; i < processorNodes.getLength(); i++) {
              Node processorNode = processorNodes.item(i);
              if (processorNode.getNodeType() == Node.ELEMENT_NODE && "processor".equals(processorNode.getNodeName())) {
                processors.add(processorNode.getTextContent());
              }
            }
            return processors;
          } else {
            return of();
          }
        })
        .orElse(of());
  }

  @Override
  public Path getDependenciesDirectory() {
    String dependencyDirectoryName = project.getBuild().getDirectory() + "/" + DIRECTORY_TO_COPY_DEPENDENCIES;
    return new File(dependencyDirectoryName).toPath();
  }

  @Override
  public Set<String> collectUsedClassesFromSource(Path sourceDirectory, Path testSourceDirectory) {
    Set<String> allImports = new HashSet<>();
    ImportsAnalyzer importsInSourceFolder = new ImportsAnalyzer(sourceDirectory);
    ImportsAnalyzer importsInTestsFolder = new ImportsAnalyzer(testSourceDirectory);
    Set<String> importsInSourceFolderSet = importsInSourceFolder.collectImportedClassesFromSource();
    Set<String> importsInTestsFolderSet = importsInTestsFolder.collectImportedClassesFromSource();
    allImports.addAll(importsInSourceFolderSet);
    allImports.addAll(importsInTestsFolderSet);
    return allImports;
  }

  @Override
  public AbstractDebloater<? extends Serializable> getDebloater(ProjectDependencyAnalysis analysis) {
    return new MavenDebloater(
        analysis,
        project,
        model
    );
  }

  @Override
  public Path getBuildDirectory() {
    return Paths.get(project.getBuild().getDirectory());
  }

  @Override
  public Path getSourceDirectory() {
    return new File(project.getBuild().getSourceDirectory()).toPath();
  }

  @Override
  public Path getTestDirectory() {
    return new File(project.getBuild().getTestSourceDirectory()).toPath();
  }

  @Override
  public void generateDependencyTree(File treeFile) throws IOException, InterruptedException {
    MavenInvoker.runCommand("mvn dependency:tree -DoutputFile=" + treeFile + " -Dverbose=true", null);
  }

  @SneakyThrows
  @Override
  public String getTreeAsJson(
      File treeFile, ProjectDependencyAnalysis analysis, File classUsageFile, boolean createCallGraphCsv) {
    return new ParsedDependencies(
        treeFile,
        analysis,
        classUsageFile,
        createCallGraphCsv
    ).parseTreeToJson();
  }
}