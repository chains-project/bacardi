package org.codehaus.plexus.archiver.zip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class implements an output stream in which the data is
 * written into a byte array. The buffer capacity is initially 1024 bytes, though its size increases if necessary.
 */
public class ByteArrayOutputStream extends OutputStream
{

    /**
     * A singleton empty byte array.
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[ 0 ];

    /**
     * The list of buffers, which grows and never reduces.
     */
    private final List<byte[]> buffers = new ArrayList<byte[]>();

    /**
     * The index of the current buffer.
     */
    private int currentBufferIndex;

    /**
     * The total count of bytes in all the filled buffers.
     */
    private int filledBufferSum;

    /**
     * The current buffer.
     */
    private byte[] currentBuffer;

    /**
     * The total count of bytes written.
     */
    private int count;

    /**
     * Flag to indicate if the buffers can be reused after reset
     */
    private boolean reuseBuffers = true;

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 1024 bytes, though its size increases if necessary.
     */
    public ByteArrayOutputStream()
    {
        this( 1024 );
    }

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param size the initial size
     *
     * @throws IllegalArgumentException if size is negative
     */
    public ByteArrayOutputStream( final int size )
    {
        if ( size < 0 )
        {
            throw new IllegalArgumentException( "Negative initial size: " + size );
        }
        synchronized ( this )
        {
            needNewBuffer( size );
        }
    }

    /**
     * Makes a new buffer available either by allocating
     * a new one or re-cycling an existing one.
     *
     * @param newcount the size of the buffer if one is created
     */
    private void needNewBuffer( final int newcount )
    {
        if ( currentBufferIndex < buffers.size() - 1 )
        {
            //Recycling old buffer
            filledBufferSum += currentBuffer.length;

            currentBufferIndex++;
            currentBuffer = buffers.get( currentBufferIndex );
        }
        else
        {
            //Creating new buffer
            int newBufferSize;
            if ( currentBuffer == null )
            {
                newBufferSize = newcount;
                filledBufferSum = 0;
            }
            else
            {
                newBufferSize = Math.max(
                    currentBuffer.length << 1,
                    newcount - filledBufferSum );
                filledBufferSum += currentBuffer.length;
            }

            currentBufferIndex++;
            currentBuffer = new byte[ newBufferSize ];
            buffers.add( currentBuffer );
        }
    }

    /**
     * Write the bytes to byte array.
     *
     * @param b the bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     */
    @Override
    public synchronized void write( final byte[] b, final int off, final int len )
    {
        if ( ( off < 0 )
                 || ( off > b.length )
                 || ( len < 0 )
                 || ( ( off + len ) > b.length )
                 || ( ( off + len ) < 0 ) )
        {
            throw new IndexOutOfBoundsException();
        }
        if ( len == 0 )
        {
            return;
        }
        synchronized ( this )
        {
            final int newcount = count + len;
            int remaining = len;
            int inBufferPos = count - filledBufferSum;
            while ( remaining > 0 )
            {
                final int part = Math.min( remaining, currentBuffer.length - inBufferPos );
                System.arraycopy( b, off + len - remaining, currentBuffer, inBufferPos, part );
                remaining -= part;
                if ( remaining > 0 )
                {
                    needNewBuffer( newcount );
                    inBufferPos = 0;
                }
            }
            count = newcount;
        }
    }

    /**
     * Write a byte to byte array.
     *
     * @param b the byte to write
     */
    @Override
    public synchronized void write( final int b )
    {
        int inBufferPos = count - filledBufferSum;
        if ( inBufferPos == currentBuffer.length )
        {
            needNewBuffer( count + 1 );
            inBufferPos = 0;
        }
        currentBuffer[inBufferPos] = (byte) b;
        count++;
    }

