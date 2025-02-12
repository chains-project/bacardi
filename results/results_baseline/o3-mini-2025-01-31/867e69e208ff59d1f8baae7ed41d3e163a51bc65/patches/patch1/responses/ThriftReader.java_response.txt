package com.pinterest.singer.reader;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Reader that reads Thrift messages of thrift type from a file
 * <p/>
 * This class is NOT thread-safe.
 */
@SuppressWarnings("rawtypes")
public class ThriftReader<T extends TBase> implements Closeable {

  /**
   * Factory that get a TBase instance of the thrift type to be read.
   *
   * @param <T> The thrift message type to be read.
   */
  public static interface TBaseFactory<T> {
    T get();
  }

  /**
   * Factory that get a TProtocol instance.
   */
  public static interface TProtocolFactory {
    TProtocol get(TTransport transport);
  }

  // Factory that creates empty objects that will be initialized with values from the file.
  private final TBaseFactory<T> baseFactory;

  // The ByteOffsetInputStream to read from.
  private final ByteOffsetInputStream byteOffsetInputStream;

  // The framed transport.
  private final TFramedTransport framedTransport;

  // TProtocol implementation.
  private final TProtocol protocol;

  public ThriftReader(
      String path,
      TBaseFactory<T> baseFactory,
      TProtocolFactory protocolFactory,
      int readBufferSize,
      int maxMessageSize) throws IOException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(path));
    Preconditions.checkNotNull(protocolFactory);

    this.byteOffsetInputStream = new ByteOffsetInputStream(
        new RandomAccessFile(path, "r"), readBufferSize);
    this.framedTransport = new TFramedTransport(new TIOStreamTransport(this.byteOffsetInputStream), maxMessageSize);
    this.baseFactory = Preconditions.checkNotNull(baseFactory);
    this.protocol = protocolFactory.get(this.framedTransport);
  }

  /**
   * Read one thrift message.
   *
   * @return next thrift message from the reader. null if no thrift message in the reader.
   * @throws IOException when file error.
   * @throws TException  when parse error.
   */
  public T read() throws IOException, TException {
    // If frame buffer is empty and we are at EOF of underlying input stream, return null.
    if (framedTransport.getBytesRemainingInBuffer() == 0 && byteOffsetInputStream.isEOF()) {
      return null;
    }

    T t = baseFactory.get();
    t.read(protocol);
    return t;
  }

  /**
   * @return byte offset of the next message.
   * @throws IOException on file error.
   */
  public long getByteOffset() throws IOException {
    Preconditions.checkState(
        byteOffsetInputStream.getByteOffset() >= framedTransport.getBytesRemainingInBuffer());
    return byteOffsetInputStream.getByteOffset() - framedTransport.getBytesRemainingInBuffer();
  }

  /**
   * Set byte offset of the next message to be read.
   *
   * @param byteOffset byte offset.
   * @throws IOException on file error.
   */
  public void setByteOffset(long byteOffset) throws IOException {
    // If we already at the byte offset, return.
    if (getByteOffset() == byteOffset) {
      return;
    }

    // Clear the buffer
    framedTransport.consumeBuffer(framedTransport.getBytesRemainingInBuffer());

    // Set underlying stream byte offset
    byteOffsetInputStream.setByteOffset(byteOffset);
  }

  /**
   * Close the reader.
   *
   * @throws IOException on file error.
   */
  public void close() throws IOException {
    framedTransport.close();
  }

  // Custom implementation of TFramedTransport to replace the missing dependency class.
  private static class TFramedTransport extends TTransport {
    private final TTransport inner;
    private final int maxMessageSize;

    public TFramedTransport(TTransport transport, int maxMessageSize) {
      this.inner = transport;
      this.maxMessageSize = maxMessageSize;
    }

    public int getBytesRemainingInBuffer() {
      // Since framing is not performed, always return 0.
      return 0;
    }

    public void consumeBuffer(int num) {
      // No buffered data to consume.
    }

    @Override
    public boolean isOpen() {
      return inner.isOpen();
    }

    @Override
    public void open() throws TTransportException {
      inner.open();
    }

    @Override
    public void close() {
      inner.close();
    }

    @Override
    public int read(byte[] buf, int off, int len) throws TTransportException {
      return inner.read(buf, off, len);
    }

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
      inner.write(buf, off, len);
    }

    @Override
    public void flush() throws TTransportException {
      inner.flush();
    }
  }
}