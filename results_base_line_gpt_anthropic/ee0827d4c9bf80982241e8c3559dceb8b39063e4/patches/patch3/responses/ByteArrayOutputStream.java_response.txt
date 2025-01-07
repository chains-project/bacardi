/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.plexus.archiver.zip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils; // Ensure the correct import for IOUtils

/**
 * This class implements an output stream in which the data is
 * written into a byte array. The buffer automatically grows as data
 * is written to it.
 * <p>
 * The data can be retrieved using <code>toByteArray()</code> and
 * <code>toString()</code>.
 * <p>
 * Closing a {@code ByteArrayOutputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 * <p>
 * This is an alternative implementation of the {@link java.io.ByteArrayOutputStream}
 * class. The original implementation only allocates 32 bytes at the beginning.
 * As this class is designed for heavy duty it starts at 1024 bytes. In contrast
 * to the original it doesn't reallocate the whole memory block but allocates
 * additional buffers. This way no buffers need to be garbage collected and
 * the contents don't have to be copied to the new buffer. This class is
 * designed to behave exactly like the original. The only exception is the
 * deprecated toString(int) method that has been ignored.
 */
public class ByteArrayOutputStream extends OutputStream
{
    // ... [rest of the class remains unchanged]

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
        if ( remaining == 0 )
        {
            return IOUtils.toInputStream("", Charset.defaultCharset()); // Use IOUtils for an empty stream
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

    // ... [rest of the class remains unchanged]
}