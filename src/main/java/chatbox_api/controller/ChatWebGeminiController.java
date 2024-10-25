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
import java.util.ArrayList;
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

        // Fetch conversation history and format for Gemini
        List<Map<String, String>> formattedHistory = getFormattedConversationHistory(conversationId);

        // Add current message
        formattedHistory.add(createMessageMap("user", userMessage));

        // Call Gemini with formatted history
        String aiResponse = googleAiService.callGeminiApi(formattedHistory);

        // Save both messages
        conversationService.addMessageToConversation(conversationId, username, userMessage, "text");
        conversationService.addMessageToConversation(conversationId, "Cherry", aiResponse, "text");

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
            // Get formatted history including system prompt
            List<Map<String, String>> formattedHistory = getFormattedConversationHistory(conversationId);

            // Add current message
            formattedHistory.add(createMessageMap("user", messageContent));

            // Call Gemini with complete history
            String aiResponse = googleAiService.callGeminiApi(formattedHistory);

            // Save messages
            Message userMessage = conversationService.addMessageToConversation(
                    conversationId, username, messageContent, "text");
            Message aiMessage = conversationService.addMessageToConversation(
                    conversationId, "Cherry", aiResponse, "text");

            Map<String, Message> response = new HashMap<>();
            response.put("userMessage", userMessage);
            response.put("aiMessage", aiMessage);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error processing message: " + e.getMessage());
        }
    }
    private List<Map<String, String>> getFormattedConversationHistory(String conversationId) {
        List<Message> history = conversationService.getMessagesByConversationId(conversationId);
        List<Map<String, String>> formattedHistory = new ArrayList<>();

        // Add system prompt as first message
        formattedHistory.add(createMessageMap("system",
                "You are Cherry, a helpful AI assistant. Be concise but friendly in your responses." +
                        "Remember previous context from our conversation to provide more relevant answers."));

        // Format existing messages
        for (Message msg : history) {
            String role = msg.getSender().equals("Cherry") ? "model" : "user";
            formattedHistory.add(createMessageMap(role, msg.getContent()));
        }

        return formattedHistory;
    }

    private Map<String, String> createMessageMap(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
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
        int totalProcessedResponses = analyticsService.getTotalProcessedResponses();
        double averageResponseTime = analyticsService.getOverallAverageResponseTime();
        int totalUniqueUsers = analyticsService.getTotalUniqueUsers();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalProcessedResponses", totalProcessedResponses);
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
