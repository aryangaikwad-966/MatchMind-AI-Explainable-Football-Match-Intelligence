package com.matchmind.backend.controller;

import com.matchmind.backend.model.Match;
import com.matchmind.backend.model.MatchEvent;
import com.matchmind.backend.service.DatasetLoader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class UploadController {
    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> EVENT_TYPES = Set.of("KICK_OFF", "HALF_TIME", "SECOND_HALF_START", "FULL_TIME", "PASS", "SHOT", "SHOT_ON_TARGET", "SHOT_OFF_TARGET", "GOAL", "GOAL_DISALLOWED", "OWN_GOAL", "PENALTY_AWARDED", "PENALTY_SCORED", "PENALTY_MISSED", "SAVE", "CORNER", "FOUL", "OFFSIDE", "RED_CARD", "YELLOW_CARD", "SUBSTITUTION");
    private static final Set<String> SCORING_TYPES = Set.of("GOAL", "OWN_GOAL", "PENALTY_SCORED");

    private final DatasetLoader datasetLoader;

    public UploadController(DatasetLoader datasetLoader) { this.datasetLoader = datasetLoader; }

    @PostMapping("/matches/upload")
    public ResponseEntity<Map<String, Object>> uploadMatch(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return bad("File is empty", List.of());
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) return bad("File must have a .csv extension", List.of());
        if (file.getSize() > MAX_BYTES) return bad("File size exceeds 10MB limit", List.of());
        List<String> errors = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build().parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            Set<String> headers = new LinkedHashSet<>(parser.getHeaderNames());
            Set<String> required = Set.of("event_id", "match_id", "event_sequence", "minute", "second", "timestamp_seconds", "event_type", "description", "is_goal", "is_disallowed", "score_home_after_event", "score_away_after_event", "home_team_id", "away_team_id", "home_team_name", "away_team_name");
            Set<String> missing = new LinkedHashSet<>(required); missing.removeAll(headers);
            if (!missing.isEmpty()) return bad("CSV lacks required match metadata or event fields", List.of("Missing required headers: " + missing));

            List<MatchEvent> events = new ArrayList<>(); Set<String> ids = new LinkedHashSet<>();
            String matchId = null, homeId = null, awayId = null, homeName = null, awayName = null;
            int row = 1, expectedSequence = 1, previousTimestamp = -1, homeScore = 0, awayScore = 0;
            for (CSVRecord record : parser) {
                row++;
                try {
                    String currentMatchId = required(record, "match_id");
                    if (matchId == null) { matchId = currentMatchId; homeId = required(record, "home_team_id"); awayId = required(record, "away_team_id"); homeName = required(record, "home_team_name"); awayName = required(record, "away_team_name"); }
                    if (!matchId.equals(currentMatchId)) errors.add("Row " + row + ": match_id must be constant");
                    if (homeId.equals(awayId)) errors.add("Row " + row + ": home and away teams must differ");
                    String eventId = required(record, "event_id"); if (!ids.add(eventId)) errors.add("Row " + row + ": duplicate event_id " + eventId);
                    int sequence = integer(record, "event_sequence"), minute = integer(record, "minute"), second = integer(record, "second"), timestamp = integer(record, "timestamp_seconds");
                    if (sequence != expectedSequence++) errors.add("Row " + row + ": event_sequence must start at 1 and be contiguous");
                    if (timestamp < 0 || timestamp < previousTimestamp || minute < 0 || second < 0 || second > 59 || minute != timestamp / 60 || second != timestamp % 60) errors.add("Row " + row + ": minute, second and timestamp_seconds are inconsistent or out of order");
                    previousTimestamp = timestamp;
                    String eventType = required(record, "event_type"); if (!EVENT_TYPES.contains(eventType)) errors.add("Row " + row + ": unsupported event_type " + eventType);
                    boolean goal = bool(record, "is_goal"), disallowed = bool(record, "is_disallowed"); boolean expectedGoal = SCORING_TYPES.contains(eventType);
                    if (goal != expectedGoal) errors.add("Row " + row + ": is_goal does not match event_type");
                    if (disallowed != "GOAL_DISALLOWED".equals(eventType)) errors.add("Row " + row + ": is_disallowed does not match event_type");
                    String teamId = optional(record, "team_id"), creditedTeamId = optional(record, "credited_team_id");
                    if (teamId != null && !teamId.equals(homeId) && !teamId.equals(awayId)) errors.add("Row " + row + ": team_id is not one of the match teams");
                    if (expectedGoal && teamId == null) errors.add("Row " + row + ": scoring event requires team_id");
                    if ("OWN_GOAL".equals(eventType) && (creditedTeamId == null || creditedTeamId.equals(teamId))) errors.add("Row " + row + ": own goal requires opposing credited_team_id");
                    if (creditedTeamId != null && !creditedTeamId.equals(homeId) && !creditedTeamId.equals(awayId)) errors.add("Row " + row + ": credited_team_id is not one of the match teams");
                    int nextHome = integer(record, "score_home_after_event"), nextAway = integer(record, "score_away_after_event");
                    if (nextHome < homeScore || nextAway < awayScore) errors.add("Row " + row + ": score snapshot moves backwards");
                    String scoringTeam = "OWN_GOAL".equals(eventType) ? creditedTeamId : teamId;
                    if (expectedGoal && !disallowed) {
                        int expectedHome = homeScore + (homeId.equals(scoringTeam) ? 1 : 0), expectedAway = awayScore + (awayId.equals(scoringTeam) ? 1 : 0);
                        if (nextHome != expectedHome || nextAway != expectedAway) errors.add("Row " + row + ": score snapshot does not match the credited scoring team");
                    } else if (nextHome != homeScore || nextAway != awayScore) errors.add("Row " + row + ": non-scoring or disallowed event changes the score");
                    events.add(toEvent(record, matchId)); homeScore = nextHome; awayScore = nextAway;
                } catch (Exception ex) { errors.add("Row " + row + ": " + ex.getMessage()); }
            }
            if (events.isEmpty()) errors.add("CSV contains no event rows");
            if (matchId == null || homeId == null || awayId == null || homeId.equals(awayId)) errors.add("Home and away teams must be present and different");
            if (matchId != null && datasetLoader.getMatch(matchId) != null) errors.add("match_id already exists; use a unique uploaded match id");
            if (!errors.isEmpty()) return bad("Upload rejected; no match was imported", errors);
            Match match = new Match(matchId, homeId, awayId, homeName, awayName); match.setDatasetId("uploaded"); match.setSynthetic(true); match.setCompetitionName("Uploaded Synthetic Match"); match.setSeasonLabel("UPLOADED"); match.setMatchDate(LocalDate.now()); match.setScheduledDurationMinutes(90); match.setStatus("COMPLETED"); match.setHomeScore(homeScore); match.setAwayScore(awayScore); match.setDataVersion("upload-1");
            datasetLoader.registerUploadedMatch(match, events);
            return ResponseEntity.ok(Map.of("success", true, "matchId", matchId, "records", events.size(), "analysisAvailable", true, "message", "Synthetic match imported and available through the standard match APIs."));
        } catch (Exception ex) { return bad("Failed to parse CSV", List.of("Parser error: " + ex.getMessage())); }
    }

    private MatchEvent toEvent(CSVRecord record, String matchId) {
        MatchEvent event = new MatchEvent(); event.setEventId(required(record, "event_id")); event.setMatchId(matchId); event.setEventSequence(integer(record, "event_sequence")); event.setMinute(integer(record, "minute")); event.setSecond(integer(record, "second")); event.setTimestampSeconds(integer(record, "timestamp_seconds")); event.setEventType(required(record, "event_type")); event.setDescription(required(record, "description")); event.setGoal(bool(record, "is_goal")); event.setDisallowed(bool(record, "is_disallowed")); event.setScoreHomeAfterEvent(integer(record, "score_home_after_event")); event.setScoreAwayAfterEvent(integer(record, "score_away_after_event")); event.setTeamId(optional(record, "team_id")); event.setTeamName(optional(record, "team_name")); event.setPlayerId(optional(record, "player_id")); event.setPlayerName(optional(record, "player_name")); event.setCreditedTeamId(optional(record, "credited_team_id")); event.setPeriod(event.getTimestampSeconds() < 2700 ? "FIRST_HALF" : "SECOND_HALF"); event.setDisplayClock(event.getMinute() + "'"); event.setDatasetId("uploaded"); event.setSourceType("UPLOADED"); event.setDataQualityFlag("VALIDATED"); event.setEventOutcome(optional(record, "event_outcome")); event.setOwnGoal("OWN_GOAL".equals(event.getEventType())); event.setPenalty(event.getEventType().startsWith("PENALTY")); event.setRedCard("RED_CARD".equals(event.getEventType())); event.setYellowCard("YELLOW_CARD".equals(event.getEventType())); event.setSubstitution("SUBSTITUTION".equals(event.getEventType())); return event;
    }
    private static String required(CSVRecord record, String key) { String value = record.get(key); if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required"); return value; }
    private static String optional(CSVRecord record, String key) { return record.isMapped(key) && !record.get(key).isBlank() ? record.get(key) : null; }
    private static int integer(CSVRecord record, String key) { try { return Integer.parseInt(required(record, key)); } catch (NumberFormatException ex) { throw new IllegalArgumentException(key + " must be an integer"); } }
    private static boolean bool(CSVRecord record, String key) { String value = required(record, key); if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) throw new IllegalArgumentException(key + " must be true or false"); return Boolean.parseBoolean(value); }
    private static ResponseEntity<Map<String, Object>> bad(String error, List<String> errors) { return ResponseEntity.badRequest().body(Map.of("success", false, "error", error, "errors", errors)); }
}
