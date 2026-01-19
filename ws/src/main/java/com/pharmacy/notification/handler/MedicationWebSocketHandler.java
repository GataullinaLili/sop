package com.pharmacy.notification.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@SuppressWarnings("unchecked")
public class MedicationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MedicationWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    // Хранилище для учета количества препаратов
    private final Map<String, AtomicInteger> medicationStock = new ConcurrentHashMap<>();

    // Счетчик для уникальных ID
    private final AtomicInteger medicationCounter = new AtomicInteger(1);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Новое подключение WebSocket для уведомлений о медикаментах: id={}, всего активных: {}",
                session.getId(), sessions.size());

        // Отправляем приветственное сообщение и текущее состояние
        try {
            Map<String, Object> welcomeMessage = new HashMap<>();
            welcomeMessage.put("type", "CONNECTION_ESTABLISHED");
            welcomeMessage.put("message", "Добро пожаловать в систему уведомлений для аптеки");
            welcomeMessage.put("timestamp", System.currentTimeMillis());
            welcomeMessage.put("activeConnections", sessions.size());
            welcomeMessage.put("totalMedications", medicationStock.size());

            // Отправляем информацию о текущем состоянии склада
            if (!medicationStock.isEmpty()) {
                Map<String, Integer> currentStock = new HashMap<>();
                medicationStock.forEach((name, count) -> currentStock.put(name, count.get()));
                welcomeMessage.put("currentStock", currentStock);
            }

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcomeMessage)));
        } catch (IOException e) {
            log.error("Ошибка отправки приветственного сообщения: {}", e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.info("Сообщение от клиента {}: {}", session.getId(), payload);

            // Обработка PING сообщений
            if ("PING".equals(payload) || payload.contains("\"type\":\"PING\"")) {
                Map<String, Object> pongResponse = new HashMap<>();
                pongResponse.put("type", "PONG");
                pongResponse.put("timestamp", System.currentTimeMillis());
                pongResponse.put("serverTime", System.currentTimeMillis());
                sendMessage(session, pongResponse);
                return;
            }

            // Пытаемся распарсить как JSON
            try {
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                String type = (String) data.get("type");

                if ("MEDICATION_OPERATION".equals(type)) {
                    // Обработка операций с медикаментами
                    handleMedicationOperation(session, data);
                } else if ("GET_STOCK_INFO".equals(type)) {
                    // Запрос информации о складе
                    sendStockInfo(session);
                }
            } catch (JsonProcessingException e) {
                log.debug("Сообщение не является JSON: {}", payload);
            }

        } catch (Exception e) {
            log.error("Ошибка обработки сообщения от сессии {}: {}",
                    session.getId(), e.getMessage());
        }
    }

    private void handleMedicationOperation(WebSocketSession session, Map<String, Object> data) {
        String operation = (String) data.get("operation");
        String medicationName = (String) data.getOrDefault("name", "Неизвестный препарат");

        Map<String, Object> response = new HashMap<>();
        response.put("type", "MEDICATION_OPERATION_RESULT");
        response.put("operation", operation);
        response.put("medicationName", medicationName);
        response.put("timestamp", System.currentTimeMillis());

        switch(operation) {
            case "ADD":
                handleAddMedication(medicationName, response);
                break;
            case "REMOVE":
                handleRemoveMedication(medicationName, response);
                break;
            case "UPDATE":
                handleUpdateMedication(medicationName, response);
                break;
            case "GET":
                handleGetMedication(medicationName, response);
                break;
            default:
                response.put("status", "ERROR");
                response.put("message", "Неизвестная операция: " + operation);
        }

        sendMessage(session, response);

        // Отправляем обновленную информацию всем клиентам
        if (!"ERROR".equals(response.get("status"))) {
            broadcastStockUpdate();
        }
    }

    private void handleAddMedication(String medicationName, Map<String, Object> response) {
        AtomicInteger count = medicationStock.computeIfAbsent(medicationName,
                k -> new AtomicInteger(0));

        // Генерируем случайное количество для нового препарата (от 5 до 50)
        int randomQuantity = 5 + (int)(Math.random() * 46);
        count.set(randomQuantity);

        int medicationId = medicationCounter.getAndIncrement();

        response.put("status", "SUCCESS");
        response.put("message", "Препарат добавлен");
        response.put("medicationId", medicationId);
        response.put("quantity", count.get());
        response.put("operationId", "ADD_" + System.currentTimeMillis());

        // Создаем уведомление для всех клиентов
        Map<String, Object> notification = createMedicationNotification(
                "NEW_MEDICATION", medicationName, count.get(), medicationId);
        broadcast(notification);
    }

    private void handleRemoveMedication(String medicationName, Map<String, Object> response) {
        AtomicInteger count = medicationStock.get(medicationName);

        if (count == null) {
            response.put("status", "ERROR");
            response.put("message", "Препарат не найден: " + medicationName);
            return;
        }

        int currentCount = count.get();
        if (currentCount <= 0) {
            response.put("status", "ERROR");
            response.put("message", "Нет доступных единиц препарата: " + medicationName);
            return;
        }

        // Уменьшаем количество на 1
        int newCount = count.decrementAndGet();

        response.put("status", "SUCCESS");
        response.put("message", "Препарат удален (1 единица)");
        response.put("previousQuantity", currentCount);
        response.put("newQuantity", newCount);
        response.put("operationId", "REMOVE_" + System.currentTimeMillis());

        // Если препарат закончился, удаляем его из хранилища
        if (newCount <= 0) {
            medicationStock.remove(medicationName);
            response.put("note", "Препарат полностью удален со склада");

            // Уведомление об удалении
            Map<String, Object> notification = createMedicationNotification(
                    "MEDICATION_REMOVED", medicationName, 0, -1);
            broadcast(notification);
        } else {
            // Уведомление об уменьшении количества
            Map<String, Object> notification = createMedicationNotification(
                    "MEDICATION_UPDATED", medicationName, newCount, -1);
            broadcast(notification);
        }
    }

    private void handleUpdateMedication(String medicationName, Map<String, Object> response) {
        AtomicInteger count = medicationStock.get(medicationName);

        if (count == null) {
            response.put("status", "ERROR");
            response.put("message", "Препарат не найден: " + medicationName);
            return;
        }

        // Генерируем случайное обновление количества
        int randomChange = -10 + (int)(Math.random() * 21); // от -10 до +10
        int oldCount = count.get();
        int newCount = Math.max(0, oldCount + randomChange);
        count.set(newCount);

        response.put("status", "SUCCESS");
        response.put("message", "Количество препарата обновлено");
        response.put("previousQuantity", oldCount);
        response.put("newQuantity", newCount);
        response.put("change", randomChange);
        response.put("operationId", "UPDATE_" + System.currentTimeMillis());

        // Уведомление об обновлении
        Map<String, Object> notification = createMedicationNotification(
                "MEDICATION_UPDATED", medicationName, newCount, -1);
        broadcast(notification);
    }

    private void handleGetMedication(String medicationName, Map<String, Object> response) {
        AtomicInteger count = medicationStock.get(medicationName);

        if (count == null) {
            response.put("status", "ERROR");
            response.put("message", "Препарат не найден: " + medicationName);
            return;
        }

        response.put("status", "SUCCESS");
        response.put("message", "Информация о препарате");
        response.put("quantity", count.get());
        response.put("operationId", "GET_" + System.currentTimeMillis());
    }

    private Map<String, Object> createMedicationNotification(String type, String name, int quantity, int id) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("notificationType", type);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("operationId", type + "_" + System.currentTimeMillis());

        switch(type) {
            case "NEW_MEDICATION":
                notification.put("id", id > 0 ? "med_" + id : "med_" + System.currentTimeMillis());
                notification.put("name", name);
                notification.put("inn", getRandomINN());
                notification.put("atcCode", getRandomATCCode());
                notification.put("dosageForm", getRandomDosageForm());
                notification.put("dosage", getRandomDosage());
                notification.put("unit", "мг");
                notification.put("manufacturer", getRandomManufacturer());
                notification.put("prescriptionRequired", Math.random() > 0.5);
                notification.put("storageConditions", "Хранить при температуре до 25°C");
                notification.put("shelfLifeMonths", 12 + (int)(Math.random() * 25));
                notification.put("quantity", quantity);
                break;

            case "MEDICATION_UPDATED":
                notification.put("medicationName", name);
                notification.put("currentQuantity", quantity);
                notification.put("unit", "шт.");
                notification.put("status", quantity <= 5 ? "LOW_STOCK" : "NORMAL");
                if (quantity <= 5) {
                    notification.put("warning", "Низкий остаток! Необходимо пополнение");
                }
                break;

            case "MEDICATION_REMOVED":
                notification.put("medicationName", name);
                notification.put("message", "Препарат полностью удален со склада");
                break;

            case "STOCK_INFO":
                notification.put("totalMedications", medicationStock.size());
                Map<String, Integer> stockDetails = new HashMap<>();
                medicationStock.forEach((medName, count) -> stockDetails.put(medName, count.get()));
                notification.put("stockDetails", stockDetails);
                break;
        }

        return notification;
    }

    private void sendStockInfo(WebSocketSession session) {
        Map<String, Object> stockInfo = createMedicationNotification("STOCK_INFO", null, 0, -1);
        sendMessage(session, stockInfo);
    }

    private void broadcastStockUpdate() {
        Map<String, Object> stockUpdate = createMedicationNotification("STOCK_INFO", null, 0, -1);
        broadcast(stockUpdate);
    }

    // Вспомогательные методы для генерации случайных данных
    private String getRandomINN() {
        String[] inns = {"Paracetamol", "Ibuprofen", "Aspirin", "Amoxicillin", "Metformin",
                "Atorvastatin", "Levothyroxine", "Lisinopril", "Metoprolol", "Simvastatin"};
        return inns[(int)(Math.random() * inns.length)];
    }

    private String getRandomATCCode() {
        String[] codes = {"N02BE01", "M01AE01", "B01AC06", "J01CA04", "A10BA02",
                "C10AA01", "H03AA01", "C09AA03", "C07AB02", "C10AA03"};
        return codes[(int)(Math.random() * codes.length)];
    }

    private String getRandomDosageForm() {
        String[] forms = {"Таблетки", "Капсулы", "Сироп", "Раствор", "Мазь",
                "Гель", "Спрей", "Капли", "Инъекция", "Порошок"};
        return forms[(int)(Math.random() * forms.length)];
    }

    private int getRandomDosage() {
        int[] dosages = {250, 500, 750, 100, 200, 300, 400, 600, 800, 1000};
        return dosages[(int)(Math.random() * dosages.length)];
    }

    private String getRandomManufacturer() {
        String[] manufacturers = {"Фармзавод №1", "Bayer AG", "Pfizer", "Novartis", "Sanofi",
                "GlaxoSmithKline", "Roche", "Merck", "AstraZeneca", "Johnson & Johnson"};
        return manufacturers[(int)(Math.random() * manufacturers.length)];
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Отключение от системы уведомлений: id={}, причина={}, осталось: {}",
                session.getId(), status.getReason(), sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Ошибка транспорта для сессии уведомлений {}: {}",
                session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    /**
     * Отправить сообщение конкретному клиенту
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        if (!session.isOpen()) {
            sessions.remove(session);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (JsonProcessingException e) {
            log.warn("Ошибка сериализации сообщения для сессии {}: {}",
                    session.getId(), e.getMessage());
        } catch (IOException e) {
            log.warn("Ошибка отправки сообщения в сессию {}: {}",
                    session.getId(), e.getMessage());
            sessions.remove(session);
        }
    }

    /**
     * Рассылка сообщения всем подключенным клиентам
     */
    public void broadcast(Map<String, Object> message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации сообщения для рассылки: {}", e.getMessage());
            return;
        }

        TextMessage textMessage = new TextMessage(json);
        int sent = 0;

        for (WebSocketSession session : sessions) {
            if (sendMessage(session, textMessage)) {
                sent++;
            }
        }

        log.info("Broadcast: отправлено {}/{} клиентам", sent, sessions.size());
    }

    /**
     * Отправить текстовое сообщение
     */
    private boolean sendMessage(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            sessions.remove(session);
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
            return true;
        } catch (IOException e) {
            log.warn("Ошибка отправки в сессию {}: {}", session.getId(), e.getMessage());
            sessions.remove(session);
            return false;
        }
    }

    /**
     * Получить статистику подключений
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", sessions.size());
        stats.put("activeSessions", sessions.stream().filter(WebSocketSession::isOpen).count());
        stats.put("totalMedications", medicationStock.size());
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    /**
     * Получить информацию о складе
     */
    public Map<String, Object> getStockInfo() {
        Map<String, Object> stockInfo = new HashMap<>();
        stockInfo.put("type", "STOCK_INFO");
        stockInfo.put("totalMedications", medicationStock.size());
        stockInfo.put("timestamp", System.currentTimeMillis());

        Map<String, Integer> stockDetails = new HashMap<>();
        int totalUnits = 0;

        for (Map.Entry<String, AtomicInteger> entry : medicationStock.entrySet()) {
            int count = entry.getValue().get();
            stockDetails.put(entry.getKey(), count);
            totalUnits += count;
        }

        stockInfo.put("stockDetails", stockDetails);
        stockInfo.put("totalUnits", totalUnits);

        return stockInfo;
    }
}