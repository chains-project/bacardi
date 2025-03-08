package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.ProjectPath;
import com.google.cloud.pubsublite.TopicName;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.internal.Publisher;
import com.google.cloud.pubsublite.internal.wire.PubsubContext;
import com.google.cloud.pubsublite.internal.wire.PublisherBuilder;
import com.google.cloud.pubsublite.internal.wire.RoutingPublisherBuilder;
import com.google.cloud.pubsublite.internal.wire.SinglePartitionPublisherBuilder;
import java.util.Map;
import org.apache.kafka.common.config.ConfigValue;

class PublisherFactoryImpl implements PublisherFactory {

  private static final PubsubContext FRAMEWORK = PubsubContext.of("KAFKA_CONNECT");

  @Override
  public Publisher newPublisher(Map<String, String> params) {
    Map<String, ConfigValue> config = ConfigDefs.config().validateAll(params);
    RoutingPublisherBuilder.Builder builder = RoutingPublisherBuilder.newBuilder();
    TopicPath topic =
        TopicPath.of(
            ProjectPath.of("projects/" + config.get(ConfigDefs.PROJECT_FLAG).value()),
            CloudZone.parse(config.get(ConfigDefs.LOCATION_FLAG).value().toString()),
            TopicName.of(config.get(ConfigDefs.TOPIC_NAME_FLAG).value().toString()));
    builder.setTopic(topic);
    builder.setPublisherFactory(
        partition ->
            SinglePartitionPublisherBuilder.newBuilder()
                .setTopic(topic)
                .setPartition(partition)
                .setContext(FRAMEWORK)
                .build());
    return builder.build();
  }
}