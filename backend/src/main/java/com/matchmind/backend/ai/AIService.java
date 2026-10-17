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
import com.matchmind.backend.dto.MatchPeriodComparison;
import com.matchmind.backend.dto.MatchRecap;
import com.matchmind.backend.dto.QuestionRequest;
import com.matchmind.backend.dto.QuestionResponse;
import com.matchmind.backend.dto.TurningPoint;
import com.matchmind.backend.model.Match;
import com.matchmind.backend.model.MatchEvent;
import com.matchmind.backend.model.MatchStatistics;
import com.matchmind.backend.model.Player;
import com.matchmind.backend.service.DatasetLoader;
import java.util.Comparator;
import java.util.Optional;

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

        String question = request.getQuestion().trim();
        String qLower = question.toLowerCase(Locale.ROOT);
        String mode = request.getMode() != null ? request.getMode().toUpperCase(Locale.ROOT) : "FAN";
        String preference = request.getPreference() != null ? request.getPreference().trim() : "";
        String prefLower = preference.toLowerCase(Locale.ROOT);

        MatchStatistics stats = analysisService.calculateStatistics(match.getMatchId());
        List<TurningPoint> turningPoints = analysisService.calculateTurningPoints(match.getMatchId());
        MatchPeriodComparison periodComparison = analysisService.comparePeriods(match.getMatchId());

        // Resolve Target Team (from preference or question)
        String homeName = match.getHomeTeamName();
        String awayName = match.getAwayTeamName();
        String homeLower = homeName.toLowerCase(Locale.ROOT);
        String awayLower = awayName.toLowerCase(Locale.ROOT);
        String targetTeam = null;

        if (!prefLower.isBlank()) {
            if (homeLower.contains(prefLower) || prefLower.contains(homeLower)) targetTeam = homeName;
            else if (awayLower.contains(prefLower) || prefLower.contains(awayLower)) targetTeam = awayName;
        }
        if (targetTeam == null) {
            String homeFirstWord = homeLower.split("\\s+")[0];
            String awayFirstWord = awayLower.split("\\s+")[0];
            if (qLower.contains(homeLower) || (homeFirstWord.length() > 3 && qLower.contains(homeFirstWord))) {
                targetTeam = homeName;
            } else if (qLower.contains(awayLower) || (awayFirstWord.length() > 3 && qLower.contains(awayFirstWord))) {
                targetTeam = awayName;
            }
        }

        // Resolve Target Player (from preference or question)
        String targetPlayer = null;
        if (!prefLower.isBlank()) {
            for (MatchEvent e : events) {
                if (e.getPlayerName() != null && !e.getPlayerName().isBlank()) {
                    String pName = e.getPlayerName().toLowerCase(Locale.ROOT);
                    if (pName.contains(prefLower) || prefLower.contains(pName)) {
                        targetPlayer = e.getPlayerName();
                        break;
                    }
                }
            }
        }
        if (targetPlayer == null) {
            for (MatchEvent e : events) {
                if (e.getPlayerName() != null && !e.getPlayerName().isBlank()) {
                    String pName = e.getPlayerName().toLowerCase(Locale.ROOT);
                    if (qLower.contains(pName)) {
                        targetPlayer = e.getPlayerName();
                        break;
                    }
                    String[] parts = pName.split("\\s+");
                    if (parts.length > 1 && parts[parts.length - 1].length() > 3 && qLower.contains(parts[parts.length - 1])) {
                        targetPlayer = e.getPlayerName();
                        break;
                    }
                }
            }
        }

        // 1. Player Inquiries
        if (targetPlayer != null && (
                !prefLower.isBlank() ||
                qLower.contains("score") || qLower.contains("goal") || qLower.contains("play") ||
                qLower.contains("perform") || qLower.contains("how did") || qLower.contains("tell me about") ||
                qLower.contains("what did") || qLower.contains("shot") || qLower.contains("card") ||
                qLower.contains("foul") || qLower.contains("assist") || qLower.contains("save") ||
                qLower.contains("who is") || qLower.contains("did ")
        )) {
            return answerPlayerInquiry(match, events, targetPlayer, mode);
        }

        // 2. Turning Points, Momentum & Key Moments
        if (qLower.contains("turning") || qLower.contains("momentum") || qLower.contains("key moment") ||
            qLower.contains("critical") || qLower.contains("crucial") || qLower.contains("game chang") ||
            qLower.contains("why did the game change") || qLower.contains("why was") || qLower.contains("shift") ||
            qLower.contains("decisive moment") || qLower.contains("importance")) {
            return answerTurningPoints(match, events, turningPoints, stats, mode, qLower);
        }

        // 3. Match State Progression & Halves Comparison
        if (qLower.contains("second half") || qLower.contains("first half") || qLower.contains("half time") ||
            qLower.contains("halftime") || qLower.contains("period") || qLower.contains("state change") ||
            qLower.contains("progression") || qLower.contains("across halves") || qLower.contains("between halves")) {
            return answerMatchStateAndHalves(match, events, periodComparison, mode, qLower);
        }

        // 4. Goals & Scorers
        if (qLower.contains("who scored") || qLower.contains("scorers") || qLower.contains("scorer") ||
            qLower.contains("goal") || qLower.contains("disallowed") || qLower.contains("own goal") ||
            qLower.contains("penalty") || qLower.contains("penalties") || qLower.contains("open the scoring") ||
            qLower.contains("first goal")) {
            return answerGoalsAndScorers(match, events, targetTeam, mode, qLower);
        }

        // 5. Result, Winner, Draw, Scoreline
        if (qLower.contains("who won") || qLower.contains("winner") || qLower.contains("win") ||
            qLower.contains("draw") || qLower.contains("tie") || qLower.contains("score") ||
            qLower.contains("result") || qLower.contains("outcome") || qLower.contains("who lost") ||
            qLower.contains("lose") || qLower.contains("lost") || qLower.contains("3 points") ||
            qLower.contains("clean sheet")) {
            return answerMatchResult(match, events, targetTeam, mode, qLower);
        }

        // 6. Cards, Red Cards, Discipline & Fouls
        if (qLower.contains("card") || qLower.contains("red") || qLower.contains("yellow") ||
            qLower.contains("sent off") || qLower.contains("send off") || qLower.contains("booking") ||
            qLower.contains("booked") || qLower.contains("foul") || qLower.contains("discipline") ||
            qLower.contains("dirty") || qLower.contains("referee")) {
            return answerCardsAndFouls(match, events, stats, targetTeam, mode, qLower);
        }

        // 7. Shots, Accuracy, Goalkeeping & Saves
        if (qLower.contains("shot") || qLower.contains("target") || qLower.contains("save") ||
            qLower.contains("goalkeeper") || qLower.contains("keeper") || qLower.contains("chance") ||
            qLower.contains("attempt") || qLower.contains("shooting")) {
            return answerShotsAndSaves(match, events, stats, targetTeam, mode, qLower);
        }

        // 8. Corners, Offsides & Set Pieces
        if (qLower.contains("corner") || qLower.contains("offside") || qLower.contains("set piece")) {
            return answerCornersAndOffsides(match, events, stats, targetTeam, mode, qLower);
        }

        // 9. Substitutions
        if (qLower.contains("substitution") || qLower.contains("sub") || qLower.contains("bench")) {
            return answerSubstitutions(match, events, targetTeam, mode);
        }

        // 10. Team Performance & Tactical Flow
        if (qLower.contains("perform") || qLower.contains("play") || qLower.contains("tactics") ||
            (targetTeam != null && (qLower.contains("how") || qLower.contains("summary")))) {
            return answerTeamPerformance(match, events, stats, turningPoints, targetTeam, mode);
        }

        // 11. General Match Summary / Overview
        if (qLower.contains("what happened") || qLower.contains("tell me about") || qLower.contains("summary") ||
            qLower.contains("overview") || qLower.contains("recap") || qLower.contains("story") ||
            qLower.contains("match") || qLower.contains("game")) {
            return answerGeneralSummary(match, events, stats, turningPoints, mode);
        }

        // 12. Fallback for genuinely unsupported or ambiguous questions
        return answerFallback(match, preference);
    }

    private QuestionResponse answerPlayerInquiry(Match match, List<MatchEvent> events, String playerName, String mode) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        List<MatchEvent> playerEvents = events.stream()
                .filter(e -> playerName.equalsIgnoreCase(e.getPlayerName()) || playerName.equalsIgnoreCase(e.getRelatedPlayerName()))
                .toList();

        List<MatchEvent> goals = playerEvents.stream()
                .filter(e -> playerName.equalsIgnoreCase(e.getPlayerName()) && e.isGoal() && !e.isDisallowed())
                .toList();
        List<MatchEvent> shots = playerEvents.stream()
                .filter(e -> playerName.equalsIgnoreCase(e.getPlayerName()) && isShot(e))
                .toList();
        List<MatchEvent> yellowCards = playerEvents.stream()
                .filter(e -> playerName.equalsIgnoreCase(e.getPlayerName()) && e.isYellowCard())
                .toList();
        List<MatchEvent> redCards = playerEvents.stream()
                .filter(e -> playerName.equalsIgnoreCase(e.getPlayerName()) && e.isRedCard())
                .toList();
        List<MatchEvent> fouls = playerEvents.stream()
                .filter(e -> playerName.equalsIgnoreCase(e.getPlayerName()) && "FOUL".equals(e.getEventType()))
                .toList();
        List<MatchEvent> saves = playerEvents.stream()
                .filter(e -> playerName.equalsIgnoreCase(e.getPlayerName()) && "SAVE".equals(e.getEventType()))
                .toList();

        String teamName = playerEvents.stream()
                .filter(e -> e.getTeamName() != null && !e.getTeamName().isBlank())
                .map(MatchEvent::getTeamName)
                .findFirst()
                .orElse("their team");

        // Lookup squad details if available
        Player playerMeta = datasetLoader.getAllPlayers().stream()
                .filter(p -> p.getPlayerName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        String position = playerMeta != null ? playerMeta.getPosition() : "";
        int squadNumber = playerMeta != null ? playerMeta.getSquadNumber() : 0;
        String playerDescriptor = playerName + (squadNumber > 0 ? " (#" + squadNumber + (position.isEmpty() ? "" : ", " + position) + ")" : "");

        List<String> evidence = playerEvents.stream().map(MatchEvent::getEventId).toList();
        response.setEvidenceEventIds(evidence);

        if ("FAN".equalsIgnoreCase(mode)) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s represented %s in this match. ", playerDescriptor, teamName));
            if (!goals.isEmpty()) {
                String minutes = goals.stream().map(MatchEvent::getDisplayClock).collect(Collectors.joining("', ", "", "'"));
                sb.append(String.format("What an impact! Scored %d goal(s) at %s! ", goals.size(), minutes));
            } else if (!shots.isEmpty()) {
                sb.append(String.format("Took %d shot(s) trying to break through the defense. ", shots.size()));
            } else if (!saves.isEmpty()) {
                sb.append(String.format("Stood strong in goal with %d key save(s). ", saves.size()));
            }

            if (!redCards.isEmpty()) {
                sb.append(String.format("Unfortunately received a red card at %s'. ", redCards.get(0).getDisplayClock()));
            } else if (!yellowCards.isEmpty()) {
                sb.append(String.format("Received a yellow card at %s'. ", yellowCards.get(0).getDisplayClock()));
            }

            if (goals.isEmpty() && shots.isEmpty() && saves.isEmpty() && yellowCards.isEmpty() && redCards.isEmpty()) {
                sb.append(String.format("Participated in %d recorded match event(s), maintaining team structure and work rate.", playerEvents.size()));
            }

            response.setAnswer(sb.toString().trim());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Player Performance Analysis: %s (%s). ", playerDescriptor, teamName));
            sb.append(String.format("Total match involvements: %d. ", playerEvents.size()));
            sb.append(String.format("Goals scored: %d", goals.size()));
            if (!goals.isEmpty()) {
                sb.append(" (").append(goals.stream().map(e -> e.getDisplayClock() + "' [" + e.getEventId() + "]").collect(Collectors.joining(", "))).append(")");
            }
            sb.append(String.format(", Shot attempts: %d, Fouls committed: %d, Saves: %d. ", shots.size(), fouls.size(), saves.size()));
            sb.append(String.format("Disciplinary: %d yellow card(s), %d red card(s).", yellowCards.size(), redCards.size()));
            response.setAnswer(sb.toString().trim());
        }

        return response;
    }

    private QuestionResponse answerTurningPoints(Match match, List<MatchEvent> events, List<TurningPoint> turningPoints,
                                                  MatchStatistics stats, String mode, String qLower) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        if (turningPoints.isEmpty()) {
            response.setAnswer("No high-leverage turning points were detected for this match. The game followed an even trajectory without lead changes or red cards.");
            response.setEvidenceEventIds(List.of());
            return response;
        }

        List<String> evidence = turningPoints.stream()
                .flatMap(tp -> tp.getEventIds().stream())
                .distinct()
                .toList();
        response.setEvidenceEventIds(evidence);

        TurningPoint primary = turningPoints.get(0);

        if ("FAN".equalsIgnoreCase(mode)) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("The biggest turning point was at %s' — %s! ", primary.getMinute(), primary.getExplanation()));
            sb.append(String.format("Why it mattered: %s. ", primary.getRankingRationale()));

            if (turningPoints.size() > 1) {
                TurningPoint secondary = turningPoints.get(1);
                sb.append(String.format("Another critical shift happened at %s' (%s), which completely altered the match momentum.",
                        secondary.getMinute(), secondary.getExplanation()));
            }
            response.setAnswer(sb.toString().trim());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Identified %d key turning points that dictated match outcome: ", turningPoints.size()));
            int rank = 1;
            for (TurningPoint tp : turningPoints.stream().limit(3).toList()) {
                sb.append(String.format("#%d [Minute %d', Score %s, Impact %.1f/10]: %s. Tactical rationale: %s (Evidence: %s). ",
                        rank++, tp.getMinute(), tp.getScoreChange() != null ? tp.getScoreChange() : "N/A", tp.getScore(),
                        tp.getExplanation(), tp.getRankingRationale(), String.join(", ", tp.getEventIds())));
            }
            response.setAnswer(sb.toString().trim());
        }

        return response;
    }

    private QuestionResponse answerMatchStateAndHalves(Match match, List<MatchEvent> events,
                                                        MatchPeriodComparison comparison, String mode, String qLower) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        var first = comparison.getFirstHalf();
        var second = comparison.getSecondHalf();

        List<MatchEvent> goalEvents = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).toList();
        response.setEvidenceEventIds(goalEvents.stream().map(MatchEvent::getEventId).toList());

        if ("FAN".equalsIgnoreCase(mode)) {
            if (qLower.contains("second half")) {
                response.setAnswer(String.format(
                        "In the second half, %s scored %d and %s scored %d. %s racked up %d shots while %s had %d shots after the break, making it an action-packed conclusion!",
                        match.getHomeTeamName(), second.getHomeScore(),
                        match.getAwayTeamName(), second.getAwayScore(),
                        match.getHomeTeamName(), second.getHomeShots(),
                        match.getAwayTeamName(), second.getAwayShots()));
            } else if (qLower.contains("first half") || qLower.contains("halftime") || qLower.contains("half time")) {
                response.setAnswer(String.format(
                        "At half-time, the score stood at %s %d - %d %s. %s registered %d shots in the opening 45 minutes compared to %d for %s.",
                        match.getHomeTeamName(), first.getHomeScore(),
                        first.getAwayScore(), match.getAwayTeamName(),
                        match.getHomeTeamName(), first.getHomeShots(),
                        first.getAwayShots(), match.getAwayTeamName()));
            } else {
                response.setAnswer(String.format(
                        "Across halves: First half was %s %d - %d %s (Shots: %d vs %d). The second half produced %d - %d (Shots: %d vs %d), leading to the %d-%d final score.",
                        match.getHomeTeamName(), first.getHomeScore(), first.getAwayScore(), match.getAwayTeamName(),
                        first.getHomeShots(), first.getAwayShots(),
                        second.getHomeScore(), second.getAwayScore(),
                        second.getHomeShots(), second.getAwayShots(),
                        match.getHomeScore(), match.getAwayScore()));
            }
        } else {
            response.setAnswer(String.format(
                    "Match State Progression Breakdown: First Half ended %s %d - %d %s (Shots: %d [%d on target] vs %d [%d on target], Corners: %d vs %d, Fouls: %d vs %d). " +
                    "Second Half delta: %s +%d goals (%d shots, %d on target) vs %s +%d goals (%d shots, %d on target). " +
                    "Cumulative final score reconstructed: %d-%d.",
                    match.getHomeTeamName(), first.getHomeScore(), first.getAwayScore(), match.getAwayTeamName(),
                    first.getHomeShots(), first.getHomeShotsOnTarget(), first.getAwayShots(), first.getAwayShotsOnTarget(),
                    first.getHomeCorners(), first.getAwayCorners(), first.getHomeFouls(), first.getAwayFouls(),
                    match.getHomeTeamName(), second.getHomeScore(), second.getHomeShots(), second.getHomeShotsOnTarget(),
                    match.getAwayTeamName(), second.getAwayScore(), second.getAwayShots(), second.getAwayShotsOnTarget(),
                    match.getHomeScore(), match.getAwayScore()));
        }

        return response;
    }

    private QuestionResponse answerGoalsAndScorers(Match match, List<MatchEvent> events, String targetTeam,
                                                   String mode, String qLower) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        List<MatchEvent> validGoals = events.stream()
                .filter(e -> e.isGoal() && !e.isDisallowed())
                .sorted(Comparator.comparingInt(MatchEvent::getEventSequence))
                .toList();

        List<MatchEvent> disallowedGoals = events.stream()
                .filter(MatchEvent::isDisallowed)
                .toList();

        // Disallowed goal question
        if (qLower.contains("disallowed")) {
            if (disallowedGoals.isEmpty()) {
                response.setAnswer("No goals were disallowed in this match. All recorded goals stood after review.");
                response.setEvidenceEventIds(List.of());
            } else {
                String details = disallowedGoals.stream()
                        .map(e -> String.format("at %s' by %s (%s) [%s]",
                                e.getDisplayClock(),
                                e.getPlayerName() != null ? e.getPlayerName() : "Unknown",
                                e.getTeamName() != null ? e.getTeamName() : "team",
                                e.getEventId()))
                        .collect(Collectors.joining("; "));
                response.setAnswer(String.format("There were %d disallowed goal(s) in this match: %s. The score was not modified by these incidents.",
                        disallowedGoals.size(), details));
                response.setEvidenceEventIds(disallowedGoals.stream().map(MatchEvent::getEventId).toList());
            }
            return response;
        }

        // Own goal question
        if (qLower.contains("own goal")) {
            List<MatchEvent> ownGoals = events.stream().filter(MatchEvent::isOwnGoal).toList();
            if (ownGoals.isEmpty()) {
                response.setAnswer("There were no own goals scored in this match.");
                response.setEvidenceEventIds(List.of());
            } else {
                String details = ownGoals.stream()
                        .map(e -> String.format("%s' by %s (%s) [%s]", e.getDisplayClock(), e.getPlayerName(), e.getTeamName(), e.getEventId()))
                        .collect(Collectors.joining("; "));
                response.setAnswer(String.format("There were %d own goal(s) scored: %s.", ownGoals.size(), details));
                response.setEvidenceEventIds(ownGoals.stream().map(MatchEvent::getEventId).toList());
            }
            return response;
        }

        // Penalty question
        if (qLower.contains("penalty") || qLower.contains("penalties")) {
            List<MatchEvent> penalties = events.stream().filter(MatchEvent::isPenalty).toList();
            if (penalties.isEmpty()) {
                response.setAnswer("No penalties were awarded or taken in this match.");
                response.setEvidenceEventIds(List.of());
            } else {
                String details = penalties.stream()
                        .map(e -> String.format("%s' by %s (%s, Outcome: %s) [%s]",
                                e.getDisplayClock(), e.getPlayerName(), e.getTeamName(), e.getEventOutcome(), e.getEventId()))
                        .collect(Collectors.joining("; "));
                response.setAnswer(String.format("Penalty incidents recorded (%d): %s.", penalties.size(), details));
                response.setEvidenceEventIds(penalties.stream().map(MatchEvent::getEventId).toList());
            }
            return response;
        }

        // Opening goal question
        if (qLower.contains("first goal") || qLower.contains("open the scoring") || qLower.contains("opened the scoring")) {
            if (validGoals.isEmpty()) {
                response.setAnswer("No goals were scored in this match (0-0 draw).");
                response.setEvidenceEventIds(List.of());
            } else {
                MatchEvent first = validGoals.get(0);
                response.setEvidenceEventIds(List.of(first.getEventId()));
                if ("FAN".equalsIgnoreCase(mode)) {
                    response.setAnswer(String.format("The opening goal was scored by %s for %s in the %s' minute!",
                            first.getPlayerName(), first.getTeamName(), first.getDisplayClock()));
                } else {
                    response.setAnswer(String.format("First goal event (%s): %s scored at %s' for %s (%s), changing score to %d-%d.",
                            first.getEventId(), first.getPlayerName(), first.getDisplayClock(), first.getTeamName(),
                            first.getEventType(), first.getScoreHomeAfterEvent(), first.getScoreAwayAfterEvent()));
                }
            }
            return response;
        }

        // Team filter if applicable
        List<MatchEvent> filteredGoals = validGoals;
        if (targetTeam != null) {
            String finalTarget = targetTeam.toLowerCase(Locale.ROOT);
            filteredGoals = validGoals.stream()
                    .filter(e -> e.getTeamName() != null && e.getTeamName().toLowerCase(Locale.ROOT).contains(finalTarget))
                    .toList();
        }

        if (validGoals.isEmpty()) {
            response.setAnswer("There were no goals scored in this match; it ended in a 0-0 goalless draw.");
            response.setEvidenceEventIds(List.of());
            return response;
        }

        if (targetTeam != null && filteredGoals.isEmpty()) {
            response.setAnswer(String.format("%s did not score any goals in this match. Total goals in match: %d.", targetTeam, validGoals.size()));
            response.setEvidenceEventIds(validGoals.stream().map(MatchEvent::getEventId).toList());
            return response;
        }

        response.setEvidenceEventIds(filteredGoals.stream().map(MatchEvent::getEventId).toList());

        if ("FAN".equalsIgnoreCase(mode)) {
            String scorersSummary = filteredGoals.stream()
                    .map(e -> String.format("%s (%s', %s)", e.getPlayerName(), e.getDisplayClock(), e.getTeamName()))
                    .collect(Collectors.joining(", "));
            if (targetTeam != null) {
                response.setAnswer(String.format("%s scored %d goal(s): %s.", targetTeam, filteredGoals.size(), scorersSummary));
            } else {
                response.setAnswer(String.format("There were %d goals scored: %s. The final score was %s %d - %d %s.",
                        validGoals.size(), scorersSummary, match.getHomeTeamName(), match.getHomeScore(), match.getAwayScore(), match.getAwayTeamName()));
            }
        } else {
            String goalDetails = filteredGoals.stream()
                    .map(e -> String.format("[%s] %s' %s (%s) - Score after: %d-%d",
                            e.getEventId(), e.getDisplayClock(), e.getPlayerName(), e.getTeamName(),
                            e.getScoreHomeAfterEvent(), e.getScoreAwayAfterEvent()))
                    .collect(Collectors.joining("; "));
            response.setAnswer(String.format("Goal Registry (%d goals%s): %s. All verified against official event timeline.",
                    filteredGoals.size(), targetTeam != null ? " for " + targetTeam : "", goalDetails));
        }

        return response;
    }

    private QuestionResponse answerMatchResult(Match match, List<MatchEvent> events, String targetTeam,
                                                String mode, String qLower) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        List<String> goalEvs = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).map(MatchEvent::getEventId).toList();
        response.setEvidenceEventIds(goalEvs);

        boolean isDraw = match.getHomeScore() == match.getAwayScore();
        String winner = match.getHomeScore() > match.getAwayScore() ? match.getHomeTeamName() :
                (match.getAwayScore() > match.getHomeScore() ? match.getAwayTeamName() : null);
        String loser = winner != null ? (winner.equals(match.getHomeTeamName()) ? match.getAwayTeamName() : match.getHomeTeamName()) : null;

        if (qLower.contains("draw") || qLower.contains("tie")) {
            if (isDraw) {
                response.setAnswer(String.format("Yes, the match ended in a %d-%d draw between %s and %s.",
                        match.getHomeScore(), match.getAwayScore(), match.getHomeTeamName(), match.getAwayTeamName()));
            } else {
                response.setAnswer(String.format("No, the match did not end in a draw. %s won %d-%d against %s.",
                        winner, Math.max(match.getHomeScore(), match.getAwayScore()),
                        Math.min(match.getHomeScore(), match.getAwayScore()), loser));
            }
            return response;
        }

        // Direct team win inquiry e.g. "Did Northbridge win?"
        if (targetTeam != null && (qLower.contains("win") || qLower.contains("won") || qLower.contains("lose") || qLower.contains("lost"))) {
            if (isDraw) {
                response.setAnswer(String.format("Neither team won; %s drew %d-%d with %s.",
                        match.getHomeTeamName(), match.getHomeScore(), match.getAwayScore(), match.getAwayTeamName()));
            } else if (targetTeam.equalsIgnoreCase(winner)) {
                response.setAnswer(String.format("Yes, %s won the match %d-%d against %s!",
                        targetTeam, Math.max(match.getHomeScore(), match.getAwayScore()),
                        Math.min(match.getHomeScore(), match.getAwayScore()), loser));
            } else {
                response.setAnswer(String.format("No, %s were defeated %d-%d by %s.",
                        targetTeam, Math.max(match.getHomeScore(), match.getAwayScore()),
                        Math.min(match.getHomeScore(), match.getAwayScore()), winner));
            }
            return response;
        }

        if ("FAN".equalsIgnoreCase(mode)) {
            if (isDraw) {
                response.setAnswer(String.format("The match ended in a %d-%d draw between %s and %s after a tightly contested 90 minutes.",
                        match.getHomeScore(), match.getAwayScore(), match.getHomeTeamName(), match.getAwayTeamName()));
            } else {
                response.setAnswer(String.format("%s took the victory with a %d-%d win against %s!",
                        winner, Math.max(match.getHomeScore(), match.getAwayScore()),
                        Math.min(match.getHomeScore(), match.getAwayScore()), loser));
            }
        } else {
            response.setAnswer(String.format("Full-Time Result: %s %d - %d %s (%s). Outcome: %s. Score sequence verified by %d goal events in match log.",
                    match.getHomeTeamName(), match.getHomeScore(), match.getAwayScore(), match.getAwayTeamName(),
                    match.getCompetitionName(),
                    isDraw ? "Draw (1 point each)" : winner + " victory (+3 points)",
                    goalEvs.size()));
        }

        return response;
    }

    private QuestionResponse answerCardsAndFouls(Match match, List<MatchEvent> events, MatchStatistics stats,
                                                 String targetTeam, String mode, String qLower) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        List<MatchEvent> yellowCards = events.stream().filter(MatchEvent::isYellowCard).toList();
        List<MatchEvent> redCards = events.stream().filter(MatchEvent::isRedCard).toList();
        List<MatchEvent> fouls = events.stream().filter(e -> "FOUL".equals(e.getEventType())).toList();

        // Red card specific question
        if (qLower.contains("red") || qLower.contains("sent off") || qLower.contains("send off") || qLower.contains("ejected")) {
            if (redCards.isEmpty()) {
                response.setAnswer("No red cards were shown in this match. Both teams maintained 11 players throughout regulation.");
                response.setEvidenceEventIds(List.of());
            } else {
                String cardDetails = redCards.stream()
                        .map(e -> String.format("%s' %s (%s) [%s]", e.getDisplayClock(), e.getPlayerName(), e.getTeamName(), e.getEventId()))
                        .collect(Collectors.joining("; "));
                response.setAnswer(String.format("Red card shown: %s. This reduced the team to 10 players, drastically impacting tactical stability.", cardDetails));
                response.setEvidenceEventIds(redCards.stream().map(MatchEvent::getEventId).toList());
            }
            return response;
        }

        // Yellow card specific question
        if (qLower.contains("yellow") || qLower.contains("booked") || qLower.contains("booking")) {
            if (yellowCards.isEmpty()) {
                response.setAnswer("No yellow cards were issued in this match.");
                response.setEvidenceEventIds(List.of());
            } else {
                String details = yellowCards.stream()
                        .map(e -> String.format("%s' %s (%s) [%s]", e.getDisplayClock(), e.getPlayerName(), e.getTeamName(), e.getEventId()))
                        .collect(Collectors.joining("; "));
                response.setAnswer(String.format("There were %d yellow card(s) shown: %s.", yellowCards.size(), details));
                response.setEvidenceEventIds(yellowCards.stream().map(MatchEvent::getEventId).toList());
            }
            return response;
        }

        // Foul specific question
        if (qLower.contains("foul")) {
            response.setEvidenceEventIds(fouls.stream().map(MatchEvent::getEventId).toList());
            if ("FAN".equalsIgnoreCase(mode)) {
                response.setAnswer(String.format("There were %d total fouls in the match. %s committed %d fouls and %s committed %d fouls.",
                        fouls.size(), match.getHomeTeamName(), stats.getHomeFouls(), match.getAwayTeamName(), stats.getAwayFouls()));
            } else {
                response.setAnswer(String.format("Foul Distribution: %d total fouls recorded. %s: %d fouls, %s: %d fouls. Backed by %d verified FOUL events in dataset.",
                        fouls.size(), match.getHomeTeamName(), stats.getHomeFouls(), match.getAwayTeamName(), stats.getAwayFouls(), fouls.size()));
            }
            return response;
        }

        // Combined disciplinary overview
        List<String> cardEvs = new ArrayList<>();
        yellowCards.forEach(e -> cardEvs.add(e.getEventId()));
        redCards.forEach(e -> cardEvs.add(e.getEventId()));
        response.setEvidenceEventIds(cardEvs);

        if ("FAN".equalsIgnoreCase(mode)) {
            response.setAnswer(String.format("Disciplinary summary: %d yellow card(s) and %d red card(s). Total fouls committed: %d (%s: %d, %s: %d).",
                    yellowCards.size(), redCards.size(), fouls.size(), match.getHomeTeamName(), stats.getHomeFouls(), match.getAwayTeamName(), stats.getAwayFouls()));
        } else {
            response.setAnswer(String.format("Match Disciplinary Report: %d total fouls (%s %d, %s %d). Bookings: %d yellow (%s %d, %s %d), %d red (%s %d, %s %d).",
                    fouls.size(), match.getHomeTeamName(), stats.getHomeFouls(), match.getAwayTeamName(), stats.getAwayFouls(),
                    yellowCards.size(), stats.getHomeYellowCards(), stats.getAwayYellowCards(),
                    redCards.size(), stats.getHomeRedCards(), stats.getAwayRedCards()));
        }

        return response;
    }

    private QuestionResponse answerShotsAndSaves(Match match, List<MatchEvent> events, MatchStatistics stats,
                                                 String targetTeam, String mode, String qLower) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        List<MatchEvent> shots = events.stream().filter(this::isShot).toList();
        List<MatchEvent> saves = events.stream().filter(e -> "SAVE".equals(e.getEventType())).toList();

        List<String> evidence = new ArrayList<>();
        shots.stream().limit(10).forEach(e -> evidence.add(e.getEventId()));
        saves.forEach(e -> evidence.add(e.getEventId()));
        response.setEvidenceEventIds(evidence);

        if ("FAN".equalsIgnoreCase(mode)) {
            response.setAnswer(String.format("%s took %d shots (%d on target) while %s had %d shots (%d on target). Goalkeepers produced %d total saves (%s: %d, %s: %d).",
                    match.getHomeTeamName(), stats.getHomeShots(), stats.getHomeShotsOnTarget(),
                    match.getAwayTeamName(), stats.getAwayShots(), stats.getAwayShotsOnTarget(),
                    stats.getHomeSaves() + stats.getAwaySaves(),
                    match.getHomeTeamName(), stats.getHomeSaves(),
                    match.getAwayTeamName(), stats.getAwaySaves()));
        } else {
            double homeAcc = stats.getHomeShots() > 0 ? (stats.getHomeShotsOnTarget() * 100.0 / stats.getHomeShots()) : 0.0;
            double awayAcc = stats.getAwayShots() > 0 ? (stats.getAwayShotsOnTarget() * 100.0 / stats.getAwayShots()) : 0.0;
            response.setAnswer(String.format("Shot Generation & Goalkeeping Analytics: %s: %d shots, %d on target (%.1f%% accuracy), %d saves recorded. " +
                    "%s: %d shots, %d on target (%.1f%% accuracy), %d saves recorded. Total shot attempts in dataset: %d.",
                    match.getHomeTeamName(), stats.getHomeShots(), stats.getHomeShotsOnTarget(), homeAcc, stats.getHomeSaves(),
                    match.getAwayTeamName(), stats.getAwayShots(), stats.getAwayShotsOnTarget(), awayAcc, stats.getAwaySaves(),
                    shots.size()));
        }

        return response;
    }

    private QuestionResponse answerCornersAndOffsides(Match match, List<MatchEvent> events, MatchStatistics stats,
                                                      String targetTeam, String mode, String qLower) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        List<MatchEvent> corners = events.stream().filter(e -> "CORNER".equals(e.getEventType())).toList();
        List<MatchEvent> offsides = events.stream().filter(e -> "OFFSIDE".equals(e.getEventType())).toList();

        List<String> evidence = new ArrayList<>();
        corners.stream().limit(8).forEach(e -> evidence.add(e.getEventId()));
        offsides.stream().limit(8).forEach(e -> evidence.add(e.getEventId()));
        response.setEvidenceEventIds(evidence);

        if ("FAN".equalsIgnoreCase(mode)) {
            response.setAnswer(String.format("There were %d corners (%s: %d, %s: %d) and %d offsides (%s: %d, %s: %d) recorded in this match.",
                    corners.size(), match.getHomeTeamName(), stats.getHomeCorners(), match.getAwayTeamName(), stats.getAwayCorners(),
                    offsides.size(), match.getHomeTeamName(), stats.getHomeOffsides(), match.getAwayTeamName(), stats.getAwayOffsides()));
        } else {
            response.setAnswer(String.format("Set Piece & Line Discipline: Corners: %s %d, %s %d (Total: %d). Offsides flagged: %s %d, %s %d (Total: %d).",
                    match.getHomeTeamName(), stats.getHomeCorners(), match.getAwayTeamName(), stats.getAwayCorners(), corners.size(),
                    match.getHomeTeamName(), stats.getHomeOffsides(), match.getAwayTeamName(), stats.getAwayOffsides(), offsides.size()));
        }

        return response;
    }

    private QuestionResponse answerSubstitutions(Match match, List<MatchEvent> events, String targetTeam, String mode) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        List<MatchEvent> subs = events.stream().filter(MatchEvent::isSubstitution).toList();
        response.setEvidenceEventIds(subs.stream().map(MatchEvent::getEventId).toList());

        if (subs.isEmpty()) {
            response.setAnswer("No substitution events were logged in the event timeline for this match.");
        } else {
            String subList = subs.stream()
                    .map(e -> String.format("%s' %s replaced %s (%s) [%s]",
                            e.getDisplayClock(),
                            e.getPlayerName() != null ? e.getPlayerName() : "Substitute",
                            e.getRelatedPlayerName() != null ? e.getRelatedPlayerName() : "Player",
                            e.getTeamName(), e.getEventId()))
                    .collect(Collectors.joining("; "));
            response.setAnswer(String.format("Substitutions recorded (%d): %s.", subs.size(), subList));
        }

        return response;
    }

    private QuestionResponse answerTeamPerformance(Match match, List<MatchEvent> events, MatchStatistics stats,
                                                   List<TurningPoint> turningPoints, String targetTeam, String mode) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        boolean isHome = targetTeam != null && targetTeam.equalsIgnoreCase(match.getHomeTeamName());
        String team = targetTeam != null ? targetTeam : match.getHomeTeamName();

        int goals = isHome ? stats.getHomeScore() : stats.getAwayScore();
        int shots = isHome ? stats.getHomeShots() : stats.getAwayShots();
        int shotsOnTarget = isHome ? stats.getHomeShotsOnTarget() : stats.getAwayShotsOnTarget();
        int corners = isHome ? stats.getHomeCorners() : stats.getAwayCorners();
        int fouls = isHome ? stats.getHomeFouls() : stats.getAwayFouls();
        int yellows = isHome ? stats.getHomeYellowCards() : stats.getAwayYellowCards();
        int reds = isHome ? stats.getHomeRedCards() : stats.getAwayRedCards();

        List<MatchEvent> teamEvents = events.stream()
                .filter(e -> team.equalsIgnoreCase(e.getTeamName()))
                .toList();

        List<String> evidence = teamEvents.stream()
                .filter(e -> e.isGoal() || e.isRedCard() || isShot(e))
                .limit(8)
                .map(MatchEvent::getEventId)
                .toList();
        response.setEvidenceEventIds(evidence);

        if ("FAN".equalsIgnoreCase(mode)) {
            response.setAnswer(String.format(
                    "%s scored %d goal(s) and produced %d shot(s) (%d on target). They earned %d corner(s), committed %d foul(s), and received %d card(s) (%d yellow, %d red).",
                    team, goals, shots, shotsOnTarget, corners, fouls, yellows + reds, yellows, reds));
        } else {
            double shotAcc = shots > 0 ? (shotsOnTarget * 100.0 / shots) : 0.0;
            response.setAnswer(String.format(
                    "%s Team Performance Profile: Goals: %d, Shot volume: %d with %d on frame (%.1f%% conversion accuracy), Set pieces: %d corners, Discipline: %d fouls, %d yellow(s), %d red(s). Data verified across %d team-attributed events.",
                    team, goals, shots, shotsOnTarget, shotAcc, corners, fouls, yellows, reds, teamEvents.size()));
        }

        return response;
    }

    private QuestionResponse answerGeneralSummary(Match match, List<MatchEvent> events, MatchStatistics stats,
                                                  List<TurningPoint> turningPoints, String mode) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);

        List<MatchEvent> goals = events.stream().filter(e -> e.isGoal() && !e.isDisallowed()).toList();
        response.setEvidenceEventIds(goals.stream().map(MatchEvent::getEventId).toList());

        String resultStr;
        if (match.getHomeScore() > match.getAwayScore()) {
            resultStr = String.format("%s defeated %s %d-%d", match.getHomeTeamName(), match.getAwayTeamName(), match.getHomeScore(), match.getAwayScore());
        } else if (match.getAwayScore() > match.getHomeScore()) {
            resultStr = String.format("%s defeated %s %d-%d", match.getAwayTeamName(), match.getHomeTeamName(), match.getAwayScore(), match.getHomeScore());
        } else {
            resultStr = String.format("%s and %s drew %d-%d", match.getHomeTeamName(), match.getAwayTeamName(), match.getHomeScore(), match.getAwayScore());
        }

        if ("FAN".equalsIgnoreCase(mode)) {
            response.setAnswer(String.format(
                    "In this %s match, %s! There were %d total shots (%d vs %d) and %d goals scored. Check the turning points and timeline to explore the key moments that shaped the game.",
                    match.getCompetitionName(), resultStr, stats.getHomeShots() + stats.getAwayShots(),
                    stats.getHomeShots(), stats.getAwayShots(), goals.size()));
        } else {
            response.setAnswer(String.format(
                    "Comprehensive Match Summary: %s. Statistical summary: Goals: %d, Shots: %d vs %d (On Target: %d vs %d), Corners: %d vs %d, Fouls: %d vs %d, Cards: %d vs %d. Total timeline events evaluated: %d.",
                    resultStr, goals.size(), stats.getHomeShots(), stats.getAwayShots(),
                    stats.getHomeShotsOnTarget(), stats.getAwayShotsOnTarget(),
                    stats.getHomeCorners(), stats.getAwayCorners(),
                    stats.getHomeFouls(), stats.getAwayFouls(),
                    stats.getHomeYellowCards() + stats.getHomeRedCards(), stats.getAwayYellowCards() + stats.getAwayRedCards(),
                    events.size()));
        }

        return response;
    }

    private QuestionResponse answerFallback(Match match, String preference) {
        QuestionResponse response = new QuestionResponse();
        response.setAiMode(aiMode);
        response.setEvidenceEventIds(List.of());

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("MatchMind AI specializes in match intelligence for %s vs %s. ", match.getHomeTeamName(), match.getAwayTeamName()));
        if (!preference.isBlank()) {
            sb.append(String.format("(Personalized around: %s). ", preference));
        }
        sb.append("You can ask about:\n")
          .append("• Match results & winner: \"Who won the match?\" or \"Did ").append(match.getHomeTeamName()).append(" win?\"\n")
          .append("• Goals & scorers: \"Who scored the goals?\" or \"When was the opening goal scored?\"\n")
          .append("• Turning points: \"What was the key turning point?\" or \"Why did momentum change?\"\n")
          .append("• Match state & halves: \"What happened in the second half?\" or \"What was the half-time score?\"\n")
          .append("• Team performance: \"How did ").append(match.getHomeTeamName()).append(" perform?\"\n")
          .append("• Player contributions: \"How did [player name] play?\" or \"Did [player name] score?\"\n")
          .append("• Discipline & cards: \"Were there any red cards?\" or \"How many fouls were committed?\"");

        response.setAnswer(sb.toString());
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
