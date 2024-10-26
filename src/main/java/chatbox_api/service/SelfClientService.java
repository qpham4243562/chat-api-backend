package chatbox_api.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;

@Service
public class SelfClientService implements InitializingBean {

    private WebSocketClient client;
    private final String SERVER_URL = "wss://chat-api-backend-x4dl.onrender.com/ws-chat";
    private boolean isRunning = true;

    @Override
    public void afterPropertiesSet() {
        startSelfClient();
    }

    private void startSelfClient() {
        try {
            client = new WebSocketClient(new URI(SERVER_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Self Client Connected: " + new Date());
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("Self Client Received: " + message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Self Client Closed: " + reason);
                    reconnect();
                }

                @Override
                public void onError(Exception ex) {
                    System.out.println("Self Client Error: " + ex.getMessage());
                    reconnect();
                }
            };

            // Khởi động client trong thread riêng
            new Thread(() -> {
                while (isRunning) {
                    try {
                        if (!client.isOpen()) {
                            client.connectBlocking();
                        }
                        // Gửi ping mỗi 30 giây
                        Thread.sleep(30000);
                        if (client.isOpen()) {
                            client.send("ping");
                        }
                    } catch (Exception e) {
                        System.err.println("Self Client Error: " + e.getMessage());
                        reconnect();
                    }
                }
            }, "self-client-thread").start();

        } catch (Exception e) {
            System.err.println("Error starting self client: " + e.getMessage());
        }
    }

    private void reconnect() {
        try {
            Thread.sleep(5000); // đợi 5 giây trước khi reconnect
            startSelfClient();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Cleanup method
    @PreDestroy
    public void cleanup() {
        isRunning = false;
        if (client != null) {
            client.close();
        }
    }
}