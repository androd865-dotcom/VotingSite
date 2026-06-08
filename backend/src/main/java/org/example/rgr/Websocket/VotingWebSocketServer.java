package org.example.rgr.websocket;

import org.example.rgr.model.User;
import org.example.rgr.model.VoteData;
import org.example.rgr.service.AuthService;
import org.example.rgr.service.TopicService;
import org.example.rgr.service.VoteService;
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
        sendAllVotes(conn);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📨 Получено: " + message);
        
        try {
            Map<String, Object> request = gson.fromJson(message, Map.class);
            String action = (String) request.get("action");
            
            switch (action) {
                case "auth":
                    handleAuth(conn, request);
                    break;
                case "register":
                    handleRegister(conn, request);
                    break;
                case "vote":
                    handleVote(conn, request);
                    break;
                case "get_topics":
                    sendAllVotes(conn);
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
        } catch (Exception e) {
            if (message.equals("Привет, сервер!")) {
                sendMessage(conn, "CONNECTED", "Сервер подключен!", null);
            } else {
                System.out.println("Не JSON: " + message);
            }
        }
    }
    
    private void handleAuth(WebSocket conn, Map<String, Object> request) {
        String login = (String) request.get("login");
        String password = (String) request.get("password");
        
        AuthService.AuthResult result = authService.login(login, password);
        
        if (result.isSuccess()) {
            sessions.put(conn, result.getUser());
            sendMessage(conn, "AUTH_SUCCESS", result.getMessage(), result.getUser());
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
            int topicId = ((Double) request.get("topicId")).intValue();
            int answerId = ((Double) request.get("answerId")).intValue();
            
            VoteService.VoteResult result = voteService.processVote(user.getId(), topicId, answerId);
            
            if (result.isSuccess()) {
                sendMessage(conn, "VOTE_SUCCESS", result.getMessage(), result.getData());
                broadcastToAll("STATS_UPDATE", "Статистика обновлена", null);
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
    
    private void sendAllVotes(WebSocket conn) {
        List<VoteData> votes = topicService.getAllVotesForFrontend();
        conn.send(gson.toJson(votes));
        System.out.println("📤 Отправлено " + votes.size() + " голосований клиенту");
    }
    
    private void sendAllVotesToAll() {
        List<VoteData> votes = topicService.getAllVotesForFrontend();
        String json = gson.toJson(votes);
        for (WebSocket conn : sessions.keySet()) {
            conn.send(json);
        }
        System.out.println("📤 Обновление отправлено всем (" + sessions.size() + " клиентов)");
    }
    
    private void sendMessage(WebSocket conn, String type, String message, Object data) {
        WebSocketMessage msg = new WebSocketMessage(type, message, data);
        conn.send(gson.toJson(msg));
    }
    
    private void broadcastToAll(String type, String message, Object data) {
        WebSocketMessage msg = new WebSocketMessage(type, message, data);
        String json = gson.toJson(msg);
        for (WebSocket conn : sessions.keySet()) {
            conn.send(json);
        }
    }
    
    private void sendError(WebSocket conn, String errorMessage) {
        WebSocketMessage error = new WebSocketMessage("ERROR", errorMessage, null);
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
        System.out.println("💡 Ожидание подключений...");
    }
}