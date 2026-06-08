package org.example.rgr.model;

import java.util.List;

public class VoteData {
    private int id;
    private String title;
    private List<String> variants;
    private boolean many;
    
    public VoteData() {}
    
    public VoteData(int id, String title, List<String> variants, boolean many) {
        this.id = id;
        this.title = title;
        this.variants = variants;
        this.many = many;
    }
    
    public int getId() { return id; }
    public String getTitle() { return title; }
    public List<String> getVariants() { return variants; }
    public boolean isMany() { return many; }
    
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setVariants(List<String> variants) { this.variants = variants; }
    public void setMany(boolean many) { this.many = many; }
}