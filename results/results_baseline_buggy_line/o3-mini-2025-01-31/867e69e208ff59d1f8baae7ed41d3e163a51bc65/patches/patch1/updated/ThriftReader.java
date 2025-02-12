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

  // The framedTransport.
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

  /**
   * A minimal implementation of a framed transport that reads a frame length (int)
   * and then the frame from the underlying transport.
   */
  public static class TFramedTransport extends TTransport {
    private final TTransport underlying;
    private final int maxFrameSize;
    private byte[] frameBuffer;
    private int frameBufferPos;
    private int frameBufferLength;

    public TFramedTransport(TTransport transport, int maxFrameSize) {
      this.underlying = transport;
      this.maxFrameSize = maxFrameSize;
      this.frameBuffer = null;
      this.frameBufferPos = 0;
      this.frameBufferLength = 0;
    }

    @Override
    public boolean isOpen() {
      return underlying.isOpen();
    }

    @Override
    public void open() throws TTransportException {
      underlying.open();
    }

    @Override
    public void close() {
      underlying.close();
    }

    @Override
    public int read(byte[] buf, int off, int len) throws TTransportException {
      if (frameBuffer == null || frameBufferPos >= frameBufferLength) {
        readFrame();
      }
      int available = frameBufferLength - frameBufferPos;
      int bytesToRead = Math.min(len, available);
      System.arraycopy(frameBuffer, frameBufferPos, buf, off, bytesToRead);
      frameBufferPos += bytesToRead;
      return bytesToRead;
    }

    private void readFrame() throws TTransportException {
      byte[] lenBuf = new byte[4];
      int bytesRead = 0;
      while (bytesRead < 4) {
        int ret = underlying.read(lenBuf, bytesRead, 4 - bytesRead);
        if (ret <= 0) {
          throw new TTransportException("Cannot read frame size");
        }
        bytesRead += ret;
      }
      int frameSize = ((lenBuf[0] & 0xff) << 24) |
                      ((lenBuf[1] & 0xff) << 16) |
                      ((lenBuf[2] & 0xff) << 8)  |
                      (lenBuf[3] & 0xff);
      if (frameSize > maxFrameSize) {
        throw new TTransportException("Frame size " + frameSize + " is greater than max allowed " + maxFrameSize);
      }
      frameBuffer = new byte[frameSize];
      frameBufferPos = 0;
      frameBufferLength = 0;
      int bytesRemaining = frameSize;
      while (bytesRemaining > 0) {
        int ret = underlying.read(frameBuffer, frameBufferLength, bytesRemaining);
        if (ret <= 0) {
          throw new TTransportException("Unable to read full frame");
        }
        frameBufferLength += ret;
        bytesRemaining -= ret;
      }
    }

    public int getBytesRemainingInBuffer() {
      return (frameBuffer != null) ? (frameBufferLength - frameBufferPos) : 0;
    }

    public void consumeBuffer(int n) throws TTransportException {
      if (n != getBytesRemainingInBuffer()) {
        throw new TTransportException("consumeBuffer: n does not equal the remaining bytes");
      }
      frameBufferPos = frameBufferLength;
    }

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
      underlying.write(buf, off, len);
    }

    @Override
    public void flush() throws TTransportException {
      underlying.flush();
    }
  }
}