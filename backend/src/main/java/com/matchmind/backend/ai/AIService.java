package com.matchmind.backend.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.matchmind.backend.dto.MatchRecap;
import com.matchmind.backend.dto.QuestionRequest;
import com.matchmind.backend.dto.QuestionResponse;
import com.matchmind.backend.model.Match;
import com.matchmind.backend.model.MatchEvent;
import com.matchmind.backend.service.DatasetLoader;

@Service
public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.mode:DEMO}")
    private String aiMode;

    @Value("${gemini.api-key:}") private String geminiApiKey;
    @Value("${gemini.model:gemini-2.0-flash}") private String geminiModel;
    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") private String geminiBaseUrl;

    private final DatasetLoader datasetLoader;
    private final com.matchmind.backend.analysis.MatchAnalysisService analysisService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public AIService(DatasetLoader datasetLoader, com.matchmind.backend.analysis.MatchAnalysisService analysisService) {
        this.datasetLoader = datasetLoader;
        this.analysisService = analysisService;
    }

    public String getAiMode() {
        return aiMode;
    }

    public boolean isProviderConfigured() {
        return "GEMINI".equalsIgnoreCase(aiMode) && geminiApiKey != null && !geminiApiKey.isBlank();
    }

    public String getProviderStatus() {
        if (!"GEMINI".equalsIgnoreCase(aiMode)) return "DEMO";
        return isProviderConfigured() ? "CONFIGURED_NOT_PROBED" : "NOT_CONFIGURED";
    }

    public MatchRecap generateMatchRecap(String matchId, String mode) {
        Match match = datasetLoader.getMatch(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found: " + matchId);
        }

        List<MatchEvent> events = datasetLoader.getMatchEvents(matchId);
        MatchRecap recap = new MatchRecap();
        recap.setMatchId(matchId);
        recap.setAiMode(aiMode);

        if ("GEMINI".equalsIgnoreCase(aiMode)) {
            try {
                recap = generateGeminiRecap(match, events, mode);
            } catch (Exception ex) {
                logger.warn("Gemini recap unavailable: {}", ex.getMessage());
                recap = generateDemoRecap(match, events, mode);
                recap.setAiMode("GEMINI_UNAVAILABLE");
            }
        } else {
            recap = generateDemoRecap(match, events, mode);
        }

        return recap;
    }

    private MatchRecap generateDemoRecap(Match match, List<MatchEvent> events, String mode) {
        MatchRecap recap = new MatchRecap();
        recap.setAiMode(aiMode);

        String result;
        if (match.getHomeScore() > match.getAwayScore()) {
            result = match.getHomeTeamName() + " won " + match.getHomeScore() + "-" + match.getAwayScore();
        } else if (match.getAwayScore() > match.getHomeScore()) {
            result = match.getAwayTeamName() + " won " + match.getAwayScore() + "-" + match.getHomeScore();
        } else {
            result = "The match ended in a " + match.getHomeScore() + "-" + match.getAwayScore() + " draw";
        }

        recap.setRecap(String.format("%s in the %s: %d-%d between %s and %s. This synthetic match has %d recorded events.",
                result, match.getCompetitionName(), match.getHomeScore(), match.getAwayScore(),
                match.getHomeTeamName(), match.getAwayTeamName(), events.size()));

        if ("FAN".equalsIgnoreCase(mode)) {
            String fanResult = match.getHomeScore() == match.getAwayScore()
                    ? String.format("shared a %d-%d draw", match.getHomeScore(), match.getAwayScore())
                    : String.format("%s won %d-%d", match.getHomeScore() > match.getAwayScore() ? match.getHomeTeamName() : match.getAwayTeamName(), Math.max(match.getHomeScore(), match.getAwayScore()), Math.min(match.getHomeScore(), match.getAwayScore()));
            recap.setFanExplanation(String.format(
                    "What a match! %s. The game had %d valid goals. Check out the key moments in the timeline to see how it all unfolded.",
                    fanResult, events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).count()));
            recap.setAnalystExplanation(null);
        } else {
            recap.setFanExplanation(null);
            recap.setAnalystExplanation(String.format(
                    "Match analysis: %s vs %s resulted in a %d-%d final score. " +
                    "Key statistical observations include %d total events, " +
                    "with %d valid goals scored. The match featured %d shots, " +
                    "%d corners, and %d fouls. For detailed turning-point analysis, " +
                    "refer to the ranked insights which identify the most significant moments " +
                    "that influenced the match outcome based on goal timing, score changes, " +
                    "and disciplinary actions.",
                    match.getHomeTeamName(), match.getAwayTeamName(),
                    match.getHomeScore(), match.getAwayScore(),
                    events.size(),
                    events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).count(),
                    events.stream().filter(e -> e.getEventType().startsWith("SHOT")).count(),
                    events.stream().filter(e -> "CORNER".equals(e.getEventType())).count(),
                    events.stream().filter(e -> "FOUL".equals(e.getEventType())).count()
            ));
        }

        // Key moments summary
        List<String> goalEvents = events.stream()
                .filter(e -> e.isGoal() && !e.isDisallowed())
                .map(e -> String.format("%s' - %s by %s (%s)",
                        e.getDisplayClock(),
                        e.getEventType(),
                        e.getPlayerName() != null ? e.getPlayerName() : "Unknown",
                        e.getTeamName()))
                .collect(Collectors.toList());

        recap.setKeyMoments(String.join("; ", goalEvents));

        return recap;
    }

    public QuestionResponse answerQuestion(String matchId, QuestionRequest request) {
        validateQuestionRequest(request);
        Match match = datasetLoader.getMatch(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Match not found: " + matchId);
        }

        List<MatchEvent> events = datasetLoader.getMatchEvents(matchId);
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        if ("GEMINI".equalsIgnoreCase(aiMode)) {
            try {
                response = generateGeminiAnswer(match, request);
            } catch (Exception ex) {
                logger.warn("Gemini Q&A unavailable: {}", ex.getMessage());
                response = generateDemoAnswer(match, events, request);
                response.setAiMode("GEMINI_UNAVAILABLE");
            }
        } else {
            response = generateDemoAnswer(match, events, request);
        }

        return response;
    }

    private QuestionResponse generateDemoAnswer(Match match, List<MatchEvent> events, QuestionRequest request) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);
        String question = request.getQuestion().toLowerCase(Locale.ROOT);
        String mode = request.getMode();
        String preference = request.getPreference() != null ? request.getPreference().trim().toLowerCase(Locale.ROOT) : "";
        List<String> evidenceIds;

        // Filter events based on preference (team or player)
        List<MatchEvent> relevantEvents = events;
        if (!preference.isBlank()) {
            relevantEvents = events.stream()
                    .filter(e -> (e.getTeamName() != null && e.getTeamName().toLowerCase(Locale.ROOT).contains(preference)) ||
                               (e.getPlayerName() != null && e.getPlayerName().toLowerCase(Locale.ROOT).contains(preference)))
                    .toList();
        }

        long goalCount = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).count();
        long shotCount = events.stream().filter(e -> e.getEventType().startsWith("SHOT")).count();
        long foulCount = events.stream().filter(e -> "FOUL".equals(e.getEventType())).count();
        long cardCount = events.stream().filter(e -> e.isYellowCard() || e.isRedCard()).count();
        long cornerCount = events.stream().filter(e -> "CORNER".equals(e.getEventType())).count();
        long saveCount = events.stream().filter(e -> "SAVE".equals(e.getEventType())).count();
        long offsideCount = events.stream().filter(e -> "OFFSIDE".equals(e.getEventType())).count();

        long teamGoalCount = relevantEvents.stream().filter(e -> e.isGoal() && !e.isDisallowed()).count();
        long teamShotCount = relevantEvents.stream().filter(e -> e.getEventType().startsWith("SHOT")).count();
        long teamFoulCount = relevantEvents.stream().filter(e -> "FOUL".equals(e.getEventType())).count();
        long teamCardCount = relevantEvents.stream().filter(e -> e.isYellowCard() || e.isRedCard()).count();

        // Determine winner
        String winner;
        if (match.getHomeScore() > match.getAwayScore()) {
            winner = match.getHomeTeamName();
        } else if (match.getAwayScore() > match.getHomeScore()) {
            winner = match.getAwayTeamName();
        } else {
            winner = "Neither team"; // draw
        }

        // General match summary questions
        if (question.contains("what happened") || question.contains("tell me about") || question.contains("summary") || question.contains("overview")) {
            if ("FAN".equals(mode)) {
                String result = match.getHomeScore() > match.getAwayScore() 
                    ? match.getHomeTeamName() + " won " + match.getHomeScore() + "-" + match.getAwayScore()
                    : (match.getAwayScore() > match.getHomeScore() 
                        ? match.getAwayTeamName() + " won " + match.getAwayScore() + "-" + match.getHomeScore()
                        : "The match ended in a " + match.getHomeScore() + "-" + match.getAwayScore() + " draw");
                response.setAnswer(String.format("%s: %s %d-%d %s. There were %d goals, %d shots, %d fouls, and %d cards in this match.",
                        match.getCompetitionName(), match.getHomeTeamName(), match.getHomeScore(),
                        match.getAwayScore(), match.getAwayTeamName(), goalCount, shotCount, foulCount, cardCount));
            } else {
                response.setAnswer(String.format("Match summary: %s vs %s ended %d-%d. Statistical breakdown: %d goals, %d shots, %d corners, %d fouls, %d saves, %d offsides, %d cards. " +
                        "This data is derived from %d recorded events in the synthetic dataset.",
                        match.getHomeTeamName(), match.getAwayTeamName(), match.getHomeScore(), match.getAwayScore(),
                        goalCount, shotCount, cornerCount, foulCount, saveCount, offsideCount, cardCount, events.size()));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Winner/draw questions
        if (question.contains("who won") || question.contains("winner")) {
            if (match.getHomeScore() == match.getAwayScore()) {
                response.setAnswer(String.format("The match ended in a %d-%d draw between %s and %s.",
                        match.getHomeScore(), match.getAwayScore(), match.getHomeTeamName(), match.getAwayTeamName()));
            } else {
                response.setAnswer(String.format("%s won the match %d-%d against %s.",
                        winner, Math.max(match.getHomeScore(), match.getAwayScore()), 
                        Math.min(match.getHomeScore(), match.getAwayScore()), 
                        winner.equals(match.getHomeTeamName()) ? match.getAwayTeamName() : match.getHomeTeamName()));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        if (question.contains("draw") || question.contains("tie")) {
            if (match.getHomeScore() == match.getAwayScore()) {
                response.setAnswer(String.format("Yes, the match ended in a %d-%d draw.", match.getHomeScore(), match.getAwayScore()));
            } else {
                response.setAnswer(String.format("No, the match did not end in a draw. %s won %d-%d.",
                        winner, match.getHomeScore(), match.getAwayScore()));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Score/result questions
        if (question.contains("score") || question.contains("result")) {
            if ("FAN".equals(mode)) {
                if (!preference.isBlank()) {
                    response.setAnswer(String.format("The final score was %s %d - %d %s. %s scored %d goal(s) in this match.",
                            match.getHomeTeamName(), match.getHomeScore(),
                            match.getAwayScore(), match.getAwayTeamName(),
                            preference, teamGoalCount));
                } else {
                    response.setAnswer(String.format("The final score was %s %d - %d %s",
                            match.getHomeTeamName(), match.getHomeScore(),
                            match.getAwayScore(), match.getAwayTeamName()));
                }
            } else {
                if (!preference.isBlank()) {
                    response.setAnswer(String.format("Match result: %s %d - %d %s. This score was derived from %d valid goal events. " +
                            "%s contributed %d goal(s) and %d shot(s) to this match.",
                            match.getHomeTeamName(), match.getHomeScore(),
                            match.getAwayScore(), match.getAwayTeamName(), goalCount,
                            preference, teamGoalCount, teamShotCount));
                } else {
                    response.setAnswer(String.format("Match result: %s %d - %d %s. This score was derived from %d valid goal events " +
                            "recorded in the event log. All goals have been verified against the match events.",
                            match.getHomeTeamName(), match.getHomeScore(),
                            match.getAwayScore(), match.getAwayTeamName(), goalCount));
                }
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Goal questions
        if (question.contains("goal")) {
            if (!preference.isBlank()) {
                response.setAnswer(String.format("%s scored %d goal(s) in this match. The match had %d total goals.",
                        preference, teamGoalCount, goalCount));
            } else {
                response.setAnswer(String.format("There were %d goals scored in this match. These were recorded as %d valid goal events " +
                        "in the event log. The score progression can be tracked through the match timeline.",
                        goalCount, goalCount));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Shot questions
        if (question.contains("shot")) {
            if (!preference.isBlank()) {
                response.setAnswer(String.format("%s had %d shot(s) in this match. The match had %d total shots.",
                        preference, teamShotCount, shotCount));
            } else {
                response.setAnswer(String.format("There were %d total shots recorded in this match, including shots on target, " +
                        "shots off target, and goals. This is based on the shot-type events in the dataset.",
                        shotCount));
            }
            evidenceIds = events.stream().filter(this::isShot).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Foul questions
        if (question.contains("foul")) {
            if (!preference.isBlank()) {
                response.setAnswer(String.format("%s committed %d foul(s) in this match. The match had %d total fouls.",
                        preference, teamFoulCount, foulCount));
            } else {
                response.setAnswer(String.format("There were %d fouls recorded in this match, as indicated by FOUL events " +
                        "in the event log.", foulCount));
            }
            evidenceIds = events.stream().filter(e -> "FOUL".equals(e.getEventType())).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Card questions
        if (question.contains("card") || question.contains("yellow") || question.contains("red")) {
            if (!preference.isBlank()) {
                response.setAnswer(String.format("%s received %d card(s) in this match. The match had %d total cards (%d yellow, %d red).",
                        preference, teamCardCount, cardCount,
                        events.stream().filter(e -> e.isYellowCard()).count(),
                        events.stream().filter(e -> e.isRedCard()).count()));
            } else {
                response.setAnswer(String.format("There were %d cards shown in this match (%d yellow, %d red). " +
                        "This is based on YELLOW_CARD and RED_CARD events in the dataset.",
                        cardCount,
                        events.stream().filter(e -> e.isYellowCard()).count(),
                        events.stream().filter(e -> e.isRedCard()).count()));
            }
            evidenceIds = events.stream().filter(e -> e.isYellowCard() || e.isRedCard()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Corner questions
        if (question.contains("corner")) {
            response.setAnswer(String.format("There were %d corners recorded in this match, as indicated by CORNER events " +
                    "in the event log.", cornerCount));
            evidenceIds = events.stream().filter(e -> "CORNER".equals(e.getEventType())).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Save questions
        if (question.contains("save")) {
            response.setAnswer(String.format("There were %d saves recorded in this match, as indicated by SAVE events " +
                    "in the event log.", saveCount));
            evidenceIds = events.stream().filter(e -> "SAVE".equals(e.getEventType())).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Offside questions
        if (question.contains("offside")) {
            response.setAnswer(String.format("There were %d offsides recorded in this match, as indicated by OFFSIDE events " +
                    "in the event log.", offsideCount));
            evidenceIds = events.stream().filter(e -> "OFFSIDE".equals(e.getEventType())).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Turning points questions
        if (question.contains("turning") || question.contains("key")) {
            response.setAnswer("Key turning points have been identified based on goal timing, score changes, " +
                    "red cards, and penalties. Use the turning-points endpoint to see the ranked insights " +
                    "with evidence references.");
            evidenceIds = analysisService.calculateTurningPoints(match.getMatchId()).stream()
                    .flatMap(tp -> tp.getEventIds().stream()).distinct().toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Team performance questions
        if (question.contains("play") || question.contains("perform")) {
            if (!preference.isBlank()) {
                if ("FAN".equals(mode)) {
                    response.setAnswer(String.format("%s scored %d goal(s) and had %d shot(s) in this match. They committed %d foul(s) and received %d card(s).",
                            preference, teamGoalCount, teamShotCount, teamFoulCount, teamCardCount));
                } else {
                    response.setAnswer(String.format("%s performance analysis: %d goal(s), %d shot(s), %d foul(s), %d card(s). " +
                            "This is based on event data from the match log. Compare with the overall match statistics for context.",
                            preference, teamGoalCount, teamShotCount, teamFoulCount, teamCardCount));
                }
                evidenceIds = relevantEvents.stream().map(MatchEvent::getEventId).toList();
                response.setEvidenceEventIds(evidenceIds);
            } else {
                response.setAnswer("To get a team performance summary, specify a team name using the preference field. " +
                        "I can then provide goals, shots, fouls, and cards for that specific team.");
                evidenceIds = List.of();
            }
            return response;
        }

        // Timeline/when questions
        if (question.contains("when") || question.contains("timeline") || question.contains("time")) {
            if ("FAN".equals(mode)) {
                response.setAnswer(String.format("The match had %d goals. Check the timeline tab to see when each goal was scored and other key events occurred.",
                        goalCount));
            } else {
                response.setAnswer(String.format("Match timeline spans %d events over approximately 90 minutes. First half events occur before minute 45, second half after. " +
                        "Use the timeline endpoint for detailed event chronology with timestamps.",
                        events.size()));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Preference-specific questions
        if (!preference.isBlank()) {
            response.setAnswer(String.format("I can provide information about %s's performance in this match, including goals, " +
                    "shots, fouls, cards, and turning points. Ask specifically about one of these topics.",
                    preference));
            evidenceIds = relevantEvents.stream().map(MatchEvent::getEventId).toList();
        } else {
            response.setAnswer("Based on the available match data, I can provide information about: " +
                    "match result, goals, shots, fouls, cards, corners, saves, offsides, turning points, team performance, and timeline. " +
                    "Please ask a specific question about one of these topics.");
            evidenceIds = List.of();
        }

        response.setEvidenceEventIds(evidenceIds);
        return response;
    }

        // Determine winner
        String winner;
        if (match.getHomeScore() > match.getAwayScore()) {
            winner = match.getHomeTeamName();
        } else if (match.getAwayScore() > match.getHomeScore()) {
            winner = match.getAwayTeamName();
        } else {
            winner = "Neither team"; // draw
        }

        // General match summary questions
        if (question.contains("what happened") || question.contains("tell me about") || question.contains("summary") || question.contains("overview")) {
            if ("FAN".equals(mode)) {
                String result = match.getHomeScore() > match.getAwayScore() 
                    ? match.getHomeTeamName() + " won " + match.getHomeScore() + "-" + match.getAwayScore()
                    : (match.getAwayScore() > match.getHomeScore() 
                        ? match.getAwayTeamName() + " won " + match.getAwayScore() + "-" + match.getHomeScore()
                        : "The match ended in a " + match.getHomeScore() + "-" + match.getAwayScore() + " draw");
                response.setAnswer(String.format("%s: %s %d-%d %s. There were %d goals, %d shots, %d fouls, and %d cards in this match.",
                        match.getCompetitionName(), match.getHomeTeamName(), match.getHomeScore(),
                        match.getAwayScore(), match.getAwayTeamName(), goalCount, shotCount, foulCount, cardCount));
            } else {
                response.setAnswer(String.format("Match summary: %s vs %s ended %d-%d. Statistical breakdown: %d goals, %d shots, %d corners, %d fouls, %d saves, %d offsides, %d cards. " +
                        "This data is derived from %d recorded events in the synthetic dataset.",
                        match.getHomeTeamName(), match.getAwayTeamName(), match.getHomeScore(), match.getAwayScore(),
                        goalCount, shotCount, cornerCount, foulCount, saveCount, offsideCount, cardCount, events.size()));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Winner/draw questions
        if (question.contains("who won") || question.contains("winner")) {
            if (match.getHomeScore() == match.getAwayScore()) {
                response.setAnswer(String.format("The match ended in a %d-%d draw between %s and %s.",
                        match.getHomeScore(), match.getAwayScore(), match.getHomeTeamName(), match.getAwayTeamName()));
            } else {
                response.setAnswer(String.format("%s won the match %d-%d against %s.",
                        winner, Math.max(match.getHomeScore(), match.getAwayScore()), 
                        Math.min(match.getHomeScore(), match.getAwayScore()), 
                        winner.equals(match.getHomeTeamName()) ? match.getAwayTeamName() : match.getHomeTeamName()));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        if (question.contains("draw") || question.contains("tie")) {
            if (match.getHomeScore() == match.getAwayScore()) {
                response.setAnswer(String.format("Yes, the match ended in a %d-%d draw.", match.getHomeScore(), match.getAwayScore()));
            } else {
                response.setAnswer(String.format("No, the match did not end in a draw. %s won %d-%d.",
                        winner, match.getHomeScore(), match.getAwayScore()));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Score/result questions
        if (question.contains("score") || question.contains("result")) {
            if ("FAN".equals(mode)) {
                if (!preference.isBlank()) {
                    response.setAnswer(String.format("The final score was %s %d - %d %s. %s scored %d goal(s) in this match.",
                            match.getHomeTeamName(), match.getHomeScore(),
                            match.getAwayScore(), match.getAwayTeamName(),
                            preference, teamGoalCount));
                } else {
                    response.setAnswer(String.format("The final score was %s %d - %d %s",
                            match.getHomeTeamName(), match.getHomeScore(),
                            match.getAwayScore(), match.getAwayTeamName()));
                }
            } else {
                if (!preference.isBlank()) {
                    response.setAnswer(String.format("Match result: %s %d - %d %s. This score was derived from %d valid goal events. " +
                            "%s contributed %d goal(s) and %d shot(s) to this match.",
                            match.getHomeTeamName(), match.getHomeScore(),
                            match.getAwayScore(), match.getAwayTeamName(), goalCount,
                            preference, teamGoalCount, teamShotCount));
                } else {
                    response.setAnswer(String.format("Match result: %s %d - %d %s. This score was derived from %d valid goal events " +
                            "recorded in the event log. All goals have been verified against the match events.",
                            match.getHomeTeamName(), match.getHomeScore(),
                            match.getAwayScore(), match.getAwayTeamName(), goalCount));
                }
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Goal questions
        if (question.contains("goal")) {
            if (!preference.isBlank()) {
                response.setAnswer(String.format("%s scored %d goal(s) in this match. The match had %d total goals.",
                        preference, teamGoalCount, goalCount));
            } else {
                response.setAnswer(String.format("There were %d goals scored in this match. These were recorded as %d valid goal events " +
                        "in the event log. The score progression can be tracked through the match timeline.",
                        goalCount, goalCount));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Shot questions
        if (question.contains("shot")) {
            if (!preference.isBlank()) {
                response.setAnswer(String.format("%s had %d shot(s) in this match. The match had %d total shots.",
                        preference, teamShotCount, shotCount));
            } else {
                response.setAnswer(String.format("There were %d total shots recorded in this match, including shots on target, " +
                        "shots off target, and goals. This is based on the shot-type events in the dataset.",
                        shotCount));
            }
            evidenceIds = events.stream().filter(this::isShot).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Foul questions
        if (question.contains("foul")) {
            if (!preference.isBlank()) {
                response.setAnswer(String.format("%s committed %d foul(s) in this match. The match had %d total fouls.",
                        preference, teamFoulCount, foulCount));
            } else {
                response.setAnswer(String.format("There were %d fouls recorded in this match, as indicated by FOUL events " +
                        "in the event log.", foulCount));
            }
            evidenceIds = events.stream().filter(e -> "FOUL".equals(e.getEventType())).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Card questions
        if (question.contains("card") || question.contains("yellow") || question.contains("red")) {
            if (!preference.isBlank()) {
                response.setAnswer(String.format("%s received %d card(s) in this match. The match had %d total cards (%d yellow, %d red).",
                        preference, teamCardCount, cardCount,
                        events.stream().filter(e -> e.isYellowCard()).count(),
                        events.stream().filter(e -> e.isRedCard()).count()));
            } else {
                response.setAnswer(String.format("There were %d cards shown in this match (%d yellow, %d red). " +
                        "This is based on YELLOW_CARD and RED_CARD events in the dataset.",
                        cardCount,
                        events.stream().filter(e -> e.isYellowCard()).count(),
                        events.stream().filter(e -> e.isRedCard()).count()));
            }
            evidenceIds = events.stream().filter(e -> e.isYellowCard() || e.isRedCard()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Corner questions
        if (question.contains("corner")) {
            response.setAnswer(String.format("There were %d corners recorded in this match, as indicated by CORNER events " +
                    "in the event log.", cornerCount));
            evidenceIds = events.stream().filter(e -> "CORNER".equals(e.getEventType())).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Save questions
        if (question.contains("save")) {
            response.setAnswer(String.format("There were %d saves recorded in this match, as indicated by SAVE events " +
                    "in the event log.", saveCount));
            evidenceIds = events.stream().filter(e -> "SAVE".equals(e.getEventType())).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Offside questions
        if (question.contains("offside")) {
            response.setAnswer(String.format("There were %d offsides recorded in this match, as indicated by OFFSIDE events " +
                    "in the event log.", offsideCount));
            evidenceIds = events.stream().filter(e -> "OFFSIDE".equals(e.getEventType())).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Turning points questions
        if (question.contains("turning") || question.contains("key")) {
            response.setAnswer("Key turning points have been identified based on goal timing, score changes, " +
                    "red cards, and penalties. Use the turning-points endpoint to see the ranked insights " +
                    "with evidence references.");
            evidenceIds = analysisService.calculateTurningPoints(match.getMatchId()).stream()
                    .flatMap(tp -> tp.getEventIds().stream()).distinct().toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Team performance questions
        if (question.contains("play") || question.contains("perform")) {
            if (!preference.isBlank()) {
                if ("FAN".equals(mode)) {
                    response.setAnswer(String.format("%s scored %d goal(s) and had %d shot(s) in this match. They committed %d foul(s) and received %d card(s).",
                            preference, teamGoalCount, teamShotCount, teamFoulCount, teamCardCount));
                } else {
                    response.setAnswer(String.format("%s performance analysis: %d goal(s), %d shot(s), %d foul(s), %d card(s). " +
                            "This is based on event data from the match log. Compare with the overall match statistics for context.",
                            preference, teamGoalCount, teamShotCount, teamFoulCount, teamCardCount));
                }
                evidenceIds = relevantEvents.stream().map(MatchEvent::getEventId).toList();
                response.setEvidenceEventIds(evidenceIds);
            } else {
                response.setAnswer("To get a team performance summary, specify a team name using the preference field. " +
                        "I can then provide goals, shots, fouls, and cards for that specific team.");
                response.setEvidenceEventIds(List.of());
            }
            return response;
        }

        // Timeline/when questions
        if (question.contains("when") || question.contains("timeline") || question.contains("time")) {
            if ("FAN".equals(mode)) {
                response.setAnswer(String.format("The match had %d goals. Check the timeline tab to see when each goal was scored and other key events occurred.",
                        goalCount));
            } else {
                response.setAnswer(String.format("Match timeline spans %d events over approximately 90 minutes. First half events occur before minute 45, second half after. " +
                        "Use the timeline endpoint for detailed event chronology with timestamps.",
                        events.size()));
            }
            evidenceIds = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
            response.setEvidenceEventIds(evidenceIds);
            return response;
        }

        // Preference-specific questions
        if (!preference.isBlank()) {
            response.setAnswer(String.format("I can provide information about %s's performance in this match, including goals, " +
                    "shots, fouls, cards, and turning points. Ask specifically about one of these topics.",
                    preference));
            response.setEvidenceEventIds(relevantEvents.stream().map(MatchEvent::getEventId).toList());
        } else {
            response.setAnswer("Based on the available match data, I can provide information about: " +
                    "match result, goals, shots, fouls, cards, corners, saves, offsides, turning points, team performance, and timeline. " +
                    "Please ask a specific question about one of these topics.");
            response.setEvidenceEventIds(List.of());
        }

        return response;
    }

    private MatchRecap generateGeminiRecap(Match match, List<MatchEvent> events, String mode) throws Exception {
        String prompt = "Create a concise evidence-grounded football recap for " + match.getMatchId() + ". " +
                "Audience=" + mode + ". State facts separately from interpretation. Use only tool evidence; cite event IDs.";
        JsonNode result = callGemini(match.getMatchId(), prompt, mode);
        MatchRecap recap = new MatchRecap();
        recap.setMatchId(match.getMatchId());
        recap.setAiMode("GEMINI");
        String text = result.path("text").asText("");
        if (text.isBlank()) throw new IllegalStateException("Gemini returned no text");
        List<String> evidence = new ArrayList<>();
        result.path("evidenceEventIds").forEach(n -> evidence.add(n.asText()));
        validateEvidence(match.getMatchId(), evidence);
        recap.setRecap(text);
        if ("FAN".equalsIgnoreCase(mode)) recap.setFanExplanation(text); else recap.setAnalystExplanation(text);
        recap.setKeyMoments(events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).limit(5)
                .map(e -> e.getEventId() + " " + e.getDisplayClock() + " - " + e.getDescription()).collect(Collectors.joining("; ")));
        return recap;
    }

    private QuestionResponse generateGeminiAnswer(Match match, QuestionRequest request) throws Exception {
        String preference = request.getPreference() == null ? "" : request.getPreference().trim();
        String prompt = request.getQuestion() + (preference.isBlank() ? "" : "\nPersonalization preference: " + preference);
        JsonNode result = callGemini(match.getMatchId(), prompt, request.getMode());
        String answer = result.path("text").asText("");
        if (answer.isBlank()) throw new IllegalStateException("Gemini returned no answer");
        QuestionResponse response = new QuestionResponse();
        response.setAnswer(answer);
        response.setAiMode("GEMINI");
        List<String> evidence = new ArrayList<>();
        if (!result.path("evidenceEventIds").isArray()) throw new IllegalStateException("Gemini response is missing evidenceEventIds");
        result.path("evidenceEventIds").forEach(n -> evidence.add(n.asText()));
        validateEvidence(match.getMatchId(), evidence);
        response.setEvidenceEventIds(evidence);
        return response;
    }

    /** Uses Gemini generateContent with backend-controlled function tools and evidence validation. */
    private JsonNode callGemini(String matchId, String userText, String mode) throws Exception {
        if (geminiApiKey == null || geminiApiKey.isBlank()) throw new IllegalStateException("GEMINI_API_KEY is required");
        if (geminiModel == null || geminiModel.isBlank()) throw new IllegalStateException("GEMINI_MODEL is required");
        String endpoint = geminiBaseUrl.replaceAll("/$", "") + "/models/" + geminiModel + ":generateContent";
        List<Map<String,Object>> contents = new ArrayList<>();
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userText))));
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", "You are MatchMind. Retrieve evidence with tools before making factual claims. Return JSON only with keys text and evidenceEventIds. Never invent event IDs. Separate facts from interpretation. Audience mode: " + mode))));
        body.put("contents", contents);
        body.put("tools", geminiToolDefinitions());
        body.put("generationConfig", Map.of("responseMimeType", "application/json"));

        JsonNode response = postGemini(endpoint, body);
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (parts.isArray()) {
            List<Map<String,Object>> functionResponses = new ArrayList<>();
            if (parts.size() > 5) throw new IllegalStateException("Gemini returned too many tool calls");
            for (JsonNode part : parts) {
                JsonNode functionCall = part.get("functionCall");
                if (functionCall == null || functionCall.isMissingNode()) continue;
                String name = functionCall.path("name").asText();
                Object output = executeTool(matchId, name, functionCall.path("args"));
                functionResponses.add(Map.of("functionResponse", Map.of("name", name, "response", Map.of("result", output))));
            }
            if (!functionResponses.isEmpty()) {
                contents.add(Map.of("role", "model", "parts", objectMapper.convertValue(parts, Object.class)));
                contents.add(Map.of("role", "user", "parts", functionResponses));
                body.put("contents", contents);
                response = postGemini(endpoint, body);
            } else {
                // Some models ignore tools. Force the same authoritative backend evidence into a second request.
                contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", "Use this authoritative evidence and return the requested JSON:\n" + buildEvidenceContext(matchId)))));
                body.remove("tools");
                body.put("contents", contents);
                response = postGemini(endpoint, body);
            }
        }
        String text = extractGeminiText(response);
        JsonNode parsed;
        try { parsed = objectMapper.readTree(stripJsonFence(text)); }
        catch (Exception ex) { throw new IllegalStateException("Malformed Gemini JSON response"); }
        if (!parsed.path("text").isTextual() || parsed.path("text").asText().isBlank() || !parsed.path("evidenceEventIds").isArray()) {
            throw new IllegalStateException("Gemini response schema is invalid");
        }
        return parsed;
    }

    private JsonNode postGemini(String endpoint, Map<String,Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", geminiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
        HttpResponse<String> result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (result.statusCode() / 100 != 2) throw new IllegalStateException("Gemini HTTP " + result.statusCode() + ": " + result.body());
        return objectMapper.readTree(result.body());
    }

    private String extractGeminiText(JsonNode response) {
        StringBuilder text = new StringBuilder();
        response.path("candidates").path(0).path("content").path("parts").forEach(part -> {
            if (part.has("text")) text.append(part.path("text").asText());
        });
        return text.toString();
    }

    private String buildEvidenceContext(String matchId) throws Exception {
        Map<String,Object> context = new LinkedHashMap<>();
        context.put("match", datasetLoader.getMatch(matchId));
        context.put("statistics", analysisService.calculateStatistics(matchId));
        context.put("turningPoints", analysisService.calculateTurningPoints(matchId));
        context.put("supportingEvents", datasetLoader.getMatchEvents(matchId).stream()
                .filter(e -> e.isGoal() || e.isRedCard() || e.isPenalty() || e.isDisallowed())
                .map(e -> Map.of("eventId", e.getEventId(), "eventType", e.getEventType(), "minute", e.getDisplayClock(), "description", e.getDescription(), "scoreHomeAfterEvent", e.getScoreHomeAfterEvent(), "scoreAwayAfterEvent", e.getScoreAwayAfterEvent()))
                .toList());
        return objectMapper.writeValueAsString(context);
    }

    private String stripJsonFence(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```") && value.endsWith("```")) {
            int firstNewline = value.indexOf('\n');
            return firstNewline >= 0 ? value.substring(firstNewline + 1, value.length() - 3).trim() : value;
        }
        return value;
    }

    private List<Map<String,Object>> geminiToolDefinitions() {
        List<Map<String,Object>> tools = new ArrayList<>();
        for (String name : List.of("get_match_state", "get_match_statistics", "get_turning_points", "get_event_evidence", "get_player_or_team_events"))
            tools.add(Map.of("name", name, "description", "Retrieve authoritative match evidence for " + name + ".", "parameters", Map.of("type", "object", "properties", Map.of("eventId", Map.of("type", "string"), "teamId", Map.of("type", "string"), "playerId", Map.of("type", "string")), "additionalProperties", false)));
        return List.of(Map.of("functionDeclarations", tools));
    }

    private Object executeTool(String matchId, String name, JsonNode args) {
        return switch (name) {
            case "get_match_state" -> datasetLoader.getMatch(matchId);
            case "get_match_statistics" -> analysisService.calculateStatistics(matchId);
            case "get_turning_points" -> analysisService.calculateTurningPoints(matchId);
            case "get_event_evidence" -> {
                String eventId = requiredToolArgument(args, "eventId");
                List<MatchEvent> result = datasetLoader.getMatchEvents(matchId).stream().filter(e -> eventId.equals(e.getEventId())).toList();
                if (result.isEmpty()) throw new IllegalArgumentException("eventId does not belong to the active match");
                yield result;
            }
            case "get_player_or_team_events" -> {
                String teamId = args.path("teamId").asText("").trim();
                String playerId = args.path("playerId").asText("").trim();
                if (teamId.isBlank() && playerId.isBlank()) throw new IllegalArgumentException("teamId or playerId is required");
                List<MatchEvent> result = datasetLoader.getMatchEvents(matchId).stream()
                        .filter(e -> (!teamId.isBlank() && teamId.equals(e.getTeamId())) || (!playerId.isBlank() && playerId.equals(e.getPlayerId()))).toList();
                yield result;
            }
            default -> throw new IllegalArgumentException("Unknown tool: " + name);
        };
    }

    private String requiredToolArgument(JsonNode args, String name) {
        String value = args.path(name).asText("").trim();
        if (value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private void validateQuestionRequest(QuestionRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        if (request.getQuestion().length() > 1000) throw new IllegalArgumentException("question must be at most 1000 characters");
        if (request.getMode() == null || !("FAN".equalsIgnoreCase(request.getMode()) || "ANALYST".equalsIgnoreCase(request.getMode()))) {
            throw new IllegalArgumentException("mode must be FAN or ANALYST");
        }
        if (request.getPreference() != null && request.getPreference().length() > 120) {
            throw new IllegalArgumentException("preference must be at most 120 characters");
        }
    }

    private boolean isShot(MatchEvent event) {
        return Set.of("SHOT", "SHOT_ON_TARGET", "SHOT_OFF_TARGET", "GOAL", "PENALTY_SCORED", "PENALTY_MISSED").contains(event.getEventType());
    }

    private void validateEvidence(String matchId, List<String> ids) {
        Set<String> valid = datasetLoader.getMatchEvents(matchId).stream().map(MatchEvent::getEventId).collect(Collectors.toSet());
        if (ids.stream().anyMatch(id -> id == null || id.isBlank() || !valid.contains(id))) throw new IllegalStateException("Gemini returned an unknown evidence id");
    }
}
