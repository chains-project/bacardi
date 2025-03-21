<repair_strategy>
1. Remove the import for `PublishMetadata` as it no longer exists.
2. Remove the `PublishMetadata` type from the `newPublisher` method signature.
3. Remove the `setContext` method calls as they no longer exist.
</repair_strategy>

package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.CloudZone;
import com.google.cloud.pubsublite.ProjectPath;
import com.google.cloud.pubsublite.TopicName;
import com.google.cloud.pubsublite.TopicPath;
import com.google.cloud.pubsublite.internal.Publisher;
import com.google.cloud.pubsublite.internal.wire.PubsubContext;
import com.google.cloud.pubsublite.internal.wire.RoutingPublisherBuilder;
import com.google.cloud.pubsublite.internal.wire.SinglePartitionPublisherBuilder;
import java.util.Map;
import org.apache.kafka.common.config.ConfigValue;

class PublisherFactoryImpl implements PublisherFactory {

  private static final Framework FRAMEWORK = Framework.of("KAFKA_CONNECT");

  @Override
  public Publisher newPublisher(Map<String, String> params) {
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
    builder.setPublisherFactory(
        partition ->
            SinglePartitionPublisherBuilder.newBuilder()
                .setTopic(topic)
                .setPartition(partition)
                .build());
    return builder.build();
  }
}
