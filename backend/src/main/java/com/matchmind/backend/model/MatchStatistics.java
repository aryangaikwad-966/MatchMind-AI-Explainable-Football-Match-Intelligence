package com.matchmind.backend.model;

import java.util.Map;

public class MatchStatistics {
    private String matchId;
    private int homeScore;
    private int awayScore;
    private int homeShots;
    private int awayShots;
    private int homeShotsOnTarget;
    private int awayShotsOnTarget;
    private int homeSaves;
    private int awaySaves;
    private int homeCorners;
    private int awayCorners;
    private int homeFouls;
    private int awayFouls;
    private int homeOffsides;
    private int awayOffsides;
    private int homeYellowCards;
    private int awayYellowCards;
    private int homeRedCards;
    private int awayRedCards;
    private int homeSubstitutions;
    private int awaySubstitutions;
    private Map<String, Integer> homeEventsByType;
    private Map<String, Integer> awayEventsByType;

    public MatchStatistics() {}

    // Getters and Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public int getHomeScore() { return homeScore; }
    public void setHomeScore(int homeScore) { this.homeScore = homeScore; }

    public int getAwayScore() { return awayScore; }
    public void setAwayScore(int awayScore) { this.awayScore = awayScore; }

    public int getHomeShots() { return homeShots; }
    public void setHomeShots(int homeShots) { this.homeShots = homeShots; }

    public int getAwayShots() { return awayShots; }
    public void setAwayShots(int awayShots) { this.awayShots = awayShots; }

    public int getHomeShotsOnTarget() { return homeShotsOnTarget; }
    public void setHomeShotsOnTarget(int homeShotsOnTarget) { this.homeShotsOnTarget = homeShotsOnTarget; }

    public int getAwayShotsOnTarget() { return awayShotsOnTarget; }
    public void setAwayShotsOnTarget(int awayShotsOnTarget) { this.awayShotsOnTarget = awayShotsOnTarget; }

    public int getHomeSaves() { return homeSaves; }
    public void setHomeSaves(int homeSaves) { this.homeSaves = homeSaves; }

    public int getAwaySaves() { return awaySaves; }
    public void setAwaySaves(int awaySaves) { this.awaySaves = awaySaves; }

    public int getHomeCorners() { return homeCorners; }
    public void setHomeCorners(int homeCorners) { this.homeCorners = homeCorners; }

    public int getAwayCorners() { return awayCorners; }
    public void setAwayCorners(int awayCorners) { this.awayCorners = awayCorners; }

    public int getHomeFouls() { return homeFouls; }
    public void setHomeFouls(int homeFouls) { this.homeFouls = homeFouls; }

    public int getAwayFouls() { return awayFouls; }
    public void setAwayFouls(int awayFouls) { this.awayFouls = awayFouls; }

    public int getHomeOffsides() { return homeOffsides; }
    public void setHomeOffsides(int homeOffsides) { this.homeOffsides = homeOffsides; }

    public int getAwayOffsides() { return awayOffsides; }
    public void setAwayOffsides(int awayOffsides) { this.awayOffsides = awayOffsides; }

    public int getHomeYellowCards() { return homeYellowCards; }
    public void setHomeYellowCards(int homeYellowCards) { this.homeYellowCards = homeYellowCards; }

    public int getAwayYellowCards() { return awayYellowCards; }
    public void setAwayYellowCards(int awayYellowCards) { this.awayYellowCards = awayYellowCards; }

    public int getHomeRedCards() { return homeRedCards; }
    public void setHomeRedCards(int homeRedCards) { this.homeRedCards = homeRedCards; }

    public int getAwayRedCards() { return awayRedCards; }
    public void setAwayRedCards(int awayRedCards) { this.awayRedCards = awayRedCards; }

    public int getHomeSubstitutions() { return homeSubstitutions; }
    public void setHomeSubstitutions(int homeSubstitutions) { this.homeSubstitutions = homeSubstitutions; }

    public int getAwaySubstitutions() { return awaySubstitutions; }
    public void setAwaySubstitutions(int awaySubstitutions) { this.awaySubstitutions = awaySubstitutions; }

    public Map<String, Integer> getHomeEventsByType() { return homeEventsByType; }
    public void setHomeEventsByType(Map<String, Integer> homeEventsByType) { this.homeEventsByType = homeEventsByType; }

    public Map<String, Integer> getAwayEventsByType() { return awayEventsByType; }
    public void setAwayEventsByType(Map<String, Integer> awayEventsByType) { this.awayEventsByType = awayEventsByType; }
}
