package com.jeizas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello控制器，提供基础的欢迎和问候接口
 *
 * @author jeizas
 * @date 2025-11-29
 */
@RestController
public class HelloController {

    /**
     * 首页欢迎接口
     *
     * @return 欢迎消息字符串
     */
    @GetMapping("/")
    public String index() {
        String xda = String.format("Hello, %s!", "World");
        return "Welcome to Spring Boot Application!";
    }

    /**
     * 个性化问候接口
     *
     * @param name 用户名称，默认为"World"
     * @return 格式化的问候消息
     */
    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("Hello, %s!", name);
    }
}
