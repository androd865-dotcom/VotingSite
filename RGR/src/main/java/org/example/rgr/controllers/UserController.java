package org.example.rgr.controllers;

import org.example.rgr.dao.TopicDAO;
import org.example.rgr.dao.VoteDAO;
import org.example.rgr.model.Topic;
import org.example.rgr.model.Option;
import org.example.rgr.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UserController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private ListView<Topic> topicsList;

    @FXML
    private TextArea statsArea;

    @FXML
    private Label messageLabel;

    private User user;
    private Stage primaryStage;

    public void setUser(User user) {
        this.user = user;
        welcomeLabel.setText("Добро пожаловать, " + user.getUsername() + "!");
        refreshTopicsList();
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    private void initialize() {
        topicsList.setCellFactory(lv -> new ListCell<Topic>() {
            @Override
            protected void updateItem(Topic item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getId() + ". " + item.getTitle());
                }
            }
        });
    }

    @FXML
    private void vote() {
        Topic selected = topicsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите тему для голосования!");
            return;
        }

        try {
            // Проверяем, голосовал ли пользователь
            if (VoteDAO.hasUserVoted(user.getId(), selected.getId())) {
                showAlert("Внимание", "Вы уже голосовали за эту тему!");
                showStatistics(selected);
                return;
            }

            // Показываем диалог с вариантами ответов
            List<Option> options = selected.getOptions();
            if (options == null || options.isEmpty()) {
                showAlert("Ошибка", "Нет вариантов ответов для этой темы!");
                return;
            }

            // Создаем диалог выбора
            ChoiceDialog<Option> dialog = new ChoiceDialog<>(options.get(0), options);
            dialog.setTitle("Голосование");
            dialog.setHeaderText("Тема: " + selected.getTitle());
            dialog.setContentText("Выберите вариант ответа:");

            // Настраиваем отображение опций
            dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);

            Optional<Option> result = dialog.showAndWait();
            if (result.isPresent()) {
                Option selectedOption = result.get();
                boolean success = VoteDAO.vote(user.getId(), selected.getId(), selectedOption.getId());
                if (success) {
                    messageLabel.setText("✓ Голос принят! Спасибо за участие!");
                    showStatistics(selected);
                } else {
                    showAlert("Ошибка", "Не удалось проголосовать!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось проголосовать: " + e.getMessage());
        }
    }

    @FXML
    private void showStatistics() {
        Topic selected = topicsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите тему для просмотра статистики!");
            return;
        }
        showStatistics(selected);
    }

    private void showStatistics(Topic topic) {
        try {
            List<Option> stats = VoteDAO.getStatistics(topic.getId());
            int totalVotes = stats.stream().mapToInt(Option::getVoteCount).sum();

            StringBuilder sb = new StringBuilder();
            sb.append("=== СТАТИСТИКА ===\n");
            sb.append("Тема: ").append(topic.getTitle()).append("\n");
            sb.append("Всего голосов: ").append(totalVotes).append("\n");
            sb.append("-----------------------------------\n\n");

            if (totalVotes == 0) {
                sb.append("Пока нет голосов. Будьте первым!\n");
            } else {
                for (Option opt : stats) {
                    double percentage = (opt.getVoteCount() * 100.0 / totalVotes);
                    sb.append(String.format("• %s:\n   %d голосов (%.1f%%)\n\n",
                            opt.getOptionText(), opt.getVoteCount(), percentage));
                }
            }

            statsArea.setText(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось загрузить статистику: " + e.getMessage());
        }
    }

    @FXML
    private void logout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/rgr/login-view.fxml"));
            Scene scene = loader.load();
            LoginController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Вход в систему");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось выйти из системы: " + e.getMessage());
        }
    }

    private void refreshTopicsList() {
        try {
            List<Topic> topics = TopicDAO.getAllTopics();
            topicsList.setItems(FXCollections.observableArrayList(topics));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось загрузить список тем: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}