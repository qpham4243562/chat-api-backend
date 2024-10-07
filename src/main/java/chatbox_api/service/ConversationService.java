package chatbox_api.service;

import chatbox_api.model.Conversation;
import chatbox_api.model.Message;
import chatbox_api.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public String getOrCreateConversationId(String username, String firstMessage) {
        List<Conversation> existingConversations = conversationRepository.findByUsername(username);

        if (!existingConversations.isEmpty()) {
            return existingConversations.get(0).getId();
        } else {
            Conversation newConversation = new Conversation();
            newConversation.setUsername(username);
            newConversation.setTitle(firstMessage);
            newConversation.setMessages(new ArrayList<>());
            newConversation.setTimestamp(LocalDateTime.now().toString());
            Conversation savedConversation = conversationRepository.save(newConversation);
            return savedConversation.getId();
        }
    }

    public List<Conversation> findConversationsByUsername(String username) {
        return conversationRepository.findByUsername(username);
    }

    public void addMessageToConversation(String conversationId, String sender, String content, String contentType) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));

        Message message = new Message(sender, content, LocalDateTime.now().toString(), contentType);
        conversation.getMessages().add(message);

        conversationRepository.save(conversation);
    }

    public Conversation findById(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));
    }

    public void addImageToConversation(String conversationId, String sender, String imageBase64) {
        addMessageToConversation(conversationId, sender, imageBase64, "image");
    }
}