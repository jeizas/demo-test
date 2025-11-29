package com.jeizas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应用程序测试类
 *
 * @author jeizas
 * @date 2025-11-29
 */
@SpringBootTest
public class AppTest {

    /**
     * 测试Spring上下文是否正常加载
     */
    @Test
    public void contextLoads() {
        assertTrue(true, "Spring context should load successfully");
    }
}
