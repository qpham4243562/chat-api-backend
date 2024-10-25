package chatbox_api.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WebSocketPingService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @Scheduled(fixedDelay = 180000)
    public void sendWebSocketPing() {
        try {
            // Gửi tin nhắn 'ping' tới một endpoint WebSocket.
            messagingTemplate.convertAndSend("/topic/ping", "ping");
            System.out.println("WebSocket ping sent successfully.");
        } catch (Exception e) {
            System.err.println("WebSocket ping failed: " + e.getMessage());
        }
    }
}
