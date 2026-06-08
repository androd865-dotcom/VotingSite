package org.example.rgr.dao;

import org.example.rgr.model.Option;
import org.example.rgr.util.DatabaseUtil;
import java.sql.*;
import java.util.*;

public class VoteDAO {

    public static boolean vote(int userId, int topicId, int optionId) throws SQLException {
        if (hasUserVoted(userId, topicId)) {
            return false;
        }

        String sql = "INSERT INTO votes (topic_id, option_id, user_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            pstmt.setInt(2, optionId);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
            return true;
        }
    }

    public static boolean hasUserVoted(int userId, int topicId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes WHERE user_id = ? AND topic_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, topicId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public static List<Option> getStatistics(int topicId) throws SQLException {
        String sql = """
            SELECT o.id, o.topic_id, o.option_text, COUNT(v.id) as vote_count 
            FROM options o 
            LEFT JOIN votes v ON o.id = v.option_id 
            WHERE o.topic_id = ? 
            GROUP BY o.id
        """;

        List<Option> statistics = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                statistics.add(new Option(
                        rs.getInt("id"),
                        rs.getInt("topic_id"),
                        rs.getString("option_text"),
                        rs.getInt("vote_count")
                ));
            }
        }
        return statistics;
    }
}