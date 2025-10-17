package com.example.eventbus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventBusApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventBusApplication.class, args);
    }
}
