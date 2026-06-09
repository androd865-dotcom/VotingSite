package org.example.rgr.model;

import java.util.List;
import java.util.Map;

public class VoteData {
    private int id;
    private String header;
    private boolean many;
    private List<Map<String, Object>> variants;
    private boolean hasVoted;  // НОВЫЙ ФЛАГ - голосовал ли пользователь
    
    public VoteData() {}
    
    public VoteData(int id, String header, boolean many, List<Map<String, Object>> variants, boolean hasVoted) {
        this.id = id;
        this.header = header;
        this.many = many;
        this.variants = variants;
        this.hasVoted = hasVoted;
    }
    
    public int getId() { return id; }
    public String getHeader() { return header; }
    public boolean isMany() { return many; }
    public List<Map<String, Object>> getVariants() { return variants; }
    public boolean isHasVoted() { return hasVoted; }
    
    public void setId(int id) { this.id = id; }
    public void setHeader(String header) { this.header = header; }
    public void setMany(boolean many) { this.many = many; }
    public void setVariants(List<Map<String, Object>> variants) { this.variants = variants; }
    public void setHasVoted(boolean hasVoted) { this.hasVoted = hasVoted; }
}