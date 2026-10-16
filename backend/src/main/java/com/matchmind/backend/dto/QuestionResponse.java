package com.matchmind.backend.dto;

import java.util.List;

public class QuestionResponse {
    private String answer;
    private List<String> evidenceEventIds;
    private String aiMode;

    public QuestionResponse() {}

    // Getters and Setters
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<String> getEvidenceEventIds() { return evidenceEventIds; }
    public void setEvidenceEventIds(List<String> evidenceEventIds) { this.evidenceEventIds = evidenceEventIds; }

    public String getAiMode() { return aiMode; }
    public void setAiMode(String aiMode) { this.aiMode = aiMode; }
}
