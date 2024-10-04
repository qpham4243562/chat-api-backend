package chatbox_api.controller;

import chatbox_api.service.ConversationService;
import chatbox_api.service.GoogleAiService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Controller
public class ChatWebGeminiController {

    private final GoogleAiService googleAiService;
    private final ConversationService conversationService;

    public ChatWebGeminiController(GoogleAiService googleAiService, ConversationService conversationService) {
        this.googleAiService = googleAiService;
        this.conversationService = conversationService;
    }

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public String handleChatMessage(String message) throws Exception {
        // Parse `userId` và `message` từ JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> messageMap = objectMapper.readValue(message, Map.class);

        String userId = messageMap.get("userId");
        String userMessage = messageMap.get("message");

        // Bước 1: Lưu tin nhắn người dùng vào database
        conversationService.saveUserMessage(userId, userMessage);

        // Bước 2: Gọi API Google AI để lấy phản hồi
        String aiResponse = googleAiService.callGeminiApi(userMessage);

        // Lưu phản hồi từ AI vào database với `username` là "GEMINI"
        conversationService.saveAiResponse("GEMINI", aiResponse);

        // Trả lại phản hồi qua WebSocket
        return aiResponse;
    }
}
