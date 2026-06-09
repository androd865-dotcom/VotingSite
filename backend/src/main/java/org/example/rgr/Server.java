package org.example.rgr;

import org.example.rgr.dao.DatabaseUtil;
import org.example.rgr.rest.RestServer;
import org.example.rgr.websocket.VotingWebSocketServer;

public class Server {
    public static void main(String[] args) {
        // 1. Инициализация базы данных
        DatabaseUtil.initializeDatabase();
        
        // 2. Запуск REST API сервера на порту 3000
        RestServer.start();
        
        // 3. Запуск WebSocket сервера на порту 8000
        VotingWebSocketServer webSocketServer = new VotingWebSocketServer(8000);
        webSocketServer.start();
        
        System.out.println("========================================");
        System.out.println("🚀 ВСЕ СЕРВЕРЫ ЗАПУЩЕНЫ!");
        System.out.println("📍 REST API:   http://localhost:3000");
        System.out.println("📍 WebSocket:  ws://localhost:8000");
        System.out.println("📝 Администратор: login=admin123, password=123adm");
        System.out.println("========================================");
        System.out.println("💡 Доступные REST маршруты:");
        System.out.println("   POST /api/register  - регистрация");
        System.out.println("   POST /api/login     - вход");
        System.out.println("   GET  /api/user/:login - информация о пользователе");
        System.out.println("========================================");
    }
}