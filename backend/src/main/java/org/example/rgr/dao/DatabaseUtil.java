package org.example.rgr.dao;

import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:sqlite:voting.db";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    public static void initializeDatabase() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                login TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
        """;
        
        String createTopicsTable = """
            CREATE TABLE IF NOT EXISTS topics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name_question TEXT NOT NULL,
                many INTEGER DEFAULT 0,
                count_of_users INTEGER DEFAULT 0
            )
        """;
        
        String createAnswersTable = """
            CREATE TABLE IF NOT EXISTS answers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name_answer TEXT NOT NULL,
                topic_id INTEGER NOT NULL,
                count INTEGER DEFAULT 0,
                FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
            )
        """;
        
        String createVotesTable = """
            CREATE TABLE IF NOT EXISTS votes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                topic_id INTEGER NOT NULL,
                answer_id INTEGER NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE,
                FOREIGN KEY (answer_id) REFERENCES answers(id) ON DELETE CASCADE,
                UNIQUE(user_id, topic_id)
            )
        """;
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createUsersTable);
            stmt.execute(createTopicsTable);
            stmt.execute(createAnswersTable);
            stmt.execute(createVotesTable);
            
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE id = 1";
            ResultSet rs = stmt.executeQuery(checkAdmin);
            if (rs.next() && rs.getInt(1) == 0) {
                String hashedPassword = BCrypt.hashpw("123adm", BCrypt.gensalt());
                String insertAdmin = "INSERT INTO users (id, login, password) VALUES (1, 'admin123', ?)";
                PreparedStatement pstmt = conn.prepareStatement(insertAdmin);
                pstmt.setString(1, hashedPassword);
                pstmt.executeUpdate();
                System.out.println("✅ Админ создан: login=admin123, pass=123adm, id=1");
            }
            
            System.out.println("✅ База данных готова к работе!");
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка БД: " + e.getMessage());
        }
    }
}