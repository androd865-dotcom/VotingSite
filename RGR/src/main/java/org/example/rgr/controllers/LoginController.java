package org.example.rgr.controllers;

import org.example.rgr.dao.UserDAO;
import org.example.rgr.model.User;
import org.example.rgr.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    @FXML private ToggleGroup roleGroup;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private Stage primaryStage;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    private void initialize() {
        roleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            RadioButton selected = (RadioButton) newVal;
            if (selected.getText().equals("Администратор")) {
                usernameField.setVisible(true);
                usernameField.setPromptText("Имя пользователя");
            } else {
                usernameField.setVisible(true);
                usernameField.setPromptText("Ваш никнейм");
            }
        });

        // По умолчанию показываем поле username для администратора
        usernameField.setVisible(true);
    }

    @FXML
    private void handleLogin() {
        RadioButton selected = (RadioButton) roleGroup.getSelectedToggle();
        String role = selected.getText();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Заполните все поля!");
            return;
        }

        try {
            if (role.equals("Администратор")) {
                // Вход администратора по имени и паролю
                User admin = UserDAO.authenticate(username, password);
                if (admin != null && admin.getRole().equals("ADMIN")) {
                    openAdminPanel(admin);
                } else {
                    errorLabel.setText("Неверные учетные данные!");
                }
            } else {
                // Пользователь: регистрация или вход
                User user;
                if (UserDAO.userExists(username)) {
                    user = UserDAO.authenticate(username, password);
                    if (user == null) {
                        errorLabel.setText("Неверный пароль!");
                        return;
                    }
                } else {
                    user = UserDAO.registerUser(username, password, "USER");
                }

                if (user != null) {
                    openUserPanel(user);
                } else {
                    errorLabel.setText("Ошибка при входе!");
                }
            }
        } catch (Exception e) {
            errorLabel.setText("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openAdminPanel(User admin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/rgr/admin-view.fxml"));
            Scene scene = new Scene(loader.load());
            AdminController controller = loader.getController();
            controller.setAdmin(admin);
            controller.setPrimaryStage(primaryStage);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Панель администратора");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openUserPanel(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/rgr/user-view.fxml"));
            Scene scene = new Scene(loader.load());
            UserController controller = loader.getController();
            controller.setUser(user);
            controller.setPrimaryStage(primaryStage);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Панель пользователя");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}