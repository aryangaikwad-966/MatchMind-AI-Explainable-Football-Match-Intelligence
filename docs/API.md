# MatchMind AI API Documentation

## Base URL

```
http://localhost:8080/api
```

## Authentication

No authentication required for this hackathon demo. Production deployments should add appropriate authentication.

## Response Format

All responses are JSON. Error responses follow this format:

```json
{
  "error": "Error message",
  "details": "Additional details"
}
```

## Endpoints

### Health Check

Check API health and current AI mode.

**Endpoint:** `GET /health`

**Response:**
```json
{
  "status": "UP",
  "aiMode": "DEMO",
  "providerStatus": "DEMO",
  "liveRequestVerified": false,
  "matchesLoaded": 12
}
```

### Matches

#### List All Matches

Get all available matches.

**Endpoint:** `GET /matches`

**Response shape:**
```json
[
  {
    "matchId": "SYN-M001",
    "datasetId": "matchmind-synthetic-v1",
    "isSynthetic": true,
    "homeTeamId": "T-NBR",
    "awayTeamId": "T-KNG",
    "homeTeamName": "Northbridge FC",
    "awayTeamName": "Kingsford United",
    "competitionName": "Synthetic Football Showcase",
    "seasonLabel": "SYN-2026/27",
    "matchDate": "2026-08-15",
    "scheduledDurationMinutes": 90,
    "status": "COMPLETED",
    "homeScore": 3,
    "awayScore": 2,
    "scenarioType": "LATE_COMEBACK_VICTORY",
    "dataVersion": "1.0.0",
    "firstHalfStoppageMinutes": 2,
    "secondHalfStoppageMinutes": 5
  }
]
```

#### Get Match Details

Get details for a specific match.

**Endpoint:** `GET /matches/{matchId}`

**Parameters:**
- `matchId` (path) - Match identifier

**Response:** Single match object (same format as list)

#### Get Match Events

Get events for a specific match with optional filtering.

**Endpoint:** `GET /matches/{matchId}/events`

**Parameters:**
- `matchId` (path) - Match identifier
- `teamId` (query, optional) - Filter by team
- `eventType` (query, optional) - Filter by event type
- `period` (query, optional) - Filter by period (FIRST_HALF, SECOND_HALF)

**Response:**
```json
[
  {
    "eventId": "SYN-M001-E0001",
    "datasetId": "matchmind-synthetic-v1",
    "matchId": "SYN-M001",
    "eventSequence": 1,
    "minute": 0,
    "second": 0,
    "displayClock": "1'",
    "period": "FIRST_HALF",
    "timestampSeconds": 0,
    "eventType": "KICK_OFF",
    "eventOutcome": "NONE",
    "teamId": null,
    "teamName": null,
    "playerId": null,
    "playerName": null,
    "relatedPlayerId": null,
    "relatedPlayerName": null,
    "creditedTeamId": null,
    "description": "Match kicks off",
    "isGoal": false,
    "isOwnGoal": false,
    "isPenalty": false,
    "isRedCard": false,
    "isYellowCard": false,
    "isSubstitution": false,
    "isDisallowed": false,
    "scoreHomeAfterEvent": 0,
    "scoreAwayAfterEvent": 0,
    "sourceType": "SYNTHETIC_OBSERVED",
    "syntheticEventGroupId": null,
    "scenarioTag": "MATCH_FLOW",
    "dataQualityFlag": "OK"
  }
]
```

#### Get Match Statistics

Get calculated statistics for a match.

**Endpoint:** `GET /matches/{matchId}/statistics`

**Parameters:**
- `matchId` (path) - Match identifier

**Response shape (numeric values are calculated for the selected match):**
```json
{
  "matchId": "SYN-M001",
  "homeScore": "final score from the selected match",
  "awayScore": "final score from the selected match",
  "homeShots": "calculated from events",
  "awayShots": "calculated from events",
  "homeSaves": "integer calculated from events",
  "awaySaves": "integer calculated from events",
  "homeCorners": "integer calculated from events",
  "awayCorners": "integer calculated from events",
  "homeFouls": "integer calculated from events",
  "awayFouls": "integer calculated from events",
  "homeOffsides": "integer calculated from events",
  "awayOffsides": "integer calculated from events",
  "homeYellowCards": "integer calculated from events",
  "awayYellowCards": "integer calculated from events",
  "homeRedCards": "integer calculated from events",
  "awayRedCards": "integer calculated from events",
  "homeSubstitutions": "integer calculated from events",
  "awaySubstitutions": "integer calculated from events",
  "homeEventsByType": {
    "EVENT_TYPE": "count"
  },
  "awayEventsByType": {
    "EVENT_TYPE": "count"
  }
}
```

