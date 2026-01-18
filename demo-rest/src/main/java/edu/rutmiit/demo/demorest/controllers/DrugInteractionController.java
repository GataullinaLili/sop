package edu.rutmiit.demo.demorest.controllers;

import edu.rutmiit.demo.demorest.config.RabbitMQConfig;
import edu.rutmiit.demo.medicinescontract.events.DrugInteractionCheckedEvent;
import grpc.demo.DrugInteractionServiceGrpc;
import grpc.demo.DrugInteractionRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
public class DrugInteractionController {

    @GrpcClient("drug-interaction-service")
    private DrugInteractionServiceGrpc.DrugInteractionServiceBlockingStub interactionStub;

    private final RabbitTemplate rabbitTemplate;

    public DrugInteractionController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/{id}/check-interaction")
    public String checkDrugInteraction(
            @PathVariable Long id,
            @RequestParam String drugName,
            @RequestParam(required = false) List<String> concurrentDrugs,
            @RequestParam(defaultValue = "30") Integer patientAge) {

        // Вызов gRPC сервиса для проверки взаимодействий
        var request = DrugInteractionRequest.newBuilder()
                .setDrugId(id)
                .setDrugName(drugName)
                .addAllConcurrentDrugs(concurrentDrugs != null ? concurrentDrugs : List.of())
                .setPatientAge(patientAge)
                .build();

        var gRpcResponse = interactionStub.checkDrugInteraction(request);

        // Отправка события в Fanout для аудита и уведомлений
        var event = new DrugInteractionCheckedEvent(
                gRpcResponse.getDrugId(),
                gRpcResponse.getDrugName(),
                gRpcResponse.getRiskLevel(),
                gRpcResponse.getSeverity(),
                gRpcResponse.getContraindicationsList(),
                gRpcResponse.getRecommendation()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.FANOUT_EXCHANGE, "", event);

        return String.format("Проверка взаимодействий для %s: %s (Уровень риска: %d)",
                drugName, gRpcResponse.getRecommendation(), gRpcResponse.getRiskLevel());
    }

    @GetMapping("/{id}/interactions")
    public String getDrugInteractions(@PathVariable Long id) {
        // Получение информации о взаимодействиях
        return "Информация о взаимодействиях препарата";
    }
}