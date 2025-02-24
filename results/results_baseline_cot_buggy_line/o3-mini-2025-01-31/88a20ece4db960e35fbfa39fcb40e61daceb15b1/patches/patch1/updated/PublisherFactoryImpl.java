package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.CloudZone;
import com.google.cloud.pubsublite.ProjectPath;
import com.google.cloud.pubsublite.internal.PublishMetadata;
import com.google.cloud.pubsublite.TopicName;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.internal.Publisher;
import com.google.cloud.pubsublite.internal.wire.PubsubContext;
import com.google.cloud.pubsublite.internal.wire.PubsubContext.Framework;
import com.google.cloud.pubsublite.internal.wire.RoutingPublisherBuilder;
import com.google.cloud.pubsublite.internal.wire.SinglePartitionPublisherBuilder;
import com.google.cloud.pubsublite.internal.wire.PartitionPublisherFactory;
import java.util.Map;
import org.apache.kafka.common.config.ConfigValue;

class PublisherFactoryImpl implements PublisherFactory {

  private static final Framework FRAMEWORK = Framework.of("KAFKA_CONNECT");

  @Override
  public Publisher<PublishMetadata> newPublisher(Map<String, String> params) {
    Map<String, ConfigValue> config = ConfigDefs.config().validateAll(params);
    RoutingPublisherBuilder.Builder builder = RoutingPublisherBuilder.newBuilder();
    TopicPath topic =
        TopicPath.newBuilder()
            .setProject(
                ProjectPath.parse("projects/" + config.get(ConfigDefs.PROJECT_FLAG).value())
                    .project())
            .setLocation(CloudZone.parse(config.get(ConfigDefs.LOCATION_FLAG).value().toString()))
            .setName(TopicName.of(config.get(ConfigDefs.TOPIC_NAME_FLAG).value().toString()))
            .build();
    builder.setTopic(topic);
    
    // Replace lambda with an anonymous inner class that implements both required methods.
    builder.setPublisherFactory(new PartitionPublisherFactory() {
      @Override
      public Publisher<PublishMetadata> newPublisher(int partition) {
        return SinglePartitionPublisherBuilder.newBuilder()
            .setTopic(topic)
            .setPartition(partition)
            .setContext(PubsubContext.of(FRAMEWORK))
            .build();
      }

      @Override
      public Publisher<PublishMetadata> newPublisher(long partition) {
        // Delegate to the int version.
        return newPublisher((int) partition);
      }
    });
    return builder.build();
  }
}