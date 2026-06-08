package org.example.rgr.dao;

import java.sql.*;

public class VoteDAO {
    
    public static boolean vote(int userId, int topicId, int answerId) throws SQLException {
        if (hasVoted(userId, topicId)) {
            return false;
        }
        
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            
            String sqlVote = "INSERT INTO votes (user_id, topic_id, answer_id) VALUES (?, ?, ?)";
            PreparedStatement pstmtVote = conn.prepareStatement(sqlVote);
            pstmtVote.setInt(1, userId);
            pstmtVote.setInt(2, topicId);
            pstmtVote.setInt(3, answerId);
            pstmtVote.executeUpdate();
            
            String sqlAnswer = "UPDATE answers SET count = count + 1 WHERE id = ?";
            PreparedStatement pstmtAnswer = conn.prepareStatement(sqlAnswer);
            pstmtAnswer.setInt(1, answerId);
            pstmtAnswer.executeUpdate();
            
            String sqlTopic = "UPDATE topics SET count_of_users = count_of_users + 1 WHERE id = ?";
            PreparedStatement pstmtTopic = conn.prepareStatement(sqlTopic);
            pstmtTopic.setInt(1, topicId);
            pstmtTopic.executeUpdate();
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }
    
    public static boolean hasVoted(int userId, int topicId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes WHERE user_id = ? AND topic_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, topicId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
    
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