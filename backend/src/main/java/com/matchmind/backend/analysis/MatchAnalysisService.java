package com.matchmind.backend.analysis;

import com.matchmind.backend.dto.MatchPeriodComparison;
import com.matchmind.backend.dto.TurningPoint;
import com.matchmind.backend.model.Match;
import com.matchmind.backend.model.MatchEvent;
import com.matchmind.backend.model.MatchStatistics;
import com.matchmind.backend.service.DatasetLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(MatchAnalysisService.class);

    private final DatasetLoader datasetLoader;

    public MatchAnalysisService(DatasetLoader datasetLoader) {
        this.datasetLoader = datasetLoader;
    }

    public MatchStatistics calculateStatistics(String matchId) {
        Match match = datasetLoader.getMatch(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found: " + matchId);
        }

        List<MatchEvent> events = datasetLoader.getMatchEvents(matchId);
        MatchStatistics stats = new MatchStatistics();
        stats.setMatchId(matchId);
        stats.setHomeScore(match.getHomeScore());
        stats.setAwayScore(match.getAwayScore());

        // Initialize counters
        Map<String, Integer> homeEvents = new HashMap<>();
        Map<String, Integer> awayEvents = new HashMap<>();

        for (MatchEvent event : events) {
            if (event.getTeamId() == null) continue;

            boolean isHome = event.getTeamId().equals(match.getHomeTeamId());
            String teamId = event.getTeamId();

            // Count events by type
            String eventType = event.getEventType();
            if (isHome) {
                homeEvents.put(eventType, homeEvents.getOrDefault(eventType, 0) + 1);
            } else {
                awayEvents.put(eventType, awayEvents.getOrDefault(eventType, 0) + 1);
            }

            // Count shots
            if (isShot(event)) {
                if (isHome) stats.setHomeShots(stats.getHomeShots() + 1);
                else stats.setAwayShots(stats.getAwayShots() + 1);
            }

            // Count shots on target
            if (isShotOnTarget(event)) {
                if (isHome) stats.setHomeShotsOnTarget(stats.getHomeShotsOnTarget() + 1);
                else stats.setAwayShotsOnTarget(stats.getAwayShotsOnTarget() + 1);
            }

            // Count saves
            if ("SAVE".equals(event.getEventType())) {
                if (isHome) stats.setHomeSaves(stats.getHomeSaves() + 1);
                else stats.setAwaySaves(stats.getAwaySaves() + 1);
            }

            // Count corners
            if ("CORNER".equals(event.getEventType())) {
                if (isHome) stats.setHomeCorners(stats.getHomeCorners() + 1);
                else stats.setAwayCorners(stats.getAwayCorners() + 1);
            }

            // Count fouls
            if ("FOUL".equals(event.getEventType())) {
                if (isHome) stats.setHomeFouls(stats.getHomeFouls() + 1);
                else stats.setAwayFouls(stats.getAwayFouls() + 1);
            }

            // Count offsides
            if ("OFFSIDE".equals(event.getEventType())) {
                if (isHome) stats.setHomeOffsides(stats.getHomeOffsides() + 1);
                else stats.setAwayOffsides(stats.getAwayOffsides() + 1);
            }

            // Count cards
            if (event.isYellowCard()) {
                if (isHome) stats.setHomeYellowCards(stats.getHomeYellowCards() + 1);
                else stats.setAwayYellowCards(stats.getAwayYellowCards() + 1);
            }
            if (event.isRedCard()) {
                if (isHome) stats.setHomeRedCards(stats.getHomeRedCards() + 1);
                else stats.setAwayRedCards(stats.getAwayRedCards() + 1);
            }

            // Count substitutions
            if (event.isSubstitution()) {
                if (isHome) stats.setHomeSubstitutions(stats.getHomeSubstitutions() + 1);
                else stats.setAwaySubstitutions(stats.getAwaySubstitutions() + 1);
            }
        }

        stats.setHomeEventsByType(homeEvents);
        stats.setAwayEventsByType(awayEvents);

        return stats;
    }

    private boolean isShot(MatchEvent event) {
        return "SHOT".equals(event.getEventType()) ||
               "SHOT_ON_TARGET".equals(event.getEventType()) ||
               "SHOT_OFF_TARGET".equals(event.getEventType()) ||
               "GOAL".equals(event.getEventType()) ||
               "PENALTY_SCORED".equals(event.getEventType()) ||
               "PENALTY_MISSED".equals(event.getEventType());
    }

    private boolean isShotOnTarget(MatchEvent event) {
        return "SHOT_ON_TARGET".equals(event.getEventType()) ||
               "GOAL".equals(event.getEventType()) ||
               "PENALTY_SCORED".equals(event.getEventType());
    }

    public List<TurningPoint> calculateTurningPoints(String matchId) {
        Match match = datasetLoader.getMatch(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found: " + matchId);
        }

        List<MatchEvent> events = datasetLoader.getMatchEvents(matchId);
        List<TurningPoint> turningPoints = new ArrayList<>();

        int previousHomeScore = 0;
        int previousAwayScore = 0;

        for (MatchEvent event : events) {
            double score = 0.0;
            String type = null;
            String explanation = null;
            String rankingRationale = null;

            // Check for goal that changes the score
            if (event.isGoal() && !event.isDisallowed()) {
                int currentHomeScore = event.getScoreHomeAfterEvent();
                int currentAwayScore = event.getScoreAwayAfterEvent();

                // Goal that equalizes
                if (currentHomeScore == currentAwayScore &&
                    (previousHomeScore != previousAwayScore)) {
                    score = 8.0;
                    type = "EQUALIZER";
                    explanation = String.format("Equalizing goal at %s', making the score %d-%d",
                            event.getDisplayClock(), currentHomeScore, currentAwayScore);
                    rankingRationale = "High significance: goal that brought the scores level";
                }
                // Goal that takes the lead
                else if ((currentHomeScore > currentAwayScore && previousHomeScore <= previousAwayScore) ||
                         (currentAwayScore > currentHomeScore && previousAwayScore <= previousHomeScore)) {
                    score = 7.0;
                    type = "GOAL_LEAD";
                    explanation = String.format("Goal that took the lead at %s', score now %d-%d",
                            event.getDisplayClock(), currentHomeScore, currentAwayScore);
                    rankingRationale = "Significant: goal that established a lead";
                }
                // Late decisive goal (75+ minutes)
                else if (event.getMinute() >= 75 && currentHomeScore != currentAwayScore) {
                    score = 6.0;
                    type = "LATE_GOAL";
                    explanation = String.format("Late goal at %s' with score %d-%d",
                            event.getDisplayClock(), currentHomeScore, currentAwayScore);
                    rankingRationale = "Moderate significance: late goal in decisive period";
                }

                previousHomeScore = currentHomeScore;
                previousAwayScore = currentAwayScore;
            }

            // Red card
            if (event.isRedCard()) {
                score = 9.0;
                type = "RED_CARD";
                explanation = String.format("Red card shown to %s at %s'",
                        event.getPlayerName(), event.getDisplayClock());
                rankingRationale = "Very high significance: team reduced to 10 players";
            }

            // Penalty awarded
            if ("PENALTY_AWARDED".equals(event.getEventType())) {
                score = 5.0;
                type = "PENALTY_AWARDED";
                explanation = String.format("Penalty awarded at %s'", event.getDisplayClock());
                rankingRationale = "Moderate significance: significant goal-scoring opportunity";
            }

            // Disallowed goal
            if (event.isDisallowed()) {
                score = 4.0;
                type = "DISALLOWED_GOAL";
                explanation = String.format("Goal disallowed at %s'", event.getDisplayClock());
                rankingRationale = "Notable: goal that would have changed the score was disallowed";
            }

            // Own goal
            if (event.isOwnGoal()) {
                score = 6.0;
                type = "OWN_GOAL";
                explanation = String.format("Own goal by %s at %s'",
                        event.getPlayerName(), event.getDisplayClock());
                rankingRationale = "Significant: own goal that changed the score";
            }

            if (score > 0) {
                TurningPoint tp = new TurningPoint();
                tp.setInsightId("TP-" + event.getEventId());
                tp.setMatchId(matchId);
                tp.setMinute(event.getMinute());
                tp.setType(type);
                tp.setExplanation(explanation);
                tp.setRankingRationale(rankingRationale);
                tp.setScore(score);
                tp.setEventIds(Collections.singletonList(event.getEventId()));
                tp.setScoreChange(String.format("%d-%d",
                        event.getScoreHomeAfterEvent(), event.getScoreAwayAfterEvent()));
                tp.setPeriod(event.getPeriod());
                turningPoints.add(tp);
            }
        }

        // Sort by score descending, then by minute
        turningPoints.sort((a, b) -> {
            int scoreCompare = Double.compare(b.getScore(), a.getScore());
            if (scoreCompare != 0) return scoreCompare;
            return Integer.compare(a.getMinute(), b.getMinute());
        });

        return turningPoints;
    }

    public MatchPeriodComparison comparePeriods(String matchId) {
        Match match = datasetLoader.getMatch(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found: " + matchId);
        }

        List<MatchEvent> events = datasetLoader.getMatchEvents(matchId);
        MatchPeriodComparison comparison = new MatchPeriodComparison();
        comparison.setMatchId(matchId);

        MatchPeriodComparison.PeriodStatistics firstHalf = new MatchPeriodComparison.PeriodStatistics();
        MatchPeriodComparison.PeriodStatistics secondHalf = new MatchPeriodComparison.PeriodStatistics();

        int firstHalfEndScoreHome = 0;
        int firstHalfEndScoreAway = 0;

        for (MatchEvent event : events) {
            if ("SECOND_HALF".equals(event.getPeriod())) {
                firstHalfEndScoreHome = event.getScoreHomeAfterEvent();
                firstHalfEndScoreAway = event.getScoreAwayAfterEvent();
                break;
            }
        }

        firstHalf.setHomeScore(firstHalfEndScoreHome);
        firstHalf.setAwayScore(firstHalfEndScoreAway);
        secondHalf.setHomeScore(match.getHomeScore() - firstHalfEndScoreHome);
        secondHalf.setAwayScore(match.getAwayScore() - firstHalfEndScoreAway);

        // Count events by period
        for (MatchEvent event : events) {
            if (event.getTeamId() == null) continue;
            boolean isHome = event.getTeamId().equals(match.getHomeTeamId());

            if ("FIRST_HALF".equals(event.getPeriod())) {
                if (isShot(event)) {
                    if (isHome) firstHalf.setHomeShots(firstHalf.getHomeShots() + 1);
                    else firstHalf.setAwayShots(firstHalf.getAwayShots() + 1);
                }
                if (isShotOnTarget(event)) {
                    if (isHome) firstHalf.setHomeShotsOnTarget(firstHalf.getHomeShotsOnTarget() + 1);
                    else firstHalf.setAwayShotsOnTarget(firstHalf.getAwayShotsOnTarget() + 1);
                }
                if ("CORNER".equals(event.getEventType())) {
                    if (isHome) firstHalf.setHomeCorners(firstHalf.getHomeCorners() + 1);
                    else firstHalf.setAwayCorners(firstHalf.getAwayCorners() + 1);
                }
                if ("FOUL".equals(event.getEventType())) {
                    if (isHome) firstHalf.setHomeFouls(firstHalf.getHomeFouls() + 1);
                    else firstHalf.setAwayFouls(firstHalf.getAwayFouls() + 1);
                }
            } else if ("SECOND_HALF".equals(event.getPeriod())) {
                if (isShot(event)) {
                    if (isHome) secondHalf.setHomeShots(secondHalf.getHomeShots() + 1);
                    else secondHalf.setAwayShots(secondHalf.getAwayShots() + 1);
                }
                if (isShotOnTarget(event)) {
                    if (isHome) secondHalf.setHomeShotsOnTarget(secondHalf.getHomeShotsOnTarget() + 1);
                    else secondHalf.setAwayShotsOnTarget(secondHalf.getAwayShotsOnTarget() + 1);
                }
                if ("CORNER".equals(event.getEventType())) {
                    if (isHome) secondHalf.setHomeCorners(secondHalf.getHomeCorners() + 1);
                    else secondHalf.setAwayCorners(secondHalf.getAwayCorners() + 1);
                }
                if ("FOUL".equals(event.getEventType())) {
                    if (isHome) secondHalf.setHomeFouls(secondHalf.getHomeFouls() + 1);
                    else secondHalf.setAwayFouls(secondHalf.getAwayFouls() + 1);
                }
            }
        }

        comparison.setFirstHalf(firstHalf);
        comparison.setSecondHalf(secondHalf);

        return comparison;
    }
}
