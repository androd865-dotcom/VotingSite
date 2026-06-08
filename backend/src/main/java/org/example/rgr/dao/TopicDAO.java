package org.example.rgr.dao;

import org.example.rgr.model.Topic;
import org.example.rgr.model.Answer;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TopicDAO {
    
    public static int create(String nameQuestion, boolean many, List<String> answerNames) throws SQLException {
        Connection conn = null;
        int topicId = -1;
        
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);
            
            String sqlTopic = "INSERT INTO topics (name_question, many) VALUES (?, ?)";
            PreparedStatement pstmtTopic = conn.prepareStatement(sqlTopic, Statement.RETURN_GENERATED_KEYS);
            pstmtTopic.setString(1, nameQuestion);
            pstmtTopic.setInt(2, many ? 1 : 0);
            pstmtTopic.executeUpdate();
            
            ResultSet rs = pstmtTopic.getGeneratedKeys();
            if (rs.next()) {
                topicId = rs.getInt(1);
            }
            
            String sqlAnswer = "INSERT INTO answers (name_answer, topic_id) VALUES (?, ?)";
            PreparedStatement pstmtAnswer = conn.prepareStatement(sqlAnswer);
            for (String answerName : answerNames) {
                pstmtAnswer.setString(1, answerName);
                pstmtAnswer.setInt(2, topicId);
                pstmtAnswer.addBatch();
            }
            pstmtAnswer.executeBatch();
            
            conn.commit();
            return topicId;
            
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }
    
    public static List<Topic> getAll() throws SQLException {
        List<Topic> topics = new ArrayList<>();
        String sql = "SELECT * FROM topics ORDER BY id DESC";
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Topic topic = new Topic();
                topic.setId(rs.getInt("id"));
                topic.setNameQuestion(rs.getString("name_question"));
                topic.setMany(rs.getInt("many") == 1);
                topic.setCountOfUsers(rs.getInt("count_of_users"));
                topic.setAnswers(getAnswersByTopicId(topic.getId()));
                topics.add(topic);
            }
        }
        return topics;
    }
    
    public static Topic getById(int id) throws SQLException {
        String sql = "SELECT * FROM topics WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Topic topic = new Topic();
                topic.setId(rs.getInt("id"));
                topic.setNameQuestion(rs.getString("name_question"));
                topic.setMany(rs.getInt("many") == 1);
                topic.setCountOfUsers(rs.getInt("count_of_users"));
                topic.setAnswers(getAnswersByTopicId(topic.getId()));
                return topic;
            }
        }
        return null;
    }
    
    private static List<Answer> getAnswersByTopicId(int topicId) throws SQLException {
        List<Answer> answers = new ArrayList<>();
        String sql = "SELECT * FROM answers WHERE topic_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Answer answer = new Answer();
                answer.setId(rs.getInt("id"));
                answer.setNameAnswer(rs.getString("name_answer"));
                answer.setTopicId(rs.getInt("topic_id"));
                answer.setCount(rs.getInt("count"));
                answers.add(answer);
            }
        }
        return answers;
    }
    
    public static void updateName(int topicId, String newName) throws SQLException {
        String sql = "UPDATE topics SET name_question = ? WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setInt(2, topicId);
            pstmt.executeUpdate();
        }
    }
    
    public static void delete(int topicId) throws SQLException {
        String sql = "DELETE FROM topics WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            pstmt.executeUpdate();
        }
    }
}