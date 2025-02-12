package org.codehaus.plexus.archiver.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.parallel.ScatterGatherBackingStore;

public class DeferredScatterOutputStream implements ScatterGatherBackingStore
{

    private final OffloadingOutputStream dfos;

    public DeferredScatterOutputStream( int threshold )
    {
        dfos = new OffloadingOutputStream( threshold, "scatterzipfragment", "zip", null );
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return dfos.getInputStream();
    }

    @Override
    public void writeOut( byte[] data, int offset, int length ) throws IOException
    {
        byte[] subData = new byte[length];
        System.arraycopy(data, offset, subData, 0, length);
        dfos.write(subData);
    }

    @Override
    public void closeForWriting() throws IOException
    {
        dfos.close();
    }

    @Override
    public void close() throws IOException
    {
        File file = dfos.getFile();
        if ( file != null )
        {
            file.delete();
        }
    }

}