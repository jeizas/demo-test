package com.jeizas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        log.error("Hello, World!");
        log.warn("Hello, World!");
        log.info("Hello, World!");
        log.debug("Hello, World!");
        log.trace("Hello, World!");
    }
}
