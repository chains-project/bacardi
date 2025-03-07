package org.apache.myfaces.tobago.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;

public abstract class AbstractThemeMojo extends AbstractMojo {

  private static final String[] INCLUDES = new String[]{
    "**"
  };
  private static final String[] EXCLUDES = new String[]{
    "META-INF/**/*",
    "**/*.properties",
    "**/*.xml",
    "**/*.class"
  };

  protected String[] getThemeFiles(final File sourceDir) {
    final DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(sourceDir);
    scanner.setIncludes(INCLUDES);
    scanner.setExcludes(EXCLUDES);
    scanner.scan();
    return scanner.getIncludedFiles();
  }

  public String[] getIncludes() {
    return INCLUDES;
  }

  public String[] getExcludes() {
    return EXCLUDES;
  }
}