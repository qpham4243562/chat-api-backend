package chatbox_api.controller;

import chatbox_api.model.Conversation;
import chatbox_api.model.Message;
import chatbox_api.service.AnalyticsService;
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
import java.util.stream.Collectors;

@RestController
@Controller
@RequestMapping("/api/conversations")
public class ChatWebGeminiController {

    private final GoogleAiService googleAiService;
    private final ConversationService conversationService;
    private final AnalyticsService analyticsService;

    public ChatWebGeminiController(GoogleAiService googleAiService, ConversationService conversationService,AnalyticsService analyticsService) {
        this.googleAiService = googleAiService;
        this.conversationService = conversationService;
        this.analyticsService=analyticsService;
    }

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public String handleChatMessage(String message) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);

        String username = (String) messageMap.get("username");
        String conversationId = (String) messageMap.get("conversationId");
        String userMessage = (String) messageMap.get("message");

        // Fetch entire conversation history
        List<Message> conversationHistory = conversationService.getMessagesByConversationId(conversationId);

        // Convert history to Gemini API format
        List<Map<String, String>> messages = conversationHistory.stream()
                .map(msg -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("role", msg.getSender().equals("GEMINI") ? "model" : "user");
                    m.put("content", msg.getContent());
                    return m;
                })
                .collect(Collectors.toList());

        // Add the new user message
        messages.add(Map.of("role", "user", "content", userMessage));

        // Call Gemini API with the conversation history
        String aiResponse = googleAiService.callGeminiApi(messages);

        // Save user message and AI response
        conversationService.addMessageToConversation(conversationId, username, userMessage, "text");
        conversationService.addMessageToConversation(conversationId, "GEMINI", aiResponse, "text");

        return aiResponse;
    }

    @GetMapping("/{conversationId}")
    public List<Message> getMessagesByConversationId(@PathVariable String conversationId) {
        Conversation conversation = conversationService.findById(conversationId);
        return conversation.getMessages();
    }

    @GetMapping("/by-username")
    public ResponseEntity<?> getConversationsByUsername(@RequestParam String username) {
        // Gọi tới service để tìm kiếm cuộc hội thoại
        List<Conversation> conversations = conversationService.findConversationsByUsername(username);

        // Nếu không có cuộc hội thoại, trả về thông báo JSON với mã trạng thái 404
        if (conversations.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Chưa có cuộc hội thoại nào cho người dùng này.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Nếu có dữ liệu, trả về danh sách cuộc hội thoại với mã trạng thái 200 OK
        return ResponseEntity.ok(conversations);
    }


    @PostMapping("/create")
    public Conversation createConversation(@RequestBody Map<String, String> requestBody) {
        String username = requestBody.get("username");

        // Create a new conversation without a title (initial message)
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
            // Fetch the entire conversation history
            List<Message> conversationHistory = conversationService.getMessagesByConversationId(conversationId);

            // Convert history to Gemini API format
            List<Map<String, String>> messages = conversationHistory.stream()
                    .map(msg -> {
                        Map<String, String> m = new HashMap<>();
                        m.put("role", msg.getSender().equals("GEMINI") ? "model" : "user");
                        m.put("content", msg.getContent());
                        return m;
                    })
                    .collect(Collectors.toList());

            // Add the new user message
            messages.add(Map.of("role", "user", "content", messageContent));

            // Call the AI API with the entire conversation history
            String aiResponse = googleAiService.callGeminiApi(messages);

            // Save user and AI messages
            Message userMessage = conversationService.addMessageToConversation(conversationId, username, messageContent, "text");
            Message aiMessage = conversationService.addMessageToConversation(conversationId, "GEMINI", aiResponse, "text");

            // Return both user and AI messages in the response
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
    @GetMapping("/analytics")
    public ResponseEntity<?> getOverallAnalytics() {
        int totalProcessedQuestions = analyticsService.getTotalProcessedQuestions();
        double averageResponseTime = analyticsService.getOverallAverageResponseTime();
        int totalUniqueUsers = analyticsService.getTotalUniqueUsers();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalProcessedQuestions", totalProcessedQuestions);
        analytics.put("averageResponseTime", averageResponseTime);
        analytics.put("totalUniqueUsers", totalUniqueUsers);

        return ResponseEntity.ok(analytics);
    }
    @DeleteMapping("/by-username")
    public ResponseEntity<?> deleteAllConversationsByUsername(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username is required");
        }

        try {
            conversationService.deleteAllConversationsByUsername(username);
            return ResponseEntity.ok().body("All conversations for user " + username + " have been deleted.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting conversations: " + e.getMessage());
        }
    }

}
