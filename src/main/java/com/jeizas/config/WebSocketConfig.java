package com.jeizas.config;

import com.jeizas.websocket.GomokuWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类，用于配置WebSocket处理器和端点
 *
 * @author jeizas
 * @date 2025-11-29
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * 创建五子棋WebSocket处理器Bean
     *
     * @return 五子棋WebSocket处理器实例
     */
    @Bean
    public GomokuWebSocketHandler gomokuWebSocketHandler() {
        return new GomokuWebSocketHandler();
    }

    /**
     * 注册WebSocket处理器
     *
     * @param registry WebSocket处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gomokuWebSocketHandler(), "/gomoku")
                .setAllowedOrigins("*");
    }
}

