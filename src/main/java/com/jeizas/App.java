package com.jeizas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Boot应用程序主入口类
 *
 * @author jeizas
 * @date 2025-11-29
 */
@Slf4j
@SpringBootApplication
public class App {
    /**
     * 应用程序启动入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        log.error("Hello, World!");
        log.warn("Hello, World!");
        log.info("Hello, World!");
        log.debug("Hello, World!");
        log.trace("Hello, World!");
    }
}
