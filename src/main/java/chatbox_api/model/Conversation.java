package chatbox_api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;
    private String title;
    private List<Message> messages;
    private String timestamp;
    private String username;
    private int processedQuestions;
    private long totalResponseTime;

    public Conversation() {
    }

    public Conversation(String id, String title, List<Message> messages, String timestamp, String username) {
        this.id = id;
        this.title = title;
        this.messages = messages;
        this.timestamp = timestamp;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Message> getMessages() {
        if (this.messages == null) {
            this.messages = new ArrayList<>(); // Đảm bảo danh sách không null
        }
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void addMessage(Message message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
    public int getProcessedQuestions() {
        return processedQuestions;
    }

    public void setProcessedQuestions(int processedQuestions) {
        this.processedQuestions = processedQuestions;
    }

    public long getTotalResponseTime() {
        return totalResponseTime;
    }

    public void setTotalResponseTime(long totalResponseTime) {
        this.totalResponseTime = totalResponseTime;
    }

    public void incrementProcessedQuestions() {
        this.processedQuestions++;
    }

    public void addResponseTime(long responseTime) {
        this.totalResponseTime += responseTime;
    }

    public double getAverageResponseTime() {
        return processedQuestions > 0 ? (double) totalResponseTime / processedQuestions : 0;
    }
}

