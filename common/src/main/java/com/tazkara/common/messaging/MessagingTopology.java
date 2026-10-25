package com.tazkara.common.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class MessagingTopology {

    /** Topic exchange carrying all integration events. */
    public static final String EXCHANGE = "tazkara.events";
    /** Dead-letter exchange: messages that still fail after retries end up in a *.dlq queue. */
    public static final String DEAD_LETTER_EXCHANGE = "tazkara.events.dlx";

    /** Published by the Event Service when an organizer cancels a published event. */
    public static final String EVENT_CANCELLED = "event.cancelled";
    /** Published by the Booking Service when held or sold tickets must go back to availability. */
    public static final String TICKETS_RELEASED = "booking.tickets-released";

    /** Consumed by the Booking Service. */
    public static final String EVENT_CANCELLED_QUEUE = "booking-service.event-cancelled";
    /** Consumed by the Event Service. */
    public static final String TICKETS_RELEASED_QUEUE = "event-service.tickets-released";

    @Bean
    public Declarables tazkaraTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        List<Declarable> declarables = new ArrayList<>(List.of(exchange, deadLetterExchange));
        declarables.addAll(queueWithDeadLetter(EVENT_CANCELLED_QUEUE, EVENT_CANCELLED, exchange, deadLetterExchange));
        declarables.addAll(queueWithDeadLetter(TICKETS_RELEASED_QUEUE, TICKETS_RELEASED, exchange, deadLetterExchange));
        return new Declarables(declarables);
    }

    private static List<Declarable> queueWithDeadLetter(String queueName, String routingKey,
                                                        TopicExchange exchange, DirectExchange deadLetterExchange) {
        String deadLetterQueueName = queueName + ".dlq";
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(deadLetterQueueName)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(deadLetterQueueName).build();
        return List.of(
                queue,
                deadLetterQueue,
                BindingBuilder.bind(queue).to(exchange).with(routingKey),
                BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterQueueName));
    }
}
