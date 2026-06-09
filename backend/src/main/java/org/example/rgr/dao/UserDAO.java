package org.example.rgr.dao;

import org.example.rgr.model.User;
import org.mindrot.jbcrypt.BCrypt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final Gson gson = new Gson();

    // Регистрация
    public static User register(String login, String password) throws SQLException {
        if (exists(login)) {
            return null;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (login, password, voted_topics) VALUES (?, ?, '[]')";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, login);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt(1));
                user.setLogin(login);
                user.setPassword(hashedPassword);
                user.setVotedTopics(new ArrayList<>());
                return user;
            }
        }
        return null;
    }

    // Вход
    public static User login(String login, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    String votedTopicsJson = rs.getString("voted_topics");
                    List<Integer> votedTopics = parseVotedTopics(votedTopicsJson);

                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setLogin(rs.getString("login"));
                    user.setPassword(hashedPassword);
                    user.setVotedTopics(votedTopics);
                    return user;
                }
            }
        }
        return null;
    }

    // Проверка существования
    public static boolean exists(String login) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE login = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // Получить пользователя по логину
    public static User getUserByLogin(String login) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String votedTopicsJson = rs.getString("voted_topics");
                List<Integer> votedTopics = parseVotedTopics(votedTopicsJson);

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setLogin(rs.getString("login"));
                user.setPassword(rs.getString("password"));
                user.setVotedTopics(votedTopics);
                return user;
            }
        }
        return null;
    }

    // Получить пользователя по ID
    public static User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String votedTopicsJson = rs.getString("voted_topics");
                List<Integer> votedTopics = parseVotedTopics(votedTopicsJson);

                User user = new User();
                user.setId(rs.getInt("id"));
                user.setLogin(rs.getString("login"));
                user.setPassword(rs.getString("password"));
                user.setVotedTopics(votedTopics);
                return user;
            }
        }
        return null;
    }

    // Получить пользователя по ID (для обратной совместимости)
    public static User getById(int id) throws SQLException {
        return getUserById(id);
    }

    // Добавить тему в список проголосованных
    public static void addVotedTopic(int userId, int topicId) throws SQLException {
        User user = getUserById(userId);
        if (user == null) return;
        
        if (!user.getVotedTopics().contains(topicId)) {
            user.getVotedTopics().add(topicId);
        }
        
        String newVotedTopicsJson = gson.toJson(user.getVotedTopics());
        String sql = "UPDATE users SET voted_topics = ? WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newVotedTopicsJson);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    // Проверить, голосовал ли пользователь за тему
    public static boolean hasVoted(int userId, int topicId) throws SQLException {
        User user = getUserById(userId);
        return user != null && user.getVotedTopics().contains(topicId);
    }

    // Парсим JSON строку в List<Integer>
    private static List<Integer> parseVotedTopics(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return gson.fromJson(json, new TypeToken<List<Integer>>(){}.getType());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}