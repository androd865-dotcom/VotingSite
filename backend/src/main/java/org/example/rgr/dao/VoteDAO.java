package org.example.rgr.dao;

import java.sql.*;

public class VoteDAO {
    
    // Голосование за вариант ответа
    public static boolean vote(int userId, int topicId, int answerId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Увеличиваем count в таблице answers
            String sqlAnswer = "UPDATE answers SET count = count + 1 WHERE id = ?";
            PreparedStatement pstmtAnswer = conn.prepareStatement(sqlAnswer);
            pstmtAnswer.setInt(1, answerId);
            pstmtAnswer.executeUpdate();
            
            // 2. Увеличиваем count_of_users в таблице topics
            String sqlTopic = "UPDATE topics SET count_of_users = count_of_users + 1 WHERE id = ?";
            PreparedStatement pstmtTopic = conn.prepareStatement(sqlTopic);
            pstmtTopic.setInt(1, topicId);
            pstmtTopic.executeUpdate();
            
            // 3. Добавляем тему в список проголосованных у пользователя
            UserDAO.addVotedTopic(userId, topicId);
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
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