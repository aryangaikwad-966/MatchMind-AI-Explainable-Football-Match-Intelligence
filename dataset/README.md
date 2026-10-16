# MatchMind AI - Synthetic Football Dataset

**SYNTHETIC DATA.** Dataset `matchmind-synthetic-v1` v1.0.0. All clubs, players, matches and events are invented. No real clubs, crests, players or results are used, and nothing here is affiliated with any real league.

- Matches: 12  |  Events: 1262  |  Teams: 10  |  Players: 180
- Seed: `20261015` (configurable)

## Regenerate and verify

```bash
python3 -m tools.synthetic_data.generate                 # writes data/synthetic/, exits nonzero on critical failure
python3 -m tools.synthetic_data.generate --seed 123 --out /tmp/other
cd ../backend && mvn clean test                         # loads and validates the canonical dataset
```

The original generator package is not included in this application repository. The Java `DatasetLoader` is the runtime validator and checks references, chronology, score reconstruction, scoring flags, own-goal attribution, and final scores during startup and tests.

Regeneration is deterministic: the same seed and configuration produce byte-identical files. `generated_at` is a fixed configuration value by default; pass `--generated-at now` for wall-clock time.

## Files

| File | Content |
|---|---|
| `matches.csv` | One row per match; scores derived from valid scoring events |
| `events.csv` | Canonical event log (all analysis must use this) |
| `teams.csv`, `players.csv` | Fictional clubs and 18-player squads (1-11 start, 12-18 bench) |
| `event_types.csv` | Event semantics, shot flags, related-player rules, allowed outcomes |
| `match_scenarios.csv` | Generation intent only; not evidence |
| `schema.json` | Machine-readable data contract (all fields, types, enums) |
| `generation_manifest.json` | Version, seed, counts, SHA-256 per file |
| `validation_report.json` | Result of the independent validator |
| `fixtures/*.csv` | Five complete single-match event files, rows identical to `events.csv` |

## Key semantics

- **Time:** `timestamp_seconds` is elapsed playing time (half-time break excluded, stoppage included). `minute` is `timestamp_seconds // 60`; `display_clock` gives the broadcast-style label (e.g. `45+2'`, `90+3'`).
- **Scoring:** an event changes the score only if `is_goal=true` and `is_disallowed=false` (types `GOAL`, `PENALTY_SCORED`, `OWN_GOAL`). `GOAL_DISALLOWED` never changes the score.
- **Own goals:** `team_id`/`player_id` are the culprit's side; `credited_team_id` is the opposing team that receives the goal.
- **Penalties:** `FOUL` -> `PENALTY_AWARDED` -> `PENALTY_SCORED` or `PENALTY_MISSED` (saved misses are followed by a `SAVE`).
- **Substitutions:** `player` leaves, `related_player` enters. Sent-off and substituted-off players never appear again.
- **Groups and tags:** `synthetic_event_group_id` and `scenario_tag` are generation metadata, not evidence of causality, and must not drive turning-point rankings.

## Limitations (by design)

- The log is **sampled**: passes are not exhaustive. Possession %, pass accuracy, xG, ratings, tracking coordinates and passing networks are **not** in the data and must not be shown.
- Statistics that are valid: goals, shots (`SHOT`, `SHOT_ON_TARGET`, `SHOT_OFF_TARGET`, `GOAL`, `PENALTY_SCORED`, `PENALTY_MISSED`), shots on target (`SHOT_ON_TARGET`, `GOAL`, `PENALTY_SCORED`, saved `PENALTY_MISSED`), saves, corners, fouls, offsides, cards, substitutions.
- Hackathon rules were not verified: no public rules for the challenge could be found while building this dataset. Check them before submission.

## Events by type

| Type | Count |
|---|---|
| `CORNER` | 115 |
| `FOUL` | 175 |
| `FULL_TIME` | 12 |
| `GOAL` | 29 |
| `GOAL_DISALLOWED` | 1 |
| `HALF_TIME` | 12 |
| `KICK_OFF` | 12 |
| `OFFSIDE` | 48 |
| `OWN_GOAL` | 1 |
| `PASS` | 385 |
| `PENALTY_AWARDED` | 2 |
| `PENALTY_MISSED` | 1 |
| `PENALTY_SCORED` | 1 |
| `RED_CARD` | 1 |
| `SAVE` | 112 |
| `SECOND_HALF_START` | 12 |
| `SHOT` | 46 |
| `SHOT_OFF_TARGET` | 103 |
| `SHOT_ON_TARGET` | 111 |
| `SUBSTITUTION` | 45 |
| `YELLOW_CARD` | 38 |

## Field reference

Complete field definitions (types, enums, meaning) are in `schema.json`.

### matches.csv

