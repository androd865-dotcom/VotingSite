package org.example.rgr.websocket;

public class WebSocketMessage {
    private String type;
    private String message;
    private Object data;
    private long timestamp;
    
    public WebSocketMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public WebSocketMessage(String type, String message, Object data) {
        this.type = type;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getType() { return type; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public long getTimestamp() { return timestamp; }
    
    public void setType(String type) { this.type = type; }
    public void setMessage(String message) { this.message = message; }
    public void setData(Object data) { this.data = data; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}