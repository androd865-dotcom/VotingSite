package org.example.rgr.websocket;

import org.example.rgr.model.User;
import org.example.rgr.model.VoteData;
import org.example.rgr.service.AuthService;
import org.example.rgr.service.TopicService;
import org.example.rgr.service.VoteService;
import org.example.rgr.dao.UserDAO;
import org.example.rgr.dao.TopicDAO;
import org.example.rgr.dao.VoteDAO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VotingWebSocketServer extends WebSocketServer {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<WebSocket, User> sessions = new ConcurrentHashMap<>();
    
    private final AuthService authService = new AuthService();
    private final TopicService topicService = new TopicService();
    private final VoteService voteService = new VoteService();
    
    public VotingWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✅ Новое подключение: " + conn.getRemoteSocketAddress());
        sessions.put(conn, null);
        sendAllVotes(conn, null);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📨 Получено: " + message);
        
        if (message.equals("Привет, сервер!")) {
            sendAllVotes(conn, sessions.get(conn));
            return;
        }
        
        try {
            Map<String, Object> request = gson.fromJson(message, Map.class);
            
            // 🔥 ПРОВЕРКА НА НАЛИЧИЕ КОМАНДЫ (create, update, delete)
            if (request.containsKey("type")) {
                String command = (String) request.get("type");
                handleCommand(conn, command, request);
            } else if (request.containsKey("action")) {
                String action = (String) request.get("action");
                handleAction(conn, action, request);
            } else if (request.containsKey("id") && request.containsKey("votes")) {
                handleVote(conn, request);
            } else {
                sendError(conn, "Неизвестный формат сообщения");
            }
            
        } catch (Exception e) {
            System.out.println("Ошибка парсинга: " + e.getMessage());
            sendError(conn, "Ошибка обработки сообщения");
        }
    }
    
    // 🔥 ОБРАБОТКА КОМАНД create, update, delete
    private void handleCommand(WebSocket conn, String command, Map<String, Object> request) {
        User user = sessions.get(conn);
        
        // Проверка авторизации
        if (user == null) {
            sendError(conn, "Необходимо авторизоваться");
            return;
        }
        
        // Проверка прав администратора
        if (!user.getLogin().equals("admin123")) {
            sendError(conn, "Доступ запрещен. Только администратор может выполнять эту операцию");
            return;
        }
        
        switch (command) {
            case "create":
                handleCreate(conn, request);
                break;
            case "update":
                handleUpdate(conn, request);
                break;
            case "delete":
                handleDelete(conn, request);
                break;
            default:
                sendError(conn, "Неизвестная команда: " + command);
        }
    }
    
    // 🔥 CREATE - создание нового голосования
    // Формат: { type: "create", header: "...", many: true/false, variants: ["...", "..."] }
    private void handleCreate(WebSocket conn, Map<String, Object> request) {
        try {
            String header = (String) request.get("header");
            Boolean many = (Boolean) request.get("many");
            List<String> variants = (List<String>) request.get("variants");
            
            System.out.println("📝 CREATE: Создание голосования");
            System.out.println("   Заголовок: " + header);
            System.out.println("   Множественный выбор: " + many);
            System.out.println("   Варианты: " + variants);
            
            // Валидация
            if (header == null || header.trim().isEmpty()) {
                sendError(conn, "Введите заголовок голосования");
                return;
            }
            
            if (variants == null || variants.size() < 2) {
                sendError(conn, "Добавьте минимум 2 варианта ответа");
                return;
            }
            
            // Сохраняем в БД
            int topicId = TopicDAO.create(header, many, variants);
            
            if (topicId > 0) {
                System.out.println("✅ Голосование создано! ID: " + topicId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("type", "CREATE_SUCCESS");
                response.put("message", "Голосование \"" + header + "\" успешно создано!");
                response.put("id", topicId);
                conn.send(gson.toJson(response));
                
                // Обновляем всех клиентов
                sendAllVotesToAll();
            } else {
                sendError(conn, "Ошибка при сохранении голосования");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка CREATE: " + e.getMessage());
            sendError(conn, "Ошибка создания: " + e.getMessage());
        }
    }
    
    // 🔥 UPDATE - обновление существующего голосования
    // Формат: { type: "update", id: 1, header: "...", many: true/false, variants: ["...", "..."] }
    private void handleUpdate(WebSocket conn, Map<String, Object> request) {
        try {
            Integer id = null;
            if (request.containsKey("id")) {
                id = ((Double) request.get("id")).intValue();
            }
            
            String header = (String) request.get("header");
            Boolean many = (Boolean) request.get("many");
            List<String> variants = (List<String>) request.get("variants");
            
            System.out.println("📝 UPDATE: Обновление голосования ID=" + id);
            System.out.println("   Новый заголовок: " + header);
            System.out.println("   Множественный выбор: " + many);
            System.out.println("   Варианты: " + variants);
            
            // Валидация
            if (id == null) {
                sendError(conn, "Укажите ID голосования для обновления");
                return;
            }
            
            if (header == null || header.trim().isEmpty()) {
                sendError(conn, "Введите заголовок голосования");
                return;
            }
            
            if (variants == null || variants.size() < 2) {
                sendError(conn, "Добавьте минимум 2 варианта ответа");
                return;
            }
            
            // Проверяем, существует ли тема
            var existingTopic = TopicDAO.getById(id);
            if (existingTopic == null) {
                sendError(conn, "Голосование с ID=" + id + " не найдено");
                return;
            }
            
            // Обновляем заголовок темы
            TopicDAO.updateName(id, header);
            
            // Обновляем флаг many
            TopicDAO.updateMany(id, many);
            
            // Обновляем варианты ответов (сохраняя старые id, новые получают свободные id)
            TopicDAO.updateAnswers(id, variants);
            
            System.out.println("✅ Голосование ID=" + id + " обновлено!");
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "UPDATE_SUCCESS");
            response.put("message", "Голосование \"" + header + "\" успешно обновлено!");
            response.put("id", id);
            conn.send(gson.toJson(response));
            
            // Обновляем всех клиентов
            sendAllVotesToAll();
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка UPDATE: " + e.getMessage());
            sendError(conn, "Ошибка обновления: " + e.getMessage());
        }
    }
    
    // 🔥 DELETE - удаление голосования
    // Формат: { type: "delete", id: 1 }
    private void handleDelete(WebSocket conn, Map<String, Object> request) {
        try {
            Integer id = null;
            if (request.containsKey("id")) {
                id = ((Double) request.get("id")).intValue();
            }
            
            System.out.println("📝 DELETE: Удаление голосования ID=" + id);
            
            if (id == null) {
                sendError(conn, "Укажите ID голосования для удаления");
                return;
            }
            
            // Проверяем, существует ли тема
            var existingTopic = TopicDAO.getById(id);
            if (existingTopic == null) {
                sendError(conn, "Голосование с ID=" + id + " не найдено");
                return;
            }
            
            String deletedHeader = existingTopic.getNameQuestion();
            
            // Удаляем тему (ответы удалятся каскадно)
            TopicDAO.delete(id);
            
            System.out.println("✅ Голосование ID=" + id + " удалено!");
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "DELETE_SUCCESS");
            response.put("message", "Голосование \"" + deletedHeader + "\" успешно удалено!");
            response.put("id", id);
            conn.send(gson.toJson(response));
            
            // Обновляем всех клиентов
            sendAllVotesToAll();
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка DELETE: " + e.getMessage());
            sendError(conn, "Ошибка удаления: " + e.getMessage());
        }
    }
    
    private void handleAction(WebSocket conn, String action, Map<String, Object> request) {
        switch (action) {
            case "auth":
                handleAuth(conn, request);
                break;
            case "register":
                handleRegister(conn, request);
                break;
            case "get_topics":
                User user = sessions.get(conn);
                sendAllVotes(conn, user);
                break;
            default:
                sendError(conn, "Неизвестное действие: " + action);
        }
    }
    
    private void handleAuth(WebSocket conn, Map<String, Object> request) {
        String login = (String) request.get("login");
        String password = (String) request.get("password");
        
        AuthService.AuthResult result = authService.login(login, password);
        
        if (result.isSuccess()) {
            sessions.put(conn, result.getUser());
            sendAllVotes(conn, result.getUser());
        } else {
            sendError(conn, result.getMessage());
        }
    }
    
    private void handleRegister(WebSocket conn, Map<String, Object> request) {
        String login = (String) request.get("login");
        String password = (String) request.get("password");
        
        AuthService.AuthResult result = authService.register(login, password);
        
        if (!result.isSuccess()) {
            sendError(conn, result.getMessage());
        }
    }
    
    private void handleVote(WebSocket conn, Map<String, Object> request) {
        User user = sessions.get(conn);
        if (user == null) {
            sendError(conn, "Необходимо авторизоваться");
            return;
        }
        
        try {
            int topicId = ((Double) request.get("id")).intValue();
            List<Double> votesDouble = (List<Double>) request.get("votes");
            
            List<Integer> answerIds = new ArrayList<>();
            for (Double d : votesDouble) {
                answerIds.add(d.intValue());
            }
            
            if (answerIds.isEmpty()) {
                sendError(conn, "Не выбран ни один вариант");
                return;
            }
            
            VoteService.VoteResult result = voteService.processVote(user.getId(), topicId, answerIds);
            
            if (result.isSuccess()) {
                User updatedUser = UserDAO.getById(user.getId());
                sessions.put(conn, updatedUser);
                sendAllVotesToAll();
            } else {
                sendError(conn, result.getMessage());
            }
        } catch (Exception e) {
            sendError(conn, "Ошибка голосования: " + e.getMessage());
        }
    }
    
    private void sendAllVotes(WebSocket conn, User user) {
        try {
            int userId = (user != null) ? user.getId() : -1;
            List<VoteData> votes = topicService.getAllVotesForFrontend(userId);
            String jsonResponse = gson.toJson(votes);
            conn.send(jsonResponse);
            System.out.println("📤 Отправлено " + votes.size() + " голосований" + 
                              (user != null ? " для " + user.getLogin() : " (гость)"));
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки: " + e.getMessage());
            conn.send("[]");
        }
    }
    
    private void sendAllVotesToAll() {
        for (Map.Entry<WebSocket, User> entry : sessions.entrySet()) {
            WebSocket conn = entry.getKey();
            User user = entry.getValue();
            int userId = (user != null) ? user.getId() : -1;
            List<VoteData> votes = topicService.getAllVotesForFrontend(userId);
            conn.send(gson.toJson(votes));
        }
        System.out.println("📤 Обновление отправлено всем (" + sessions.size() + " клиентов)");
    }
    
    private void sendError(WebSocket conn, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "ERROR");
        error.put("message", errorMessage);
        error.put("timestamp", System.currentTimeMillis());
        conn.send(gson.toJson(error));
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        User user = sessions.remove(conn);
        if (user != null) {
            System.out.println("👋 Пользователь " + user.getLogin() + " отключился");
        }
        System.out.println("❌ Отключение: " + conn.getRemoteSocketAddress());
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ Ошибка: " + ex.getMessage());
        if (conn != null) {
            sendError(conn, "Внутренняя ошибка сервера");
        }
    }
    
    @Override
    public void onStart() {
        System.out.println("========================================");
        System.out.println("🚀 WEBSOCKET СЕРВЕР ЗАПУЩЕН!");
        System.out.println("📍 Адрес: ws://localhost:8000");
        System.out.println("📝 Администратор: login=admin123, password=123adm");
        System.out.println("========================================");
        System.out.println("💡 Форматы команд:");
        System.out.println("   CREATE: { type: 'create', header: '...', many: true/false, variants: [...] }");
        System.out.println("   UPDATE: { type: 'update', id: 1, header: '...', many: true/false, variants: [...] }");
        System.out.println("   DELETE: { type: 'delete', id: 1 }");
        System.out.println("========================================");
    }
}