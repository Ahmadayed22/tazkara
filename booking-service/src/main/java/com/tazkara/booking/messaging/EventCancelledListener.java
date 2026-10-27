package com.tazkara.booking.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazkara.booking.service.EventCancellationService;
import com.tazkara.common.messaging.EventCancelledMessage;
import com.tazkara.common.messaging.MessagingTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumes EventCancelled messages. If this service was down when the organizer cancelled, the message
 * simply waited in the durable queue and is processed when the service comes back.
 */
@Component
public class EventCancelledListener {

    private static final Logger log = LoggerFactory.getLogger(EventCancelledListener.class);

    private final EventCancellationService eventCancellationService;
    private final ObjectMapper objectMapper;

    public EventCancelledListener(EventCancellationService eventCancellationService, ObjectMapper objectMapper) {
        this.eventCancellationService = eventCancellationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = MessagingTopology.EVENT_CANCELLED_QUEUE)
    public void onEventCancelled(Message message) throws IOException {
        EventCancelledMessage payload = objectMapper.readValue(message.getBody(), EventCancelledMessage.class);
        log.info("Received event-cancelled for event {}", payload.eventId());
        eventCancellationService.cancelBookingsOfEvent(payload.eventId());
    }
}
