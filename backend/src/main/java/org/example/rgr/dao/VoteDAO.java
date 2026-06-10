package org.example.rgr.dao;

import java.sql.*;

public class VoteDAO {

    // 🔥 Только увеличивает count в answers, без проверки voted_topics
    public static boolean voteForAnswer(int answerId) throws SQLException {
        return DatabaseUtil.executeWrite(conn -> {
            // Увеличиваем count ТОЛЬКО в таблице answers
            String sqlAnswer = "UPDATE answers SET count = count + 1 WHERE id = ?";
            PreparedStatement pstmtAnswer = conn.prepareStatement(sqlAnswer);
            pstmtAnswer.setInt(1, answerId);
            int updated = pstmtAnswer.executeUpdate();
            System.out.println("   📊 Answer " + answerId + " count увеличен на 1, строк обновлено: " + updated);
            return updated > 0;
        });
    }

    // 🔥 Увеличить count_of_users в topics
    public static void incrementTopicVoteCount(int topicId) throws SQLException {
        String sql = "UPDATE topics SET count_of_users = count_of_users + 1 WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            int updated = pstmt.executeUpdate();
            System.out.println("   📊 Topic " + topicId + " count_of_users увеличен, строк обновлено: " + updated);
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