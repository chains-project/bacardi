package com.google.pubsublite.kafka.sink;

import com.google.cloud.pubsublite.internal.Publisher;
import java.util.Map;

interface PublisherFactory {
  Publisher<Void> newPublisher(Map<String, String> params);
}