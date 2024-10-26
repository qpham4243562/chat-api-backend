package chatbox_api.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing() {
        return "pong-" + System.currentTimeMillis();
    }
}