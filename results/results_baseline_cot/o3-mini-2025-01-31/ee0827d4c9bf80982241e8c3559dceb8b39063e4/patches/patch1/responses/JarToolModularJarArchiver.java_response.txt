package org.codehaus.plexus.archiver.jar;

import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ConcurrentJarCreator;
import org.codehaus.plexus.util.IOUtil;

@Named( "mjar" )
public class JarToolModularJarArchiver
    extends ModularJarArchiver
{
    private static final String MODULE_DESCRIPTOR_FILE_NAME
        = "module-info.class";

    private static final Pattern MRJAR_VERSION_AREA
        = Pattern.compile( "META-INF/versions/\\d+/" );

    private Object jarTool;

    private boolean moduleDescriptorFound;

    private boolean hasJarDateOption;

    public JarToolModularJarArchiver()
    {
        try
        {
            Class<?> toolProviderClass =
                Class.forName( "java.util.spi.ToolProvider" );
            Object jarToolOptional = toolProviderClass
                .getMethod( "findFirst", String.class )
                .invoke( null, "jar" );

            jarTool = jarToolOptional.getClass().getMethod( "get" )
                .invoke( jarToolOptional );
        }
        catch ( ReflectiveOperationException | SecurityException e )
        {
            // Ignore. It is expected that the jar tool
            // may not be available.
        }
    }

    @Override
    protected void zipFile( InputStreamSupplier is, ConcurrentJarCreator zOut,
                            String vPath, long lastModified, File fromArchive,
                            int mode, String symlinkDestination,
                            boolean addInParallel )
        throws IOException, ArchiverException
    {
        if ( jarTool != null && isModuleDescriptor( vPath ) )
        {
            getLogger().debug( "Module descriptor found: " + vPath );

            moduleDescriptorFound = true;
        }

        super.zipFile( is, zOut, vPath, lastModified,
            fromArchive, mode, symlinkDestination, addInParallel );
    }

    @Override
    protected void postCreateArchive()
        throws ArchiverException
    {
        if ( !moduleDescriptorFound )
        {
            // no need to update the JAR archive
            return;
        }

        try
        {
            getLogger().debug( "Using the jar tool to " +
                "update the archive to modular JAR." );

            final Method jarRun = jarTool.getClass()
                .getMethod( "run", PrintStream.class, PrintStream.class, String[].class );

            if ( getLastModifiedTime() != null )
            {
                hasJarDateOption = isJarDateOptionSupported( jarRun );
                getLogger().debug( "jar tool --date option is supported: " + hasJarDateOption );
            }

            Integer result = (Integer) jarRun.invoke( jarTool, System.out, System.err, getJarToolArguments() );

            if ( result != null && result != 0 )
            {
                throw new ArchiverException( "Could not create modular JAR file. " +
                    "The JDK jar tool exited with " + result );
            }

            if ( !hasJarDateOption && getLastModifiedTime() != null )
            {
                getLogger().debug( "Fix last modified time zip entries." );
                fixLastModifiedTimeZipEntries();
            }
        }
        catch ( IOException | ReflectiveOperationException | SecurityException e )
        {
            throw new ArchiverException( "Exception occurred " +
                "while creating modular JAR file", e );
        }
    }

    private void fixLastModifiedTimeZipEntries()
        throws IOException
    {
        long timeMillis = getLastModifiedTime().toMillis();
        Path destFile = getDestFile().toPath();
        Path tmpZip = Files.createTempFile( destFile.getParent(), null, null );
        try ( ZipFile zipFile = new ZipFile( getDestFile() );
              ZipOutputStream out = new ZipOutputStream( Files.newOutputStream( tmpZip ) ) )
        {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while ( entries.hasMoreElements() )
            {
                ZipEntry entry = entries.nextElement();
                entry.setTime( timeMillis );
                out.putNextEntry( entry );
                if ( !entry.isDirectory() )
                {
                    IOUtil.copy( zipFile.getInputStream( entry ), out );
                }
                out.closeEntry();
            }
        }
        Files.move( tmpZip, destFile, StandardCopyOption.REPLACE_EXISTING );
    }

    private boolean isModuleDescriptor( String path )
    {
        if ( path.endsWith( MODULE_DESCRIPTOR_FILE_NAME ) )
        {
            String prefix = path.substring( 0,
                path.lastIndexOf( MODULE_DESCRIPTOR_FILE_NAME ) );

            return prefix.isEmpty() ||
                MRJAR_VERSION_AREA.matcher( prefix ).matches();
        }
        else
        {
            return false;
        }
    }

    private String[] getJarToolArguments()
        throws IOException
    {
        File tempEmptyDir = Files.createTempDirectory( null ).toFile();
        tempEmptyDir.deleteOnExit();

        List<String> args = new ArrayList<>();

        args.add( "--update" );
        args.add( "--file" );
        args.add( getDestFile().getAbsolutePath() );

        String mainClass = getModuleMainClass() != null
                           ? getModuleMainClass()
                           : getManifestMainClass();

        if ( mainClass != null )
        {
            args.add( "--main-class" );
            args.add( mainClass );
        }

        if ( getModuleVersion() != null )
        {
            args.add( "--module-version" );
            args.add( getModuleVersion() );
        }

        if ( !isCompress() )
        {
            args.add( "--no-compress" );
        }

        if ( hasJarDateOption )
        {
            FileTime localTime = revertToLocalTime( getLastModifiedTime() );
            args.add( "--date" );
            args.add( localTime.toString() );
        }

        args.add( "-C" );
        args.add( tempEmptyDir.getAbsolutePath() );
        args.add( "." );

        return args.toArray( new String[0] );
    }

    private static FileTime revertToLocalTime( FileTime time )
    {
        long restoreToLocalTime = time.toMillis();
        Calendar cal = Calendar.getInstance( TimeZone.getDefault(), Locale.ROOT );
        cal.setTimeInMillis( restoreToLocalTime );
        restoreToLocalTime = restoreToLocalTime + ( cal.get( Calendar.ZONE_OFFSET ) + cal.get( Calendar.DST_OFFSET ) );
        return FileTime.fromMillis( restoreToLocalTime );
    }

    private boolean isJarDateOptionSupported( Method runMethod )
    {
        try
        {
            String[] args = { "--date", "2099-12-31T23:59:59Z", "--version" };

            PrintStream nullPrintStream = new PrintStream(OutputStream.nullOutputStream());
            Integer result = (Integer) runMethod.invoke( jarTool, nullPrintStream, nullPrintStream, args );

            return result != null && result.intValue() == 0;
        }
        catch ( ReflectiveOperationException | SecurityException e )
        {
            return false;
        }
    }

}