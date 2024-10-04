package chatbox_api.service;

import chatbox_api.model.Conversation;
import chatbox_api.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation saveConversation(String userId, String message, String response) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setMessage(message);
        conversation.setResponse(response);
        conversation.setTimestamp(LocalDateTime.now().toString());
        return conversationRepository.save(conversation);
    }
}
