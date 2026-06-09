package org.example.rgr.dao;

import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:sqlite:voting.db";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    public static void initializeDatabase() {
        // 1. Таблица пользователей
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                login TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                voted_topics TEXT DEFAULT '[]'
            )
        """;
        
        // 2. Таблица тем
        String createTopicsTable = """
            CREATE TABLE IF NOT EXISTS topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name_question TEXT NOT NULL,
                many INTEGER DEFAULT 0,
                count_of_users INTEGER DEFAULT 0
            )
        """;
        
        // 3. Таблица ответов
        String createAnswersTable = """
            CREATE TABLE IF NOT EXISTS answers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name_answer TEXT NOT NULL,
                topic_id INTEGER NOT NULL,
                count INTEGER DEFAULT 0,
                FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
            )
        """;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createUsersTable);
            stmt.execute(createTopicsTable);
            stmt.execute(createAnswersTable);
            
            // Создаем админа
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE login = 'admin123'";
            ResultSet rs = stmt.executeQuery(checkAdmin);
            if (rs.next() && rs.getInt(1) == 0) {
                String hashedPassword = BCrypt.hashpw("123adm", BCrypt.gensalt());
                String insertAdmin = "INSERT INTO users (login, password, voted_topics) VALUES (?, ?, '[]')";
                PreparedStatement pstmt = conn.prepareStatement(insertAdmin);
                pstmt.setString(1, "admin123");
                pstmt.setString(2, hashedPassword);
                pstmt.executeUpdate();
                System.out.println("✅ Админ создан: login=admin123, pass=123adm");
            }
            
            System.out.println("✅ База данных инициализирована!");
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка БД: " + e.getMessage());
        }
    }
}