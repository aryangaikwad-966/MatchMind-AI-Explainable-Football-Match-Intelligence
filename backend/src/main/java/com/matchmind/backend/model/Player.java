package com.matchmind.backend.model;

public class Player {
    private String playerId;
    private String playerName;
    private String teamId;
    private int squadNumber;
    private String position;
    private boolean isSynthetic;
    private String datasetId;

    public Player() {}

    // Getters and Setters
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public int getSquadNumber() { return squadNumber; }
    public void setSquadNumber(int squadNumber) { this.squadNumber = squadNumber; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public boolean isSynthetic() { return isSynthetic; }
    public void setSynthetic(boolean synthetic) { isSynthetic = synthetic; }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
}
