package chatbox_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Component
public class KeepAliveScheduler {
    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);

    @Value("${base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public KeepAliveScheduler() {
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedDelay = 60000) // 1 phút
    public void pingServer() {
        try {
            // Sử dụng baseUrl từ file cấu hình
            String pingUrl = baseUrl + "/api/call";

            ResponseEntity<String> response = restTemplate.getForEntity(pingUrl, String.class);
            logger.info("Ping status: {}, time: {}", response.getStatusCode(), LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Error while pinging server: {}", e.getMessage());
        }
    }
}
