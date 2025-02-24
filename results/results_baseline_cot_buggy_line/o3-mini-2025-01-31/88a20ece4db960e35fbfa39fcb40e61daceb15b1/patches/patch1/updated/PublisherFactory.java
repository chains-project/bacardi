package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.internal.PublishMetadata;
import com.google.cloud.pubsublite.internal.Publisher;
import java.util.Map;

interface PublisherFactory {

  Publisher<PublishMetadata> newPublisher(Map<String, String> params);
}