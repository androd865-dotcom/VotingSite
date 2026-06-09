package org.example.rgr.websocket;

import org.example.rgr.model.User;
import org.example.rgr.model.VoteData;
import org.example.rgr.service.AuthService;
import org.example.rgr.service.TopicService;
import org.example.rgr.service.VoteService;
import org.example.rgr.dao.UserDAO;

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
        
        // Отправляем все голосования (без авторизации - только просмотр)
        sendAllVotes(conn, null);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📨 Получено: " + message);
        
        if (message.equals("Привет, сервер!")) {
            sendMessage(conn, "CONNECTED", "Сервер подключен!", null);
            return;
        }
        
        try {
            Map<String, Object> request = gson.fromJson(message, Map.class);
            
            if (request.containsKey("action")) {
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
            case "create_topic":
                handleCreateTopic(conn, request);
                break;
            case "delete_topic":
                handleDeleteTopic(conn, request);
                break;
            case "update_topic":
                handleUpdateTopic(conn, request);
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
            sendMessage(conn, "AUTH_SUCCESS", result.getMessage(), result.getUser());
            
            // После входа отправляем обновленный список тем с флагом hasVoted
            sendAllVotes(conn, result.getUser());
            
            broadcastToAll("USER_JOINED", "Пользователь " + login + " присоединился", null);
        } else {
            sendError(conn, result.getMessage());
        }
    }
    
    private void handleRegister(WebSocket conn, Map<String, Object> request) {
        String login = (String) request.get("login");
        String password = (String) request.get("password");
        
        AuthService.AuthResult result = authService.register(login, password);
        
        if (result.isSuccess()) {
            sendMessage(conn, "REGISTER_SUCCESS", result.getMessage(), result.getUser());
        } else {
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
                sendMessage(conn, "VOTE_SUCCESS", result.getMessage(), result.getData());
                
                // Обновляем пользователя в сессии (обновленный список votedTopics)
                User updatedUser = UserDAO.getById(user.getId());
                sessions.put(conn, updatedUser);
                
                // Отправляем обновленный список всем клиентам
                sendAllVotesToAll();
            } else {
                sendError(conn, result.getMessage());
            }
        } catch (Exception e) {
            sendError(conn, "Ошибка голосования: " + e.getMessage());
        }
    }
    
    private void handleCreateTopic(WebSocket conn, Map<String, Object> request) {
        User user = sessions.get(conn);
        if (user == null || user.getId() != 1) {
            sendError(conn, "Доступ запрещен. Требуются права администратора");
            return;
        }
        
        String nameQuestion = (String) request.get("name_question");
        boolean many = (boolean) request.get("many");
        List<String> answers = (List<String>) request.get("answers");
        
        TopicService.TopicResult result = topicService.createTopic(nameQuestion, many, answers, user.getId());
        
        if (result.isSuccess()) {
            sendMessage(conn, "TOPIC_CREATED", result.getMessage(), null);
            sendAllVotesToAll();
        } else {
            sendError(conn, result.getMessage());
        }
    }
    
    private void handleDeleteTopic(WebSocket conn, Map<String, Object> request) {
        User user = sessions.get(conn);
        if (user == null || user.getId() != 1) {
            sendError(conn, "Доступ запрещен. Требуются права администратора");
            return;
        }
        
        int topicId = ((Double) request.get("topicId")).intValue();
        
        TopicService.TopicResult result = topicService.deleteTopic(topicId, user.getId());
        
        if (result.isSuccess()) {
            sendMessage(conn, "TOPIC_DELETED", result.getMessage(), null);
            sendAllVotesToAll();
        } else {
            sendError(conn, result.getMessage());
        }
    }
    
    private void handleUpdateTopic(WebSocket conn, Map<String, Object> request) {
        User user = sessions.get(conn);
        if (user == null || user.getId() != 1) {
            sendError(conn, "Доступ запрещен. Требуются права администратора");
            return;
        }
        
        int topicId = ((Double) request.get("topicId")).intValue();
        String newName = (String) request.get("name_question");
        
        TopicService.TopicResult result = topicService.updateTopicName(topicId, newName, user.getId());
        
        if (result.isSuccess()) {
            sendMessage(conn, "TOPIC_UPDATED", result.getMessage(), null);
            sendAllVotesToAll();
        } else {
            sendError(conn, result.getMessage());
        }
    }
    
    // Отправка всех голосований с учетом проголосованных тем пользователя
    private void sendAllVotes(WebSocket conn, User user) {
        int userId = (user != null) ? user.getId() : -1;
        List<VoteData> votes = topicService.getAllVotesForFrontend(userId);
        String jsonResponse = gson.toJson(votes);
        conn.send(jsonResponse);
        System.out.println("📤 Отправлено " + votes.size() + " голосований" + 
                          (user != null ? " для пользователя " + user.getLogin() : " (гость)"));
    }
    
    // Отправка обновлений всем клиентам
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
    
    private void sendMessage(WebSocket conn, String type, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        conn.send(gson.toJson(response));
    }
    
    private void broadcastToAll(String type, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        String json = gson.toJson(response);
        for (WebSocket conn : sessions.keySet()) {
            conn.send(json);
        }
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
            broadcastToAll("USER_LEFT", "Пользователь " + user.getLogin() + " покинул чат", null);
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
        System.out.println("💡 Формат для фронта:");
        System.out.println("   [ { id, header, many, hasVoted, variants: [{id, name}] } ]");
        System.out.println("========================================");
    }
}