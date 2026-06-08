package org.example.rgr.model;

public class Answer {
    private int id;
    private String nameAnswer;
    private int topicId;
    private int count;
    
    public Answer() {}
    
    public Answer(int id, String nameAnswer, int topicId, int count) {
        this.id = id;
        this.nameAnswer = nameAnswer;
        this.topicId = topicId;
        this.count = count;
    }
    
    public int getId() { return id; }
    public String getNameAnswer() { return nameAnswer; }
    public int getTopicId() { return topicId; }
    public int getCount() { return count; }
    
    public void setId(int id) { this.id = id; }
    public void setNameAnswer(String nameAnswer) { this.nameAnswer = nameAnswer; }
    public void setTopicId(int topicId) { this.topicId = topicId; }
    public void setCount(int count) { this.count = count; }
}