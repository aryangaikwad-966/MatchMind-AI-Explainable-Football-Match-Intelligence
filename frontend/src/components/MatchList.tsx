import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Trophy, Clock, Calendar, AlertCircle } from 'lucide-react';
import apiClient from '../api/client';
import type { Match } from '../types';

export default function MatchList() {
  const [matches, setMatches] = useState<Match[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [aiMode, setAiMode] = useState<string>('DEMO');

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
      setMatches(matchesData);
      setAiMode(healthData.aiMode || 'DEMO');
      setError(null);
    } catch (err) {
      setError('Failed to load matches. Please ensure the backend is running.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-football-500"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-900/20 border border-red-800 rounded-lg p-6">
        <div className="flex items-center space-x-3">
          <AlertCircle className="w-6 h-6 text-red-400" />
          <div>
            <h3 className="text-red-400 font-semibold">Error</h3>
            <p className="text-red-300">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white">Match Dashboard</h2>
          <p className="text-navy-300 mt-1">
            {matches.length} synthetic matches available
          </p>
        </div>
        <div className="flex items-center space-x-2 bg-navy-800 px-4 py-2 rounded-lg">
          <div className={`w-2 h-2 rounded-full ${aiMode === 'GEMINI' ? 'bg-green-500' : 'bg-yellow-500'}`} />
          <span className="text-sm text-navy-300">
            AI Mode: <span className="font-semibold text-white">{aiMode}</span>
          </span>
        </div>
      </div>

      {/* AI Mode Notice */}
      {aiMode === 'DEMO' && (
        <div className="bg-yellow-900/20 border border-yellow-800 rounded-lg p-4">
          <p className="text-yellow-300 text-sm">
            <strong>Demo Mode:</strong> AI explanations are generated using deterministic analysis and templates.
            Configure `GEMINI_API_KEY` on the backend to enable Gemini AI.
          </p>
        </div>
      )}

      {/* Matches Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {matches.map((match) => (
          <Link
            key={match.matchId}
            to={`/match/${match.matchId}`}
            className="bg-navy-900 border border-navy-800 rounded-lg p-6 hover:border-football-500 transition cursor-pointer group"
          >
            <div className="space-y-4">
              {/* Teams */}
              <div className="flex items-center justify-between">
                <div className="text-center flex-1">
                  <p className="text-sm text-navy-400 mb-1">Home</p>
                  <p className="font-semibold text-white group-hover:text-football-400 transition">
                    {match.homeTeamName}
                  </p>
                </div>
                <div className="px-4">
                  <Trophy className="w-6 h-6 text-navy-600" />
                </div>
                <div className="text-center flex-1">
                  <p className="text-sm text-navy-400 mb-1">Away</p>
                  <p className="font-semibold text-white group-hover:text-football-400 transition">
                    {match.awayTeamName}
                  </p>
                </div>
              </div>

              {/* Score */}
              <div className="bg-navy-800 rounded-lg py-3 px-4 text-center">
                <p className="text-3xl font-bold text-white">
                  {match.homeScore} - {match.awayScore}
                </p>
              </div>

              {/* Metadata */}
              <div className="flex items-center justify-between text-sm text-navy-400">
                <div className="flex items-center space-x-1">
                  <Calendar className="w-4 h-4" />
                  <span>{new Date(match.matchDate).toLocaleDateString()}</span>
                </div>
                <div className="flex items-center space-x-1">
                  <Clock className="w-4 h-4" />
                  <span>{match.status}</span>
                </div>
              </div>

              {/* Scenario Type */}
              <div className="pt-3 border-t border-navy-800">
                <p className="text-xs text-navy-500 uppercase tracking-wide">Scenario</p>
                <p className="text-sm text-navy-300 mt-1">{formatScenario(match.scenarioType)}</p>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}

function formatScenario(scenario: string): string {
  return scenario
    .split('_')
    .map(word => word.charAt(0) + word.slice(1).toLowerCase())
    .join(' ');
}
