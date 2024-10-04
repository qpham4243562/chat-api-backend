package chatbox_api.service;

import chatbox_api.model.Conversation;
import chatbox_api.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.io.IOException;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    // Hàm lưu tin nhắn của người dùng
    public void saveUserMessage(String userId, String message) {
        Conversation userConversation = new Conversation();
        userConversation.setUserId(userId);
        userConversation.setMessage(message);
        userConversation.setTimestamp(LocalDateTime.now().toString());
        conversationRepository.save(userConversation);
    }

    // Hàm lưu phản hồi từ AI
    public void saveAiResponse(String aiUsername, String aiResponse) {
        try {
            // Parse nội dung tin nhắn từ phản hồi JSON của AI
            JsonNode responseNode = objectMapper.readTree(aiResponse);
            String aiMessage = responseNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Lưu tin nhắn của AI vào database với `username` là "GEMINI"
            Conversation aiConversation = new Conversation();
            aiConversation.setUserId(aiUsername);
            aiConversation.setMessage(aiMessage);
            aiConversation.setTimestamp(LocalDateTime.now().toString());
            conversationRepository.save(aiConversation);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle exception nếu cần
        }
    }
}
