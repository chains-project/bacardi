import org.apache.thrift.transport.TFramedTransport;

private static final class ByteOffsetTFramedTransport extends TFramedTransport {

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
      super.write(buf, off, len);
      byteOffset += len;
    }

    @Override
    public void flush() throws TTransportException {
      super.flush();
      // Add 4 bytes for the frame size.
      byteOffset += 4;
    }