package com.chikchak;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling

public class ChikchakBackendApplication {

    // Takin is an Israel-only app. Pin the whole JVM to Asia/Jerusalem so that
    // server-generated timestamps (@CreationTimestamp, LocalDateTime.now(), schedulers)
    // use Tel Aviv wall-clock time, matching the frontend-supplied booking times and the
    // device clock. This keeps all LocalDateTime values consistent regardless of the host
    // OS timezone (e.g. a UTC cloud host) and handles DST automatically.
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Jerusalem"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ChikchakBackendApplication.class, args);
    }

}
