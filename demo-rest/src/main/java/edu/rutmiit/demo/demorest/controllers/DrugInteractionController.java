package edu.rutmiit.demo.demorest.controllers;

import edu.rutmiit.demo.demorest.config.RabbitMQConfig;
import edu.rutmiit.demo.events.DrugInteractionCheckedEvent;
import grpc.demo.DrugInteractionServiceGrpc;
import grpc.demo.DrugInteractionRequest;
import grpc.demo.DrugInteractionResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drug-interactions")
public class DrugInteractionController {

    @GrpcClient("drug-interaction-service")
    private DrugInteractionServiceGrpc.DrugInteractionServiceBlockingStub drugInteractionStub;

    private final RabbitTemplate rabbitTemplate;

    public DrugInteractionController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/check")
    public DrugInteractionResponse checkDrugInteraction(@RequestBody InteractionCheckRequest request) {
        // Вызов gRPC сервиса
        DrugInteractionRequest grpcRequest = DrugInteractionRequest.newBuilder()
                .setDrugId(request.drugId())
                .setDrugName(request.drugName())
                .addAllConcurrentDrugs(request.concurrentDrugs())
                .setPatientAge(request.patientAge())
                .setPatientCondition(request.patientCondition())
                .build();

        DrugInteractionResponse grpcResponse = drugInteractionStub.checkDrugInteraction(grpcRequest);

        // Отправка события
        DrugInteractionCheckedEvent event = new DrugInteractionCheckedEvent(
                grpcResponse.getDrugId(),
                grpcResponse.getDrugName(),
                grpcResponse.getRiskLevel(),
                grpcResponse.getSeverity(),
                grpcResponse.getContraindicationsList(),
                grpcResponse.getRecommendation()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.INTERACTIONS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_INTERACTION_CHECKED,
                event);

        return grpcResponse;
    }

    public record InteractionCheckRequest(
            Long drugId,
            String drugName,
            List<String> concurrentDrugs,
            int patientAge,
            String patientCondition
    ) {}
}