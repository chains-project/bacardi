package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.CloudZone;
import com.google.cloud.pubsublite.ProjectPath;
import com.google.cloud.pubsublite.TopicName;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.internal.Publisher;
import com.google.cloud.pubsublite.internal.wire.PubsubContext;
import com.google.cloud.pubsublite.internal.wire.PubsubContext.Framework;
import com.google.cloud.pubsublite.internal.wire.RoutingPublisherBuilder;
import com.google.cloud.pubsublite.internal.wire.SinglePartitionPublisherBuilder;
import java.util.Map;
import org.apache.kafka.common.config.ConfigValue;

class PublisherFactoryImpl implements PublisherFactory {

  private static final Framework FRAMEWORK = Framework.of("KAFKA_CONNECT");

  @Override
  public Publisher<PublishMetadata> newPublisher(Map<String, String> params) {
    Map<String, ConfigValue> config = ConfigDefs.config().validateAll(params);
    RoutingPublisherBuilder.Builder builder = RoutingPublisherBuilder.newBuilder();
    TopicPath topic = TopicPath.of(
        ProjectPath.parse("projects/" + config.get(ConfigDefs.PROJECT_FLAG).value()).project(),
        CloudZone.parse(config.get(ConfigDefs.LOCATION_FLAG).value().toString()),
        TopicName.of(config.get(ConfigDefs.TOPIC_NAME_FLAG).value().toString()));
    builder.setTopic(topic);
    builder.setPublisherFactory(new com.google.cloud.pubsublite.internal.wire.PartitionPublisherFactory() {
      @Override
      public Publisher<?> newPublisher(int partition) {
        return SinglePartitionPublisherBuilder.newBuilder()
            .setTopic(topic)
            .setPartition(partition)
            .build();
      }

      @Override
      public void shutdown() {
        // No additional shutdown logic required.
      }
    });
    return (Publisher<PublishMetadata>) builder.build();
  }

  public static final class PublishMetadata {
    // Dummy class to satisfy the removed dependency.
  }
}