package grpc.demo;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@GrpcService
public class DrugInteractionServiceImpl extends DrugInteractionServiceGrpc.DrugInteractionServiceImplBase {

    private static final Map<String, List<String>> INTERACTION_DB = new HashMap<>();

    static {
        INTERACTION_DB.put("warfarin", List.of("aspirin", "ibuprofen", "naproxen"));
        INTERACTION_DB.put("simvastatin", List.of("clarithromycin", "itraconazole", "cyclosporine"));
        INTERACTION_DB.put("digoxin", List.of("quinidine", "verapamil", "amiodarone"));
        INTERACTION_DB.put("levothyroxine", List.of("calcium", "iron", "omeprazole"));
    }

    @Override
    public void checkDrugInteraction(DrugInteractionRequest request,
                                     StreamObserver<DrugInteractionResponse> responseObserver) {

        String drugName = request.getDrugName().toLowerCase();
        List<String> contraindications = INTERACTION_DB.getOrDefault(drugName, List.of());

        int riskLevel = calculateRiskLevel(drugName);
        String recommendation = generateRecommendation(riskLevel, contraindications);

        DrugInteractionResponse response = DrugInteractionResponse.newBuilder()
                .setDrugId(request.getDrugId())
                .setDrugName(drugName)
                .setRiskLevel(riskLevel)
                .addAllContraindications(contraindications)
                .setRecommendation(recommendation)
                .setSeverity(riskLevel > 7 ? "HIGH" : riskLevel > 4 ? "MEDIUM" : "LOW")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private int calculateRiskLevel(String drugName) {
        // Имитация расчета уровня риска
        int baseScore = Math.abs(drugName.hashCode() % 10);
        return baseScore + 1; // 1-10
    }

    private String generateRecommendation(int riskLevel, List<String> contraindications) {
        if (riskLevel > 7) {
            return "КРИТИЧЕСКАЯ ВЗАИМОДЕЙСТВИЕ: Необходима консультация врача перед применением!";
        } else if (riskLevel > 4) {
            return "УМЕРЕННЫЙ РИСК: Требуется наблюдение при совместном применении.";
        } else if (!contraindications.isEmpty()) {
            return "НИЗКИЙ РИСК: Избегать совместного применения с указанными препаратами.";
        } else {
            return "БЕЗОПАСНО: Нет известных критических взаимодействий.";
        }
    }
}