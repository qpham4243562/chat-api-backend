package chatbox_api.service;

import chatbox_api.model.Conversation;
import chatbox_api.model.Message;
import chatbox_api.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public String getOrCreateConversationId(String username, String firstMessage) {
        // Tìm kiếm cuộc hội thoại dựa trên `username`
        List<Conversation> existingConversations = conversationRepository.findByUsername(username);

        if (!existingConversations.isEmpty()) {
            // Lấy cuộc hội thoại đầu tiên trong danh sách (nếu có)
            return existingConversations.get(0).getId();
        } else {
            // Nếu không có, tạo cuộc hội thoại mới
            Conversation newConversation = new Conversation();
            newConversation.setUsername(username); // Lưu username
            newConversation.setTitle(firstMessage); // Thiết lập tiêu đề bằng tin nhắn đầu tiên
            newConversation.setMessages(new ArrayList<>());
            newConversation.setTimestamp(LocalDateTime.now().toString());
            Conversation savedConversation = conversationRepository.save(newConversation);
            return savedConversation.getId();
        }
    }



    public List<Conversation> findConversationsByUsername(String username) {
        return conversationRepository.findByUsername(username);
    }


    public void addMessageToConversation(String conversationId, String sender, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));

        // Lưu nội dung tin nhắn của người dùng, không lưu chuỗi JSON
        Message message = new Message(sender, content, LocalDateTime.now().toString());
        conversation.getMessages().add(message);

        // Lưu cuộc hội thoại lại vào database
        conversationRepository.save(conversation);
    }

    public Conversation findById(String conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));
    }

}
