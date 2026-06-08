package org.example.rgr.service;

import org.example.rgr.dao.TopicDAO;
import org.example.rgr.dao.VoteDAO;
import org.example.rgr.model.Topic;
import org.example.rgr.model.Answer;
import org.example.rgr.model.VoteData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    
    public List<VoteData> getAllVotesForFrontend() {
        try {
            List<Topic> topics = TopicDAO.getAll();
            List<VoteData> voteDataList = new ArrayList<>();
            
            for (Topic topic : topics) {
                List<String> variants = topic.getAnswers().stream()
                    .map(Answer::getNameAnswer)
                    .collect(Collectors.toList());
                
                VoteData voteData = new VoteData(
                    topic.getId(),
                    topic.getNameQuestion(),
                    variants,
                    topic.isMany()
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