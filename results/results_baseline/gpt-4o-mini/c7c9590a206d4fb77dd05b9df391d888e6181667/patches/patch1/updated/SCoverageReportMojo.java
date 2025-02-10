package org.scoverage.plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;

import org.codehaus.plexus.util.StringUtils;

import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

import scoverage.domain.Constants;
import scoverage.domain.Coverage;
import scoverage.domain.Statement;
import scoverage.reporter.IOUtils;
import scoverage.serialize.Serializer;
import scoverage.reporter.CoberturaXmlWriter;
import scoverage.reporter.CoverageAggregator;
import scoverage.reporter.ScoverageHtmlWriter;
import scoverage.reporter.ScoverageXmlWriter;

/**
 * Generates code coverage by unit tests report in forked {@code scoverage} life cycle.
 * <br>
 * <br>
 * In forked {@code scoverage} life cycle project is compiled with SCoverage instrumentation
 * and unit tests are executed before report generation.
 * <br>
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @since 1.0.0
 */
@Mojo( name = "report", threadSafe = false )
@Execute( lifecycle = "scoverage", phase = LifecyclePhase.TEST )
public class SCoverageReportMojo
    extends AbstractMojo
    implements MavenReport
{

    // ... (rest of the code remains unchanged)

    /**
     * Generates SCoverage report.
     * 
     * @throws MojoExecutionException if unexpected problem occurs
     */
    @Override
    public void execute()
        throws MojoExecutionException
    {
        if ( !canGenerateReport() )
        {
            getLog().info( "Skipping SCoverage report generation" );
            return;
        }

        try
        {
            // Updated to use the correct constructor for RenderingContext
            scoverage.reporter.RenderingContext context = new scoverage.reporter.RenderingContext( outputDirectory, getOutputName() + ".html" );
            SiteRendererSink sink = new SiteRendererSink( context );
            Locale locale = Locale.getDefault();
            generate( sink, locale );
        }
        catch ( MavenReportException e )
        {
            String prefix = "An error has occurred in " + getName( Locale.ENGLISH ) + " report generation";
            throw new MojoExecutionException( prefix + ": " + e.getMessage(), e );
        }
    }

    // ... (rest of the code remains unchanged)

}