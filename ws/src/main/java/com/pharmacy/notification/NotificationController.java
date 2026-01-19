package com.pharmacy.notification.controller;

import com.pharmacy.notification.handler.MedicationWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final MedicationWebSocketHandler handler;
    private final Random random = new Random();

    // Список тестовых препаратов
    private final String[] testMedications = {
            "Парацетамол", "Ибупрофен", "Аспирин", "Амоксициллин", "Метформин",
            "Аторвастатин", "Левотироксин", "Лизиноприл", "Метопролол", "Симвастатин",
            "Омепразол", "Лозартан", "Глибенкламид", "Цетиризин", "Дексаметазон"
    };

    public NotificationController(MedicationWebSocketHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Pharmacy Notification Service");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/medication/add")
    public ResponseEntity<Map<String, Object>> addMedication(@RequestBody Map<String, Object> request) {
        String name = (String) request.getOrDefault("name", getRandomMedication());

        // Создаем операцию для WebSocket
        Map<String, Object> operation = new HashMap<>();
        operation.put("type", "MEDICATION_OPERATION");
        operation.put("operation", "ADD");
        operation.put("name", name);
        operation.put("timestamp", System.currentTimeMillis());
        operation.put("operationId", "REST_ADD_" + System.currentTimeMillis());

        // Отправляем операцию через WebSocket
        sendToWebSocketClients(operation);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("operation", "ADD");
        response.put("medication", name);
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Запрос на добавление препарата отправлен");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/medication/remove")
    public ResponseEntity<Map<String, Object>> removeMedication(@RequestBody Map<String, Object> request) {
        String name = (String) request.getOrDefault("name", getRandomMedication());

        // Создаем операцию для WebSocket
        Map<String, Object> operation = new HashMap<>();
        operation.put("type", "MEDICATION_OPERATION");
        operation.put("operation", "REMOVE");
        operation.put("name", name);
        operation.put("timestamp", System.currentTimeMillis());
        operation.put("operationId", "REST_REMOVE_" + System.currentTimeMillis());

        // Отправляем операцию через WebSocket
        sendToWebSocketClients(operation);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("operation", "REMOVE");
        response.put("medication", name);
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Запрос на удаление препарата отправлен");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/medication/update")
    public ResponseEntity<Map<String, Object>> updateMedication(@RequestBody Map<String, Object> request) {
        String name = (String) request.getOrDefault("name", getRandomMedication());

        // Создаем операцию для WebSocket
        Map<String, Object> operation = new HashMap<>();
        operation.put("type", "MEDICATION_OPERATION");
        operation.put("operation", "UPDATE");
        operation.put("name", name);
        operation.put("timestamp", System.currentTimeMillis());
        operation.put("operationId", "REST_UPDATE_" + System.currentTimeMillis());

        // Отправляем операцию через WebSocket
        sendToWebSocketClients(operation);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("operation", "UPDATE");
        response.put("medication", name);
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Запрос на обновление препарата отправлен");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/medication/list")
    public ResponseEntity<Map<String, Object>> listMedications() {
        Map<String, Object> response = handler.getStockInfo();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(handler.getConnectionStats());
    }

    @PostMapping("/test/{operation}")
    public ResponseEntity<Map<String, Object>> testOperation(@PathVariable String operation) {
        String medication = getRandomMedication();
        Map<String, Object> request = new HashMap<>();
        request.put("name", medication);

        switch(operation.toUpperCase()) {
            case "ADD":
                return addMedication(request);
            case "REMOVE":
                return removeMedication(request);
            case "UPDATE":
                return updateMedication(request);
            default:
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Неизвестная операция: " + operation);
                return ResponseEntity.badRequest().body(response);
        }
    }

    private void sendToWebSocketClients(Map<String, Object> message) {
        handler.broadcast(message);
    }

    private String getRandomMedication() {
        return testMedications[random.nextInt(testMedications.length)];
    }
}