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
import java.util.ArrayList;
import java.util.List;

public class AdminController {
    @FXML
    private Label welcomeLabel;

    @FXML
    private TextField topicTitle;

    @FXML
    private ListView<String> optionsList;

    @FXML
    private TextField newOption;

    @FXML
    private ListView<Topic> topicsList;

    @FXML
    private TextField editTitle;

    @FXML
    private ListView<Topic> statsTopicsList;

    @FXML
    private TextArea statsArea;

    private User admin;
    private Stage primaryStage;
    private List<String> options = new ArrayList<>();

    public void setAdmin(User admin) {
        this.admin = admin;
        welcomeLabel.setText("Добро пожаловать, " + admin.getUsername() + "!");
        refreshTopicsList();
        refreshStatsTopicsList();
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    private void initialize() {
        optionsList.setItems(FXCollections.observableArrayList(options));

        // Настройка отображения тем в списке
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

        statsTopicsList.setCellFactory(lv -> new ListCell<Topic>() {
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
    private void addOption() {
        String option = newOption.getText().trim();
        if (!option.isEmpty()) {
            options.add(option);
            optionsList.setItems(FXCollections.observableArrayList(options));
            newOption.clear();
        }
    }

    @FXML
    private void removeOption() {
        String selected = optionsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            options.remove(selected);
            optionsList.setItems(FXCollections.observableArrayList(options));
        }
    }

    @FXML
    private void createVoting() {
        String title = topicTitle.getText().trim();
        if (title.isEmpty()) {
            showAlert("Ошибка", "Введите тему голосования!");
            return;
        }
        if (options.size() < 2) {
            showAlert("Ошибка", "Добавьте минимум 2 варианта ответа!");
            return;
        }

        try {
            TopicDAO.createTopic(title, options);
            showAlert("Успех", "Голосование создано!");
            topicTitle.clear();
            options.clear();
            optionsList.setItems(FXCollections.observableArrayList(options));
            refreshTopicsList();
            refreshStatsTopicsList();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось создать голосование: " + e.getMessage());
        }
    }

    @FXML
    private void editTopic() {
        Topic selected = topicsList.getSelectionModel().getSelectedItem();
        String newTitle = editTitle.getText().trim();
        if (selected == null || newTitle.isEmpty()) {
            showAlert("Ошибка", "Выберите тему и введите новое название!");
            return;
        }

        try {
            TopicDAO.updateTopic(selected.getId(), newTitle);
            showAlert("Успех", "Тема обновлена!");
            refreshTopicsList();
            refreshStatsTopicsList();
            editTitle.clear();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось обновить тему!");
        }
    }

    @FXML
    private void deleteTopic() {
        Topic selected = topicsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите тему для удаления!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удаление темы");
        confirm.setContentText("Вы уверены, что хотите удалить тему \"" + selected.getTitle() + "\"?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                TopicDAO.deleteTopic(selected.getId());
                showAlert("Успех", "Тема удалена!");
                refreshTopicsList();
                refreshStatsTopicsList();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Не удалось удалить тему!");
            }
        }
    }

    @FXML
    private void showStatistics() {
        Topic selected = statsTopicsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите тему для просмотра статистики!");
            return;
        }

        try {
            List<Option> stats = VoteDAO.getStatistics(selected.getId());
            int totalVotes = stats.stream().mapToInt(Option::getVoteCount).sum();

            StringBuilder sb = new StringBuilder();
            sb.append("Тема: ").append(selected.getTitle()).append("\n");
            sb.append("Всего голосов: ").append(totalVotes).append("\n");
            sb.append("-----------------------------------\n");

            if (totalVotes == 0) {
                sb.append("Пока нет голосов.\n");
            } else {
                for (Option opt : stats) {
                    double percentage = (opt.getVoteCount() * 100.0 / totalVotes);
                    sb.append(String.format("• %s: %d голосов (%.1f%%)\n",
                            opt.getOptionText(), opt.getVoteCount(), percentage));
                }
            }

            statsArea.setText(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось загрузить статистику!");
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
        }
    }

    private void refreshStatsTopicsList() {
        try {
            List<Topic> topics = TopicDAO.getAllTopics();
            statsTopicsList.setItems(FXCollections.observableArrayList(topics));
        } catch (Exception e) {
            e.printStackTrace();
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