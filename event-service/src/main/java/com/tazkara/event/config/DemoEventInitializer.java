package com.tazkara.event.config;

import com.tazkara.event.domain.Event;
import com.tazkara.event.domain.EventDetails;
import com.tazkara.event.domain.TicketTypeSpec;
import com.tazkara.event.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@ConditionalOnProperty(name = "tazkara.demo.enabled", havingValue = "true")
public class DemoEventInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoEventInitializer.class);

    private final EventRepository eventRepository;
    private final Clock clock;
    private final Long organizerId;

    public DemoEventInitializer(EventRepository eventRepository, Clock clock,
                                @Value("${tazkara.demo.organizer-id}") Long organizerId) {
        this.eventRepository = eventRepository;
        this.clock = clock;
        this.organizerId = organizerId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (eventRepository.count() > 0) {
            return;
        }
        Instant now = clock.instant();
        Instant startsAt = now.plus(Duration.ofDays(30)).truncatedTo(ChronoUnit.HOURS);
        Event event = Event.createDraft(organizerId,
                new EventDetails("Tazkara Launch Concert",
                        "Demo event: an open-air concert celebrating the launch of Tazkara.",
                        "Roman Theatre", "Amman", startsAt),
                List.of(new TicketTypeSpec("Standard", new BigDecimal("25.00"), 200),
                        new TicketTypeSpec("VIP", new BigDecimal("75.00"), 20)),
                now);
        event.publish(now);
        eventRepository.save(event);
        log.info("Demo event {} created and published", event.getId());
    }
}
