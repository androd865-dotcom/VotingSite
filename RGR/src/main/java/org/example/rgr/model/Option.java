package org.example.rgr.model;

public class Option {
    private int id;
    private int topicId;
    private String optionText;
    private int voteCount;

    public Option(int id, int topicId, String optionText) {
        this.id = id;
        this.topicId = topicId;
        this.optionText = optionText;
        this.voteCount = 0;
    }

    public Option(int id, int topicId, String optionText, int voteCount) {
        this.id = id;
        this.topicId = topicId;
        this.optionText = optionText;
        this.voteCount = voteCount;
    }

    public int getId() { return id; }
    public int getTopicId() { return topicId; }
    public String getOptionText() { return optionText; }
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
}