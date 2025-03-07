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
    // ... (rest of the class remains unchanged)

    /**
     * Gets the current contents of this byte stream as a Input Stream. The
     * returned stream is backed by buffers of <code>this</code> stream,
     * avoiding memory allocation and copy, thus saving space and time.<br>
     *
     * @return the current contents of this output stream.
     *
     * @see java.io.ByteArrayOutputStream#toByteArray()
     * @see #reset()
     * @since 2.5
     */
    public synchronized InputStream toInputStream()
    {
        int remaining = count;
        if (remaining == 0)
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
        final List<ByteArrayInputStream> list = new ArrayList<>(buffers.size());
        for (final byte[] buf : buffers)
        {
            final int c = Math.min(buf.length, remaining);
            list.add(new ByteArrayInputStream(buf, 0, c));
            remaining -= c;
            if (remaining == 0)
            {
                break;
            }
        }
        reuseBuffers = false;
        return new SequenceInputStream(Collections.enumeration(list));
    }

    // ... (rest of the class remains unchanged)
}