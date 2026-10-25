package com.tazkara.event;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.tazkara.event",
        "com.tazkara.common.security",
        "com.tazkara.common.web",
        "com.tazkara.common.messaging"
})
@EntityScan(basePackages = {"com.tazkara.event", "com.tazkara.common.messaging"})
@EnableJpaRepositories(basePackages = {"com.tazkara.event", "com.tazkara.common.messaging"})
@EnableScheduling
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
