package com.matchmind.backend.dto;

public class MatchRecap {
    private String matchId;
    private String recap;
    private String fanExplanation;
    private String analystExplanation;
    private String keyMoments;
    private String aiMode;

    public MatchRecap() {}

    // Getters and Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getRecap() { return recap; }
    public void setRecap(String recap) { this.recap = recap; }

    public String getFanExplanation() { return fanExplanation; }
    public void setFanExplanation(String fanExplanation) { this.fanExplanation = fanExplanation; }

    public String getAnalystExplanation() { return analystExplanation; }
    public void setAnalystExplanation(String analystExplanation) { this.analystExplanation = analystExplanation; }

    public String getKeyMoments() { return keyMoments; }
    public void setKeyMoments(String keyMoments) { this.keyMoments = keyMoments; }

    public String getAiMode() { return aiMode; }
    public void setAiMode(String aiMode) { this.aiMode = aiMode; }
}
