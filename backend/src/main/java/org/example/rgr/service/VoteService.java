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
// Упростите метод, убрав лишние возвращаемые данные
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

        // Для одиночного выбора
        if (!topic.isMany() && answerIds.size() > 1) {
            return VoteResult.error("Можно выбрать только один вариант");
        }

        // Сохраняем голоса
        for (int answerId : answerIds) {
            VoteDAO.vote(userId, topicId, answerId);
        }

        // Добавляем тему в список проголосованных
        UserDAO.addVotedTopic(userId, topicId);

        return VoteResult.success(null, "Голос принят!");

    } catch (Exception e) {
        return VoteResult.error("Ошибка: " + e.getMessage());
    }
}
}