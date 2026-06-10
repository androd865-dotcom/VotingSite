package org.example.rgr.service;

import org.example.rgr.dao.TopicDAO;
import org.example.rgr.dao.UserDAO;
import org.example.rgr.model.Topic;
import org.example.rgr.model.Answer;
import org.example.rgr.model.VoteData;
import org.example.rgr.model.User;

import java.sql.SQLException;
import java.util.*;

public class TopicService {

    public List<VoteData> getAllVotesForFrontend(int userId) {
        List<VoteData> votesData = new ArrayList<>();

        try {
            List<Topic> topics = TopicDAO.getAll();

            // Получаем список проголосованных тем пользователя
            List<Integer> votedTopics = new ArrayList<>();
            if (userId > 0) {
                try {
                    User user = UserDAO.getUserById(userId);
                    if (user != null && user.getVotedTopics() != null) {
                        votedTopics = user.getVotedTopics();
                    }
                } catch (SQLException e) {
                    System.err.println("⚠️ Ошибка получения votedTopics: " + e.getMessage());
                }
            }

            for (Topic topic : topics) {
                VoteData voteData = new VoteData();
                voteData.setId(topic.getId());
                voteData.setHeader(topic.getNameQuestion());
                voteData.setMany(topic.isMany());
                voteData.setHasVoted(votedTopics.contains(topic.getId()));

                List<Map<String, Object>> variants = new ArrayList<>();
                for (Answer answer : topic.getAnswers()) {
                    Map<String, Object> variant = new HashMap<>();
                    variant.put("id", answer.getId());
                    variant.put("name", answer.getNameAnswer());
                    variant.put("count", answer.getCount());
                    variants.add(variant);
                }
                voteData.setVariants(variants);

                votesData.add(voteData);
            }
        } catch (SQLException e) {
            System.err.println("❌ Ошибка получения списка голосований: " + e.getMessage());
        }

        return votesData;
    }
}