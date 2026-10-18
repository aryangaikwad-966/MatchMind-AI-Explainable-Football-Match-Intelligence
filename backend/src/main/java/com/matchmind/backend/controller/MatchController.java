package com.matchmind.backend.controller;

import com.matchmind.backend.ai.AIService;
import com.matchmind.backend.analysis.MatchAnalysisService;
import com.matchmind.backend.dto.*;
import com.matchmind.backend.model.Match;
import com.matchmind.backend.model.MatchEvent;
import com.matchmind.backend.model.MatchStatistics;
import com.matchmind.backend.service.DatasetLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = {"${cors.allowed-origins}", "https://*.vercel.app", "https://*.onrender.com"})
public class MatchController {

    private final DatasetLoader datasetLoader;
    private final MatchAnalysisService analysisService;
    private final AIService aiService;

    public MatchController(DatasetLoader datasetLoader,
                          MatchAnalysisService analysisService,
                          AIService aiService) {
        this.datasetLoader = datasetLoader;
        this.analysisService = analysisService;
        this.aiService = aiService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "aiMode", aiService.getAiMode(),
                "providerStatus", aiService.getProviderStatus(),
                "liveRequestVerified", aiService.hasVerifiedLiveRequest(),
                "matchesLoaded", datasetLoader.getAllMatches().size()
        ));
    }

    @GetMapping("/matches")
    public ResponseEntity<Collection<Match>> getAllMatches() {
        return ResponseEntity.ok(datasetLoader.getAllMatches());
    }

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<Match> getMatch(@PathVariable String matchId) {
        Match match = datasetLoader.getMatch(matchId);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(match);
    }

    @GetMapping("/matches/{matchId}/events")
    public ResponseEntity<List<MatchEvent>> getMatchEvents(
            @PathVariable String matchId,
            @RequestParam(required = false) String teamId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String period) {

        List<MatchEvent> events = datasetLoader.getMatchEvents(matchId);

        if (events.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Apply filters
        if (teamId != null) {
            events = events.stream()
                    .filter(e -> teamId.equals(e.getTeamId()))
                    .toList();
        }
        if (eventType != null) {
            events = events.stream()
                    .filter(e -> eventType.equals(e.getEventType()))
                    .toList();
        }
        if (period != null) {
            events = events.stream()
                    .filter(e -> period.equals(e.getPeriod()))
                    .toList();
        }

        return ResponseEntity.ok(events);
    }

    @GetMapping("/matches/{matchId}/statistics")
    public ResponseEntity<MatchStatistics> getMatchStatistics(@PathVariable String matchId) {
        try {
            MatchStatistics stats = analysisService.calculateStatistics(matchId);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/matches/{matchId}/turning-points")
    public ResponseEntity<List<TurningPoint>> getTurningPoints(@PathVariable String matchId) {
        try {
            List<TurningPoint> turningPoints = analysisService.calculateTurningPoints(matchId);
            return ResponseEntity.ok(turningPoints);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/matches/{matchId}/evidence/{evidenceId}")
    public ResponseEntity<List<MatchEvent>> getEvidence(
            @PathVariable String matchId,
            @PathVariable String evidenceId) {

        // Extract event IDs from the turning point ID
        // Format: TP-{eventId} or extract from database in a real implementation
        String eventId = evidenceId.replace("TP-", "");

        List<MatchEvent> events = datasetLoader.getMatchEvents(matchId);
        List<MatchEvent> evidenceEvents = events.stream()
                .filter(e -> e.getEventId().equals(eventId))
                .toList();

        if (evidenceEvents.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(evidenceEvents);
    }

    @GetMapping("/matches/{matchId}/recap")
    public ResponseEntity<MatchRecap> getMatchRecap(
            @PathVariable String matchId,
            @RequestParam(defaultValue = "FAN") String mode) {

        try {
            MatchRecap recap = aiService.generateMatchRecap(matchId, mode);
            return ResponseEntity.ok(recap);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/matches/{matchId}/questions")
    public ResponseEntity<QuestionResponse> askQuestion(
            @PathVariable String matchId,
            @RequestBody QuestionRequest request) {

        try {
            QuestionResponse response = aiService.answerQuestion(matchId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Match not found")) return ResponseEntity.notFound().build();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/matches/{matchId}/comparison")
    public ResponseEntity<MatchPeriodComparison> getMatchComparison(@PathVariable String matchId) {
        try {
            MatchPeriodComparison comparison = analysisService.comparePeriods(matchId);
            return ResponseEntity.ok(comparison);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/matches/{matchId}/replay")
    public ResponseEntity<ReplayState> replay(@PathVariable String matchId, @RequestParam(defaultValue = "0") int sequence) {
        Match match = datasetLoader.getMatch(matchId); if (match == null) return ResponseEntity.notFound().build();
        List<MatchEvent> all = datasetLoader.getMatchEvents(matchId); int max = Math.max(0, Math.min(sequence, all.size()));
        List<MatchEvent> visible = all.subList(0, max); ReplayState state = new ReplayState(); state.setMatchId(matchId); state.setEventSequence(max); state.setEvents(visible); state.setLabel("SYNTHETIC REPLAY");
        if (!visible.isEmpty()) { MatchEvent last=visible.get(visible.size()-1); state.setSimulatedMinute(last.getMinute()); state.setHomeScore(last.getScoreHomeAfterEvent()); state.setAwayScore(last.getScoreAwayAfterEvent()); }
        else { state.setSimulatedMinute(0); state.setHomeScore(0); state.setAwayScore(0); }
        Set<String> ids=visible.stream().map(MatchEvent::getEventId).collect(java.util.stream.Collectors.toSet()); state.setInsights(analysisService.calculateTurningPoints(matchId).stream().filter(tp -> tp.getEventIds().stream().allMatch(ids::contains)).toList()); return ResponseEntity.ok(state);
    }
}
