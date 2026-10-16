package com.matchmind.backend.dto;

public class QuestionRequest {
    private String question;
    private String mode; // FAN or ANALYST
    private String preference;

    public QuestionRequest() {}

    // Getters and Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
}
