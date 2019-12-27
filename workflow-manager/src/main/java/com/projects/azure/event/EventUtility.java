package com.projects.azure.event;

import com.microsoft.azure.eventgrid.EventGridClient;
import com.microsoft.azure.eventgrid.TopicCredentials;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.Arrays;

import java.util.UUID;

public final class EventUtility {
    public static void publish(final EventNotification eventNotification, final String topicKey, final String topicEndpoint) {
        try {
            final EventGridClient client = new EventGridClientImpl(new TopicCredentials(topicKey));
            final String topicEndpointURI = String.format("https://%s/", new URI(topicEndpoint).getHost());
            client.publishEvents(topicEndpointURI, Arrays.asList(new EventGridEvent(UUID.randomUUID().toString(), "subject", eventNotification, eventNotification.getType().toString(), DateTime.now(), "2.0")));
        } catch (final Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
