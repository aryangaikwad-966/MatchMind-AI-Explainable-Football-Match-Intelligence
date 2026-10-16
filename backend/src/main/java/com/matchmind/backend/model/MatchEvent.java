package com.matchmind.backend.model;

public class MatchEvent {
    private String eventId;
    private String datasetId;
    private String matchId;
    private int eventSequence;
    private int minute;
    private int second;
    private String displayClock;
    private String period;
    private int timestampSeconds;
    private String eventType;
    private String eventOutcome;
    private String teamId;
    private String teamName;
    private String playerId;
    private String playerName;
    private String relatedPlayerId;
    private String relatedPlayerName;
    private String creditedTeamId;
    private String description;
    private boolean isGoal;
    private boolean isOwnGoal;
    private boolean isPenalty;
    private boolean isRedCard;
    private boolean isYellowCard;
    private boolean isSubstitution;
    private boolean isDisallowed;
    private int scoreHomeAfterEvent;
    private int scoreAwayAfterEvent;
    private String sourceType;
    private String syntheticEventGroupId;
    private String scenarioTag;
    private String dataQualityFlag;

    public MatchEvent() {}

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public int getEventSequence() { return eventSequence; }
    public void setEventSequence(int eventSequence) { this.eventSequence = eventSequence; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public int getSecond() { return second; }
    public void setSecond(int second) { this.second = second; }

    public String getDisplayClock() { return displayClock; }
    public void setDisplayClock(String displayClock) { this.displayClock = displayClock; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public int getTimestampSeconds() { return timestampSeconds; }
    public void setTimestampSeconds(int timestampSeconds) { this.timestampSeconds = timestampSeconds; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventOutcome() { return eventOutcome; }
    public void setEventOutcome(String eventOutcome) { this.eventOutcome = eventOutcome; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getRelatedPlayerId() { return relatedPlayerId; }
    public void setRelatedPlayerId(String relatedPlayerId) { this.relatedPlayerId = relatedPlayerId; }

    public String getRelatedPlayerName() { return relatedPlayerName; }
    public void setRelatedPlayerName(String relatedPlayerName) { this.relatedPlayerName = relatedPlayerName; }

    public String getCreditedTeamId() { return creditedTeamId; }
    public void setCreditedTeamId(String creditedTeamId) { this.creditedTeamId = creditedTeamId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isGoal() { return isGoal; }
    public void setGoal(boolean goal) { isGoal = goal; }

    public boolean isOwnGoal() { return isOwnGoal; }
    public void setOwnGoal(boolean ownGoal) { isOwnGoal = ownGoal; }

    public boolean isPenalty() { return isPenalty; }
    public void setPenalty(boolean penalty) { isPenalty = penalty; }

    public boolean isRedCard() { return isRedCard; }
    public void setRedCard(boolean redCard) { isRedCard = redCard; }

    public boolean isYellowCard() { return isYellowCard; }
    public void setYellowCard(boolean yellowCard) { isYellowCard = yellowCard; }

    public boolean isSubstitution() { return isSubstitution; }
    public void setSubstitution(boolean substitution) { isSubstitution = substitution; }

    public boolean isDisallowed() { return isDisallowed; }
    public void setDisallowed(boolean disallowed) { isDisallowed = disallowed; }

    public int getScoreHomeAfterEvent() { return scoreHomeAfterEvent; }
    public void setScoreHomeAfterEvent(int scoreHomeAfterEvent) { this.scoreHomeAfterEvent = scoreHomeAfterEvent; }

    public int getScoreAwayAfterEvent() { return scoreAwayAfterEvent; }
    public void setScoreAwayAfterEvent(int scoreAwayAfterEvent) { this.scoreAwayAfterEvent = scoreAwayAfterEvent; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSyntheticEventGroupId() { return syntheticEventGroupId; }
    public void setSyntheticEventGroupId(String syntheticEventGroupId) { this.syntheticEventGroupId = syntheticEventGroupId; }

    public String getScenarioTag() { return scenarioTag; }
    public void setScenarioTag(String scenarioTag) { this.scenarioTag = scenarioTag; }

    public String getDataQualityFlag() { return dataQualityFlag; }
    public void setDataQualityFlag(String dataQualityFlag) { this.dataQualityFlag = dataQualityFlag; }
}
