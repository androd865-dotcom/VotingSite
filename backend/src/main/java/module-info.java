module org.example.rgr {
    // JavaFX модули (если используете JavaFX)
    requires javafx.controls;
    requires javafx.fxml;
    
    // WebSocket и JSON
    requires java.websocket;
    requires com.google.gson;
    
    // SQLite и BCrypt
    requires java.sql;
    requires sqlite.jdbc;
    requires jbcrypt;
    
    // Экспортируем наши пакеты
    exports org.example.rgr;
    exports org.example.rgr.model;
    exports org.example.rgr.dao;
    exports org.example.rgr.service;
    exports org.example.rgr.websocket;
    
    // Открываем пакеты для рефлексии (JavaFX FXML, Gson)
    opens org.example.rgr to javafx.fxml, com.google.gson;
    opens org.example.rgr.model to com.google.gson;
    opens org.example.rgr.dao to com.google.gson;
    opens org.example.rgr.service to com.google.gson;
    opens org.example.rgr.websocket to com.google.gson;
}