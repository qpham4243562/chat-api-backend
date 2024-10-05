package chatbox_api.controller;

import chatbox_api.model.Conversation;
import chatbox_api.model.Message;
import chatbox_api.service.ConversationService;
import chatbox_api.service.GoogleAiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@Controller
@RequestMapping("/api/conversations")
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
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> messageMap = objectMapper.readValue(message, Map.class);

        String username = messageMap.get("username");
        String userMessage = messageMap.get("message");

        // Nếu là tin nhắn đầu tiên, dùng nó làm tiêu đề của cuộc hội thoại
        String conversationId = conversationService.getOrCreateConversationId(username, userMessage);

        conversationService.addMessageToConversation(conversationId, username, userMessage);

        String aiResponse = googleAiService.callGeminiApi(userMessage);
        conversationService.addMessageToConversation(conversationId, "GEMINI", aiResponse);

        return aiResponse;
    }

    @GetMapping("/{conversationId}")
    public List<Message> getMessagesByConversationId(@PathVariable String conversationId) {
        Conversation conversation = conversationService.findById(conversationId);
        return conversation.getMessages();
    }

    // Lấy tất cả đoạn hội thoại dựa trên `username`
    @GetMapping("/by-username")
    public List<Conversation> getConversationsByUsername(@RequestParam String username) {
        return conversationService.findConversationsByUsername(username);
    }
}
