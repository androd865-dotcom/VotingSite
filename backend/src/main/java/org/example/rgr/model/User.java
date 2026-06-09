package org.example.rgr.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String login;
    private String password;
    private List<Integer> votedTopics;  // список ID тем, за которые проголосовал
    
    public User() {
        this.votedTopics = new ArrayList<>();
    }
    
    public User(int id, String login, String password, List<Integer> votedTopics) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.votedTopics = votedTopics != null ? votedTopics : new ArrayList<>();
    }
    
    // Геттеры
    public int getId() { return id; }
    public String getLogin() { return login; }
    public String getPassword() { return password; }
    public List<Integer> getVotedTopics() { return votedTopics; }
    
    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setLogin(String login) { this.login = login; }
    public void setPassword(String password) { this.password = password; }
    public void setVotedTopics(List<Integer> votedTopics) { this.votedTopics = votedTopics; }
    
    // Добавить голос за тему
    public void addVotedTopic(int topicId) {
        if (!votedTopics.contains(topicId)) {
            votedTopics.add(topicId);
        }
    }
    
    // Проверить, голосовал ли за тему
    public boolean hasVoted(int topicId) {
        return votedTopics.contains(topicId);
    }
    
    public boolean isAdmin() {
        return id == 1 && "admin123".equals(login);
    }
    
    @Override
    public String toString() {
        return "User{id=" + id + ", login='" + login + "', votedTopics=" + votedTopics + "}";
    }
}