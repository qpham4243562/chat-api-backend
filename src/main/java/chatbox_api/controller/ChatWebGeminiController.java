package chatbox_api.controller;

import chatbox_api.model.Conversation;
import chatbox_api.model.Message;
import chatbox_api.service.ConversationService;
import chatbox_api.service.GoogleAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
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
        Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);

        String username = (String) messageMap.get("username");
        String conversationId = (String) messageMap.get("conversationId");

        Object userMessageObj = messageMap.get("message");
        String userMessage;
        String contentType = "text";

        if (userMessageObj instanceof String) {
            userMessage = (String) userMessageObj;
        } else if (userMessageObj instanceof Map) {
            Map<String, Object> messageContent = (Map<String, Object>) userMessageObj;
            contentType = (String) messageContent.get("type");
            userMessage = (String) messageContent.get("content");
        } else {
            throw new RuntimeException("Unsupported message format");
        }

        if (conversationId == null) {
            conversationId = conversationService.getOrCreateConversationId(username, userMessage);
        }

        // Save user message to conversation
        conversationService.addMessageToConversation(conversationId, username, userMessage, contentType);

        String aiResponse;
        if (contentType.equals("image")) {
            // Process image with Gemini API
            aiResponse = googleAiService.callGeminiApiWithImage(userMessage.getBytes());
        } else {
            // Process text with Gemini API
            aiResponse = googleAiService.callGeminiApi(userMessage);
        }

        // Save AI response to conversation
        conversationService.addMessageToConversation(conversationId, "GEMINI", aiResponse, "text");

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
}