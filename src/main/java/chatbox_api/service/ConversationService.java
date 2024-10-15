package chatbox_api.service;

import chatbox_api.model.Conversation;
import chatbox_api.model.Message;
import chatbox_api.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }
    public Conversation save(Conversation conversation) {
        return conversationRepository.save(conversation);
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

    public Message addMessageToConversation(String conversationId, String sender, String content, String contentType) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));

        Message message = new Message(sender, content, LocalDateTime.now().toString(), contentType);
        conversation.getMessages().add(message);

        // Set the title if it's the first message and not from GEMINI
        if (conversation.getMessages().size() == 1 && !sender.equals("GEMINI") && conversation.getTitle().isEmpty()) {
            conversation.setTitle(content);
        }

        conversationRepository.save(conversation);
        return message;
    }

    public Conversation findById(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));
    }

    public String createNewConversation(String username, String title) {
        Conversation newConversation = new Conversation();
        newConversation.setUsername(username);
        newConversation.setTitle(title);
        newConversation.setMessages(new ArrayList<>());
        newConversation.setTimestamp(LocalDateTime.now().toString());

        Conversation savedConversation = conversationRepository.save(newConversation);
        return savedConversation.getId();
    }
    public void updateConversationTitle(String conversationId, String newTitle) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));
        conversation.setTitle(newTitle);
        conversationRepository.save(conversation);
    }

    public void deleteById(String conversationId) {
        conversationRepository.deleteById(conversationId);
    }
    public void addImageToConversation(String conversationId, String sender, String imageBase64) {
        addMessageToConversation(conversationId, sender, imageBase64, "image");
    }
    public List<Message> getMessagesByConversationId(String conversationId) {
        Conversation conversation = findById(conversationId);
        return conversation.getMessages();
    }
}
