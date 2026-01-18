package ws.demo.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "websocket.medications")
public class WebSocketProperties {

    private int maxTextMessageBufferSize = 8192;
    private int maxBinaryMessageBufferSize = 8192;
    private long maxSessionIdleTimeout = 300000; // 5 минут
    private int heartbeatInterval = 30000; // 30 секунд
    private boolean allowOriginsAll = true;
    private String[] allowedOrigins = {"http://localhost:3000", "http://localhost:8080"};

    // Геттеры и сеттеры
    public int getMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }

    public void setMaxTextMessageBufferSize(int maxTextMessageBufferSize) {
        this.maxTextMessageBufferSize = maxTextMessageBufferSize;
    }

    public int getMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }

    public void setMaxBinaryMessageBufferSize(int maxBinaryMessageBufferSize) {
        this.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize;
    }

    public long getMaxSessionIdleTimeout() {
        return maxSessionIdleTimeout;
    }

    public void setMaxSessionIdleTimeout(long maxSessionIdleTimeout) {
        this.maxSessionIdleTimeout = maxSessionIdleTimeout;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public boolean isAllowOriginsAll() {
        return allowOriginsAll;
    }

    public void setAllowOriginsAll(boolean allowOriginsAll) {
        this.allowOriginsAll = allowOriginsAll;
    }

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}