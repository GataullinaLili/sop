package ws.demo.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicationNotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedicationNotificationApplication.class, args);
		printBanner();
	}

	private static void printBanner() {
		System.out.println("\n" +
				"╔══════════════════════════════════════════════════════════╗\n" +
				"║                                                          ║\n" +
				"║    💊  СИСТЕМА УВЕДОМЛЕНИЙ О ЛЕКАРСТВЕННЫХ ПРЕПАРАТАХ   ║\n" +
				"║                                                          ║\n" +
				"║    Версия: 1.0.0                                         ║\n" +
				"║    Режим: WebSocket Notification Service                 ║\n" +
				"║                                                          ║\n" +
				"╚══════════════════════════════════════════════════════════╝\n");

		System.out.println("🚀 Сервис уведомлений запущен!");
		System.out.println("📡 WebSocket endpoints доступны:");
		System.out.println("   • ws://localhost:8080/ws/medications (с SockJS)");
		System.out.println("   • ws://localhost:8080/ws/medications/ws (чистый WS)");
		System.out.println("   • ws://localhost:8080/ws/medications/admin");
		System.out.println("   • ws://localhost:8080/ws/medications/public");
		System.out.println("\n📊 REST API endpoints:");
		System.out.println("   • http://localhost:8080/api/medications/notifications/*");
		System.out.println("   • http://localhost:8080/actuator/health");
		System.out.println("   • http://localhost:8080/actuator/metrics");
		System.out.println("\n🔗 Документация: http://localhost:8080/swagger-ui.html");
	}
}