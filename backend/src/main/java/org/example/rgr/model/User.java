package org.example.rgr.model;

public class User {
    private int id;
    private String login;
    private String password;
    
    public User() {}
    
    public User(int id, String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password;
    }
    
    public int getId() { return id; }
    public String getLogin() { return login; }
    public String getPassword() { return password; }
    
    public void setId(int id) { this.id = id; }
    public void setLogin(String login) { this.login = login; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean isAdmin() {
        return id == 1 && "admin123".equals(login);
    }
    
    @Override
    public String toString() {
        return "User{id=" + id + ", login='" + login + "'}";
    }
}