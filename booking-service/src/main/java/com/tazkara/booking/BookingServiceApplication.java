
package com.tazkara.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.tazkara.booking",
        "com.tazkara.common.security",
        "com.tazkara.common.web",
        "com.tazkara.common.messaging"
})
@EntityScan(basePackages = {"com.tazkara.booking", "com.tazkara.common.messaging"})
@EnableJpaRepositories(basePackages = {"com.tazkara.booking", "com.tazkara.common.messaging"})
@ConfigurationPropertiesScan
@EnableScheduling
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
