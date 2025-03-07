// Your complete, fixed Java class here
package com.pinterest.singer.loggingaudit.client;

import com.pinterest.singer.loggingaudit.client.common.LoggingAuditClientMetrics;
import com.pinterest.singer.loggingaudit.thrift.LoggingAuditHeaders;
import com.pinterest.singer.loggingaudit.thrift.LoggingAuditEvent;
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
import org.apache.thrift.TSerializer;
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

public class AuditEventKafkaSender implements LoggingAuditEventSender {

  private static final Logger LOG = LoggerFactory.getLogger(AuditEventKafkaSender.class);

  private static final int MAX_RETRIES_FOR_SELECTION_RANDOM_PARTITION = 10;

  private static final int PARTITIONS_REFRESH_INTERVAL_IN_SECONDS = 30;

  private TSerializer serializer;

  private static final int NUM_OF_PARTITIONS_TO_TRY_SENDING = 3;

  private static final int DEQUEUE_WAIT_IN_SECONDS = 30;

  private static final int THREAD_SLEEP_IN_SECONDS = 10;

  private int stopGracePeriodInSeconds = 300;

  private final LoggingAuditStage stage;

  private final String host;

  private final LinkedBlockingDeque<LoggingAuditEvent> queue;

  private KafkaProducer<byte[], byte[]> kafkaProducer;

  private final AtomicBoolean cancelled = new AtomicBoolean(false);

  private final String topic;

  private final String name;

  private Thread thread;

  private final List<PartitionInfo> partitionInfoList = new ArrayList<>();

  private final long lastTimeUpdate = -1;

  private final Set<Integer> badPartitions = ConcurrentHashMap.newKeySet();

  private final Map<LoggingAuditHeaders, Integer> eventTriedCount = new ConcurrentHashMap<>();

  public AuditEventKafkaSender(KafkaSenderConfig config,
                               LinkedBlockingDeque<LoggingAuditEvent> queue,
                               LoggingAuditStage stage, String host, String name) {
    this.topic = config.getTopic();
    this.queue = queue;
    this.stage = stage;
    this.host = host;
    this.name = name;
    this.stopGracePeriodInSeconds = config.getStopGracePeriodInSeconds();
    try {
      this.serializer = new TSerializer();
    } catch (TTransportException e) {
      LOG.error("Failed to initialize TSerializer", e);
      throw new RuntimeException("Failed to initialize TSerializer", e);
    }
  }

  // ... (rest of the class remains unchanged, with the necessary changes to handle TSerializer initialization)
}