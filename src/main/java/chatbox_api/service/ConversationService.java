package chatbox_api.service;

import chatbox_api.model.Conversation;
import chatbox_api.model.Message;
import chatbox_api.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final String SECRET_KEY = "YourSecretKey123"; // Thay thế bằng khóa bí mật an toàn của bạn

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    private String encrypt(String data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    private String decrypt(String encryptedData) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
    }

    public Conversation save(Conversation conversation) {
        try {
            conversation.setUsername(encrypt(conversation.getUsername()));
            conversation.setTitle(encrypt(conversation.getTitle()));
            conversation.setTimestamp(encrypt(conversation.getTimestamp()));
            List<Message> encryptedMessages = conversation.getMessages().stream()
                    .map(this::encryptMessage)
                    .collect(Collectors.toList());
            conversation.setMessages(encryptedMessages);
            return conversationRepository.save(conversation);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting conversation data", e);
        }
    }

    private Message encryptMessage(Message message) {
        try {
            return new Message(
                    encrypt(message.getSender()),
                    encrypt(message.getContent()),
                    encrypt(message.getTimestamp()),
                    encrypt(message.getContentType())
            );
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting message", e);
        }
    }

    public String getOrCreateConversationId(String username, String firstMessage) {
        try {
            List<Conversation> existingConversations = findConversationsByUsername(username);

            if (!existingConversations.isEmpty()) {
                return existingConversations.get(0).getId();
            } else {
                Conversation newConversation = new Conversation();
                newConversation.setUsername(encrypt(username));
                newConversation.setTitle(encrypt(firstMessage));
                newConversation.setMessages(new ArrayList<>());
                newConversation.setTimestamp(encrypt(LocalDateTime.now().toString()));
                Conversation savedConversation = conversationRepository.save(newConversation);
                return savedConversation.getId();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating or retrieving conversation", e);
        }
    }

    public List<Conversation> findConversationsByUsername(String username) {
        try {
            // Mã hóa username trước khi tìm kiếm
            String encryptedUsername = encrypt(username);

            // Tìm kiếm danh sách cuộc hội thoại dựa trên username đã mã hóa
            List<Conversation> conversations = conversationRepository.findByUsername(encryptedUsername);

            // Giải mã các cuộc hội thoại trước khi trả về
            return conversations.stream()
                    .map(this::decryptConversation)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Ném lỗi nếu có vấn đề xảy ra trong quá trình tìm kiếm hoặc giải mã
            throw new RuntimeException("Error finding conversations", e);
        }
    }



    private Conversation decryptConversation(Conversation conversation) {
        try {
            conversation.setUsername(decrypt(conversation.getUsername()));
            conversation.setTitle(decrypt(conversation.getTitle()));
            conversation.setTimestamp(decrypt(conversation.getTimestamp()));
            List<Message> decryptedMessages = conversation.getMessages().stream()
                    .map(this::decryptMessage)
                    .collect(Collectors.toList());
            conversation.setMessages(decryptedMessages);
            return conversation;
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting conversation", e);
        }
    }

    private Message decryptMessage(Message message) {
        try {
            return new Message(
                    decrypt(message.getSender()),
                    decrypt(message.getContent()),
                    decrypt(message.getTimestamp()),
                    decrypt(message.getContentType())
            );
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting message", e);
        }
    }

    public Message addMessageToConversation(String conversationId, String sender, String content, String contentType) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));

            Message message = new Message(
                    encrypt(sender),
                    encrypt(content),
                    encrypt(LocalDateTime.now().toString()),
                    encrypt(contentType)
            );
            conversation.getMessages().add(message);

            if (conversation.getMessages().size() == 1 && !sender.equals("GEMINI") && decrypt(conversation.getTitle()).isEmpty()) {
                conversation.setTitle(encrypt(content));
            }

            conversationRepository.save(conversation);
            return decryptMessage(message);
        } catch (Exception e) {
            throw new RuntimeException("Error adding message to conversation", e);
        }
    }

    public Conversation findById(String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));
        return decryptConversation(conversation);
    }

    public String createNewConversation(String username, String title) {
        try {
            Conversation newConversation = new Conversation();
            newConversation.setUsername(encrypt(username));
            newConversation.setTitle(encrypt(title));
            newConversation.setMessages(new ArrayList<>());
            newConversation.setTimestamp(encrypt(LocalDateTime.now().toString()));

            Conversation savedConversation = conversationRepository.save(newConversation);
            return savedConversation.getId();
        } catch (Exception e) {
            throw new RuntimeException("Error creating new conversation", e);
        }
    }

    public void updateConversationTitle(String conversationId, String newTitle) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));
            conversation.setTitle(encrypt(newTitle));
            conversationRepository.save(conversation);
        } catch (Exception e) {
            throw new RuntimeException("Error updating conversation title", e);
        }
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



