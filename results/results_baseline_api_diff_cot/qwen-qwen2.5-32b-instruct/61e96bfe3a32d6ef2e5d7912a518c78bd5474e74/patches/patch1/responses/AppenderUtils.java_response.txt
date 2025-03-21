package com.pinterest.singer.client.logback;

import com.pinterest.singer.thrift.LogMessage;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class AppenderUtils {

  public static final String PATH_SEP = "/";

  private AppenderUtils() {
  }

  public static class LogMessageEncoder implements Encoder<LogMessage> {

    private TTransport framedTransport;
    private TProtocol protocol;
    private OutputStream os;

    @Override
    public void start() {
      // No-op
    }

    @Override
    public void stop() {
      if (framedTransport != null) {
        try {
          framedTransport.close();
        } catch (IOException e) {
          // Handle or log the exception
        }
      }
    }

    @Override
    public byte[] encode(LogMessage logMessage) {
      try {
        if (os == null) {
          os = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
              // No-op
            }
          };
          final int bufferCapacity = 10;
          framedTransport = new TFastFramedTransport(new TIOStreamTransport(os), bufferCapacity);
          protocol = new TBinaryProtocol(framedTransport);
        }
        logMessage.write(protocol);
        framedTransport.flush();
        return new byte[0]; // Return an empty byte array as a placeholder
      } catch (TException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public byte[] getHeader() {
      return new byte[0]; // Return an empty byte array as a placeholder
    }

    @Override
    public byte[] getFooter() {
      return new byte[0]; // Return an empty byte array as a placeholder
    }
  }

  public static Appender<LogMessage> createFileRollingThriftAppender(
      File basePath,
      String topic,
      long rotateThresholdKBytes,
      Context context,
      int maxRetentionHours) {
    RollingFileAppender<LogMessage> appender = new RollingFileAppender<>();
    appender.setContext(context);
    appender.setAppend(true);
    appender.setPrudent(false);

    LogMessageEncoder encoder = new LogMessageEncoder();
    appender.setEncoder(encoder);
    appender.setFile(basePath + PATH_SEP + topic);

    TimeBasedRollingPolicy<LogMessage> policy = new TimeBasedRollingPolicy<>();
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