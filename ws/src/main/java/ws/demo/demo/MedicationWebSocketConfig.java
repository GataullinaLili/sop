package ws.demo.demo;

import ws.demo.demo.handler.MedicationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class MedicationWebSocketConfig implements WebSocketConfigurer {

    private final MedicationWebSocketHandler medicationHandler;

    public MedicationWebSocketConfig(MedicationWebSocketHandler medicationHandler) {
        this.medicationHandler = medicationHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Основной endpoint для уведомлений о лекарствах с SockJS fallback
        registry.addHandler(medicationHandler, "/ws/medications")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .addInterceptors(new MedicationHandshakeInterceptor())
                .setAllowedOriginPatterns("*") // Для разработки
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000);

        // WebSocket без SockJS для современных браузеров
        registry.addHandler(medicationHandler, "/ws/medications/ws")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .addInterceptors(new MedicationHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        // Endpoint для административных уведомлений
        registry.addHandler(medicationHandler, "/ws/medications/admin")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .addInterceptors(new MedicationHandshakeInterceptor())
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Endpoint для публичных уведомлений (только чтение)
        registry.addHandler(medicationHandler, "/ws/medications/public")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .addInterceptors(new MedicationHandshakeInterceptor())
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}