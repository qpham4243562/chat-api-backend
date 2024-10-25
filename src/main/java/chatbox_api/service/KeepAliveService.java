package chatbox_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KeepAliveService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.self-ping-url:http://chat-api-backend-x4dl.onrender.com/ping}")  // URL của chính ứng dụng
    private String selfPingUrl;

    // Cron job ping ứng dụng mỗi 5 phút
    @Scheduled(cron = "0 */3 * * * *")  // Cứ mỗi 3 phút một lần
    public void pingSelf() {
        try {
            String response = restTemplate.getForObject(selfPingUrl, String.class);
            System.out.println("Ping thành công: " + response);
        } catch (Exception e) {
            System.err.println("Ping thất bại: " + e.getMessage());
        }
    }
}
