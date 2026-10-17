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

const normalizeApiUrl = (url?: string | null): string => {
  if (!url || !url.trim()) {
    return import.meta.env.DEV ? 'http://localhost:8080/api' : '/api';
  }
  let clean = url.trim().replace(/\/+$/, '');
  if (!clean.endsWith('/api')) {
    clean = `${clean}/api`;
  }
  return clean;
};

export const getApiBaseUrl = (): string => {
  if (typeof window !== 'undefined') {
    const custom = localStorage.getItem('matchmind_api_base_url');
    if (custom) return normalizeApiUrl(custom);
  }
  return normalizeApiUrl(import.meta.env.VITE_API_BASE_URL);
};

export const setCustomApiBaseUrl = (url: string) => {
  if (typeof window !== 'undefined') {
    if (!url || !url.trim()) {
      localStorage.removeItem('matchmind_api_base_url');
    } else {
      localStorage.setItem('matchmind_api_base_url', normalizeApiUrl(url));
    }
  }
};

const api = axios.create({
  baseURL: getApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  config.baseURL = getApiBaseUrl();
  return config;
});

api.interceptors.response.use(
  (response) => {
    if (typeof response.data === 'string' && (
      response.data.trim().startsWith('<!DOCTYPE') ||
      response.data.trim().startsWith('<html') ||
      response.data.includes('<head>')
    )) {
      return Promise.reject(new Error(
        'Server returned HTML page instead of API JSON data. Ensure your backend URL points to your Render backend web service.'
      ));
    }
    return response;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const apiClient = {
  getBaseUrl(): string {
    return getApiBaseUrl();
  },

  setBaseUrl(url: string) {
    setCustomApiBaseUrl(url);
  },

  // Health check
  async health() {
    const response = await api.get('/health');
    return response.data;
  },

  // Matches
  async getMatches(): Promise<Match[]> {
    const response = await api.get('/matches');
    if (Array.isArray(response.data)) {
      return response.data;
    }
    if (response.data && typeof response.data === 'object' && Array.isArray((response.data as any).matches)) {
      return (response.data as any).matches;
    }
    throw new Error('Invalid match data received from server. Expected array of matches.');
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
    return Array.isArray(response.data) ? response.data : [];
  },

  // Statistics
  async getMatchStatistics(matchId: string): Promise<MatchStatistics> {
    const response = await api.get(`/matches/${matchId}/statistics`);
    return response.data;
  },

  // Turning Points
  async getTurningPoints(matchId: string): Promise<TurningPoint[]> {
    const response = await api.get(`/matches/${matchId}/turning-points`);
    return Array.isArray(response.data) ? response.data : [];
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
