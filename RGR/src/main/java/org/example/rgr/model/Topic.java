package org.example.rgr.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Topic {
    private int id;
    private String title;
    private ObservableList<Option> options;

    public Topic(int id, String title) {
        this.id = id;
        this.title = title;
        this.options = FXCollections.observableArrayList();
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public ObservableList<Option> getOptions() { return options; }
    public void setOptions(ObservableList<Option> options) { this.options = options; }
}