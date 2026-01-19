package com.pharmacy.notification.config;

import com.pharmacy.notification.handler.MedicationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MedicationWebSocketHandler medicationHandler;

    public WebSocketConfig(MedicationWebSocketHandler medicationHandler) {
        this.medicationHandler = medicationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(medicationHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*");

        // Для отладки, добавим альтернативный путь
        registry.addHandler(medicationHandler, "/ws/medication-notifications")
                .setAllowedOriginPatterns("*");
    }
}