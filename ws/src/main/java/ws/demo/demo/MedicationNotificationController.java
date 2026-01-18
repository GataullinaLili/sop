package ws.demo.demo;

import ws.demo.demo.handler.MedicationWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/medications/notifications")
public class MedicationNotificationController {

    private final MedicationWebSocketHandler medicationHandler;

    public MedicationNotificationController(MedicationWebSocketHandler medicationHandler) {
        this.medicationHandler = medicationHandler;
    }

    /**
     * Отправить тестовое уведомление о лекарстве
     */
    @PostMapping("/test/medication")
    public ResponseEntity<Map<String, Object>> sendTestMedicationNotification(
            @RequestParam(defaultValue = "Тестовое лекарство") String name,
            @RequestParam(defaultValue = "test_inn") String inn,
            @RequestParam(defaultValue = "1") Long id) {

        medicationHandler.sendNewMedicationNotification(name, inn, id);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Тестовое уведомление о лекарстве отправлено",
                "medicationName", name,
                "inn", inn,
                "medicationId", id,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Отправить тестовое уведомление о взаимодействии
     */
    @PostMapping("/test/interaction")
    public ResponseEntity<Map<String, Object>> sendTestInteractionNotification(
            @RequestParam(defaultValue = "Тестовое лекарство") String name,
            @RequestParam(defaultValue = "5") int riskLevel,
            @RequestParam(defaultValue = "MEDIUM") String severity,
            @RequestParam(defaultValue = "Требуется наблюдение") String recommendation,
            @RequestParam(defaultValue = "1") Long id) {

        medicationHandler.sendDrugInteractionNotification(name, riskLevel, severity, recommendation, id);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Тестовое уведомление о взаимодействии отправлено",
                "medicationName", name,
                "riskLevel", riskLevel,
                "severity", severity,
                "medicationId", id,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Отправить тестовое уведомление об истечении срока годности
     */
    @PostMapping("/test/expiration")
    public ResponseEntity<Map<String, Object>> sendTestExpirationNotification(
            @RequestParam(defaultValue = "Лекарство с истекшим сроком") String name,
            @RequestParam(defaultValue = "2024-12-31") String expirationDate,
            @RequestParam(defaultValue = "30") int daysLeft,
            @RequestParam(defaultValue = "1") Long id) {

        medicationHandler.sendExpirationNotification(name, expirationDate, id, daysLeft);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Тестовое уведомление об истечении срока отправлено",
                "medicationName", name,
                "expirationDate", expirationDate,
                "daysLeft", daysLeft,
                "medicationId", id,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Отправить тестовое уведомление о низком запасе
     */
    @PostMapping("/test/low-stock")
    public ResponseEntity<Map<String, Object>> sendTestLowStockNotification(
            @RequestParam(defaultValue = "Лекарство с низким запасом") String name,
            @RequestParam(defaultValue = "5") int currentStock,
            @RequestParam(defaultValue = "20") int minStock,
            @RequestParam(defaultValue = "1") Long id) {

        medicationHandler.sendLowStockNotification(name, currentStock, minStock, id);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Тестовое уведомление о низком запасе отправлено",
                "medicationName", name,
                "currentStock", currentStock,
                "minimumStock", minStock,
                "medicationId", id,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Отправить системное сообщение всем клиентам
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastSystemMessage(
            @RequestBody Map<String, String> request) {

        String message = request.getOrDefault("message", "Системное уведомление");
        String level = request.getOrDefault("level", "info");

        medicationHandler.sendSystemNotification("Системное сообщение", message, level);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Системное сообщение отправлено всем клиентам",
                "content", message,
                "level", level,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Отправить персонализированное сообщение пользователю
     */
    @PostMapping("/send-to-user")
    public ResponseEntity<Map<String, Object>> sendToUser(
            @RequestBody Map<String, String> request) {

        String userId = request.get("userId");
        String message = request.getOrDefault("message", "Персональное уведомление");

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Не указан userId",
                    "timestamp", LocalDateTime.now().toString()
            ));
        }

        boolean sent = medicationHandler.sendToUser(userId,
                String.format("{\"type\":\"personal\",\"message\":\"%s\",\"userId\":\"%s\"}",
                        message, userId));

        return ResponseEntity.ok(Map.of(
                "status", sent ? "success" : "warning",
                "message", sent ? "Сообщение отправлено пользователю" : "Пользователь не найден",
                "userId", userId,
                "sent", sent,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Получить статистику системы
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.putAll(medicationHandler.getSystemStats());
        stats.put("status", "success");
        stats.put("service", "medication-notification-service");
        stats.put("uptime", System.currentTimeMillis() - getStartTime());

        return ResponseEntity.ok(stats);
    }

    /**
     * Получить подробную статистику подключений
     */
    @GetMapping("/stats/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedStatistics() {
        Map<String, Object> detailedStats = new HashMap<>();

        // Основная статистика
        detailedStats.put("activeConnections", medicationHandler.getActiveConnections());
        detailedStats.put("totalConnections", medicationHandler.getTotalConnections());

        // Информация о системе
        Runtime runtime = Runtime.getRuntime();
        detailedStats.put("system", Map.of(
                "availableProcessors", runtime.availableProcessors(),
                "freeMemory", runtime.freeMemory(),
                "totalMemory", runtime.totalMemory(),
                "maxMemory", runtime.maxMemory(),
                "memoryUsage", String.format("%.1f%%",
                        (1.0 - (double) runtime.freeMemory() / runtime.totalMemory()) * 100)
        ));

        // Время работы
        detailedStats.put("uptime", System.currentTimeMillis() - getStartTime());
        detailedStats.put("uptimeFormatted", formatUptime(System.currentTimeMillis() - getStartTime()));

        // Информация о сервисе
        detailedStats.put("service", Map.of(
                "name", "Medication Notification Service",
                "version", "1.0.0",
                "status", "RUNNING",
                "timestamp", LocalDateTime.now().toString()
        ));

        return ResponseEntity.ok(detailedStats);
    }

    /**
     * Проверка здоровья сервиса
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean isHealthy = medicationHandler.getActiveConnections() >= 0; // Простая проверка

        Map<String, Object> health = new HashMap<>();
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("service", "medication-notification-service");
        health.put("activeConnections", medicationHandler.getActiveConnections());
        health.put("timestamp", LocalDateTime.now().toString());

        if (!isHealthy) {
            health.put("issues", "Проверьте подключение WebSocket");
        }

        return ResponseEntity.ok(health);
    }

    /**
     * Получить информацию о сервисе
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> serviceInfo() {
        return ResponseEntity.ok(Map.of(
                "service", "Medication Notification Service",
                "description", "Сервис уведомлений в реальном времени для системы управления лекарственными препаратами",
                "version", "1.0.0",
                "developer", "Кафедра программной инженерии",
                "endpoints", Map.of(
                        "websocket", new String[]{
                                "/ws/medications",
                                "/ws/medications/ws",
                                "/ws/medications/admin",
                                "/ws/medications/public"
                        },
                        "rest", new String[]{
                                "POST /api/medications/notifications/broadcast",
                                "POST /api/medications/notifications/send-to-user",
                                "GET /api/medications/notifications/stats",
                                "GET /api/medications/notifications/health"
                        }
                ),
                "supportedNotifications", new String[]{
                        "Новые лекарства",
                        "Обновления лекарств",
                        "Удаление лекарств",
                        "Лекарственные взаимодействия",
                        "Истечение срока годности",
                        "Низкий запас"
                }
        ));
    }

    /**
     * Сбросить статистику (только для администраторов)
     */
    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, Object>> resetStatistics(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {

        // Простая проверка администратора (в реальном приложении нужна полноценная аутентификация)
        if (!"admin-secret-token".equals(adminToken)) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", "error",
                    "message", "Требуется административный доступ",
                    "timestamp", LocalDateTime.now().toString()
            ));
        }

        // В реальном приложении здесь был бы сброс статистики
        // medicationHandler.resetStatistics();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Статистика сброшена (заглушка - в реальном приложении реализовать)",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // Вспомогательные методы
    private static final long START_TIME = System.currentTimeMillis();

    private long getStartTime() {
        return START_TIME;
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        return String.format("%d дней, %d часов, %d минут, %d секунд",
                days, hours % 24, minutes % 60, seconds % 60);
    }
}