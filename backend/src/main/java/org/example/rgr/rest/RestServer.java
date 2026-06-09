package org.example.rgr.rest;

import org.example.rgr.dao.UserDAO;
import org.example.rgr.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import spark.Spark;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class RestServer {
    private static final Gson gson = new Gson();
    private static final int PORT = 3000;
    
    public static void start() {
        Spark.port(PORT);
        enableCORS();
        
        post("/api/register", (req, res) -> {
            JsonObject request = gson.fromJson(req.body(), JsonObject.class);
            String username = request.get("username").getAsString();
            String password = request.get("password").getAsString();
            
            if (username == null || username.trim().isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("error", "Имя пользователя не может быть пустым"));
            }
            if (password == null || password.length() < 5) {
                res.status(400);
                return gson.toJson(Map.of("error", "Пароль должен быть не менее 5 символов"));
            }
            if (UserDAO.exists(username)) {
                res.status(409);
                return gson.toJson(Map.of("error", "Пользователь уже существует"));
            }
            
            User user = UserDAO.register(username, password);
            if (user != null) {
                res.status(200);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Регистрация успешна");
                response.put("user", Map.of("id", user.getId(), "login", user.getLogin()));
                return gson.toJson(response);
            } else {
                res.status(500);
                return gson.toJson(Map.of("error", "Ошибка регистрации"));
            }
        });
        
        post("/api/login", (req, res) -> {
            JsonObject request = gson.fromJson(req.body(), JsonObject.class);
            String username = request.get("username").getAsString();
            String password = request.get("password").getAsString();
            
            User user = UserDAO.login(username, password);
            if (user != null) {
                res.status(200);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Вход выполнен успешно");
                response.put("user", Map.of("id", user.getId(), "login", user.getLogin()));
                return gson.toJson(response);
            } else {
                res.status(401);
                return gson.toJson(Map.of("error", "Неверный логин или пароль"));
            }
        });
        
        get("/api/health", (req, res) -> {
            return gson.toJson(Map.of("status", "ok"));
        });
        
        System.out.println("✅ REST API сервер запущен на порту " + PORT);
    }
    
    private static void enableCORS() {
        options("/*", (request, response) -> {
            response.header("Access-Control-Allow-Headers", "Content-Type");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            return "OK";
        });
        
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type");
            response.type("application/json");
        });
    }
}