package org.example.rgr;

import org.example.rgr.dao.DatabaseUtil;
import org.example.rgr.rest.RestServer;
import org.example.rgr.websocket.VotingWebSocketServer;

public class Server {
    public static void main(String[] args) {
        DatabaseUtil.initializeDatabase();
        RestServer.start();
        
        VotingWebSocketServer webSocketServer = new VotingWebSocketServer(8000);
        webSocketServer.start();
        
        System.out.println("========================================");
        System.out.println("🚀 ВСЕ СЕРВЕРЫ ЗАПУЩЕНЫ!");
        System.out.println("📍 REST API:   http://localhost:3000");
        System.out.println("📍 WebSocket:  ws://localhost:8000");
        System.out.println("📝 Администратор: login=admin123, password=123adm");
        System.out.println("========================================");
    }
}