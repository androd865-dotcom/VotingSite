package org.example.rgr.dao;

import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;
import java.util.Arrays;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:sqlite:voting.db";
    private static final Object LOCK = new Object();

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            // При первом запуске инициализируем БД с правильными настройками
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA busy_timeout = 30000");
                stmt.execute("PRAGMA synchronous = NORMAL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            // Эти настройки нужно применять к каждому соединению
            stmt.execute("PRAGMA busy_timeout = 30000");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = 10000");
        }
        return conn;
    }

    // Метод для инициализации базы данных
    public static void initializeDatabase() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                login TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                voted_topics TEXT DEFAULT '[]'
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

            createSampleTopics(conn);

            System.out.println("✅ База данных инициализирована!");

        } catch (SQLException e) {
            System.err.println("❌ Ошибка БД: " + e.getMessage());
        }
    }

    private static void createSampleTopics(Connection conn) {
        try {
            String checkTopics = "SELECT COUNT(*) FROM topics";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(checkTopics);

            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("📝 Создаем тестовые темы голосования...");

                // Тема 1
                String insertTopic1 = "INSERT INTO topics (name_question, many, count_of_users) VALUES (?, ?, 0)";
                PreparedStatement pstmt1 = conn.prepareStatement(insertTopic1, Statement.RETURN_GENERATED_KEYS);
                pstmt1.setString(1, "Каких животных ты любишь?");
                pstmt1.setInt(2, 1);
                pstmt1.executeUpdate();

                ResultSet topicKeys1 = pstmt1.getGeneratedKeys();
                int topicId1 = topicKeys1.next() ? topicKeys1.getInt(1) : -1;

                if (topicId1 != -1) {
                    List<String> answers1 = Arrays.asList("кошки", "собаки");
                    String insertAnswer = "INSERT INTO answers (name_answer, topic_id, count) VALUES (?, ?, 0)";
                    PreparedStatement answerStmt = conn.prepareStatement(insertAnswer);
                    for (String answer : answers1) {
                        answerStmt.setString(1, answer);
                        answerStmt.setInt(2, topicId1);
                        answerStmt.executeUpdate();
                    }
                    System.out.println("   ✅ Тема 1: 'Каких животных ты любишь?'");
                }

                // Тема 2
                String insertTopic2 = "INSERT INTO topics (name_question, many, count_of_users) VALUES (?, ?, 0)";
                PreparedStatement pstmt2 = conn.prepareStatement(insertTopic2, Statement.RETURN_GENERATED_KEYS);
                pstmt2.setString(1, "Какое покрытие лучше?");
                pstmt2.setInt(2, 0);
                pstmt2.executeUpdate();

                ResultSet topicKeys2 = pstmt2.getGeneratedKeys();
                int topicId2 = topicKeys2.next() ? topicKeys2.getInt(1) : -1;

                if (topicId2 != -1) {
                    List<String> answers2 = Arrays.asList("ламинат", "паркет", "линолеум");
                    String insertAnswer = "INSERT INTO answers (name_answer, topic_id, count) VALUES (?, ?, 0)";
                    PreparedStatement answerStmt = conn.prepareStatement(insertAnswer);
                    for (String answer : answers2) {
                        answerStmt.setString(1, answer);
                        answerStmt.setInt(2, topicId2);
                        answerStmt.executeUpdate();
                    }
                    System.out.println("   ✅ Тема 2: 'Какое покрытие лучше?'");
                }

                System.out.println("✅ Тестовые темы успешно созданы!");
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Ошибка при создании тестовых тем: " + e.getMessage());
        }
    }

    // Синхронизированный метод для операций записи
    public static <T> T executeWrite(DatabaseOperation<T> operation) throws SQLException {
        synchronized (LOCK) {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try {
                    T result = operation.execute(conn);
                    conn.commit();
                    return result;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
        }
    }

    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection conn) throws SQLException;
    }
}