package org.example.rgr.service;

import org.example.rgr.dao.VoteDAO;
import org.example.rgr.dao.TopicDAO;
import org.example.rgr.dao.UserDAO;
import org.example.rgr.model.Topic;
import org.example.rgr.model.Answer;
import java.util.*;

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
    
    // Обработка голосования
    public VoteResult processVote(int userId, int topicId, List<Integer> answerIds) {
        try {
            if (userId <= 0) {
                return VoteResult.error("Пользователь не авторизован");
            }
            
            // Проверяем, не голосовал ли уже пользователь
            if (UserDAO.hasVoted(userId, topicId)) {
                return VoteResult.error("Вы уже голосовали за эту тему");
            }
            
            Topic topic = TopicDAO.getById(topicId);
            if (topic == null) {
                return VoteResult.error("Тема не найдена");
            }
            
            // Проверяем, что все выбранные ответы принадлежат этой теме
            Set<Integer> validAnswerIds = new HashSet<>();
            for (Answer a : topic.getAnswers()) {
                validAnswerIds.add(a.getId());
            }
            
            for (int answerId : answerIds) {
                if (!validAnswerIds.contains(answerId)) {
                    return VoteResult.error("Неверный вариант ответа: " + answerId);
                }
            }
            
            // Сохраняем голоса
            int successCount = 0;
            for (int answerId : answerIds) {
                boolean success = VoteDAO.vote(userId, topicId, answerId);
                if (success) successCount++;
            }
            
            if (successCount == 0) {
                return VoteResult.error("Ошибка при сохранении голосов");
            }
            
            // Формируем статистику для ответа
            List<Map<String, Object>> stats = new ArrayList<>();
            for (Answer a : topic.getAnswers()) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("id", a.getId());
                stat.put("name", a.getNameAnswer());
                stat.put("count", VoteDAO.getAnswerCount(a.getId()));
                stats.add(stat);
            }
            
            String message = successCount == 1 ? "Голос принят!" : "Голоса приняты!";
            return VoteResult.success(stats, message);
            
        } catch (Exception e) {
            return VoteResult.error("Ошибка: " + e.getMessage());
        }
    }
}