    /**
     * Writes the entire contents of an <code>InputStream</code> to the
     * specified output stream.
     *
     * @param in the input stream to write from
     *
     * @return total number of bytes read from the input stream
     * (and written to this stream)
     *
     * @throws java.io.IOException if an I/O error occurs, such as if the stream is closed
     */
    public synchronized int write( final InputStream in ) throws IOException
    {
        int readCount = 0;
        int inBufferPos = count - filledBufferSum;
        int n = in.read( currentBuffer, inBufferPos, currentBuffer.length - inBufferPos );
        while ( n != -1 )
        {
            readCount += n;
            inBufferPos += n;
            count += n;
            if ( inBufferPos == currentBuffer.length )
            {
                needNewBuffer( currentBuffer.length );
                inBufferPos = 0;
            }
            n = in.read( currentBuffer, inBufferPos, currentBuffer.length - inBufferPos );
        }
        return readCount;
    }

    /**
     * Return the current contents of this byte stream as a Input Stream. The
     * returned stream is backed by buffers of <code>this</code> stream,
     * avoiding memory allocation and copy, thus saving space and time.
     *
     * @return the current contents of this output stream.
     */
    public synchronized InputStream toInputStream()
    {
        int remaining = count;
        if ( remaining == 0 )
        {
            return new InputStream()
            {
                @Override
                public int read() throws IOException
                {
                    return -1;
                }
            };
        }
        final List<ByteArrayInputStream> list = new ArrayList<ByteArrayInputStream>( buffers.size() );
        for ( final byte[] buf : buffers )
        {
            final int c = Math.min( buf.length, remaining );
            list.add( new ByteArrayInputStream( buf, 0, c ) );
            remaining -= c;
            if ( remaining == 0 )
            {
                break;
            }
        }
        reuseBuffers = false;
        return new SequenceInputStream( Collections.enumeration( list ) );
    }

    /**
     * Gets the current contents of this byte stream as a byte array.
     * The result is independent of this stream.
     *
     * @return the current contents of this output stream, as a byte array
     */
    public synchronized byte[] toByteArray()
    {
        int remaining = count;
        if ( remaining == 0 )
        {
            return EMPTY_BYTE_ARRAY;
        }
        final byte newbuf[] = new byte[ remaining ];
        int pos = 0;
        for ( final byte[] buf : buffers )
        {
            final int c = Math.min( buf.length, remaining );
            System.arraycopy( buf, 0, newbuf, pos, c );
            pos += c;
            remaining -= c;
            if ( remaining == 0 )
            {
                break;
            }
        }
        return newbuf;
    }

    /**
     * Gets the current contents of this byte stream as a string
     * using the platform default charset.
     *
     * @return the contents of the byte array as a String
     */
    @Override
    @Deprecated
    public String toString()
    {
        // make explicit the use of the default charset
        return new String( toByteArray(), Charset.defaultCharset() );
    }

    /**
     * Gets the current contents of this byte stream as a string
     * using the specified encoding.
     *
     * @param enc the name of the character encoding
     *
     * @return the string converted from the byte array
     *
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported
     */
    public String toString( final String enc ) throws UnsupportedEncodingException
    {
        return new String( toByteArray(), enc );
    }

    /**
     * Gets the current contents of this byte stream as a string
     * using the specified encoding.
     *
     * @param charset the character encoding
     *
     * @return the string converted from the byte array
     */
    public String toString( final Charset charset )
    {
        return new String( toByteArray(), charset );
    }

    /**
     * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an {@code IOException}.
     *
     * @throws java.io.IOException never (this method should not declare this exception)
     */
    @Override
    public void close() throws IOException
    {
        //nop
    }

    /**
     * Resets the stream to its initial state.
     */
    public synchronized void reset()
    {
        count = 0;
        filledBufferSum = 0;
        currentBufferIndex = 0;
        if ( reuseBuffers )
        {
            currentBuffer = buffers.get( currentBufferIndex );
        }
        else
        {
            //Throw away old buffers
            currentBuffer = null;
            int size = buffers.get( 0 ).length;
            buffers.clear();
            needNewBuffer( size );
            reuseBuffers = true;
        }
    }

}