import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Clock, Users, Target, TrendingUp, MessageSquare, FileText, BarChart3 } from 'lucide-react';
import apiClient from '../api/client';
import type { Match, MatchStatistics, TurningPoint, MatchEvent, MatchRecap, ReplayState } from '../types';

export default function MatchDetail() {
  const { matchId } = useParams<{ matchId: string }>();
  const [match, setMatch] = useState<Match | null>(null);
  const [statistics, setStatistics] = useState<MatchStatistics | null>(null);
  const [turningPoints, setTurningPoints] = useState<TurningPoint[]>([]);
  const [events, setEvents] = useState<MatchEvent[]>([]);
  const [recap, setRecap] = useState<MatchRecap | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'timeline' | 'turning' | 'ai'>('overview');
  const [mode, setMode] = useState<'FAN' | 'ANALYST'>('FAN');
  const [preference, setPreference] = useState('');
  const [replay, setReplay] = useState<ReplayState | null>(null);
  const [playing, setPlaying] = useState(false);
  const [playbackMs, setPlaybackMs] = useState(700);

  useEffect(() => {
    if (matchId) {
      setPlaying(false);
      setReplay(null);
      setPreference('');
      loadMatchData(matchId);
    }
  }, [matchId]);

  useEffect(() => {
    if (matchId) {
      loadRecap(matchId, mode);
    }
  }, [matchId, mode]);

  useEffect(() => {
    if (!matchId || !playing || !replay || replay.eventSequence >= events.length) return;
    const timer = window.setTimeout(async () => setReplay(await apiClient.getReplay(matchId, replay.eventSequence + 1)), playbackMs);
    return () => window.clearTimeout(timer);
  }, [playing, replay, matchId, events.length, playbackMs]);

  const loadMatchData = async (id: string) => {
    try {
      setLoading(true);
      const [matchData, statsData, turningPointsData, eventsData, initialReplay] = await Promise.all([
        apiClient.getMatch(id),
        apiClient.getMatchStatistics(id),
        apiClient.getTurningPoints(id),
        apiClient.getMatchEvents(id),
        apiClient.getReplay(id, 0)
      ]);
      setMatch(matchData);
      setStatistics(statsData);
      setTurningPoints(turningPointsData);
      setEvents(eventsData);
      setReplay(initialReplay);
      setError(null);
    } catch (err) {
      setError('Failed to load match data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadRecap = async (id: string, currentMode: 'FAN' | 'ANALYST') => {
    try {
      const recapData = await apiClient.getMatchRecap(id, currentMode);
      setRecap(recapData);
    } catch (err) {
      console.error('Failed to load recap:', err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-football-500"></div>
      </div>
    );
  }

  if (error || !match) {
    return (
      <div className="bg-red-900/20 border border-red-800 rounded-lg p-6">
        <p className="text-red-400">{error || 'Match not found'}</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center space-x-4">
        <Link to="/" className="p-2 hover:bg-navy-800 rounded-lg transition">
          <ArrowLeft className="w-6 h-6 text-navy-300" />
        </Link>
        <div className="flex-1">
          <h2 className="text-2xl font-bold text-white">{match.homeTeamName} vs {match.awayTeamName}</h2>
          <p className="text-navy-300">{match.competitionName} • {new Date(match.matchDate).toLocaleDateString()}</p>
        </div>
      </div>

      {/* Score Banner */}
      <div className="bg-navy-900 border border-navy-800 rounded-lg p-8">
        <div className="flex items-center justify-between">
          <div className="text-center flex-1">
            <p className="text-sm text-navy-400 mb-2">Home</p>
            <p className="text-3xl font-bold text-white">{match.homeTeamName}</p>
          </div>
          <div className="px-8">
            <div className="bg-navy-800 rounded-lg py-4 px-8 text-center">
              <p className="text-5xl font-bold text-white">
                {match.homeScore} - {match.awayScore}
              </p>
              <p className="text-sm text-navy-400 mt-2 flex items-center justify-center space-x-2">
                <Clock className="w-4 h-4" />
                <span>{match.status}</span>
              </p>
            </div>
          </div>
          <div className="text-center flex-1">
            <p className="text-sm text-navy-400 mb-2">Away</p>
            <p className="text-3xl font-bold text-white">{match.awayTeamName}</p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex space-x-2 border-b border-navy-800">
        {[
          { id: 'overview' as const, label: 'Overview', icon: BarChart3 },
          { id: 'timeline' as const, label: 'Timeline', icon: Clock },
          { id: 'turning' as const, label: 'Turning Points', icon: TrendingUp },
          { id: 'ai' as const, label: 'AI Insights', icon: MessageSquare },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center space-x-2 px-4 py-3 border-b-2 transition ${activeTab === tab.id
              ? 'border-football-500 text-football-400'
              : 'border-transparent text-navy-400 hover:text-white'
              }`}
          >
            <tab.icon className="w-5 h-5" />
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && statistics && (
        <><OverviewTab statistics={statistics} /><ReplayPanel matchId={matchId!} events={events} replay={replay} setReplay={setReplay} playing={playing} setPlaying={setPlaying} playbackMs={playbackMs} setPlaybackMs={setPlaybackMs} /></>
      )}
      {activeTab === 'timeline' && (
        <TimelineTab events={events} />
      )}
      {activeTab === 'turning' && (
        <TurningPointsTab turningPoints={turningPoints} />
      )}
      {activeTab === 'ai' && recap && matchId && (
        <AITab recap={recap} mode={mode} onModeChange={setMode} matchId={matchId} preference={preference} setPreference={setPreference} />
      )}
    </div>
  );
}

function OverviewTab({ statistics }: { statistics: MatchStatistics }) {
  return (
    <div className="space-y-6">
      <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center space-x-2">
          <Target className="w-5 h-5 text-football-500" />
          <span>Match Statistics</span>
        </h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard label="Shots" home={statistics.homeShots} away={statistics.awayShots} />
          <StatCard label="Shots on Target" home={statistics.homeShotsOnTarget} away={statistics.awayShotsOnTarget} />
          <StatCard label="Corners" home={statistics.homeCorners} away={statistics.awayCorners} />
          <StatCard label="Fouls" home={statistics.homeFouls} away={statistics.awayFouls} />
          <StatCard label="Saves" home={statistics.homeSaves} away={statistics.awaySaves} />
          <StatCard label="Offsides" home={statistics.homeOffsides} away={statistics.awayOffsides} />
          <StatCard label="Yellow Cards" home={statistics.homeYellowCards} away={statistics.awayYellowCards} />
          <StatCard label="Red Cards" home={statistics.homeRedCards} away={statistics.awayRedCards} />
        </div>
      </div>

      <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center space-x-2">
          <Users className="w-5 h-5 text-football-500" />
          <span>Substitutions</span>
        </h3>
        <div className="grid grid-cols-2 gap-4">
          <StatCard label="Substitutions" home={statistics.homeSubstitutions} away={statistics.awaySubstitutions} />
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, home, away }: { label: string; home: number; away: number }) {
  return (
    <div className="bg-navy-800 rounded-lg p-4">
      <p className="text-xs text-navy-400 uppercase tracking-wide mb-2">{label}</p>
      <div className="flex items-center justify-between">
        <span className="text-2xl font-bold text-white">{home}</span>
        <span className="text-navy-500">-</span>
        <span className="text-2xl font-bold text-white">{away}</span>
      </div>
    </div>
  );
}

function TimelineTab({ events }: { events: MatchEvent[] }) {
  return (
    <div className="space-y-4">
      <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Event Timeline</h3>
        <div className="space-y-3 max-h-96 overflow-y-auto">
          {events.map((event) => (
            <div
              key={event.eventId}
              className="flex items-start space-x-4 p-3 bg-navy-800 rounded-lg hover:bg-navy-700 transition"
            >
              <div className="flex-shrink-0 w-16 text-center">
                <p className="text-lg font-bold text-white">{event.displayClock}</p>
                <p className="text-xs text-navy-400">{event.period}</p>
              </div>
              <div className="flex-1">
                <div className="flex items-center space-x-2 mb-1">
                  <span className={`px-2 py-1 rounded text-xs font-semibold ${event.isGoal ? 'bg-football-500 text-white' :
                    event.isRedCard ? 'bg-red-500 text-white' :
                      event.isYellowCard ? 'bg-yellow-500 text-black' :
                        'bg-navy-600 text-white'
                    }`}>
                    {event.eventType}
                  </span>
                  {event.teamName && (
                    <span className="text-sm text-navy-300">{event.teamName}</span>
                  )}
                </div>
                <p className="text-sm text-white">{event.description}</p>
                {event.playerName && (
                  <p className="text-xs text-navy-400 mt-1">Player: {event.playerName}</p>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function TurningPointsTab({ turningPoints }: { turningPoints: TurningPoint[] }) {
  return (
    <div className="space-y-4">
      <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Key Turning Points</h3>
        <div className="space-y-4">
          {turningPoints.slice(0, 5).map((tp) => (
            <div key={tp.insightId} className="bg-navy-800 rounded-lg p-4 border-l-4 border-football-500">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-semibold text-football-400">{tp.type}</span>
                <span className="text-sm text-navy-400">{tp.minute}'</span>
              </div>
              <p className="text-white mb-2">{tp.explanation}</p>
              <p className="text-xs text-navy-400">{tp.rankingRationale}</p>
              <div className="mt-2 pt-2 border-t border-navy-700">
                <p className="text-xs text-navy-500">Score: {tp.scoreChange}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function ReplayPanel({ matchId, events, replay, setReplay, playing, setPlaying, playbackMs, setPlaybackMs }: { matchId: string; events: MatchEvent[]; replay: ReplayState | null; setReplay: (v: ReplayState) => void; playing: boolean; setPlaying: (v: boolean) => void; playbackMs: number; setPlaybackMs: (v: number) => void }) {
  const load = async (sequence: number) => setReplay(await apiClient.getReplay(matchId, sequence));

  return (
    <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h3 className="text-lg font-semibold text-white">Evidence-to-Overlay</h3>
          <p className="text-xs text-navy-400">
            SYNTHETIC REPLAY • {replay?.simulatedMinute ?? 0}' • Score {replay?.homeScore ?? 0}-{replay?.awayScore ?? 0}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <select
            aria-label="Replay speed"
            value={playbackMs}
            onChange={e => setPlaybackMs(Number(e.target.value))}
            className="px-2 py-2 bg-navy-800 text-white rounded"
          >
            <option value={1400}>0.5×</option>
            <option value={700}>1×</option>
            <option value={350}>2×</option>
          </select>
          <button
            className="px-3 py-2 bg-football-500 rounded"
            onClick={() => setPlaying(!playing)}
          >
            {playing ? 'Pause' : 'Play'}
          </button>
          <button
            className="px-3 py-2 bg-navy-700 rounded"
            onClick={() => load(Math.min((replay?.eventSequence ?? 0) + 1, events.length))}
          >
            Step
          </button>
          <button
            className="px-3 py-2 bg-navy-700 rounded"
            onClick={() => { setPlaying(false); load(0); }}
          >
            Reset
          </button>
        </div>
      </div>
      {replay?.insights?.map(i => (
        <div key={i.insightId} className="mt-4 p-4 border-l-4 border-football-500 bg-navy-800">
          <p className="text-football-400 text-xs">{i.type} • {i.minute}'</p>
          <p className="text-white">{i.explanation}</p>
          <p className="text-xs text-navy-400">
            Evidence: {i.eventIds.map(id => (
              <a
                key={id}
                href={`/api/matches/${matchId}/evidence/${id}`}
                target="_blank"
                rel="noreferrer"
                className="underline mr-2"
              >
                {id}
              </a>
            ))}
          </p>
        </div>
      ))}
    </div>
  );
}

function AITab({ recap, mode, onModeChange, matchId, preference, setPreference }: { recap: MatchRecap; mode: 'FAN' | 'ANALYST'; onModeChange: (mode: 'FAN' | 'ANALYST') => void; matchId: string; preference: string; setPreference: (value: string) => void }) {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const askQuestion = async () => {
    if (!question.trim()) return;
    try {
      setLoading(true);
      const response = await apiClient.askQuestion(matchId, { question, mode, preference });
      setAnswer(response.answer);
    } catch (err) {
      console.error('Failed to ask question:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Mode Toggle */}
      <div className="flex items-center space-x-4 bg-navy-900 border border-navy-800 rounded-lg p-4">
        <span className="text-sm text-navy-300">Mode:</span>
        <button
          onClick={() => onModeChange('FAN')}
          className={`px-4 py-2 rounded-lg transition ${mode === 'FAN' ? 'bg-football-500 text-white' : 'bg-navy-800 text-navy-300 hover:text-white'
            }`}
        >
          Fan
        </button>
        <button
          onClick={() => onModeChange('ANALYST')}
          className={`px-4 py-2 rounded-lg transition ${mode === 'ANALYST' ? 'bg-football-500 text-white' : 'bg-navy-800 text-navy-300 hover:text-white'
            }`}
        >
          Analyst
        </button>
        <div className="ml-auto flex items-center space-x-2">
          <div className={`w-2 h-2 rounded-full ${recap.aiMode === 'GEMINI' ? 'bg-green-500' : 'bg-yellow-500'}`} />
          <span className="text-xs text-navy-400">{recap.aiMode}</span>
        </div>
      </div>
      <label className="block text-sm text-navy-300">Personalize around <input value={preference} onChange={e => setPreference(e.target.value)} placeholder="team or player name" className="ml-2 bg-navy-800 border border-navy-700 rounded px-3 py-2 text-white" /></label>

      {/* Recap */}
      <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center space-x-2">
          <FileText className="w-5 h-5 text-football-500" />
          <span>Match Recap</span>
        </h3>
        <p className="text-white mb-4">{recap.recap}</p>
        {mode === 'FAN' && recap.fanExplanation && (
          <div className="bg-navy-800 rounded-lg p-4">
            <p className="text-sm text-navy-300">{recap.fanExplanation}</p>
          </div>
        )}
        {mode === 'ANALYST' && recap.analystExplanation && (
          <div className="bg-navy-800 rounded-lg p-4">
            <p className="text-sm text-navy-300">{recap.analystExplanation}</p>
          </div>
        )}
        {recap.keyMoments && (
          <div className="mt-4 pt-4 border-t border-navy-800">
            <p className="text-xs text-navy-500 uppercase tracking-wide mb-2">Key Moments</p>
            <p className="text-sm text-navy-300">{recap.keyMoments}</p>
          </div>
        )}
      </div>

      {/* Q&A */}
      <div className="bg-navy-900 border border-navy-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center space-x-2">
          <MessageSquare className="w-5 h-5 text-football-500" />
          <span>Ask a Question</span>
        </h3>
        <div className="space-y-4">
          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask about goals, shots, fouls, cards, or turning points..."
            className="w-full bg-navy-800 border border-navy-700 rounded-lg p-4 text-white placeholder-navy-500 focus:outline-none focus:border-football-500 resize-none"
            rows={3}
          />
          <button
            onClick={askQuestion}
            disabled={loading || !question.trim()}
            className="px-6 py-2 bg-football-500 text-white rounded-lg hover:bg-football-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Asking...' : 'Ask'}
          </button>
          {answer && (
            <div className="bg-navy-800 rounded-lg p-4">
              <p className="text-white">{answer}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
