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

            // 🔥 Шаг 1: Увеличиваем count для КАЖДОГО выбранного варианта
            for (int answerId : answerIds) {
                VoteDAO.voteForAnswer(answerId);  // Теперь без проверки voted_topics
            }

            // 🔥 Шаг 2: Увеличиваем count_of_users ОДИН раз
            VoteDAO.incrementTopicVoteCount(topicId);

            // 🔥 Шаг 3: Добавляем тему в список проголосованных
            UserDAO.addVotedTopic(userId, topicId);

            System.out.println("✅ Голосование: пользователь " + userId +
                             " проголосовал за тему " + topicId +
                             " выбрал варианты: " + answerIds);

            return VoteResult.success(null, "Голос принят!");

        } catch (Exception e) {
            return VoteResult.error("Ошибка: " + e.getMessage());
        }
    }
}