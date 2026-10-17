import { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Trophy, Clock, Calendar, AlertCircle, Search, Sparkles, Filter, ChevronRight, Globe } from 'lucide-react';
import apiClient from '../api/client';
import type { Match } from '../types';

import { getTeamBadge, getScenarioMeta } from '../utils/teamThemes';

export default function MatchList() {
  const [matches, setMatches] = useState<Match[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [aiMode, setAiMode] = useState<string>('DEMO');
  const [searchTerm, setSearchTerm] = useState('');
  const [scenarioFilter, setScenarioFilter] = useState<string>('ALL');
  const [customApiUrl, setCustomApiUrl] = useState('');
  const [isSavingUrl, setIsSavingUrl] = useState(false);

  useEffect(() => {
    loadMatches();
  }, []);

  const loadMatches = async () => {
    try {
      setLoading(true);
      const [matchesData, healthData] = await Promise.all([
        apiClient.getMatches(),
        apiClient.health()
      ]);
      if (!Array.isArray(matchesData)) {
        throw new Error('Received unexpected data format from backend. Expected list of matches.');
      }
      setMatches(matchesData);
      setAiMode(healthData?.aiMode || 'DEMO');
      setError(null);
    } catch (err: any) {
      setError(err?.message || 'Failed to load matches. Please ensure the backend is running.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const filteredMatches = useMemo(() => {
    const safeMatches = Array.isArray(matches) ? matches : [];
    return safeMatches.filter(match => {
      const matchesSearch =
        (match.homeTeamName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (match.awayTeamName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (match.competitionName || '').toLowerCase().includes(searchTerm.toLowerCase());

      const scenario = match.scenarioType || '';
      const matchesScenario =
        scenarioFilter === 'ALL' ||
        (scenarioFilter === 'COMEBACK' && scenario.includes('COMEBACK')) ||
        (scenarioFilter === 'RED_CARD' && scenario.includes('RED_CARD')) ||
        (scenarioFilter === 'HIGH_SCORING' && scenario.includes('HIGH_SCORING')) ||
        (scenarioFilter === 'DRAW' && match.homeScore === match.awayScore);

      return matchesSearch && matchesScenario;
    });
  }, [matches, searchTerm, scenarioFilter]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-24 space-y-4">
        <div className="relative">
          <div className="w-16 h-16 rounded-full border-4 border-navy-700 border-t-football-400 animate-spin" role="status" />
          <div className="absolute inset-0 flex items-center justify-center">
            <Trophy className="w-6 h-6 text-football-400/80 animate-pulse" />
          </div>
        </div>
        <p className="text-navy-300 font-medium text-sm animate-pulse">Loading synthetic match intelligence…</p>
      </div>
    );
  }

  if (error) {
    const currentBase = apiClient.getBaseUrl();
    const isRelativeOnVercel = currentBase === '/api' && typeof window !== 'undefined' && window.location.hostname !== 'localhost';

    const handleSaveCustomUrl = (e: React.FormEvent) => {
      e.preventDefault();
      if (!customApiUrl.trim()) return;
      setIsSavingUrl(true);
      apiClient.setBaseUrl(customApiUrl.trim());
      setError(null);
      loadMatches().finally(() => setIsSavingUrl(false));
    };

    return (
      <div className="max-w-2xl mx-auto space-y-6">
        <div className="bg-navy-900/90 border border-red-800/80 rounded-2xl p-6 sm:p-8 shadow-2xl backdrop-blur-sm">
          <div className="flex items-start space-x-4">
            <div className="p-3 bg-red-950/80 rounded-xl border border-red-800 shrink-0">
              <AlertCircle className="w-7 h-7 text-red-400" />
            </div>
            <div className="flex-1 space-y-4">
              <div>
                <h3 className="text-red-300 font-display font-bold text-lg">Backend Connection Error (404)</h3>
                <p className="text-navy-300 text-sm mt-1 leading-relaxed">
                  The frontend could not reach the backend API.
                </p>
                <div className="mt-2 text-xs text-navy-400 bg-navy-950/80 p-2.5 rounded-lg border border-navy-800 font-mono break-all">
                  Targeted API URL: <span className="text-football-300 font-semibold">{currentBase}</span>
                </div>
              </div>

              {isRelativeOnVercel && (
                <div className="bg-navy-950/90 border border-navy-700/80 rounded-xl p-4 text-xs text-navy-200 space-y-2">
                  <div className="flex items-center gap-2 text-football-400 font-semibold uppercase tracking-wider">
                    <Globe className="w-4 h-4" />
                    <span>Vercel Deployment Notice</span>
                  </div>
                  <p className="text-navy-300 leading-relaxed">
                    Vercel hosts the frontend static files, so calls to <code className="text-football-300 bg-navy-900 px-1 py-0.5 rounded">/api/*</code> return 404 because the Spring Boot backend is hosted on Render.
                  </p>
                </div>
              )}

              {/* Instant Connect Box */}
              <form onSubmit={handleSaveCustomUrl} className="pt-1 space-y-2">
                <label className="block text-xs font-semibold text-navy-200">
                  Enter your Render Backend URL to connect immediately:
                </label>
                <div className="flex flex-col sm:flex-row gap-2">
                  <input
                    type="url"
                    placeholder="https://your-backend.onrender.com"
                    value={customApiUrl}
                    onChange={(e) => setCustomApiUrl(e.target.value)}
                    className="flex-1 px-3.5 py-2 text-sm bg-navy-950 border border-navy-700 rounded-lg text-white placeholder-navy-500 focus:outline-none focus:border-football-400 font-mono"
                  />
                  <button
                    type="submit"
                    disabled={isSavingUrl || !customApiUrl.trim()}
                    className="px-5 py-2 bg-football-500 hover:bg-football-400 disabled:opacity-40 text-navy-950 text-sm font-bold rounded-lg transition shrink-0"
                  >
                    {isSavingUrl ? 'Connecting…' : 'Save & Connect'}
                  </button>
                </div>
                <p className="text-[11px] text-navy-400">
                  Tip: Paste your Render web service URL (with or without <code className="text-navy-300">/api</code>).
                </p>
              </form>

              <div className="pt-3 border-t border-navy-800 flex items-center justify-between">
                <button
                  type="button"
                  onClick={loadMatches}
                  className="px-4 py-2 bg-navy-800 hover:bg-navy-700 text-white text-xs font-medium rounded-lg transition"
                >
                  Retry Connection
                </button>
                {typeof window !== 'undefined' && localStorage.getItem('matchmind_api_base_url') && (
                  <button
                    type="button"
                    onClick={() => {
                      apiClient.setBaseUrl('');
                      loadMatches();
                    }}
                    className="text-xs text-navy-400 hover:text-navy-200 underline"
                  >
                    Reset to default URL
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Hero Showcase Banner */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-navy-900 via-navy-900 to-navy-950 border border-navy-700/80 shadow-2xl p-6 sm:p-8">
        <div className="absolute -right-16 -top-16 w-80 h-80 bg-football-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute right-1/4 -bottom-20 w-60 h-60 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="max-w-2xl space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-navy-800/80 border border-navy-700 text-xs font-medium text-football-300">
              <Sparkles className="w-3.5 h-3.5 text-football-400" />
              <span>Microsoft × Premier League Hackathon Edition</span>
            </div>
            <h2 className="text-3xl sm:text-4xl font-display font-extrabold text-white tracking-tight">
              Match Intelligence Center
            </h2>
            <p className="text-navy-200 text-sm sm:text-base leading-relaxed">
              Explore 12 synthetic Premier League matches with explainable turning-point detection,
              chronological event timelines, and natural-language Q&A.
            </p>
          </div>

          {/* Quick Metrics Badge */}
          <div className="flex flex-wrap sm:flex-nowrap items-center gap-3">
            <div className="bg-navy-950/70 border border-navy-700/80 rounded-2xl px-5 py-3.5 text-center flex-1 sm:flex-initial">
              <span className="block text-2xl font-display font-bold text-white">{matches.length}</span>
              <span className="text-xs text-navy-300 uppercase tracking-wider font-medium">Matches</span>
            </div>
            <div className="bg-navy-950/70 border border-navy-700/80 rounded-2xl px-5 py-3.5 text-center flex-1 sm:flex-initial">
              <div className="flex items-center justify-center gap-2">
                <span className={`w-2 h-2 rounded-full ${aiMode === 'GEMINI' ? 'bg-green-400' : 'bg-yellow-400'}`} />
                <span className="text-lg font-display font-bold text-white">{aiMode}</span>
              </div>
              <span className="text-xs text-navy-300 uppercase tracking-wider font-medium">AI Mode</span>
            </div>
          </div>
        </div>
      </section>

      {/* Search and Filters Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-navy-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search teams (e.g., Northbridge, Kingsford)..."
            className="w-full bg-navy-900/90 border border-navy-700 rounded-xl pl-10 pr-4 py-2.5 text-sm text-white placeholder-navy-400 focus:outline-none focus:border-football-400 focus-visible:ring-2 focus-visible:ring-football-400 transition"
          />
        </div>

        {/* Filter Chips */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0 styled-scroll" role="radiogroup" aria-label="Scenario filter">
          {[
            { id: 'ALL', label: 'All Matches' },
            { id: 'COMEBACK', label: 'Comebacks' },
            { id: 'HIGH_SCORING', label: 'High Scoring' },
            { id: 'RED_CARD', label: 'Red Cards' },
            { id: 'DRAW', label: 'Draws' },
          ].map(chip => (
            <button
              key={chip.id}
              onClick={() => setScenarioFilter(chip.id)}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-all focus-visible:ring-2 focus-visible:ring-football-400 ${
                scenarioFilter === chip.id
                  ? 'bg-football-500 text-white shadow-md shadow-football-900/30'
                  : 'bg-navy-900/80 text-navy-300 border border-navy-700/80 hover:text-white hover:bg-navy-800'
              }`}
            >
              {chip.label}
            </button>
          ))}
        </div>
      </div>

      {/* Matches Grid */}
      {filteredMatches.length === 0 ? (
        <div className="bg-navy-900/50 border border-navy-800 rounded-2xl p-12 text-center max-w-md mx-auto space-y-3">
          <Filter className="w-10 h-10 text-navy-500 mx-auto" />
          <h3 className="text-white font-semibold">No matching fixtures found</h3>
          <p className="text-xs text-navy-300">Try adjusting your search query or scenario filter.</p>
          <button
            onClick={() => { setSearchTerm(''); setScenarioFilter('ALL'); }}
            className="text-xs text-football-400 hover:text-football-300 font-medium underline"
          >
            Clear all filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredMatches.map((match) => {
            const homeBadge = getTeamBadge(match.homeTeamId, match.homeTeamName);
            const awayBadge = getTeamBadge(match.awayTeamId, match.awayTeamName);
            const scenarioMeta = getScenarioMeta(match.scenarioType);
            const ScenarioIcon = scenarioMeta.icon;

            const homeWon = match.homeScore > match.awayScore;
            const awayWon = match.awayScore > match.homeScore;

            return (
              <Link
                key={match.matchId}
                to={`/match/${match.matchId}`}
                className="glass-panel-hover rounded-2xl p-5 flex flex-col justify-between group focus-visible:ring-2 focus-visible:ring-football-400"
              >
                <div>
                  {/* Card Header: Competition & Scenario Tag */}
                  <div className="flex items-center justify-between gap-2 mb-4">
                    <span className="text-[11px] font-mono text-navy-300 uppercase tracking-wider truncate">
                      {match.matchId}
                    </span>
                    <span className={`inline-flex items-center gap-1 text-[11px] font-semibold px-2.5 py-0.5 rounded-full border ${scenarioMeta.color}`}>
                      <ScenarioIcon className="w-3 h-3" />
                      <span>{scenarioMeta.label}</span>
                    </span>
                  </div>

                  {/* Teams & Score Billboard */}
                  <div className="bg-navy-950/60 border border-navy-800/80 rounded-xl p-4 mb-4">
                    {/* Home Team */}
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center space-x-3 min-w-0">
                        <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${homeBadge.bg} border ${homeBadge.border} flex items-center justify-center font-display font-bold text-xs ${homeBadge.text} shadow-sm shrink-0`}>
                          {homeBadge.initials}
                        </div>
                        <span className={`font-semibold text-sm truncate ${homeWon ? 'text-white font-bold' : 'text-navy-200'}`}>
                          {match.homeTeamName}
                        </span>
                      </div>
                      <span className={`text-xl font-display font-bold ml-3 ${homeWon ? 'text-football-400' : 'text-white'}`}>
                        {match.homeScore}
                      </span>
                    </div>

                    {/* Away Team */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3 min-w-0">
                        <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${awayBadge.bg} border ${awayBadge.border} flex items-center justify-center font-display font-bold text-xs ${awayBadge.text} shadow-sm shrink-0`}>
                          {awayBadge.initials}
                        </div>
                        <span className={`font-semibold text-sm truncate ${awayWon ? 'text-white font-bold' : 'text-navy-200'}`}>
                          {match.awayTeamName}
                        </span>
                      </div>
                      <span className={`text-xl font-display font-bold ml-3 ${awayWon ? 'text-football-400' : 'text-white'}`}>
                        {match.awayScore}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Card Footer: Metadata & CTA */}
                <div className="pt-3 border-t border-navy-800/80 flex items-center justify-between text-xs text-navy-300">
                  <div className="flex items-center space-x-3">
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5 text-navy-400" />
                      <span>{new Date(match.matchDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</span>
                    </span>
                    <span className="flex items-center gap-1 font-semibold text-football-400/90">
                      <Clock className="w-3.5 h-3.5" />
                      <span>{match.status}</span>
                    </span>
                  </div>

                  <span className="flex items-center gap-1 text-football-400 group-hover:translate-x-1 transition-transform font-medium">
                    Analyze <ChevronRight className="w-3.5 h-3.5" />
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
