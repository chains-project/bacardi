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

/**
 * Utils to create logback appenders
 */
public class AppenderUtils {

  public static final String PATH_SEP = "/";

  private AppenderUtils() {
  }

  /**
   * Encoder for LogMessage objects.
   */
  public static class LogMessageEncoder extends EncoderBase<LogMessage> {

    @Override
    public byte[] headerBytes() {
      // No header required.
      return new byte[0];
    }

    @Override
    public byte[] encode(LogMessage logMessage) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final int bufferCapacity = 10;
      TTransport framedTransport = new TFastFramedTransport(new TIOStreamTransport(baos), bufferCapacity);
      TProtocol protocol = new TBinaryProtocol(framedTransport);
      try {
        // Serialize the log message using the Thrift binary protocol.
        logMessage.write(protocol);
        framedTransport.flush();
      } catch (TException e) {
        throw new IOException(e);
      } finally {
        framedTransport.close();
      }
      return baos.toByteArray();
    }

    @Override
    public byte[] footerBytes() {
      // No footer required.
      return new byte[0];
    }
  }

  /**
   * Create the basic thrift appender which logs to a file
   * and rolls the file when it exceeds a certain size.
   *
   * @param basePath base directory the files are under.
   * @param topic the topic name for the current appender.
   * @param rotateThresholdKBytes threshold in kilobytes to rotate after.
   * @param context the logback context.
   * @param maxRetentionHours maximum number of hours to retain logs.
   */
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

    // Also impose a max size per file policy.
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