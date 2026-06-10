package org.example.rgr.dao;

import java.sql.*;

public class VoteDAO {

    // Голосование за вариант ответа
    public static boolean vote(int userId, int topicId, int answerId) throws SQLException {
        return DatabaseUtil.executeWrite(conn -> {
            // 1. Проверяем, не голосовал ли пользователь за эту тему
            String checkSql = "SELECT voted_topics FROM users WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, userId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String votedTopicsJson = rs.getString("voted_topics");
                if (votedTopicsJson != null && votedTopicsJson.contains(String.valueOf(topicId))) {
                    return false; // Уже голосовал
                }
            }

            // 2. Увеличиваем count в таблице answers
            String sqlAnswer = "UPDATE answers SET count = count + 1 WHERE id = ?";
            PreparedStatement pstmtAnswer = conn.prepareStatement(sqlAnswer);
            pstmtAnswer.setInt(1, answerId);
            pstmtAnswer.executeUpdate();

            // 3. Увеличиваем count_of_users в таблице topics
            String sqlTopic = "UPDATE topics SET count_of_users = count_of_users + 1 WHERE id = ?";
            PreparedStatement pstmtTopic = conn.prepareStatement(sqlTopic);
            pstmtTopic.setInt(1, topicId);
            pstmtTopic.executeUpdate();

            // 4. Обновляем voted_topics у пользователя
            String updateUserSql = "UPDATE users SET voted_topics = json_insert(voted_topics, '$[' || (SELECT count(*) FROM json_each(voted_topics)) || ']', ?) WHERE id = ?";
            // Для SQLite без JSON поддержки используем простой вариант:
            String getCurrentVotes = "SELECT voted_topics FROM users WHERE id = ?";
            PreparedStatement getStmt = conn.prepareStatement(getCurrentVotes);
            getStmt.setInt(1, userId);
            ResultSet rs2 = getStmt.executeQuery();

            if (rs2.next()) {
                String currentVotes = rs2.getString("voted_topics");
                if (currentVotes == null || currentVotes.equals("[]")) {
                    currentVotes = "[" + topicId + "]";
                } else {
                    currentVotes = currentVotes.substring(0, currentVotes.length() - 1) + "," + topicId + "]";
                }

                String updateSql = "UPDATE users SET voted_topics = ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, currentVotes);
                updateStmt.setInt(2, userId);
                updateStmt.executeUpdate();
            }

            return true;
        });
    }

    // Получить количество голосов за вариант ответа
    public static int getAnswerCount(int answerId) throws SQLException {
        String sql = "SELECT count FROM answers WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, answerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    // Получить общее количество проголосовавших за тему
    public static int getTopicVoteCount(int topicId) throws SQLException {
        String sql = "SELECT count_of_users FROM topics WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count_of_users");
            }
        }
        return 0;
    }
}