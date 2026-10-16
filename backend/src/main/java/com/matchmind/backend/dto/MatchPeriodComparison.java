package com.matchmind.backend.dto;

public class MatchPeriodComparison {
    private String matchId;
    private PeriodStatistics firstHalf;
    private PeriodStatistics secondHalf;

    public MatchPeriodComparison() {}

    // Getters and Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public PeriodStatistics getFirstHalf() { return firstHalf; }
    public void setFirstHalf(PeriodStatistics firstHalf) { this.firstHalf = firstHalf; }

    public PeriodStatistics getSecondHalf() { return secondHalf; }
    public void setSecondHalf(PeriodStatistics secondHalf) { this.secondHalf = secondHalf; }

    public static class PeriodStatistics {
        private int homeScore;
        private int awayScore;
        private int homeShots;
        private int awayShots;
        private int homeShotsOnTarget;
        private int awayShotsOnTarget;
        private int homeCorners;
        private int awayCorners;
        private int homeFouls;
        private int awayFouls;

        public PeriodStatistics() {}

        // Getters and Setters
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

        public int getHomeCorners() { return homeCorners; }
        public void setHomeCorners(int homeCorners) { this.homeCorners = homeCorners; }

        public int getAwayCorners() { return awayCorners; }
        public void setAwayCorners(int awayCorners) { this.awayCorners = awayCorners; }

        public int getHomeFouls() { return homeFouls; }
        public void setHomeFouls(int homeFouls) { this.homeFouls = homeFouls; }

        public int getAwayFouls() { return awayFouls; }
        public void setAwayFouls(int awayFouls) { this.awayFouls = awayFouls; }
    }
}
