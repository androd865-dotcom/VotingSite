module org.example.rgr {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires sqlite.jdbc;

    // Экспортируем пакеты для JavaFX
    exports org.example.rgr;
    exports org.example.rgr.controllers;
    exports org.example.rgr.model;
    exports org.example.rgr.dao;
    exports org.example.rgr.util;

    // Открываем пакеты для JavaFX FXML
    opens org.example.rgr.controllers to javafx.fxml;
    opens org.example.rgr.model to javafx.base;
    opens org.example.rgr to javafx.fxml;
}