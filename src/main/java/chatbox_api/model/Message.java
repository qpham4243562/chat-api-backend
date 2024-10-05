package chatbox_api.model;

public class Message {
    private String sender; // Người gửi (user hoặc bot)
    private String content; // Nội dung tin nhắn
    private String timestamp; // Thời gian tin nhắn

    // Constructor, Getter, Setter
    public Message() {}

    public Message(String sender, String content, String timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

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
}
