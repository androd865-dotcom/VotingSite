package org.example.rgr.model;

import java.util.List;

public class Topic {
    private int id;
    private String nameQuestion;
    private boolean many;
    private int countOfUsers;
    private List<Answer> answers;
    
    public Topic() {}
    
    public Topic(int id, String nameQuestion, boolean many, int countOfUsers) {
        this.id = id;
        this.nameQuestion = nameQuestion;
        this.many = many;
        this.countOfUsers = countOfUsers;
    }
    
    public int getId() { return id; }
    public String getNameQuestion() { return nameQuestion; }
    public boolean isMany() { return many; }
    public int getCountOfUsers() { return countOfUsers; }
    public List<Answer> getAnswers() { return answers; }
    
    public void setId(int id) { this.id = id; }
    public void setNameQuestion(String nameQuestion) { this.nameQuestion = nameQuestion; }
    public void setMany(boolean many) { this.many = many; }
    public void setCountOfUsers(int countOfUsers) { this.countOfUsers = countOfUsers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }
}