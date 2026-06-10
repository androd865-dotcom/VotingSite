package org.example.rgr.rest;

import org.example.rgr.dao.UserDAO;
import org.example.rgr.dao.TopicDAO;
import org.example.rgr.model.User;
import org.example.rgr.model.Topic;
import org.example.rgr.model.Answer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import spark.Spark;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static spark.Spark.*;

public class RestServer {
    private static final Gson gson = new Gson();
    private static final int PORT = 3000;

    public static void start() {
        Spark.port(PORT);
        enableCORS();

        // ============ API РОУТЫ ============

        // Регистрация
        post("/api/register", (req, res) -> {
            JsonObject request = gson.fromJson(req.body(), JsonObject.class);
            String username = request.get("username").getAsString();
            String password = request.get("password").getAsString();

            System.out.println("📝 Регистрация: " + username);

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
                return gson.toJson(Map.of("error", "Пользователь с таким именем уже существует"));
            }

            User user = UserDAO.register(username, password);
            if (user != null) {
                res.status(200);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Регистрация успешна");
                response.put("user", Map.of(
                    "id", user.getId(),
                    "login", user.getLogin()
                ));
                return gson.toJson(response);
            } else {
                res.status(500);
                return gson.toJson(Map.of("error", "Ошибка регистрации"));
            }
        });

        // Вход
        post("/api/login", (req, res) -> {
            JsonObject request = gson.fromJson(req.body(), JsonObject.class);
            String username = request.get("username").getAsString();
            String password = request.get("password").getAsString();

            System.out.println("🔐 Вход: " + username);

            if (username == null || username.trim().isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("error", "Введите имя пользователя"));
            }

            if (password == null || password.isEmpty()) {
                res.status(400);
                return gson.toJson(Map.of("error", "Введите пароль"));
            }

            User user = UserDAO.login(username, password);
            if (user != null) {
                res.status(200);
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Вход выполнен успешно");
                response.put("user", Map.of(
                    "id", user.getId(),
                    "login", user.getLogin()
                ));
                return gson.toJson(response);
            } else {
                res.status(401);
                return gson.toJson(Map.of("error", "Неверный логин или пароль"));
            }
        });

        // Проверка авторизации
        get("/api/auth/check", (req, res) -> {
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("login", "admin123");
            return gson.toJson(response);
        });

        // Получить информацию о пользователе
        get("/api/user/:login", (req, res) -> {
            String login = req.params(":login");
            User user = UserDAO.getUserByLogin(login);

            if (user != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("login", user.getLogin());
                response.put("votedTopics", user.getVotedTopics());
                return gson.toJson(response);
            } else {
                res.status(404);
                return gson.toJson(Map.of("error", "Пользователь не найден"));
            }
        });

        // НОВЫЙ ЭНДПОИНТ: Получить статус голосов пользователя
        get("/api/votes/:username", (req, res) -> {
            String username = req.params(":username");

            User user = UserDAO.getUserByLogin(username);
            if (user == null) {
                res.status(404);
                return gson.toJson(Map.of("error", "Пользователь не найден"));
            }

            // Получаем все темы
            List<Topic> allTopics = TopicDAO.getAll();

            // Формируем ответ
            List<Map<String, Object>> result = new ArrayList<>();
            for (Topic topic : allTopics) {
                Map<String, Object> topicData = new HashMap<>();
                topicData.put("id", topic.getId());
                topicData.put("name", topic.getNameQuestion());
                topicData.put("many", topic.isMany());

                // Проверяем, голосовал ли пользователь
                boolean hasVoted = user.getVotedTopics().contains(topic.getId());
                topicData.put("hasVoted", hasVoted);

                // Варианты ответов
                List<Map<String, Object>> variants = new ArrayList<>();
                for (Answer answer : topic.getAnswers()) {
                    Map<String, Object> variant = new HashMap<>();
                    variant.put("id", answer.getId());
                    variant.put("name", answer.getNameAnswer());
                    variant.put("count", answer.getCount());
                    variants.add(variant);
                }
                topicData.put("variants", variants);

                result.add(topicData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("votes", result);

            return gson.toJson(response);
        });

        // Проверка здоровья сервера
        get("/api/health", (req, res) -> {
            return gson.toJson(Map.of("status", "ok", "timestamp", System.currentTimeMillis()));
        });

        System.out.println("✅ REST API сервер запущен на порту " + PORT);
        System.out.println("   POST   /api/register  - регистрация");
        System.out.println("   POST   /api/login     - вход");
        System.out.println("   GET    /api/auth/check - проверка авторизации");
        System.out.println("   GET    /api/user/:login - информация о пользователе");
        System.out.println("   GET    /api/votes/:username - статус голосов пользователя");
        System.out.println("   GET    /api/health    - проверка сервера");
    }

    private static void enableCORS() {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            response.type("application/json");
        });
    }

    public static void stop() {
        Spark.stop();
        System.out.println("🛑 REST API сервер остановлен");
    }
}