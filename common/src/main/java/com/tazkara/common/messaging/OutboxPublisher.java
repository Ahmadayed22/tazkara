package com.tazkara.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;


@Component
public class OutboxPublisher {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxPublisher(OutboxRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Stores the message inside the caller's transaction (one must be active). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String routingKey, Object payload) {
        repository.save(new OutboxMessage(routingKey, toJson(payload), clock.instant()));
    }

    /** Stores the message in its own transaction; used for compensations after the caller's transaction rolled back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishInNewTransaction(String routingKey, Object payload) {
        repository.save(new OutboxMessage(routingKey, toJson(payload), clock.instant()));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialise outbox payload " + payload.getClass().getSimpleName(), ex);
        }
    }
}
