package ws.demo.demo.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MedicationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MedicationWebSocketHandler.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Множество активных WebSocket сессий
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    // Статистика подключений
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    // Хранилище пользовательских данных (если нужно идентифицировать пользователей)
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();

        log.info("💊 Новое подключение к системе медикаментов: id={}, адрес={}, всего активных: {}",
                session.getId(), session.getRemoteAddress(), activeConnections.get());

        // Отправляем приветственное сообщение
        sendWelcomeMessage(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("📨 Сообщение от сессии {}: {}", session.getId(), payload);

        try {
            // Обработка команд от клиента
            handleClientCommand(session, payload);
        } catch (Exception e) {
            log.error("❌ Ошибка обработки сообщения от сессии {}: {}", session.getId(), e.getMessage());
            sendErrorMessage(session, "Ошибка обработки команды: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        userSessions.remove(session.getId());
        activeConnections.decrementAndGet();

        log.info("🔌 Отключение от системы медикаментов: id={}, причина={}, код={}, осталось активных: {}",
                session.getId(), status.getReason(), status.getCode(), activeConnections.get());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("🚨 Ошибка транспорта WebSocket для сессии {}: {}",
                session.getId(), exception.getMessage());

        sessions.remove(session);
        userSessions.remove(session.getId());
        if (activeConnections.get() > 0) {
            activeConnections.decrementAndGet();
        }
    }

    /**
     * Отправить уведомление о новом лекарстве
     */
    public void sendNewMedicationNotification(String medicationName, String inn, Long medicationId) {
        String notification = createMedicationNotification(
                "Новое лекарство",
                String.format("💊 %s добавлено в систему", medicationName),
                String.format("МНН: %s | ID: %d", inn, medicationId),
                "medication-created",
                medicationId
        );

        broadcast(notification);
        log.info("📤 Уведомление о новом лекарстве отправлено: {}", medicationName);
    }

    /**
     * Отправить уведомление об обновлении лекарства
     */
    public void sendUpdatedMedicationNotification(String oldName, String newName,
                                                  String oldInn, String newInn,
                                                  Long medicationId) {
        String notification = createMedicationNotification(
                "Обновление лекарства",
                String.format("✏️ %s → %s", oldName, newName),
                String.format("МНН: %s → %s | ID: %d", oldInn, newInn, medicationId),
                "medication-updated",
                medicationId
        );

        broadcast(notification);
        log.info("📤 Уведомление об обновлении лекарства отправлено: ID={}", medicationId);
    }

    /**
     * Отправить уведомление об удалении лекарства
     */
    public void sendDeletedMedicationNotification(String medicationName, String inn,
                                                  Long medicationId, String manufacturerName) {
        String notification = createMedicationNotification(
                "Удаление лекарства",
                String.format("🗑️ %s удалено из системы", medicationName),
                String.format("МНН: %s | Производитель: %s | ID: %d",
                        inn, manufacturerName, medicationId),
                "medication-deleted",
                medicationId
        );

        broadcast(notification);
        log.info("📤 Уведомление об удалении лекарства отправлено: {}", medicationName);
    }

    /**
     * Отправить уведомление о лекарственном взаимодействии
     */
    public void sendDrugInteractionNotification(String medicationName, int riskLevel,
                                                String severity, String recommendation,
                                                Long medicationId) {
        String severityEmoji = getSeverityEmoji(severity);
        String notification = createDrugInteractionNotification(
                "Проверка взаимодействий",
                String.format("%s %s", severityEmoji, medicationName),
                String.format("Уровень риска: %d/10 (%s)", riskLevel, severity),
                recommendation,
                severity,
                medicationId
        );

        broadcast(notification);

        // Отдельное уведомление для критических взаимодействий
        if ("HIGH".equalsIgnoreCase(severity) || riskLevel > 7) {
            String criticalNotification = createSystemNotification(
                    "critical-alert",
                    String.format("🚨 КРИТИЧЕСКОЕ ВЗАИМОДЕЙСТВИЕ: %s", medicationName),
                    "critical"
            );
            broadcast(criticalNotification);
            log.warn("⚠️ Критическое взаимодействие обнаружено для: {}", medicationName);
        }

        log.info("📤 Уведомление о взаимодействиях отправлено: {}", medicationName);
    }

    /**
     * Отправить уведомление об истечении срока годности
     */
    public void sendExpirationNotification(String medicationName, String expirationDate,
                                           Long medicationId, int daysLeft) {
        String notification = createSystemNotification(
                "expiration-warning",
                String.format("⏰ Срок годности истекает: %s", medicationName),
                "warning",
                Map.of(
                        "expirationDate", expirationDate,
                        "daysLeft", daysLeft,
                        "medicationId", medicationId
                )
        );

        broadcast(notification);
        log.warn("⏰ Уведомление об истечении срока годности отправлено: {} (осталось {} дней)",
                medicationName, daysLeft);
    }

    /**
     * Отправить уведомление о низком запасе лекарства
     */
    public void sendLowStockNotification(String medicationName, int currentStock,
                                         int minStock, Long medicationId) {
        String notification = createSystemNotification(
                "low-stock",
                String.format("📉 Низкий запас: %s", medicationName),
                "warning",
                Map.of(
                        "currentStock", currentStock,
                        "minStock", minStock,
                        "medicationId", medicationId
                )
        );

        broadcast(notification);
        log.warn("📉 Уведомление о низком запасе отправлено: {} (осталось: {}/{})",
                medicationName, currentStock, minStock);
    }

    /**
     * Отправить системное уведомление
     */
    public void sendSystemNotification(String title, String message, String level) {
        String notification = createSystemNotification("system", title, level, message);
        broadcast(notification);
        log.info("📢 Системное уведомление отправлено: {}", title);
    }

    /**
     * Рассылка сообщения всем подключенным клиентам.
     * Возвращает количество успешно отправленных сообщений.
     */
    public int broadcast(String message) {
        TextMessage textMessage = new TextMessage(message);
        int sent = 0;

        for (WebSocketSession session : sessions) {
            if (sendMessage(session, textMessage)) {
                sent++;
            }
        }

        log.info("📡 Broadcast: отправлено {}/{} клиентам", sent, sessions.size());
        return sent;
    }

    /**
     * Отправить сообщение конкретному пользователю (если идентифицирован)
     */
    public boolean sendToUser(String userId, String message) {
        // Поиск сессии по userId (нужно реализовать маппинг userId → session)
        for (WebSocketSession session : sessions) {
            String sessionUserId = userSessions.get(session.getId());
            if (userId.equals(sessionUserId)) {
                return sendMessage(session, new TextMessage(message));
            }
        }
        return false;
    }

    /**
     * Отправить приветственное сообщение при подключении
     */
    private void sendWelcomeMessage(WebSocketSession session) {
        String welcomeMessage = createSystemNotification(
                "system",
                "Добро пожаловать в систему мониторинга лекарственных препаратов!",
                "info"
        );

        sendMessage(session, new TextMessage(welcomeMessage));

        // Отправляем информацию о доступных командах
        String helpMessage = createSystemNotification(
                "system",
                "Доступные команды: {\"command\": \"ping\"}, {\"command\": \"stats\"}",
                "info"
        );

        sendMessage(session, new TextMessage(helpMessage));
    }

    /**
     * Отправить сообщение об ошибке
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        String error = createSystemNotification(
                "error",
                errorMessage,
                "error"
        );

        sendMessage(session, new TextMessage(error));
    }

    /**
     * Обработка команд от клиента
     */
    private void handleClientCommand(WebSocketSession session, String payload) {
        try {
            // Простая обработка JSON команд
            if (payload.trim().startsWith("{")) {
                // В реальном приложении используйте Jackson ObjectMapper
                if (payload.contains("\"command\":\"ping\"")) {
                    sendPongMessage(session);
                } else if (payload.contains("\"command\":\"stats\"")) {
                    sendStatsMessage(session);
                } else if (payload.contains("\"command\":\"identify\"")) {
                    handleUserIdentification(session, payload);
                }
            } else {
                // Текстовые команды
                switch (payload.trim().toLowerCase()) {
                    case "ping":
                        sendPongMessage(session);
                        break;
                    case "stats":
                        sendStatsMessage(session);
                        break;
                    case "help":
                        sendHelpMessage(session);
                        break;
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось обработать команду от клиента {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * Обработка идентификации пользователя
     */
    private void handleUserIdentification(WebSocketSession session, String payload) {
        // Простая реализация - в реальном приложении нужна аутентификация
        try {
            // Извлекаем userId из payload
            String userId = extractUserId(payload);
            if (userId != null && !userId.isEmpty()) {
                userSessions.put(session.getId(), userId);
                sendMessage(session, new TextMessage(createSystemNotification(
                        "system",
                        "Идентификация успешна. Привет, пользователь " + userId + "!",
                        "success"
                )));
                log.info("👤 Пользователь идентифицирован: sessionId={}, userId={}",
                        session.getId(), userId);
            }
        } catch (Exception e) {
            log.warn("Ошибка идентификации пользователя: {}", e.getMessage());
        }
    }

    /**
     * Отправить статистику клиенту
     */
    private void sendStatsMessage(WebSocketSession session) {
        String statsMessage = createSystemNotification(
                "stats",
                "Статистика системы медикаментов",
                "info",
                Map.of(
                        "activeConnections", activeConnections.get(),
                        "totalConnections", totalConnections.get(),
                        "timestamp", LocalDateTime.now().format(TIME_FORMATTER),
                        "serverTime", System.currentTimeMillis()
                )
        );

        sendMessage(session, new TextMessage(statsMessage));
    }

    /**
     * Отправить сообщение PONG
     */
    private void sendPongMessage(WebSocketSession session) {
        String pongMessage = createSystemNotification(
                "pong",
                "pong",
                "info",
                Map.of("timestamp", LocalDateTime.now().format(TIME_FORMATTER))
        );

        sendMessage(session, new TextMessage(pongMessage));
    }

    /**
     * Отправить справочное сообщение
     */
    private void sendHelpMessage(WebSocketSession session) {
        String helpMessage = createSystemNotification(
                "help",
                "Доступные команды:",
                "info",
                Map.of(
                        "commands", new String[]{
                                "ping - Проверка соединения",
                                "stats - Статистика системы",
                                "help - Эта справка"
                        },
                        "supportedEvents", new String[]{
                                "medication-created - Новые лекарства",
                                "medication-updated - Обновления",
                                "medication-deleted - Удаления",
                                "drug-interaction - Взаимодействия",
                                "expiration-warning - Истечение срока",
                                "low-stock - Низкий запас"
                        }
                )
        );

        sendMessage(session, new TextMessage(helpMessage));
    }

    /**
     * Отправить сообщение конкретной сессии
     */
    private boolean sendMessage(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            sessions.remove(session);
            userSessions.remove(session.getId());
            return false;
        }
        try {
            session.sendMessage(message);
            return true;
        } catch (IOException e) {
            log.warn("Ошибка отправки в сессию {}: {}", session.getId(), e.getMessage());
            sessions.remove(session);
            userSessions.remove(session.getId());
            return false;
        }
    }

    /**
     * Создать уведомление о лекарстве
     */
    private String createMedicationNotification(String type, String title,
                                                String description,
                                                String notificationType,
                                                Long medicationId) {
        return String.format("""
            {
                "type": "%s",
                "timestamp": "%s",
                "title": "%s",
                "description": "%s",
                "notificationType": "%s",
                "medicationId": %d,
                "priority": "%s"
            }
            """,
                notificationType,
                LocalDateTime.now().format(TIME_FORMATTER),
                escapeJson(title),
                escapeJson(description),
                notificationType,
                medicationId,
                notificationType.equals("critical-alert") ? "high" : "normal"
        );
    }

    /**
     * Создать уведомление о взаимодействиях
     */
    private String createDrugInteractionNotification(String type, String title,
                                                     String riskInfo,
                                                     String recommendation,
                                                     String severity,
                                                     Long medicationId) {
        return String.format("""
            {
                "type": "drug-interaction",
                "timestamp": "%s",
                "title": "%s",
                "riskInfo": "%s",
                "recommendation": "%s",
                "severity": "%s",
                "medicationId": %d,
                "priority": "%s"
            }
            """,
                LocalDateTime.now().format(TIME_FORMATTER),
                escapeJson(title),
                escapeJson(riskInfo),
                escapeJson(recommendation),
                severity,
                medicationId,
                severity.equalsIgnoreCase("HIGH") ? "high" : "normal"
        );
    }

    /**
     * Создать системное уведомление
     */
    private String createSystemNotification(String notificationType,
                                            String message,
                                            String level) {
        return createSystemNotification(notificationType, message, level, null);
    }

    private String createSystemNotification(String notificationType,
                                            String message,
                                            String level,
                                            Object data) {
        String dataJson = "{}";
        if (data != null) {
            if (data instanceof Map) {
                dataJson = simpleMapToJson((Map<?, ?>) data);
            } else if (data instanceof String) {
                dataJson = String.format("\"%s\"", escapeJson((String) data));
            }
        }

        return String.format("""
            {
                "type": "%s",
                "timestamp": "%s",
                "message": "%s",
                "level": "%s",
                "data": %s
            }
            """,
                notificationType,
                LocalDateTime.now().format(TIME_FORMATTER),
                escapeJson(message),
                level,
                dataJson
        );
    }

    /**
     * Получить эмодзи для уровня серьезности
     */
    private String getSeverityEmoji(String severity) {
        return switch (severity.toUpperCase()) {
            case "HIGH" -> "🚨";
            case "MEDIUM" -> "⚠️";
            case "LOW" -> "ℹ️";
            default -> "📋";
        };
    }

    /**
     * Экранирование строк для JSON
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Простое преобразование Map в JSON (для упрощения)
     */
    private String simpleMapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }

            first = false;
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * Извлечение userId из payload (упрощенная реализация)
     */
    private String extractUserId(String payload) {
        // В реальном приложении используйте парсинг JSON
        if (payload.contains("\"userId\":")) {
            int start = payload.indexOf("\"userId\":\"") + 10;
            int end = payload.indexOf("\"", start);
            if (start > 9 && end > start) {
                return payload.substring(start, end);
            }
        }
        return null;
    }

    /** Количество активных подключений */
    public int getActiveConnections() {
        return activeConnections.get();
    }

    /** Общее количество подключений */
    public int getTotalConnections() {
        return totalConnections.get();
    }

    /** Получить статистику системы */
    public Map<String, Object> getSystemStats() {
        return Map.of(
                "activeConnections", activeConnections.get(),
                "totalConnections", totalConnections.get(),
                "identifiedUsers", userSessions.size(),
                "timestamp", LocalDateTime.now().format(TIME_FORMATTER)
        );
    }
}