| Field | Type | Required | Meaning |
|---|---|---|---|
| `match_id` | string | yes | Stable unique match id, e.g. SYN-M001. |
| `dataset_id` | string | yes | Dataset identifier; always matchmind-synthetic-v1. |
| `is_synthetic` | boolean | yes | Always true. |
| `home_team_id` | string | yes | References teams.csv. |
| `away_team_id` | string | yes | References teams.csv; must differ from home_team_id. |
| `home_team_name` | string | yes | Fictional club name. |
| `away_team_name` | string | yes | Fictional club name. |
| `competition_name` | string | yes | Invented competition: Synthetic Football Showcase. |
| `season_label` | string | yes | Invented season label. |
| `match_date` | date | yes | ISO-8601 date (YYYY-MM-DD). |
| `scheduled_duration_minutes` | integer | yes | Regulation length, 90. |
| `status` | string | yes | Match status. |
| `home_score` | integer | yes | Final home score, derived from valid scoring events. |
| `away_score` | integer | yes | Final away score, derived from valid scoring events. |
| `scenario_type` | string | yes | Generation target label. Not evidence. |
| `data_version` | string | yes | Dataset version. |
| `first_half_stoppage_minutes` | integer | yes | Whole minutes of first-half stoppage; equals (HALF_TIME timestamp - 2700) / 60. |
| `second_half_stoppage_minutes` | integer | yes | Whole minutes of second-half stoppage; equals (FULL_TIME timestamp - HALF_TIME timestamp - 2700) / 60. |

### events.csv

| Field | Type | Required | Meaning |
|---|---|---|---|
| `event_id` | string | yes | Stable unique id: <match_id>-E<sequence>. |
| `dataset_id` | string | yes | Dataset identifier. |
| `match_id` | string | yes | References matches.csv. |
| `event_sequence` | integer | yes | 1..N contiguous per match, in chronological order. |
| `minute` | integer | yes | ELAPSED playing minute = timestamp_seconds // 60 (continuous across stoppage; NOT the broadcast clock). |
| `second` | integer | yes | timestamp_seconds % 60. |
| `display_clock` | string | yes | Broadcast-style label such as 45+2' or 90+3', derived from period and elapsed time. |
| `period` | string | yes | Half the event belongs to. |
| `timestamp_seconds` | integer | yes | Elapsed playing seconds since kick-off. Half-time break is not counted; stoppage time is. |
| `event_type` | string | yes | See event_types.csv. |
| `event_outcome` | string | yes | Outcome enum; allowed values depend on event_type. |
| `team_id` | string | no | Team of player_id. Empty for match-flow events. |
| `team_name` | string | no | Team name matching teams.csv. |
| `player_id` | string | no | References players.csv. Empty for match-flow events. |
| `player_name` | string | no | Player name matching players.csv. |
| `related_player_id` | string | no | Second participant; meaning per event type (see event_types.csv). |
| `related_player_name` | string | no | Name matching players.csv. |
| `credited_team_id` | string | no | Team credited with the score for GOAL, PENALTY_SCORED and OWN_GOAL (opposing team of the culprit). Empty otherwise. |
| `description` | string | yes | Neutral factual description of the single event. Contains no causal or analytical claims. |
| `is_goal` | boolean | yes | True only for valid scoring events: GOAL, OWN_GOAL, PENALTY_SCORED. |
| `is_own_goal` | boolean | yes | True only for OWN_GOAL. |
| `is_penalty` | boolean | yes | True for PENALTY_AWARDED, PENALTY_SCORED, PENALTY_MISSED. |
| `is_red_card` | boolean | yes | True only for RED_CARD. |
| `is_yellow_card` | boolean | yes | True only for YELLOW_CARD. |
| `is_substitution` | boolean | yes | True only for SUBSTITUTION. |
| `is_disallowed` | boolean | yes | True only for GOAL_DISALLOWED (never changes the score). |
| `score_home_after_event` | integer | yes | Home score after applying this event. |
| `score_away_after_event` | integer | yes | Away score after applying this event. |
| `source_type` | string | yes | Provenance of the record. |
| `synthetic_event_group_id` | string | no | Generation metadata grouping a generated sequence. NOT proof of causality. |
| `scenario_tag` | string | yes | Generation metadata. Must not drive analysis results. |
| `data_quality_flag` | string | yes | Record quality marker. |

### teams.csv

| Field | Type | Required | Meaning |
|---|---|---|---|
| `team_id` | string | yes | Stable id, e.g. T-NBR. |
| `team_name` | string | yes | Fictional club name. |
| `short_name` | string | yes | Three-letter code. |
| `home_ground` | string | yes | Fictional ground name. |
| `synthetic_seed_rank` | integer | yes | Fictional pre-season seeding 1-10 used only as generation context (e.g. underdog scenario). Not a performance statistic. |
| `is_synthetic` | boolean | yes | Always true. |
| `dataset_id` | string | yes | Dataset identifier. |

### players.csv

| Field | Type | Required | Meaning |
|---|---|---|---|
| `player_id` | string | yes | Synthetic id, e.g. P-NBR-09. |
| `player_name` | string | yes | Invented name. |
| `team_id` | string | yes | References teams.csv. |
| `squad_number` | integer | yes | 1-18. Numbers 1-11 are starters in every match; 12-18 are the bench. |
| `position` | string | yes | Nominal position. |
| `is_synthetic` | boolean | yes | Always true. |
| `dataset_id` | string | yes | Dataset identifier. |

### match_scenarios.csv

| Field | Type | Required | Meaning |
|---|---|---|---|
| `match_id` | string | yes | References matches.csv. |
| `scenario_type` | string | yes | Generation target label. |
| `intended_narrative` | string | yes | What the generator aimed for. |
| `generation_target` | string | yes | Scripted targets (not evidence; the events are authoritative). |
| `home_seed_rank` | integer | yes | Fictional seeding of home team. |
| `away_seed_rank` | integer | yes | Fictional seeding of away team. |
| `evidence_note` | string | yes | Reminder that analysis must derive from events.csv. |
