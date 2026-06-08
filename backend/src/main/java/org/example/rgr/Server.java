package org.example.rgr;

import org.example.rgr.dao.DatabaseUtil;
import org.example.rgr.websocket.VotingWebSocketServer;

public class Server {
    public static void main(String[] args) {
        DatabaseUtil.initializeDatabase();
        
        VotingWebSocketServer webSocketServer = new VotingWebSocketServer(8000);
        webSocketServer.start();
        
        System.out.println("========================================");
        System.out.println("🚀 СЕРВЕР ЗАПУЩЕН!");
        System.out.println("📍 WebSocket: ws://localhost:8000");
        System.out.println("========================================");
    }
}