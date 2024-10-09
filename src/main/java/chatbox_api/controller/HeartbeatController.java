package chatbox_api.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class HeartbeatController {

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing(String message) {
        return "pong";
    }
}
