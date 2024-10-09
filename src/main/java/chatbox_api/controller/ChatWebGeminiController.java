package chatbox_api.controller;

import chatbox_api.model.Conversation;
import chatbox_api.model.Message;
import chatbox_api.service.ConversationService;
import chatbox_api.service.GoogleAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
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
        Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);

        String username = (String) messageMap.get("username");
        String conversationId = (String) messageMap.get("conversationId");
        String userMessage = (String) messageMap.get("message");

        // Xử lý tin nhắn và lấy phản hồi từ AI
        String aiResponse = googleAiService.callGeminiApi(userMessage);

        // Lưu tin nhắn và phản hồi vào cơ sở dữ liệu
        conversationService.addMessageToConversation(conversationId, username, userMessage, "text");
        conversationService.addMessageToConversation(conversationId, "GEMINI", aiResponse, "text");

        // Trả về phản hồi để gửi qua WebSocket
        return aiResponse;
    }


    @GetMapping("/{conversationId}")
    public List<Message> getMessagesByConversationId(@PathVariable String conversationId) {
        Conversation conversation = conversationService.findById(conversationId);
        return conversation.getMessages();
    }

    @GetMapping("/by-username")
    public List<Conversation> getConversationsByUsername(@RequestParam String username) {
        return conversationService.findConversationsByUsername(username);
    }

    @PostMapping("/create")
    public Conversation createConversation(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");

        // Tạo một cuộc hội thoại mới nhưng chưa có tiêu đề (title)
        String conversationId = conversationService.createNewConversation(username, "");

        return conversationService.findById(conversationId);
    }

    @DeleteMapping("/{conversationId}")
    public void deleteConversation(@PathVariable String conversationId) {
        conversationService.deleteById(conversationId);
    }
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<?> addMessageToConversation(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> requestBody) {

        String username = requestBody.get("username");
        String messageContent = requestBody.get("message");

        if (username == null || messageContent == null) {
            return ResponseEntity.badRequest().body("Username or message content is missing");
        }

        try {
            // Lưu tin nhắn người dùng
            Message userMessage = conversationService.addMessageToConversation(conversationId, username, messageContent, "text");

            // Gọi API AI để lấy phản hồi
            String aiResponse = googleAiService.callGeminiApi(messageContent);

            // Lưu phản hồi AI
            Message aiMessage = conversationService.addMessageToConversation(conversationId, "GEMINI", aiResponse, "text");

            // Trả về cả tin nhắn người dùng và phản hồi AI
            Map<String, Message> response = new HashMap<>();
            response.put("userMessage", userMessage);
            response.put("aiMessage", aiMessage);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing message: " + e.getMessage());
        }
    }
    @PutMapping("/{conversationId}/title")
    public ResponseEntity<?> updateConversationTitle(@PathVariable String conversationId, @RequestBody Map<String, String> requestBody) {
        String newTitle = requestBody.get("title");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("New title is required");
        }
        try {
            conversationService.updateConversationTitle(conversationId, newTitle.trim());
            return ResponseEntity.ok().body("Title updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating title: " + e.getMessage());
        }
    }
}
