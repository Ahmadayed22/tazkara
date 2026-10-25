package com.tazkara.common.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;


@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final long CONFIRM_TIMEOUT_MS = 5_000;

    private final OutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public OutboxRelay(OutboxRepository repository, RabbitTemplate rabbitTemplate,
                       TransactionTemplate transactionTemplate, Clock clock) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${tazkara.outbox.relay-interval:PT2S}", initialDelayString = "PT10S")
    public void relayPendingMessages() {
        List<OutboxMessage> pending = repository.findTop50ByPublishedAtIsNullOrderByIdAsc();
        for (OutboxMessage message : pending) {
            if (!send(message)) {
                return;
            }
        }
    }

    private boolean send(OutboxMessage outboxMessage) {
        try {
            Message amqpMessage = MessageBuilder
                    .withBody(outboxMessage.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setMessageId("outbox-" + outboxMessage.getId())
                    .build();
            rabbitTemplate.invoke(operations -> {
                operations.send(MessagingTopology.EXCHANGE, outboxMessage.getRoutingKey(), amqpMessage);
                operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
                return null;
            });
            transactionTemplate.executeWithoutResult(status -> {
                outboxMessage.markPublished(clock.instant());
                repository.save(outboxMessage);
            });
            log.debug("Published outbox message {} with routing key {}", outboxMessage.getId(), outboxMessage.getRoutingKey());
            return true;
        } catch (AmqpException ex) {
            if (outboxMessage.getAttempts() % 30 == 0) {
                log.warn("Could not publish outbox message {} ({}), will retry: {}",
                        outboxMessage.getId(), outboxMessage.getRoutingKey(), ex.getMessage());
            }
            transactionTemplate.executeWithoutResult(status -> {
                outboxMessage.markFailedAttempt();
                repository.save(outboxMessage);
            });
            return false;
        }
    }
}
