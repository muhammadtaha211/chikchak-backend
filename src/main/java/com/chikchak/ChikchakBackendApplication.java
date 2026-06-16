package com.chikchak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling

public class ChikchakBackendApplication {

    public static void main(String[] args) {
        // Takin is an Israel-only app. Pin the whole JVM to Asia/Jerusalem BEFORE Spring
        // boots, so server-generated timestamps use Tel Aviv wall-clock time, matching the
        // frontend-supplied booking times and the device clock. This must run before
        // SpringApplication.run(): Hibernate's @CreationTimestamp generator caches a Clock
        // (Clock.systemDefaultZone()) when the SessionFactory is built, and Hikari/pgjdbc
        // capture the timezone on first connection — all at startup. Setting it here (not in
        // @PostConstruct) guarantees every component sees Asia/Jerusalem. Handles DST too.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jerusalem"));

        SpringApplication.run(ChikchakBackendApplication.class, args);
    }

}
