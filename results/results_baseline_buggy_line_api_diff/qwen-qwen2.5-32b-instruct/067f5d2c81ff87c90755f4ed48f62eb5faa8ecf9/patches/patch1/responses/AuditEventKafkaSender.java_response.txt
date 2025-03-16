package com.pinterest.singer.loggingaudit.client;

import com.pinterest.singer.loggingaudit.client.common.LoggingAuditClientMetrics;
import com.pinterest.singer.loggingaudit.thrift.LoggingAuditEvent;
import com.pinterest.singer.loggingaudit.thrift.LoggingAuditHeaders;
import com.pinterest.singer.loggingaudit.thrift.LoggingAuditStage;
import com.pinterest.singer.loggingaudit.thrift.configuration.KafkaSenderConfig;
import com.pinterest.singer.metrics.OpenTsdbMetricConverter;
import com.pinterest.singer.utils.CommonUtils;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuditEventKafkaSender implements LoggingAuditEventSender {

  private static final Logger LOG = LoggerFactory.getLogger(AuditEventKafkaSender.class);

  private static final int MAX_RETRIES_FOR_SELECTION_RANDOM_PARTITION = 10;

  private static final int PARTITIONS_REFRESH_INTERVAL_IN_SECONDS = 30;

  private final LoggingAuditStage stage;

  private final String host;

  private final LinkedBlockingDeque<LoggingAuditEvent> queue;

  private KafkaProducer<byte[], byte[]> kafkaProducer;

  private TSerializer serializer;

  private AtomicBoolean cancelled = new AtomicBoolean(false);

  private String topic;

  private String name;

  private Thread thread;

  private List<PartitionInfo> partitionInfoList = new ArrayList<>();

  private long lastTimeUpdate = -1;

  private Set<Integer> badPartitions = ConcurrentHashMap.newKeySet();

  private Map<LoggingAuditHeaders, Integer> eventTriedCount = new ConcurrentHashMap<>();

  private int currentPartitionId = -1;

  public AuditEventKafkaSender(KafkaSenderConfig config,
                               LinkedBlockingDeque<LoggingAuditEvent> queue,
                               LoggingAuditStage stage, String host, String name) {
    this.topic = config.getTopic();
    this.queue = queue;
    this.stage = stage;
    this.host = host;
    this.name = name;
    this.stopGracePeriodInSeconds = config.getStopGracePeriodInSeconds();
    this.badPartitions.add(-1);
    try {
      TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
      this.serializer = new TSerializer(protocolFactory);
    } catch (TTransportException e) {
      LOG.error("Failed to initialize TSerializer", e);
    }
  }

  // ... (rest of the class remains unchanged)
}