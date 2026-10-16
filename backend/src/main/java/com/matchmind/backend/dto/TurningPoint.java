package com.matchmind.backend.dto;

import java.util.List;

public class TurningPoint {
    private String insightId;
    private String matchId;
    private int minute;
    private String type;
    private String explanation;
    private String rankingRationale;
    private double score;
    private List<String> eventIds;
    private String scoreChange;
    private String period;

    public TurningPoint() {}

    // Getters and Setters
    public String getInsightId() { return insightId; }
    public void setInsightId(String insightId) { this.insightId = insightId; }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getRankingRationale() { return rankingRationale; }
    public void setRankingRationale(String rankingRationale) { this.rankingRationale = rankingRationale; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public List<String> getEventIds() { return eventIds; }
    public void setEventIds(List<String> eventIds) { this.eventIds = eventIds; }

    public String getScoreChange() { return scoreChange; }
    public void setScoreChange(String scoreChange) { this.scoreChange = scoreChange; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
