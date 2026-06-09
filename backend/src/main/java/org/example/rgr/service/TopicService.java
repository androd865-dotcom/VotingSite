package org.example.rgr.service;

import org.example.rgr.dao.TopicDAO;
import org.example.rgr.dao.UserDAO;
import org.example.rgr.model.Topic;
import org.example.rgr.model.User; 
import org.example.rgr.model.Answer;
import org.example.rgr.model.VoteData;
import java.util.*;

public class TopicService {
    
    public static class TopicResult {
        private boolean success;
        private String message;
        private Object data;
        
        public static TopicResult success(Object data, String message) {
            TopicResult result = new TopicResult();
            result.success = true;
            result.message = message;
            result.data = data;
            return result;
        }
        
        public static TopicResult error(String message) {
            TopicResult result = new TopicResult();
            result.success = false;
            result.message = message;
            return result;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }
    
    // Формирует данные для фронта
    public List<VoteData> getAllVotesForFrontend(int userId) {
        try {
            List<Topic> topics = TopicDAO.getAll();
            List<VoteData> voteDataList = new ArrayList<>();
            
            List<Integer> votedTopics = new ArrayList<>();
            if (userId > 0) {
                try {
                    User user = UserDAO.getById(userId);
                    if (user != null) {
                        votedTopics = user.getVotedTopics();
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка получения votedTopics: " + e.getMessage());
                }
            }
            
            for (Topic topic : topics) {
                List<Map<String, Object>> variants = new ArrayList<>();
                for (Answer answer : topic.getAnswers()) {
                    Map<String, Object> variant = new HashMap<>();
                    variant.put("id", answer.getId());
                    variant.put("name", answer.getNameAnswer());
                    variants.add(variant);
                }
                
                boolean hasVoted = votedTopics.contains(topic.getId());
                
                VoteData voteData = new VoteData(
                    topic.getId(),
                    topic.getNameQuestion(),
                    topic.isMany(),
                    variants,
                    hasVoted
                );
                voteDataList.add(voteData);
            }
            
            return voteDataList;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public TopicResult createTopic(String nameQuestion, boolean many, List<String> answerNames, int adminId) {
        try {
            if (adminId != 1) {
                return TopicResult.error("Доступ запрещен. Только администратор");
            }
            if (nameQuestion == null || nameQuestion.trim().isEmpty()) {
                return TopicResult.error("Введите тему голосования");
            }
            if (answerNames == null || answerNames.size() < 2) {
                return TopicResult.error("Добавьте минимум 2 варианта ответа");
            }
            
            int topicId = TopicDAO.create(nameQuestion, many, answerNames);
            return TopicResult.success(topicId, "Голосование создано!");
            
        } catch (Exception e) {
            return TopicResult.error("Ошибка создания: " + e.getMessage());
        }
    }
    
    public TopicResult deleteTopic(int topicId, int adminId) {
        try {
            if (adminId != 1) {
                return TopicResult.error("Доступ запрещен");
            }
            
            Topic topic = TopicDAO.getById(topicId);
            if (topic == null) {
                return TopicResult.error("Тема не найдена");
            }
            
            TopicDAO.delete(topicId);
            return TopicResult.success(null, "Голосование удалено");
            
        } catch (Exception e) {
            return TopicResult.error("Ошибка удаления: " + e.getMessage());
        }
    }
    
    public TopicResult updateTopicName(int topicId, String newName, int adminId) {
        try {
            if (adminId != 1) {
                return TopicResult.error("Доступ запрещен");
            }
            if (newName == null || newName.trim().isEmpty()) {
                return TopicResult.error("Введите новое название");
            }
            
            TopicDAO.updateName(topicId, newName);
            return TopicResult.success(null, "Название обновлено");
            
        } catch (Exception e) {
            return TopicResult.error("Ошибка обновления: " + e.getMessage());
        }
    }
}