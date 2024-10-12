package chatbox_api.controller;


import chatbox_api.model.UserMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private List<String> connectedUsers = new ArrayList<>();

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Xử lý tin nhắn công khai
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public UserMessage sendMessage(@Payload UserMessage message) {
        return message; // Gửi tin nhắn công khai đến tất cả mọi người
    }

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload UserMessage message) {
        String recipientTopic = "/topic/private." + message.getRecipient();  // Tạo topic riêng cho từng người nhận
        messagingTemplate.convertAndSend(recipientTopic, message);  // Gửi tin nhắn tới topic đó
    }




    @MessageMapping("/chat.register")
    public void register(@Payload UserMessage message, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", message.getSender());
        connectedUsers.add(message.getSender());  // Thêm người dùng vào danh sách

        // Gửi cập nhật danh sách người dùng cho tất cả
        messagingTemplate.convertAndSend("/topic/users", connectedUsers);
    }


}
