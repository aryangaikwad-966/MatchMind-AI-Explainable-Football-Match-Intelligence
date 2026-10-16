package com.matchmind.backend.dto;

import com.matchmind.backend.model.MatchEvent;
import java.util.List;

public class ReplayState {
    private String matchId; private int eventSequence; private int simulatedMinute; private int homeScore; private int awayScore; private List<MatchEvent> events; private List<TurningPoint> insights; private String label;
    public String getMatchId(){return matchId;} public void setMatchId(String v){matchId=v;} public int getEventSequence(){return eventSequence;} public void setEventSequence(int v){eventSequence=v;} public int getSimulatedMinute(){return simulatedMinute;} public void setSimulatedMinute(int v){simulatedMinute=v;} public int getHomeScore(){return homeScore;} public void setHomeScore(int v){homeScore=v;} public int getAwayScore(){return awayScore;} public void setAwayScore(int v){awayScore=v;} public List<MatchEvent> getEvents(){return events;} public void setEvents(List<MatchEvent> v){events=v;} public List<TurningPoint> getInsights(){return insights;} public void setInsights(List<TurningPoint> v){insights=v;} public String getLabel(){return label;} public void setLabel(String v){label=v;}
}
