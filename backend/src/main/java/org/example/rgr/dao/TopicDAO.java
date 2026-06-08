package org.example.rgr.dao;

import org.example.rgr.model.Topic;
import org.example.rgr.model.Option;
import org.example.rgr.util.DatabaseUtil;
import javafx.collections.FXCollections;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TopicDAO {

    public static void createTopic(String title, List<String> optionTexts) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);

            String insertTopic = "INSERT INTO topics (title) VALUES (?)";
            PreparedStatement topicStmt = conn.prepareStatement(insertTopic, Statement.RETURN_GENERATED_KEYS);
            topicStmt.setString(1, title);
            topicStmt.executeUpdate();

            ResultSet topicKeys = topicStmt.getGeneratedKeys();
            int topicId = topicKeys.next() ? topicKeys.getInt(1) : -1;

            String insertOption = "INSERT INTO options (topic_id, option_text) VALUES (?, ?)";
            PreparedStatement optionStmt = conn.prepareStatement(insertOption);
            for (String optionText : optionTexts) {
                optionStmt.setInt(1, topicId);
                optionStmt.setString(2, optionText);
                optionStmt.addBatch();
            }
            optionStmt.executeBatch();

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    public static List<Topic> getAllTopics() throws SQLException {
        List<Topic> topics = new ArrayList<>();
        String sql = "SELECT * FROM topics ORDER BY id DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Topic topic = new Topic(rs.getInt("id"), rs.getString("title"));
                topic.setOptions(FXCollections.observableArrayList(getOptionsByTopicId(topic.getId())));
                topics.add(topic);
            }
        }
        return topics;
    }

    public static Topic getTopicById(int id) throws SQLException {
        String sql = "SELECT * FROM topics WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Topic topic = new Topic(rs.getInt("id"), rs.getString("title"));
                topic.setOptions(FXCollections.observableArrayList(getOptionsByTopicId(topic.getId())));
                return topic;
            }
        }
        return null;
    }

    private static List<Option> getOptionsByTopicId(int topicId) throws SQLException {
        List<Option> options = new ArrayList<>();
        String sql = "SELECT * FROM options WHERE topic_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                options.add(new Option(
                        rs.getInt("id"),
                        rs.getInt("topic_id"),
                        rs.getString("option_text")
                ));
            }
        }
        return options;
    }

    public static void updateTopic(int topicId, String newTitle) throws SQLException {
        String sql = "UPDATE topics SET title = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newTitle);
            pstmt.setInt(2, topicId);
            pstmt.executeUpdate();
        }
    }

    public static void deleteTopic(int topicId) throws SQLException {
        String sql = "DELETE FROM topics WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            pstmt.executeUpdate();
        }
    }
}