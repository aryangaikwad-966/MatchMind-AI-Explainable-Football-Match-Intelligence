package com.matchmind.backend.model;

public class Team {
    private String teamId;
    private String teamName;
    private String shortName;
    private String homeGround;
    private int syntheticSeedRank;
    private boolean isSynthetic;
    private String datasetId;

    public Team() {}

    // Getters and Setters
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public String getHomeGround() { return homeGround; }
    public void setHomeGround(String homeGround) { this.homeGround = homeGround; }

    public int getSyntheticSeedRank() { return syntheticSeedRank; }
    public void setSyntheticSeedRank(int syntheticSeedRank) { this.syntheticSeedRank = syntheticSeedRank; }

    public boolean isSynthetic() { return isSynthetic; }
    public void setSynthetic(boolean synthetic) { isSynthetic = synthetic; }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
}
