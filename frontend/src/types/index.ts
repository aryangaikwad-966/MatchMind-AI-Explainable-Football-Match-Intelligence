export interface Match {
  matchId: string;
  datasetId: string;
  isSynthetic: boolean;
  homeTeamId: string;
  awayTeamId: string;
  homeTeamName: string;
  awayTeamName: string;
  competitionName: string;
  seasonLabel: string;
  matchDate: string;
  scheduledDurationMinutes: number;
  status: string;
  homeScore: number;
  awayScore: number;
  scenarioType: string;
  dataVersion: string;
  firstHalfStoppageMinutes: number;
  secondHalfStoppageMinutes: number;
}

export interface MatchEvent {
  eventId: string;
  datasetId: string;
  matchId: string;
  eventSequence: number;
  minute: number;
  second: number;
  displayClock: string;
  period: string;
  timestampSeconds: number;
  eventType: string;
  eventOutcome: string;
  teamId: string | null;
  teamName: string | null;
  playerId: string | null;
  playerName: string | null;
  relatedPlayerId: string | null;
  relatedPlayerName: string | null;
  creditedTeamId: string | null;
  description: string;
  isGoal: boolean;
  isOwnGoal: boolean;
  isPenalty: boolean;
  isRedCard: boolean;
  isYellowCard: boolean;
  isSubstitution: boolean;
  isDisallowed: boolean;
  scoreHomeAfterEvent: number;
  scoreAwayAfterEvent: number;
  sourceType: string;
  syntheticEventGroupId: string | null;
  scenarioTag: string;
  dataQualityFlag: string;
}

export interface MatchStatistics {
  matchId: string;
  homeScore: number;
  awayScore: number;
  homeShots: number;
  awayShots: number;
  homeShotsOnTarget: number;
  awayShotsOnTarget: number;
  homeSaves: number;
  awaySaves: number;
  homeCorners: number;
  awayCorners: number;
  homeFouls: number;
  awayFouls: number;
  homeOffsides: number;
  awayOffsides: number;
  homeYellowCards: number;
  awayYellowCards: number;
  homeRedCards: number;
  awayRedCards: number;
  homeSubstitutions: number;
  awaySubstitutions: number;
  homeEventsByType: Record<string, number>;
  awayEventsByType: Record<string, number>;
}

export interface TurningPoint {
  insightId: string;
  matchId: string;
  minute: number;
  type: string;
  explanation: string;
  rankingRationale: string;
  score: number;
  eventIds: string[];
  scoreChange: string;
  period: string;
}

export interface MatchRecap {
  matchId: string;
  recap: string;
  fanExplanation: string | null;
  analystExplanation: string | null;
  keyMoments: string;
  aiMode: string;
}

export interface QuestionRequest {
  question: string;
  mode: 'FAN' | 'ANALYST';
  preference?: string;
}

export interface QuestionResponse {
  answer: string;
  evidenceEventIds: string[];
  aiMode: string;
}

export interface MatchPeriodComparison {
  matchId: string;
  firstHalf: PeriodStatistics;
  secondHalf: PeriodStatistics;
}

export interface PeriodStatistics {
  homeScore: number;
  awayScore: number;
  homeShots: number;
  awayShots: number;
  homeShotsOnTarget: number;
  awayShotsOnTarget: number;
  homeCorners: number;
  awayCorners: number;
  homeFouls: number;
  awayFouls: number;
}

export interface ReplayState {
  matchId: string;
  eventSequence: number;
  simulatedMinute: number;
  homeScore: number;
  awayScore: number;
  events: MatchEvent[];
  insights: TurningPoint[];
  label: string;
}
