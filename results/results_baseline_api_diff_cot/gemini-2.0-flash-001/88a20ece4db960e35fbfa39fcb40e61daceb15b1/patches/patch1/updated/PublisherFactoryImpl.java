/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.Map;
import org.apache.kafka.common.config.ConfigValue;

class PublisherFactoryImpl implements PublisherFactory {

  private static final Framework FRAMEWORK = Framework.of("KAFKA_CONNECT");

  @Override
  public Publisher<?> newPublisher(Map<String, String> params) {
    Map<String, ConfigValue> config = ConfigDefs.config().validateAll(params);
    RoutingPublisherBuilder.Builder builder = RoutingPublisherBuilder.newBuilder();
    String projectValue = "projects/" + config.get(ConfigDefs.PROJECT_FLAG).value();

    String locationValue = config.get(ConfigDefs.LOCATION_FLAG).value().toString();

    String topicNameValue = config.get(ConfigDefs.TOPIC_NAME_FLAG).value().toString();

    TopicPath topic =
        TopicPath.of(
            ProjectName.parse(projectValue),
            CloudZone.parse(locationValue),
            TopicName.of(topicNameValue));
    builder.setTopic(topic);
    builder.setPublisherFactory(
        partition -> {
          SinglePartitionPublisherBuilder.Builder singlePartitionBuilder =
              SinglePartitionPublisherBuilder.newBuilder();
          singlePartitionBuilder.setTopic(topic);
          singlePartitionBuilder.setPartition(partition);
          return singlePartitionBuilder.build();
        });
    return builder.build();
  }
}