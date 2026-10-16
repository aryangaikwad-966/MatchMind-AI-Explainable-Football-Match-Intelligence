package com.matchmind.backend.model;

import java.time.LocalDate;

public class Match {
    private String matchId;
    private String datasetId;
    private boolean isSynthetic;
    private String homeTeamId;
    private String awayTeamId;
    private String homeTeamName;
    private String awayTeamName;
    private String competitionName;
    private String seasonLabel;
    private LocalDate matchDate;
    private int scheduledDurationMinutes;
    private String status;
    private int homeScore;
    private int awayScore;
    private String scenarioType;
    private String dataVersion;
    private int firstHalfStoppageMinutes;
    private int secondHalfStoppageMinutes;

    public Match() {}

    public Match(String matchId, String homeTeamId, String awayTeamId,
                 String homeTeamName, String awayTeamName) {
        this.matchId = matchId;
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
    }

    // Getters and Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }

    public boolean isSynthetic() { return isSynthetic; }
    public void setSynthetic(boolean synthetic) { isSynthetic = synthetic; }

    public String getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(String homeTeamId) { this.homeTeamId = homeTeamId; }

    public String getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(String awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getHomeTeamName() { return homeTeamName; }
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }

    public String getAwayTeamName() { return awayTeamName; }
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }

    public String getCompetitionName() { return competitionName; }
    public void setCompetitionName(String competitionName) { this.competitionName = competitionName; }

    public String getSeasonLabel() { return seasonLabel; }
    public void setSeasonLabel(String seasonLabel) { this.seasonLabel = seasonLabel; }

    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }

    public int getScheduledDurationMinutes() { return scheduledDurationMinutes; }
    public void setScheduledDurationMinutes(int scheduledDurationMinutes) { this.scheduledDurationMinutes = scheduledDurationMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getHomeScore() { return homeScore; }
    public void setHomeScore(int homeScore) { this.homeScore = homeScore; }

    public int getAwayScore() { return awayScore; }
    public void setAwayScore(int awayScore) { this.awayScore = awayScore; }

    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }

    public String getDataVersion() { return dataVersion; }
    public void setDataVersion(String dataVersion) { this.dataVersion = dataVersion; }

    public int getFirstHalfStoppageMinutes() { return firstHalfStoppageMinutes; }
    public void setFirstHalfStoppageMinutes(int firstHalfStoppageMinutes) { this.firstHalfStoppageMinutes = firstHalfStoppageMinutes; }

    public int getSecondHalfStoppageMinutes() { return secondHalfStoppageMinutes; }
    public void setSecondHalfStoppageMinutes(int secondHalfStoppageMinutes) { this.secondHalfStoppageMinutes = secondHalfStoppageMinutes; }
}
