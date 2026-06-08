package org.example.rgr;

import org.example.rgr.controllers.LoginController;
import org.example.rgr.util.DatabaseUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Инициализация базы данных
        DatabaseUtil.initializeDatabase();

        // Загрузка FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/rgr/login-view.fxml"));
        Scene scene = new Scene(loader.load());

        // Передаем Stage в контроллер
        LoginController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        primaryStage.setTitle("Система голосования");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}