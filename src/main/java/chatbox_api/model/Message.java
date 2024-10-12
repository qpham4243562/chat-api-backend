package chatbox_api.model;

import java.time.LocalDateTime;

public class Message {
    private String sender;
    private String content;
    private String timestamp;
    private String contentType; // "text" or "image"


    public Message() {}

    public Message(String sender, String content, String timestamp, String contentType) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.contentType = contentType;
    }

    // Getters and setters
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}