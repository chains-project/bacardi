/**
 * Copyright 2019 Pinterest, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  // Custom implementation of TFramedTransport to replace the missing dependency.
  public static class TFramedTransport extends TTransport {
    private final TTransport underlyingTransport;
    private final int maxFrameSize;
    private byte[] frameBuffer = null;
    private int frameReadPos = 0;
    private int frameLength = 0;

    public TFramedTransport(TTransport transport, int maxFrameSize) {
      this.underlyingTransport = transport;
      this.maxFrameSize = maxFrameSize;
    }

    @Override
    public boolean isOpen() {
      return underlyingTransport.isOpen();
    }

    @Override
    public void open() throws TTransportException {
      underlyingTransport.open();
    }

    @Override
    public void close() {
      underlyingTransport.close();
    }

    @Override
    public int read(byte[] buf, int off, int len) throws TTransportException {
      if (frameBuffer == null || frameReadPos >= frameLength) {
        // Read frame length (4 bytes)
        byte[] lengthBuffer = new byte[4];
        int got = 0;
        while (got < 4) {
          int bytesRead = underlyingTransport.read(lengthBuffer, got, 4 - got);
          if (bytesRead <= 0) {
            throw new TTransportException(TTransportException.END_OF_FILE, "Unable to read frame size");
          }
          got += bytesRead;
        }
        int frameSize = ((lengthBuffer[0] & 0xff) << 24) |
                        ((lengthBuffer[1] & 0xff) << 16) |
                        ((lengthBuffer[2] & 0xff) << 8) |
                        (lengthBuffer[3] & 0xff);
        if (frameSize > maxFrameSize) {
          throw new TTransportException(TTransportException.INVALID_FRAME_SIZE, "Frame size " + frameSize + " exceeds max " + maxFrameSize);
        }
        frameBuffer = new byte[frameSize];
        frameReadPos = 0;
        frameLength = frameSize;
        int offset = 0;
        while (offset < frameSize) {
          int rd = underlyingTransport.read(frameBuffer, offset, frameSize - offset);
          if (rd <= 0) {
            throw new TTransportException(TTransportException.END_OF_FILE, "Unable to read frame data");
          }
          offset += rd;
        }
      }
      int bytesRemaining = frameLength - frameReadPos;
      int bytesToRead = Math.min(len, bytesRemaining);
      System.arraycopy(frameBuffer, frameReadPos, buf, off, bytesToRead);
      frameReadPos += bytesToRead;
      return bytesToRead;
    }

    @Override
    public void write(byte[] buf, int off, int len) throws TTransportException {
      throw new UnsupportedOperationException("Write not supported in TFramedTransport");
    }

    @Override
    public void flush() throws TTransportException {
      // No-op for read-only transport
    }

    public int getBytesRemainingInBuffer() {
      return (frameBuffer == null) ? 0 : (frameLength - frameReadPos);
    }

    public void consumeBuffer(int numBytes) {
      if (frameBuffer != null && numBytes <= (frameLength - frameReadPos)) {
        frameReadPos += numBytes;
        if (frameReadPos >= frameLength) {
          frameBuffer = null;
          frameReadPos = 0;
          frameLength = 0;
        }
      }
    }
  }
}