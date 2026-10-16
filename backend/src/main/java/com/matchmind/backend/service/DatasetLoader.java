package com.matchmind.backend.service;

import com.matchmind.backend.model.Match;
import com.matchmind.backend.model.MatchEvent;
import com.matchmind.backend.model.Player;
import com.matchmind.backend.model.Team;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Service
public class DatasetLoader {
    private static final Logger logger = LoggerFactory.getLogger(DatasetLoader.class);

    @Value("${dataset.path:../dataset}")
    private String datasetPath;

    private final Map<String, Match> matches = new HashMap<>();
    private final Map<String, List<MatchEvent>> matchEvents = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<String, Player> players = new HashMap<>();

    @PostConstruct
    public void loadDataset() {
        try {
            logger.info("Loading dataset from: {}", datasetPath);
            loadTeams();
            loadPlayers();
            loadMatches();
            loadEvents();
            validateDataset();
            logger.info("Dataset loaded successfully: {} matches, {} events, {} teams, {} players",
                    matches.size(), matchEvents.values().stream().mapToInt(List::size).sum(),
                    teams.size(), players.size());
        } catch (Exception e) {
            logger.error("Failed to load dataset", e);
            throw new RuntimeException("Dataset loading failed", e);
        }
    }

    private void loadTeams() throws IOException {
        Path path = Paths.get(datasetPath, "teams.csv");
        try (FileReader reader = new FileReader(path.toFile());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                Team team = new Team();
                team.setTeamId(record.get("team_id"));
                team.setTeamName(record.get("team_name"));
                team.setShortName(record.get("short_name"));
                team.setHomeGround(record.get("home_ground"));
                team.setSyntheticSeedRank(Integer.parseInt(record.get("synthetic_seed_rank")));
                team.setSynthetic(Boolean.parseBoolean(record.get("is_synthetic")));
                team.setDatasetId(record.get("dataset_id"));
                if (teams.containsKey(team.getTeamId())) throw new RuntimeException("Duplicate team id: " + team.getTeamId());
                teams.put(team.getTeamId(), team);
            }
            logger.info("Loaded {} teams", teams.size());
        }
    }

    private void loadPlayers() throws IOException {
        Path path = Paths.get(datasetPath, "players.csv");
        try (FileReader reader = new FileReader(path.toFile());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                Player player = new Player();
                player.setPlayerId(record.get("player_id"));
                player.setPlayerName(record.get("player_name"));
                player.setTeamId(record.get("team_id"));
                player.setSquadNumber(Integer.parseInt(record.get("squad_number")));
                player.setPosition(record.get("position"));
                player.setSynthetic(Boolean.parseBoolean(record.get("is_synthetic")));
                player.setDatasetId(record.get("dataset_id"));
                if (players.containsKey(player.getPlayerId())) throw new RuntimeException("Duplicate player id: " + player.getPlayerId());
                players.put(player.getPlayerId(), player);
            }
            logger.info("Loaded {} players", players.size());
        }
    }

    private void loadMatches() throws IOException {
        Path path = Paths.get(datasetPath, "matches.csv");
        try (FileReader reader = new FileReader(path.toFile());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                Match match = new Match();
                match.setMatchId(record.get("match_id"));
                match.setDatasetId(record.get("dataset_id"));
                match.setSynthetic(Boolean.parseBoolean(record.get("is_synthetic")));
                match.setHomeTeamId(record.get("home_team_id"));
                match.setAwayTeamId(record.get("away_team_id"));
                match.setHomeTeamName(record.get("home_team_name"));
                match.setAwayTeamName(record.get("away_team_name"));
                match.setCompetitionName(record.get("competition_name"));
                match.setSeasonLabel(record.get("season_label"));
                match.setMatchDate(LocalDate.parse(record.get("match_date")));
                match.setScheduledDurationMinutes(Integer.parseInt(record.get("scheduled_duration_minutes")));
                match.setStatus(record.get("status"));
                match.setHomeScore(Integer.parseInt(record.get("home_score")));
                match.setAwayScore(Integer.parseInt(record.get("away_score")));
                match.setScenarioType(record.get("scenario_type"));
                match.setDataVersion(record.get("data_version"));
                match.setFirstHalfStoppageMinutes(Integer.parseInt(record.get("first_half_stoppage_minutes")));
                match.setSecondHalfStoppageMinutes(Integer.parseInt(record.get("second_half_stoppage_minutes")));
                if (matches.containsKey(match.getMatchId())) throw new RuntimeException("Duplicate match id: " + match.getMatchId());
                matches.put(match.getMatchId(), match);
                matchEvents.put(match.getMatchId(), new ArrayList<>());
            }
            logger.info("Loaded {} matches", matches.size());
        }
    }

    private void loadEvents() throws IOException {
        Path path = Paths.get(datasetPath, "events.csv");
        try (FileReader reader = new FileReader(path.toFile());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                MatchEvent event = new MatchEvent();
                event.setEventId(record.get("event_id"));
                event.setDatasetId(record.get("dataset_id"));
                event.setMatchId(record.get("match_id"));
                event.setEventSequence(Integer.parseInt(record.get("event_sequence")));
                event.setMinute(Integer.parseInt(record.get("minute")));
                event.setSecond(Integer.parseInt(record.get("second")));
                event.setDisplayClock(record.get("display_clock"));
                event.setPeriod(record.get("period"));
                event.setTimestampSeconds(Integer.parseInt(record.get("timestamp_seconds")));
                event.setEventType(record.get("event_type"));
                event.setEventOutcome(record.get("event_outcome"));
                event.setTeamId(getOptionalField(record, "team_id"));
                event.setTeamName(getOptionalField(record, "team_name"));
                event.setPlayerId(getOptionalField(record, "player_id"));
                event.setPlayerName(getOptionalField(record, "player_name"));
                event.setRelatedPlayerId(getOptionalField(record, "related_player_id"));
                event.setRelatedPlayerName(getOptionalField(record, "related_player_name"));
                event.setCreditedTeamId(getOptionalField(record, "credited_team_id"));
                event.setDescription(record.get("description"));
                event.setGoal(Boolean.parseBoolean(record.get("is_goal")));
                event.setOwnGoal(Boolean.parseBoolean(record.get("is_own_goal")));
                event.setPenalty(Boolean.parseBoolean(record.get("is_penalty")));
                event.setRedCard(Boolean.parseBoolean(record.get("is_red_card")));
                event.setYellowCard(Boolean.parseBoolean(record.get("is_yellow_card")));
                event.setSubstitution(Boolean.parseBoolean(record.get("is_substitution")));
                event.setDisallowed(Boolean.parseBoolean(record.get("is_disallowed")));
                event.setScoreHomeAfterEvent(Integer.parseInt(record.get("score_home_after_event")));
                event.setScoreAwayAfterEvent(Integer.parseInt(record.get("score_away_after_event")));
                event.setSourceType(record.get("source_type"));
                event.setSyntheticEventGroupId(getOptionalField(record, "synthetic_event_group_id"));
                event.setScenarioTag(record.get("scenario_tag"));
                event.setDataQualityFlag(record.get("data_quality_flag"));

                if (matchEvents.containsKey(event.getMatchId())) {
                    matchEvents.get(event.getMatchId()).add(event);
                }
            }
            logger.info("Loaded {} events across {} matches",
                    matchEvents.values().stream().mapToInt(List::size).sum(),
                    matchEvents.size());
        }
    }

    private String getOptionalField(CSVRecord record, String fieldName) {
        return record.isMapped(fieldName) && !record.get(fieldName).isEmpty()
                ? record.get(fieldName) : null;
    }

    private void validateDataset() {
        Set<String> eventIds = new HashSet<>();
        // Validate team references
        for (Match match : matches.values()) {
            if (match.getHomeTeamId().equals(match.getAwayTeamId())) {
                throw new RuntimeException("Match has identical home and away teams: " + match.getMatchId());
            }
            if (!teams.containsKey(match.getHomeTeamId())) {
                throw new RuntimeException("Invalid home team reference: " + match.getHomeTeamId());
            }
            if (!teams.containsKey(match.getAwayTeamId())) {
                throw new RuntimeException("Invalid away team reference: " + match.getAwayTeamId());
            }
        }

        // Validate event references
        for (Map.Entry<String, List<MatchEvent>> entry : matchEvents.entrySet()) {
            String matchId = entry.getKey();
            if (!matches.containsKey(matchId)) {
                throw new RuntimeException("Event references non-existent match: " + matchId);
            }
            Match match = matches.get(matchId);
            int expectedSequence = 1;
            int previousTimestamp = -1;
            int homeScore = 0;
            int awayScore = 0;
            for (MatchEvent event : entry.getValue()) {
                if (!eventIds.add(event.getEventId())) throw new RuntimeException("Duplicate event id: " + event.getEventId());
                if (event.getEventSequence() != expectedSequence++) throw new RuntimeException("Non-contiguous event sequence for " + matchId);
                if (event.getTimestampSeconds() < previousTimestamp || event.getTimestampSeconds() < 0
                        || event.getMinute() != event.getTimestampSeconds() / 60
                        || event.getSecond() != event.getTimestampSeconds() % 60) {
                    throw new RuntimeException("Invalid chronology for event: " + event.getEventId());
                }
                previousTimestamp = event.getTimestampSeconds();
                if (event.getTeamId() != null && !teams.containsKey(event.getTeamId())) {
                    throw new RuntimeException("Event references non-existent team: " + event.getTeamId());
                }
                if (event.getPlayerId() != null && !players.containsKey(event.getPlayerId())) {
                    throw new RuntimeException("Event references non-existent player: " + event.getPlayerId());
                }
                if (event.getCreditedTeamId() != null && !teams.containsKey(event.getCreditedTeamId())) {
                    throw new RuntimeException("Event references non-existent credited team: " + event.getCreditedTeamId());
                }
                if (event.getPlayerId() != null && event.getTeamId() != null
                        && !event.getTeamId().equals(players.get(event.getPlayerId()).getTeamId())) {
                    throw new RuntimeException("Player/team attribution mismatch: " + event.getEventId());
                }
                boolean validGoalType = Set.of("GOAL", "OWN_GOAL", "PENALTY_SCORED").contains(event.getEventType());
                if (event.isGoal() != validGoalType || event.isDisallowed() != "GOAL_DISALLOWED".equals(event.getEventType())
                        || event.isOwnGoal() != "OWN_GOAL".equals(event.getEventType())) {
                    throw new RuntimeException("Inconsistent scoring flags: " + event.getEventId());
                }
                if (event.isOwnGoal() && (event.getCreditedTeamId() == null || event.getCreditedTeamId().equals(event.getTeamId()))) {
                    throw new RuntimeException("Own goal is missing opposing credited team: " + event.getEventId());
                }
                int expectedHome = homeScore;
                int expectedAway = awayScore;
                if (validGoalType && !event.isDisallowed()) {
                    String creditedTeam = event.isOwnGoal() ? event.getCreditedTeamId() : event.getTeamId();
                    if (match.getHomeTeamId().equals(creditedTeam)) expectedHome++;
                    else if (match.getAwayTeamId().equals(creditedTeam)) expectedAway++;
                    else throw new RuntimeException("Scoring event has no valid credited team: " + event.getEventId());
                }
                if (event.getScoreHomeAfterEvent() != expectedHome || event.getScoreAwayAfterEvent() != expectedAway) {
                    throw new RuntimeException("Score snapshot mismatch: " + event.getEventId());
                }
                homeScore = expectedHome;
                awayScore = expectedAway;
            }
            if (homeScore != match.getHomeScore() || awayScore != match.getAwayScore()) {
                throw new RuntimeException("Final score does not reconstruct for match: " + matchId);
            }
        }

        logger.info("Dataset validation passed");
    }

    // Public accessors
    public Collection<Match> getAllMatches() {
        return matches.values();
    }

    public Match getMatch(String matchId) {
        return matches.get(matchId);
    }

    public List<MatchEvent> getMatchEvents(String matchId) {
        return matchEvents.getOrDefault(matchId, Collections.emptyList());
    }

    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    public Collection<Player> getAllPlayers() {
        return players.values();
    }

    public Team getTeam(String teamId) {
        return teams.get(teamId);
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    /** Registers an isolated uploaded match without mutating the bundled dataset files. */
    public synchronized void registerUploadedMatch(Match match, List<MatchEvent> events) {
        if (matches.containsKey(match.getMatchId())) {
            throw new IllegalArgumentException("Match id already exists: " + match.getMatchId());
        }
        matches.put(match.getMatchId(), match);
        matchEvents.put(match.getMatchId(), new ArrayList<>(events));
    }
}
