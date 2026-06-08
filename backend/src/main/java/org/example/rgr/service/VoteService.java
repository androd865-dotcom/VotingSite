package org.example.rgr.service;

import org.example.rgr.dao.VoteDAO;
import org.example.rgr.dao.TopicDAO;
import org.example.rgr.model.Topic;
import org.example.rgr.model.Answer;
import java.util.List;

public class VoteService {
    
    public static class VoteResult {
        private boolean success;
        private String message;
        private Object data;
        
        public static VoteResult success(Object data, String message) {
            VoteResult result = new VoteResult();
            result.success = true;
            result.message = message;
            result.data = data;
            return result;
        }
        
        public static VoteResult error(String message) {
            VoteResult result = new VoteResult();
            result.success = false;
            result.message = message;
            return result;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }
    
    public VoteResult processVote(int userId, int topicId, int answerId) {
        try {
            if (userId <= 0) {
                return VoteResult.error("Пользователь не авторизован");
            }
            
            if (VoteDAO.hasVoted(userId, topicId)) {
                return VoteResult.error("Вы уже голосовали за эту тему");
            }
            
            Topic topic = TopicDAO.getById(topicId);
            if (topic == null) {
                return VoteResult.error("Тема не найдена");
            }
            
            boolean answerValid = topic.getAnswers().stream()
                .anyMatch(a -> a.getId() == answerId);
            if (!answerValid) {
                return VoteResult.error("Неверный вариант ответа");
            }
            
            boolean success = VoteDAO.vote(userId, topicId, answerId);
            if (!success) {
                return VoteResult.error("Ошибка при сохранении голоса");
            }
            
            List<Answer> stats = topic.getAnswers();
            for (Answer a : stats) {
                a.setCount(VoteDAO.getAnswerCount(a.getId()));
            }
            
            return VoteResult.success(stats, "Голос принят!");
            
        } catch (Exception e) {
            return VoteResult.error("Ошибка: " + e.getMessage());
        }
    }
}