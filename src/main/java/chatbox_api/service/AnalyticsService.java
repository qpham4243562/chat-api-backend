package chatbox_api.service;

import chatbox_api.model.Conversation;
import chatbox_api.repository.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    private final ConversationRepository conversationRepository;

    public AnalyticsService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public void updateAnalytics(String conversationId, long responseTime) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conversation.incrementProcessedQuestions();
        conversation.addResponseTime(responseTime);
        conversationRepository.save(conversation);
    }

    public int getTotalProcessedQuestions() {
        List<Conversation> allConversations = conversationRepository.findAll();
        return allConversations.stream().mapToInt(Conversation::getProcessedQuestions).sum();
    }

    public double getOverallAverageResponseTime() {
        List<Conversation> allConversations = conversationRepository.findAll();
        long totalProcessedQuestions = allConversations.stream().mapToInt(Conversation::getProcessedQuestions).sum();
        long totalResponseTime = allConversations.stream().mapToLong(Conversation::getTotalResponseTime).sum();
        return totalProcessedQuestions > 0 ? (double) totalResponseTime / totalProcessedQuestions : 0;
    }

    public int getTotalUniqueUsers() {
        return (int) conversationRepository.findAll().stream()
                .map(Conversation::getUsername)
                .distinct()
                .count();
    }
}