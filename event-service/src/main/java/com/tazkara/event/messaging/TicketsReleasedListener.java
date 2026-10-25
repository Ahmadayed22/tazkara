package com.tazkara.event.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazkara.common.messaging.MessagingTopology;
import com.tazkara.common.messaging.TicketsReleasedMessage;
import com.tazkara.event.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class TicketsReleasedListener {

    private static final Logger log = LoggerFactory.getLogger(TicketsReleasedListener.class);

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    public TicketsReleasedListener(ReservationService reservationService, ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = MessagingTopology.TICKETS_RELEASED_QUEUE)
    public void onTicketsReleased(Message message) throws IOException {
        TicketsReleasedMessage payload = objectMapper.readValue(message.getBody(), TicketsReleasedMessage.class);
        log.info("Received tickets-released for booking {} ({})", payload.bookingReference(), payload.reason());
        reservationService.release(payload.bookingReference(), payload.eventId());
    }
}
