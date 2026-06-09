package org.example.rgr.service;

import org.example.rgr.dao.TopicDAO;
import org.example.rgr.dao.UserDAO;
import org.example.rgr.model.Topic;
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
    
    // Формирует данные для фронта с учетом проголосованных тем
    public List<VoteData> getAllVotesForFrontend(int userId) {
        try {
            List<Topic> topics = TopicDAO.getAll();
            List<VoteData> voteDataList = new ArrayList<>();
            
            // Получаем список тем, за которые пользователь уже проголосовал
            List<Integer> votedTopics = userId > 0 ? UserDAO.getById(userId).getVotedTopics() : new ArrayList<>();
            
            for (Topic topic : topics) {
                // Создаем массив variants
                List<Map<String, Object>> variants = new ArrayList<>();
                for (Answer answer : topic.getAnswers()) {
                    Map<String, Object> variant = new HashMap<>();
                    variant.put("id", answer.getId());
                    variant.put("name", answer.getNameAnswer());
                    variants.add(variant);
                }
                
                // Добавляем флаг hasVoted - знает ли фронт, что пользователь уже голосовал
                boolean hasVoted = votedTopics.contains(topic.getId());
                
                VoteData voteData = new VoteData(
                    topic.getId(),
                    topic.getNameQuestion(),
                    topic.isMany(),
                    variants,
                    hasVoted  // ← новый флаг для фронта
                );
                voteDataList.add(voteData);
            }
            
            return voteDataList;
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Остальные методы без изменений...
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
            return TopicResult.success(topicId, "Голосование \"" + nameQuestion + "\" создано!");
            
        } catch (Exception e) {
            return TopicResult.error("Ошибка создания: " + e.getMessage());
        }
    }
    
    public TopicResult deleteTopic(int topicId, int adminId) {
        try {
            if (adminId != 1) {
                return TopicResult.error("Доступ запрещен. Только администратор");
            }
            
            Topic topic = TopicDAO.getById(topicId);
            if (topic == null) {
                return TopicResult.error("Тема не найдена");
            }
            
            TopicDAO.delete(topicId);
            return TopicResult.success(null, "Голосование \"" + topic.getNameQuestion() + "\" удалено");
            
        } catch (Exception e) {
            return TopicResult.error("Ошибка удаления: " + e.getMessage());
        }
    }
    
    public TopicResult updateTopicName(int topicId, String newName, int adminId) {
        try {
            if (adminId != 1) {
                return TopicResult.error("Доступ запрещен. Только администратор");
            }
            if (newName == null || newName.trim().isEmpty()) {
                return TopicResult.error("Введите новое название");
            }
            
            TopicDAO.updateName(topicId, newName);
            return TopicResult.success(null, "Название обновлено на \"" + newName + "\"");
            
        } catch (Exception e) {
            return TopicResult.error("Ошибка обновления: " + e.getMessage());
        }
    }
}