package org.codehaus.plexus.archiver.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;

/**
 * Offloads to disk when a given memory consumption has been reached.
 *
 * This implementation no longer extends Commons IO's ThresholdingOutputStream since that class
 * is no longer present in the updated dependency. Instead, it extends OutputStream and implements
 * the threshold checking logic directly.
 */
class OffloadingOutputStream extends OutputStream
{
    // ----------------------------------------------------------- Data members

    /**
     * The output stream to which data will be written prior to the threshold
     * being reached.
     */
    private ByteArrayOutputStream memoryOutputStream;

    /**
     * The output stream to which data is written at any time. This will always be either the
     * in–memory stream or the disk–based stream.
     */
    private OutputStream currentOutputStream;

    /**
     * The file to which output will be directed if the threshold is exceeded.
     */
    private File outputFile = null;

    /**
     * The temporary file prefix.
     */
    private final String prefix;

    /**
     * The temporary file suffix.
     */
    private final String suffix;

    /**
     * The directory to use for temporary files.
     */
    private final File directory;

    /**
     * True when close() has been called successfully.
     */
    private boolean closed = false;

    /**
     * The threshold at which to switch from the in–memory stream to the disk–based stream.
     */
    private final int thresholdLimit;

    /**
     * The counter of bytes written.
     */
    private long writtenBytes = 0;

    // ----------------------------------------------------------- Constructors

    /**
     * Constructs an instance of this class which will trigger an event at the specified threshold,
     * and save data to a temporary file beyond that point.
     *
     * @param threshold The number of bytes at which to trigger a threshold event.
     * @param prefix Prefix to use for the temporary file.
     * @param suffix Suffix to use for the temporary file.
     * @param directory Temporary file directory.
     * @throws IllegalArgumentException if the prefix is null.
     *
     * @since 1.4
     */
    public OffloadingOutputStream( int threshold, String prefix, String suffix, File directory )
    {
        this( threshold, null, prefix, suffix, directory );
        if ( prefix == null )
        {
            throw new IllegalArgumentException( "Temporary file prefix is missing" );
        }
    }

    /**
     * Constructs an instance of this class which will trigger an event at the specified threshold,
     * and save data either to a file beyond that point.
     *
     * @param threshold The number of bytes at which to trigger an event.
     * @param outputFile The file to which data is saved beyond the threshold.
     * @param prefix Prefix to use for the temporary file.
     * @param suffix Suffix to use for the temporary file.
     * @param directory Temporary file directory.
     */
    private OffloadingOutputStream( int threshold, File outputFile, String prefix, String suffix, File directory )
    {
        this.thresholdLimit = threshold;
        this.outputFile = outputFile;
        // Initialize the in–memory stream with an initial capacity of threshold/10.
        memoryOutputStream = new ByteArrayOutputStream( threshold / 10 );
        currentOutputStream = memoryOutputStream;
        this.prefix = prefix;
        this.suffix = suffix;
        this.directory = directory;
    }

    // ----------------------------------------------------------- Helper methods

    /**
     * Returns the current output stream.
     *
     * @return The underlying output stream.
     *
     * @throws IOException if an error occurs.
     */
    protected OutputStream getStream() throws IOException
    {
        return currentOutputStream;
    }

    /**
     * Switches the underlying output stream from a memory–based stream to one that is backed by disk.
     * This method is invoked when too much data is written to be kept in memory.
     *
     * @throws IOException if an error occurs.
     */
    protected void thresholdReached() throws IOException
    {
        if ( prefix != null )
        {
            outputFile = File.createTempFile( prefix, suffix, directory );
        }
        currentOutputStream = Files.newOutputStream( outputFile.toPath() );
    }

    // ----------------------------------------------------------- Overridden OutputStream methods

    @Override
    public void write( int b ) throws IOException
    {
        if ( closed )
        {
            throw new IOException( "Stream is closed" );
        }
        // Check if writing one more byte would exceed the threshold.
        if ( currentOutputStream == memoryOutputStream && (writtenBytes + 1 > thresholdLimit) )
        {
            int bytesToMemory = thresholdLimit - (int) writtenBytes;
            if ( bytesToMemory > 0 )
            {
                // Write the remaining bytes that fit in memory.
                memoryOutputStream.write( b );
                writtenBytes++;
            }
            else
            {
                // Already at or beyond the threshold: switch stream.
                thresholdReached();
                currentOutputStream.write( b );
                writtenBytes++;
            }
        }
        else
        {
            currentOutputStream.write( b );
            writtenBytes++;
        }
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException
    {
        if ( closed )
        {
            throw new IOException( "Stream is closed" );
        }
        if ( currentOutputStream == memoryOutputStream && (writtenBytes + len > thresholdLimit) )
        {
            int bytesToMemory = thresholdLimit - (int) writtenBytes;
            if ( bytesToMemory > 0 )
            {
                // Write as many bytes as possible into the memory output.
                memoryOutputStream.write( b, off, bytesToMemory );
                writtenBytes += bytesToMemory;
                off += bytesToMemory;
                len -= bytesToMemory;
            }
            // Switch to disk–based stream.
            thresholdReached();
            currentOutputStream.write( b, off, len );
            writtenBytes += len;
        }
        else
        {
            currentOutputStream.write( b, off, len );
            writtenBytes += len;
        }
    }

    @Override
    public void flush() throws IOException
    {
        if ( !closed )
        {
            currentOutputStream.flush();
        }
    }

    @Override
    public void close() throws IOException
    {
        if ( !closed )
        {
            currentOutputStream.close();
            closed = true;
        }
    }

    // ----------------------------------------------------------- Public methods

    /**
     * Returns an InputStream to read back the data written through this stream.
     * If the data has not exceeded the threshold, only the in–memory data is returned.
     * Otherwise, both the in–memory and disk–based data are returned sequentially.
     *
     * @return An InputStream for reading the data.
     * @throws IOException if an error occurs.
     */
    public InputStream getInputStream() throws IOException
    {
        InputStream memoryAsInput = new ByteArrayInputStream( memoryOutputStream.toByteArray() );
        if ( outputFile == null )
        {
            return memoryAsInput;
        }
        return new SequenceInputStream( memoryAsInput, Files.newInputStream( outputFile.toPath() ) );
    }

    /**
     * Returns the data for this output stream as a byte array, provided that the data is still kept in memory.
     * If the data was written to disk, this method returns null.
     *
     * @return The byte array representing the in–memory data, or null if no such data is available.
     */
    public byte[] getData()
    {
        if ( memoryOutputStream != null )
        {
            return memoryOutputStream.toByteArray();
        }
        return null;
    }

    /**
     * Returns either the output file specified in the constructor or the temporary file created.
     * This will be non–null only if the threshold has been exceeded.
     *
     * @return The file associated with the output stream, or null if no such file exists.
     */
    public File getFile()
    {
        return outputFile;
    }
}