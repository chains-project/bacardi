package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.PublishInfo; // Assuming PublishMetadata is replaced by PublishInfo
import com.google.cloud.pubsublite.internal.Publisher;
import java.util.Map;

interface PublisherFactory {

  Publisher<PublishInfo> newPublisher(Map<String, String> params); // Update the type parameter
}