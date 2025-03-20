package com.pinterest.singer.client.logback;

import com.pinterest.singer.thrift.LogMessage;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class AppenderUtils {

  public static final String PATH_SEP = "/";

  private AppenderUtils() {
  }

  public static class LogMessageEncoder extends EncoderBase<LogMessage> {

    @Override
    public void init(OutputStream os) throws IOException {
      // No initialization needed for this stateless encoder.
    }

    @Override
    public byte[] encode(LogMessage logMessage) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final int bufferCapacity = 10;
      TTransport transport = new TFastFramedTransport(new TIOStreamTransport(baos), bufferCapacity);
      TProtocol protocol = new TBinaryProtocol(transport);
      try {
        logMessage.write(protocol);
        transport.flush();
      } catch (TException e) {
        throw new IOException(e);
      }
      return baos.toByteArray();
    }

    @Override
    public void close() throws IOException {
      // No resources to clean up.
    }
    
    @Override
    public byte[] headerBytes() {
      return null;
    }

    @Override
    public byte[] footerBytes() {
      return null;
    }
  }

  public static Appender<LogMessage> createFileRollingThriftAppender(
      File basePath,
      String topic,
      long rotateThresholdKBytes,
      Context context,
      int maxRetentionHours) {
    RollingFileAppender<LogMessage> appender = new RollingFileAppender<LogMessage>();
    appender.setContext(context);
    appender.setAppend(true);
    appender.setPrudent(false);

    LogMessageEncoder encoder = new LogMessageEncoder();
    appender.setEncoder(encoder);
    appender.setFile(basePath + PATH_SEP + topic);

    TimeBasedRollingPolicy policy = new TimeBasedRollingPolicy();
    policy.setMaxHistory(maxRetentionHours);
    policy.setFileNamePattern(basePath + PATH_SEP + topic + ".%d{yyyy-MM-dd-HH}.%i");
    policy.setCleanHistoryOnStart(false);
    policy.setContext(context);
    policy.setParent(appender);

    SizeAndTimeBasedFNATP fnatp = new SizeAndTimeBasedFNATP();
    fnatp.setContext(context);
    fnatp.setTimeBasedRollingPolicy(policy);
    fnatp.setMaxFileSize(FileSize.valueOf(String.format("%sKB", rotateThresholdKBytes)));

    policy.setTimeBasedFileNamingAndTriggeringPolicy(fnatp);
    appender.setRollingPolicy(policy);
    appender.setTriggeringPolicy(policy);

    policy.start();
    appender.start();

    return appender;
  }
}