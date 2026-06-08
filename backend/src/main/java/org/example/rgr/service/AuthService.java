package org.example.rgr.service;

import org.example.rgr.dao.UserDAO;
import org.example.rgr.model.User;

public class AuthService {
    
    public static class AuthResult {
        private boolean success;
        private String message;
        private User user;
        
        public static AuthResult success(User user, String message) {
            AuthResult result = new AuthResult();
            result.success = true;
            result.message = message;
            result.user = user;
            return result;
        }
        
        public static AuthResult error(String message) {
            AuthResult result = new AuthResult();
            result.success = false;
            result.message = message;
            return result;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }
    
    public AuthResult register(String login, String password) {
        try {
            if (login == null || login.trim().isEmpty()) {
                return AuthResult.error("Логин не может быть пустым");
            }
            if (password == null || password.length() < 4) {
                return AuthResult.error("Пароль должен быть не менее 4 символов");
            }
            
            if (UserDAO.exists(login)) {
                return AuthResult.error("Пользователь уже существует");
            }
            
            User user = UserDAO.register(login, password);
            if (user != null) {
                user.setPassword(null);
                return AuthResult.success(user, "Регистрация успешна!");
            }
            return AuthResult.error("Ошибка регистрации");
            
        } catch (Exception e) {
            return AuthResult.error("Ошибка: " + e.getMessage());
        }
    }
    
    public AuthResult login(String login, String password) {
        try {
            User user = UserDAO.login(login, password);
            if (user != null) {
                user.setPassword(null);
                return AuthResult.success(user, "Вход выполнен успешно!");
            }
            return AuthResult.error("Неверный логин или пароль");
            
        } catch (Exception e) {
            return AuthResult.error("Ошибка: " + e.getMessage());
        }
    }
}