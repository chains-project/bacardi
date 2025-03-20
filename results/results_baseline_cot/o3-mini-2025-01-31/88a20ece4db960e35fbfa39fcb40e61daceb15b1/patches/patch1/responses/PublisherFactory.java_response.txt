package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.TopicPublishMetadata;
import com.google.cloud.pubsublite.internal.Publisher;
import java.util.Map;

interface PublisherFactory {

  Publisher<TopicPublishMetadata> newPublisher(Map<String, String> params);
}