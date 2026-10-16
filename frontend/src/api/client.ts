import axios from 'axios';
import type {
  Match,
  MatchEvent,
  MatchStatistics,
  TurningPoint,
  MatchRecap,
  QuestionRequest,
  QuestionResponse,
  MatchPeriodComparison,
  ReplayState
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api');

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const apiClient = {
  // Health check
  async health() {
    const response = await api.get('/health');
    return response.data;
  },

  // Matches
  async getMatches(): Promise<Match[]> {
    const response = await api.get('/matches');
    return response.data;
  },

  async getMatch(matchId: string): Promise<Match> {
    const response = await api.get(`/matches/${matchId}`);
    return response.data;
  },

  // Events
  async getMatchEvents(
    matchId: string,
    filters?: { teamId?: string; eventType?: string; period?: string }
  ): Promise<MatchEvent[]> {
    const response = await api.get(`/matches/${matchId}/events`, { params: filters });
    return response.data;
  },

  // Statistics
  async getMatchStatistics(matchId: string): Promise<MatchStatistics> {
    const response = await api.get(`/matches/${matchId}/statistics`);
    return response.data;
  },

  // Turning Points
  async getTurningPoints(matchId: string): Promise<TurningPoint[]> {
    const response = await api.get(`/matches/${matchId}/turning-points`);
    return response.data;
  },

  // Evidence
  async getEvidence(matchId: string, evidenceId: string): Promise<MatchEvent[]> {
    const response = await api.get(`/matches/${matchId}/evidence/${evidenceId}`);
    return response.data;
  },

  // Recap
  async getMatchRecap(matchId: string, mode: 'FAN' | 'ANALYST' = 'FAN'): Promise<MatchRecap> {
    const response = await api.get(`/matches/${matchId}/recap`, { params: { mode } });
    return response.data;
  },

  // Questions
  async askQuestion(matchId: string, request: QuestionRequest): Promise<QuestionResponse> {
    const response = await api.post(`/matches/${matchId}/questions`, request);
    return response.data;
  },

  // Period Comparison
  async getMatchComparison(matchId: string): Promise<MatchPeriodComparison> {
    const response = await api.get(`/matches/${matchId}/comparison`);
    return response.data;
  },

  async getReplay(matchId: string, sequence: number): Promise<ReplayState> {
    const response = await api.get(`/matches/${matchId}/replay`, { params: { sequence } });
    return response.data;
  },

  // Upload
  async uploadMatch(file: File): Promise<any> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/matches/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
};

export default apiClient;