#### Get Turning Points

Get ranked turning points for a match.

**Endpoint:** `GET /matches/{matchId}/turning-points`

**Parameters:**
- `matchId` (path) - Match identifier

**Response shape (values vary by match):**
```json
[
  {
    "insightId": "TP-<actual-event-id>",
    "matchId": "selected match id",
    "minute": "integer",
    "type": "documented turning-point type",
    "explanation": "evidence-backed explanation",
    "rankingRationale": "deterministic ranking rationale",
    "score": "numeric ranking score",
    "eventIds": ["actual event ID from the selected match"],
    "scoreChange": "actual score snapshot",
    "period": "FIRST_HALF or SECOND_HALF"
  }
]
```

**Turning Point Types:**
- `EQUALIZER` - Goal that brought scores level
- `GOAL_LEAD` - Goal that established a lead
- `LATE_GOAL` - Goal in decisive period (75+ minutes)
- `RED_CARD` - Player sent off
- `PENALTY_AWARDED` - Penalty kick awarded
- `DISALLOWED_GOAL` - Goal disallowed by VAR
- `OWN_GOAL` - Goal scored by own team

#### Get Match Period Comparison

Compare first and second half statistics.

**Endpoint:** `GET /matches/{matchId}/comparison`

**Parameters:**
- `matchId` (path) - Match identifier

**Response:**
```json
{
  "matchId": "SYN-M001",
  "firstHalf": {
    "homeScore": 1,
    "awayScore": 1,
    "homeShots": 8,
    "awayShots": 6,
    "homeShotsOnTarget": 4,
    "awayShotsOnTarget": 3,
    "homeCorners": 4,
    "awayCorners": 2,
    "homeFouls": 5,
    "awayFouls": 6
  },
  "secondHalf": {
    "homeScore": 2,
    "awayScore": 1,
    "homeShots": 7,
    "awayShots": 6,
    "homeShotsOnTarget": 4,
    "awayShotsOnTarget": 2,
    "homeCorners": 3,
    "awayCorners": 2,
    "homeFouls": 5,
    "awayFouls": 6
  }
}
```

### AI Features

#### Get Match Recap

Generate a match recap with AI.

**Endpoint:** `GET /matches/{matchId}/recap`

**Parameters:**
- `matchId` (path) - Match identifier
- `mode` (query) - `FAN` or `ANALYST` (default: `FAN`)

**Response:**
```json
{
  "matchId": "SYN-M001",
  "recap": "provider-generated or deterministic match-specific text",
  "fanExplanation": "fan-mode text when mode is FAN",
  "analystExplanation": "analyst-mode text when mode is ANALYST",
  "keyMoments": "actual event IDs and descriptions from this match",
  "aiMode": "DEMO"
}
```

#### Ask Question

Ask a question about the match.

**Endpoint:** `POST /matches/{matchId}/questions`

**Parameters:**
- `matchId` (path) - Match identifier

**Request Body:**
```json
{
  "question": "How many goals were scored?",
  "mode": "FAN"
}
```

**Response:**
```json
{
  "answer": "match-specific answer grounded in the requested evidence",
  "evidenceEventIds": ["actual event IDs returned for this question"],
  "aiMode": "DEMO"
}
```

### Evidence

#### Get Evidence

Get source events for an insight.

**Endpoint:** `GET /matches/{matchId}/evidence/{evidenceId}`

**Parameters:**
- `matchId` (path) - Match identifier
- `evidenceId` (path) - Insight/turning point ID

**Response:** Array of event objects (same format as events endpoint)

### Upload

#### Upload CSV

Upload a CSV file for analysis.

**Endpoint:** `POST /matches/upload`

**Request:** `multipart/form-data` with file field

**Response:**
```json
{
  "success": true,
  "matchId": "the imported unique match id",
  "records": "number of validated rows",
  "analysisAvailable": true,
  "message": "Synthetic match imported and available through the standard match APIs."
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Missing required columns: event_id, match_id"
}
```

## Error Codes

- `400 Bad Request` - Invalid input or parameters
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## Rate Limiting

No rate limiting in demo mode. Production should implement appropriate rate limits.

## CORS

Allowed origins (configurable):
- `http://localhost:5173`
- `http://localhost:3000`

Configure via `cors.allowed-origins` in `application.properties`.
