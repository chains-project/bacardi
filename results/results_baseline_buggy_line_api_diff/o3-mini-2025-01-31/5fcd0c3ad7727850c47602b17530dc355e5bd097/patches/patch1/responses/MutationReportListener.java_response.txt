package org.pitest.elements;

import org.pitest.classinfo.ClassInfoVisitor;
import org.pitest.classinfo.ClassName;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.mutationtest.ClassMutationResults;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.SourceLocator;
import org.pitest.elements.models.MutationTestSummaryData;
import org.pitest.elements.models.PackageSummaryMap;
import org.pitest.elements.utils.JsonParser;
import org.pitest.util.FileUtil;
import org.pitest.util.ResultOutputStrategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class MutationReportListener implements MutationResultListener {

  private final ResultOutputStrategy outputStrategy;

  private final JsonParser jsonParser;

  private final CoverageDatabase coverage;
  private final PackageSummaryMap packageSummaryData = new PackageSummaryMap();

  private static final String HTML_PAGE = "<!DOCTYPE html>\n" + "<html lang=\"en\">\n"
    + "<head>\n"
    + "  <meta charset=\"UTF-8\">\n"
    + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
    + "  <script src=\"mutation-test-elements.js\"></script>\n"
    + "</head>\n"
    + "<body>\n"
    + "  <mutation-test-report-app title-postfix=\"Pit Test Coverage Report\">\n"
    + "    Your browser doesn't support <a href=\"https://caniuse.com/#search=custom%20elements\">custom elements</a>.\n"
    + "    Please use a latest version of an evergreen browser (Firefox, Chrome, Safari, Opera, etc).\n"
    + "  </mutation-test-report-app>\n"
    + "  <script>\n"
    + "    const app = document.getElementsByTagName('mutation-test-report-app').item(0);\n"
    + "    function updateTheme() {\n"
    + "    document.body.style.backgroundColor = app.themeBackgroundColor;\n"
    + "    }\n"
    + "    app.addEventListener('theme-changed', updateTheme);\n"
    + "    updateTheme();\n"
    + "  </script>\n"
    + "  <script src=\"report.js\"></script>\n"
    + "</body>\n"
    + "</html>";

  public MutationReportListener(final CoverageDatabase coverage,
      final ResultOutputStrategy outputStrategy, final SourceLocator... locators) {
    this.coverage = coverage;
    this.outputStrategy = outputStrategy;
    this.jsonParser = new JsonParser(
        new HashSet<>(Arrays.asList(locators)));
  }

  private String loadMutationTestElementsJs() throws IOException {
    final String htmlReportResource = "elements/mutation-test-elements.js";
    return FileUtil.readToString(this.getClass().getClassLoader().getResourceAsStream(htmlReportResource));
  }

  private void createHtml() {
    final String content = HTML_PAGE;
    final Writer writer = this.outputStrategy
        .createWriterForFile("html2" + File.separatorChar + "index.html");
    try {
      writer.write(content);
      writer.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private void createJs(final String json) {
    final String content =
        "document.querySelector('mutation-test-report-app').report = " + json;
    final Writer writer = this.outputStrategy
        .createWriterForFile("html2" + File.separatorChar + "report.js");
    try {
      writer.write(content);
      writer.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  private void createMutationTestingElementsJs() {
    final Writer writer = this.outputStrategy
      .createWriterForFile("html2" + File.separatorChar + "mutation-test-elements.js");
    try {
      final String content = this.loadMutationTestElementsJs();
      writer.write(content);
      writer.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private MutationTestSummaryData createSummaryData(
      final CoverageDatabase coverage, final ClassMutationResults data) {
    try {
      byte[] classBytes = loadClassBytes(data.getMutatedClass());
      ClassInfoVisitor visitor = new ClassInfoVisitor();
      Object classInfo = visitor.getClassInfo(data.getMutatedClass(), classBytes, System.currentTimeMillis());
      return new MutationTestSummaryData(data.getFileName(),
          data.getMutations(), classInfo);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load class bytes for " + data.getMutatedClass(), e);
    }
  }

  private void updatePackageSummary(
      final ClassMutationResults mutationMetaData) {
    final String packageName = mutationMetaData.getPackageName();

    this.packageSummaryData.update(packageName,
        createSummaryData(this.coverage, mutationMetaData));
  }

  @Override
  public void runStart() {
    // Nothing to do
  }

  @Override
  public void handleMutationResult(ClassMutationResults metaData) {
    updatePackageSummary(metaData);
  }

  @Override
  public void runEnd() {
    try {
      String json = jsonParser.toJson(this.packageSummaryData);
      createHtml();
      createMutationTestingElementsJs();
      createJs(json);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private byte[] loadClassBytes(ClassName clazzName) throws IOException {
    String resourcePath = clazzName.asJavaName().replace('.', '/') + ".class";
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
    if (is == null) {
      throw new IOException("Resource not found: " + resourcePath);
    }
    try {
      return readAllBytes(is);
    } finally {
      is.close();
    }
  }
  
  private byte[] readAllBytes(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[8192];
    int nRead;
    while ((nRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    return buffer.toByteArray();
  }
}