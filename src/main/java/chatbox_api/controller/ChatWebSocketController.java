package chatbox_api.controller;

import chatbox_api.service.ChatGptService;
import chatbox_api.service.ConversationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatGptService chatGptService;
    private final ConversationService conversationService;

    public ChatWebSocketController(ChatGptService chatGptService, ConversationService conversationService) {
        this.chatGptService = chatGptService;
        this.conversationService = conversationService;
    }

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public String handleChatMessage(String userId, String message) {
        String response = chatGptService.chatWithGpt(message);
        conversationService.saveConversation(userId, message, response);
        return response;
    }
}
