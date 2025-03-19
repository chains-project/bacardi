package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.CloudZone;
import com.google.cloud.pubsublite.ProjectName;
import com.google.cloud.pubsublite.TopicName;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.internal.Publisher;
import com.google.cloud.pubsublite.internal.wire.PubsubContext;
import com.google.cloud.pubsublite.internal.wire.PubsubContext.Framework;
import com.google.cloud.pubsublite.internal.wire.RoutingPublisherBuilder;
import com.google.cloud.pubsublite.internal.wire.SinglePartitionPublisherBuilder;
import com.google.protobuf.ByteString;
import java.util.Map;
import org.apache.kafka.common.config.ConfigValue;

class PublisherFactoryImpl implements PublisherFactory {

  private static final Framework FRAMEWORK = Framework.of("KAFKA_CONNECT");

  @Override
  public Publisher<ByteString> newPublisher(Map<String, String> params) {
    Map<String, ConfigValue> config = ConfigDefs.config().validateAll(params);
    RoutingPublisherBuilder.Builder builder = RoutingPublisherBuilder.newBuilder();
    TopicPath topic =
        TopicPath.newBuilder()
            .setProject(
                ProjectName.of(config.get(ConfigDefs.PROJECT_FLAG).value().toString()))
            .setLocation(CloudZone.parse(config.get(ConfigDefs.LOCATION_FLAG).value().toString()))
            .setName(TopicName.of(config.get(ConfigDefs.TOPIC_NAME_FLAG).value().toString()))
            .build();
    builder.setTopic(topic);
    builder.setPublisherFactory(
        (partition) -> {
          SinglePartitionPublisherBuilder singlePartitionPublisherBuilder =
              SinglePartitionPublisherBuilder.newBuilder();
          singlePartitionPublisherBuilder.setTopic(topic);
          singlePartitionPublisherBuilder.setPartition(partition);
          singlePartitionPublisherBuilder.setContext(PubsubContext.of(FRAMEWORK));
          return singlePartitionPublisherBuilder.build();
        });
    return builder.build();
  }
}