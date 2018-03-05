package com.telenordigital.prime.analytics;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Timestamps;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import com.telenordigital.prime.ocs.DataTrafficInfo;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static com.telenordigital.prime.disruptor.PrimeEventMessageType.CREDIT_CONTROL_REQUEST;

/**
 * This class publishes the data consumption information events to the Google Cloud Pub/Sub.
 */
public class DataConsumptionInfoPublisher implements EventHandler<PrimeEvent>, Managed {

    private static final Logger LOG = LoggerFactory.getLogger(DataConsumptionInfoPublisher.class);

    private final String projectId;
    private final String topicId;

    public DataConsumptionInfoPublisher(String projectId, String topicId) {
        this.projectId = projectId;
        this.topicId = topicId;
    }

    private Publisher publisher = null;

    @Override
    public void start() throws Exception {

        TopicName topicName = TopicName.of(projectId, topicId);

        // Create a publisher instance with default settings bound to the topic
        publisher = Publisher.newBuilder(topicName).build();
    }

    @Override
    public void stop() throws Exception {
        if (publisher != null) {
            // When finished with the publisher, shutdown to free up resources.
            publisher.shutdown();
        }
    }

    @Override
    public void onEvent(
            final PrimeEvent event,
            final long sequence,
            final boolean endOfBatch) throws Exception {

        if (event.getMessageType() != CREDIT_CONTROL_REQUEST) {
            return;
        }

        // FiXMe : We only report the requested bucket. Should probably report the Used-Units instead
        final ByteString data = DataTrafficInfo.newBuilder()
                .setMsisdn(event.getMsisdn())
                .setBucketBytes(event.getRequestedBucketBytes())
                .setBundleBytes(event.getBundleBytes())
                .setTimestamp(Timestamps.fromMillis((Instant.now().toEpochMilli())))
                .build()
                .toByteString();

        final PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(data)
                .build();

        //schedule a message to be published, messages are automatically batched
        ApiFuture<String> future = publisher.publish(pubsubMessage);

        // add an asynchronous callback to handle success / failure
        ApiFutures.addCallback(future, new ApiFutureCallback<String>() {

            @Override
            public void onFailure(Throwable throwable) {
                if (throwable instanceof ApiException) {
                    ApiException apiException = ((ApiException) throwable);
                    // details on the API exception
                    LOG.warn("Status code: {}", apiException.getStatusCode().getCode());
                    LOG.warn("Retrying: {}", apiException.isRetryable());
                }
                LOG.warn("Error publishing message for msisdn: {}", event.getMsisdn());
            }

            @Override
            public void onSuccess(String messageId) {
                // Once published, returns server-assigned message ids (unique within the topic)
                LOG.debug(messageId);
            }
        });
    }
}
