package ws.demo.demo;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

public class MedicationHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        // Логирование информации о подключении
        String clientIp = request.getRemoteAddress() != null ?
                request.getRemoteAddress().toString() : "unknown";

        System.out.printf("💊 Попытка подключения к системе медикаментов с IP: %s, URI: %s%n",
                clientIp, request.getURI());

        // Извлечение заголовков для аутентификации (в реальном приложении)
        String authToken = request.getHeaders().getFirst("X-Auth-Token");
        String userId = request.getHeaders().getFirst("X-User-Id");

        // Сохранение атрибутов для использования в WebSocket handler
        if (authToken != null) {
            attributes.put("authToken", authToken);
        }
        if (userId != null) {
            attributes.put("userId", userId);
        }

        attributes.put("clientIp", clientIp);
        attributes.put("connectionTime", System.currentTimeMillis());

        return super.beforeHandshake(request, response, wsHandler, attributes);
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {

        if (exception == null) {
            System.out.println("✅ Рукопожатие WebSocket успешно завершено для: " +
                    request.getURI());
        } else {
            System.err.println("❌ Ошибка рукопожатия WebSocket: " + exception.getMessage());
        }

        super.afterHandshake(request, response, wsHandler, exception);
    }
}