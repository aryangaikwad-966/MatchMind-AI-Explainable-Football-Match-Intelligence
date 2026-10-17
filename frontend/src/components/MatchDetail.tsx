import { useEffect, useState, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Clock,
  Users,
  Target,
  TrendingUp,
  MessageSquare,
  FileText,
  BarChart3,
  Play,
  Pause,
  SkipForward,
  RotateCcw,
  Sparkles,
  Shield,
  Zap,
  Activity,
  Award,
  Send,
  Search,
  Compass
} from 'lucide-react';
import apiClient from '../api/client';
import type {
  Match,
  MatchStatistics,
  TurningPoint,
  MatchEvent,
  MatchRecap,
  ReplayState,
  MatchPeriodComparison
} from '../types';
import { getTeamBadge, getScenarioMeta } from '../utils/teamThemes';

type TabType = 'overview' | 'timeline' | 'turning' | 'replay' | 'ai';

export default function MatchDetail() {
  const { matchId } = useParams<{ matchId: string }>();
  const [match, setMatch] = useState<Match | null>(null);
  const [statistics, setStatistics] = useState<MatchStatistics | null>(null);
  const [comparison, setComparison] = useState<MatchPeriodComparison | null>(null);
  const [turningPoints, setTurningPoints] = useState<TurningPoint[]>([]);
  const [events, setEvents] = useState<MatchEvent[]>([]);
  const [recap, setRecap] = useState<MatchRecap | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('overview');
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
    const timer = window.setTimeout(async () => {
      try {
        const nextState = await apiClient.getReplay(matchId, replay.eventSequence + 1);
        setReplay(nextState);
        if (nextState.eventSequence >= events.length) {
          setPlaying(false);
        }
      } catch (e) {
        console.error('Replay step error', e);
        setPlaying(false);
      }
    }, playbackMs);
    return () => window.clearTimeout(timer);
  }, [playing, replay, matchId, events.length, playbackMs]);

  const loadMatchData = async (id: string) => {
    try {
      setLoading(true);
      const [matchData, statsData, comparisonData, turningPointsData, eventsData, initialReplay] = await Promise.all([
        apiClient.getMatch(id),
        apiClient.getMatchStatistics(id),
        apiClient.getMatchComparison(id).catch(() => null),
        apiClient.getTurningPoints(id),
        apiClient.getMatchEvents(id),
        apiClient.getReplay(id, 0).catch(() => null)
      ]);
      setMatch(matchData);
      setStatistics(statsData);
      setComparison(comparisonData);
      setTurningPoints(turningPointsData);
      setEvents(eventsData);
      setReplay(initialReplay);
      setError(null);
    } catch (err) {
      setError('Failed to load match data. Please ensure the backend is running.');
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

  // Extract goal events for scoreboard goal ticker
  const goalEvents = useMemo(() => {
    return events.filter(e => e.isGoal && !e.isDisallowed);
  }, [events]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-24 space-y-4">
        <div className="relative">
          <div className="w-14 h-14 rounded-full border-2 border-navy-700 border-t-football-400 animate-spin" />
          <div className="absolute inset-0 flex items-center justify-center">
            <Activity className="w-6 h-6 text-football-400 animate-pulse" />
          </div>
        </div>
        <p className="text-sm font-medium text-navy-300 tracking-wide animate-pulse">
          Reconstructing Match Intelligence & Tactical Dynamics…
        </p>
      </div>
    );
  }

  if (error || !match) {
    return (
      <div className="glass-panel border-red-500/30 bg-red-950/20 p-8 rounded-2xl max-w-2xl mx-auto text-center space-y-4">
        <div className="w-12 h-12 rounded-full bg-red-500/10 border border-red-500/30 flex items-center justify-center mx-auto text-red-400">
          <Shield className="w-6 h-6" />
        </div>
        <h3 className="text-xl font-bold text-white">Match Unavailable</h3>
        <p className="text-red-300 text-sm">{error || 'Could not find the requested match.'}</p>
        <Link
          to="/"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-navy-800 hover:bg-navy-700 text-white text-sm font-medium rounded-xl border border-navy-600 transition"
        >
          <ArrowLeft className="w-4 h-4" /> Return to Match Dashboard
        </Link>
      </div>
    );
  }

  const homeTheme = getTeamBadge(match.homeTeamId, match.homeTeamName);
  const awayTheme = getTeamBadge(match.awayTeamId, match.awayTeamName);
  const scenarioMeta = getScenarioMeta(match.scenarioType);
  const ScenarioIcon = scenarioMeta.icon;

  return (
    <div className="space-y-6 pb-12">
      {/* Top Breadcrumb & Quick Actions */}
      <div className="flex items-center justify-between">
        <Link
          to="/"
          className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium text-navy-200 hover:text-white bg-navy-800/80 hover:bg-navy-800 border border-navy-700 transition focus-visible:ring-2 focus-visible:ring-football-400"
          aria-label="Back to matches"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>All Matches</span>
        </Link>
        <div className="flex items-center gap-2">
          <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border ${scenarioMeta.color}`}>
            <ScenarioIcon className="w-3.5 h-3.5" />
            <span>{scenarioMeta.label}</span>
          </span>
          {match.isSynthetic && (
            <span className="px-2.5 py-1 rounded-full text-xs font-mono font-medium bg-purple-500/10 text-purple-300 border border-purple-500/30">
              SYNTHETIC ENGINE
            </span>
          )}
        </div>
      </div>

      {/* Stadium Billboard Scoreboard */}
      <div className="relative rounded-2xl overflow-hidden glass-panel border-navy-700 p-6 sm:p-8 pitch-pattern">
        {/* Ambient team color glows */}
        <div className={`absolute top-0 left-0 w-72 h-72 bg-gradient-to-br ${homeTheme.gradient} opacity-20 blur-3xl pointer-events-none`} />
        <div className={`absolute bottom-0 right-0 w-72 h-72 bg-gradient-to-tl ${awayTheme.gradient} opacity-20 blur-3xl pointer-events-none`} />

        <div className="relative z-10 flex flex-col items-center">
          {/* Competition & Meta bar */}
          <div className="flex items-center gap-2 text-xs font-medium text-navy-300 mb-6 bg-navy-950/70 border border-navy-700/80 px-4 py-1.5 rounded-full backdrop-blur-md">
            <span className="text-football-400 font-semibold">{match.competitionName}</span>
            <span>•</span>
            <span>{match.seasonLabel || 'Season 2025/26'}</span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <Clock className="w-3.5 h-3.5 text-navy-400" />
              {new Date(match.matchDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
            </span>
          </div>

          {/* Score & Teams Grid */}
          <div className="w-full grid grid-cols-1 md:grid-cols-7 items-center gap-6">
            {/* Home Team */}
            <div className="md:col-span-3 flex flex-col md:flex-row items-center justify-end gap-4 text-center md:text-right">
              <div className="order-2 md:order-1">
                <span className="text-xs uppercase tracking-wider font-semibold text-navy-400">Home</span>
                <h1 className="text-2xl sm:text-3xl font-display font-extrabold text-white tracking-tight">
                  {match.homeTeamName}
                </h1>
                <p className="text-xs text-navy-300 font-mono mt-0.5">{match.homeTeamId}</p>
              </div>
              <div
                className={`order-1 md:order-2 w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-gradient-to-br ${homeTheme.gradient} border-2 ${homeTheme.border} flex items-center justify-center font-display font-black text-2xl sm:text-3xl text-white shadow-xl flex-shrink-0`}
              >
                {homeTheme.initials}
              </div>
            </div>

            {/* Score Centerpiece */}
            <div className="md:col-span-1 flex flex-col items-center justify-center">
              <div className="bg-navy-950/90 border border-navy-700/90 rounded-2xl px-6 py-3 shadow-2xl backdrop-blur-md flex items-center gap-3">
                <span className={`text-4xl sm:text-5xl font-mono font-black ${match.homeScore > match.awayScore ? 'text-football-400' : 'text-white'}`}>
                  {match.homeScore}
                </span>
                <span className="text-2xl font-mono text-navy-500 font-light">:</span>
                <span className={`text-4xl sm:text-5xl font-mono font-black ${match.awayScore > match.homeScore ? 'text-football-400' : 'text-white'}`}>
                  {match.awayScore}
                </span>
              </div>
              <div className="mt-2.5 flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-navy-900 border border-navy-700 text-xs font-semibold text-emerald-400">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span>{match.status}</span>
              </div>
            </div>

            {/* Away Team */}
            <div className="md:col-span-3 flex flex-col md:flex-row items-center justify-start gap-4 text-center md:text-left">
              <div
                className={`w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-gradient-to-br ${awayTheme.gradient} border-2 ${awayTheme.border} flex items-center justify-center font-display font-black text-2xl sm:text-3xl text-white shadow-xl flex-shrink-0`}
              >
                {awayTheme.initials}
              </div>
              <div>
                <span className="text-xs uppercase tracking-wider font-semibold text-navy-400">Away</span>
                <h1 className="text-2xl sm:text-3xl font-display font-extrabold text-white tracking-tight">
                  {match.awayTeamName}
                </h1>
                <p className="text-xs text-navy-300 font-mono mt-0.5">{match.awayTeamId}</p>
              </div>
            </div>
          </div>

          {/* Goal Scorers Timeline Ticker */}
          {goalEvents.length > 0 && (
            <div className="mt-6 pt-5 border-t border-navy-700/80 w-full flex flex-wrap items-center justify-center gap-2 sm:gap-3">
              <span className="text-xs uppercase tracking-wider font-bold text-navy-400 flex items-center gap-1.5">
                <span>⚽</span>
                <span>Goals:</span>
              </span>
              {goalEvents.map((g) => {
                const isHomeGoal = g.teamId === match.homeTeamId;
                const teamBadge = isHomeGoal ? homeTheme : awayTheme;
                return (
                  <a
                    key={g.eventId}
                    href={`#event-${g.eventId}`}
                    onClick={() => setActiveTab('timeline')}
                    className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-navy-900/90 hover:bg-navy-800 text-navy-200 hover:text-white border border-navy-700 transition"
                    title={`Click to view goal at ${g.displayClock}`}
                  >
                    <span className="font-mono text-football-400 font-bold">{g.displayClock}</span>
                    <span className="font-semibold text-white">{g.playerName || 'Goal'}</span>
                    <span className={`px-1.5 py-0.2 rounded text-[10px] font-mono ${teamBadge.text}`}>
                      ({isHomeGoal ? match.homeTeamName.slice(0, 3).toUpperCase() : match.awayTeamName.slice(0, 3).toUpperCase()})
                    </span>
                    {g.isPenalty && <span className="text-[10px] text-amber-400 font-bold">(P)</span>}
                    {g.isOwnGoal && <span className="text-[10px] text-rose-400 font-bold">(OG)</span>}
                  </a>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Modern Segmented Navigation Tabs */}
      <div className="glass-panel border-navy-700/80 p-1.5 rounded-2xl" role="tablist" aria-label="Match views">
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-1">
          {[
            { id: 'overview' as const, label: 'Overview & Stats', icon: BarChart3, count: null },
            { id: 'timeline' as const, label: 'Timeline', icon: Clock, count: events.length },
            { id: 'turning' as const, label: 'Turning Points', icon: TrendingUp, count: turningPoints.length },
            { id: 'replay' as const, label: 'Synthetic Replay', icon: Play, count: 'Sim' },
            { id: 'ai' as const, label: 'AI Match Intelligence', icon: Sparkles, count: recap?.aiMode || 'AI' },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                role="tab"
                aria-selected={isActive}
                aria-controls={`tabpanel-${tab.id}`}
                className={`flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl text-xs sm:text-sm font-semibold transition-all focus-visible:ring-2 focus-visible:ring-football-400 ${
                  isActive
                    ? 'bg-gradient-to-r from-football-500 to-emerald-600 text-white shadow-lg shadow-football-900/30 font-bold'
                    : 'text-navy-300 hover:text-white hover:bg-navy-800/60'
                }`}
              >
                <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-navy-400'}`} />
                <span>{tab.label}</span>
                {tab.count !== null && (
                  <span
                    className={`text-[10px] px-1.5 py-0.5 rounded-full font-mono ${
                      isActive ? 'bg-white/20 text-white' : 'bg-navy-800 text-navy-400'
                    }`}
                  >
                    {tab.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Tab Panels */}
      <div className="transition-opacity duration-200">
        {activeTab === 'overview' && statistics && (
          <OverviewTab
            match={match}
            statistics={statistics}
            comparison={comparison}
            homeTheme={homeTheme}
            awayTheme={awayTheme}
          />
        )}

        {activeTab === 'timeline' && (
          <TimelineTab
            events={events}
            match={match}
            homeTheme={homeTheme}
            awayTheme={awayTheme}
          />
        )}

        {activeTab === 'turning' && (
          <TurningPointsTab
            turningPoints={turningPoints}
            onSelectEvent={() => setActiveTab('timeline')}
          />
        )}

        {activeTab === 'replay' && (
          <ReplayTab
            match={match}
            matchId={matchId!}
            events={events}
            replay={replay}
            setReplay={setReplay}
            playing={playing}
            setPlaying={setPlaying}
            playbackMs={playbackMs}
            setPlaybackMs={setPlaybackMs}
            homeTheme={homeTheme}
            awayTheme={awayTheme}
          />
        )}

        {activeTab === 'ai' && recap && matchId && (
          <AITab
            recap={recap}
            mode={mode}
            onModeChange={setMode}
            match={match}
            matchId={matchId}
            preference={preference}
            setPreference={setPreference}
            onJumpToEvent={() => setActiveTab('timeline')}
          />
        )}
      </div>
    </div>
  );
}

/* =========================================================================
   OVERVIEW & STATS TAB (Bilateral percentage bars + Half comparison)
   ========================================================================= */
function OverviewTab({
  match,
  statistics,
  comparison,
  homeTheme,
  awayTheme,
}: {
  match: Match;
  statistics: MatchStatistics;
  comparison: MatchPeriodComparison | null;
  homeTheme: any;
  awayTheme: any;
}) {
  return (
    <div className="space-y-6">
      {/* Primary Match Stat Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <SummaryMetric
          title="Total Shots"
          homeValue={statistics.homeShots}
          awayValue={statistics.awayShots}
          icon={Target}
        />
        <SummaryMetric
          title="Shots on Target"
          homeValue={statistics.homeShotsOnTarget}
          awayValue={statistics.awayShotsOnTarget}
          icon={Zap}
        />
        <SummaryMetric
          title="Corner Kicks"
          homeValue={statistics.homeCorners}
          awayValue={statistics.awayCorners}
          icon={Compass}
        />
        <SummaryMetric
          title="Fouls Committed"
          homeValue={statistics.homeFouls}
          awayValue={statistics.awayFouls}
          icon={Shield}
        />
      </div>

      {/* Bilateral Comparison Panel */}
      <div className="glass-panel border-navy-700/80 rounded-2xl p-6 sm:p-8 space-y-6">
        <div className="flex items-center justify-between pb-4 border-b border-navy-700/80">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-football-500/10 border border-football-500/30 flex items-center justify-center text-football-400">
              <BarChart3 className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">Full-Match Head-to-Head Statistics</h3>
              <p className="text-xs text-navy-300">Bilateral comparative metrics across 90+ minutes</p>
            </div>
          </div>
          <div className="hidden sm:flex items-center gap-4 text-xs font-semibold">
            <span className="flex items-center gap-1.5 text-white">
              <span className={`w-3 h-3 rounded-full ${homeTheme.bg}`} />
              {match.homeTeamName}
            </span>
            <span className="text-navy-500">vs</span>
            <span className="flex items-center gap-1.5 text-white">
              <span className={`w-3 h-3 rounded-full ${awayTheme.bg}`} />
              {match.awayTeamName}
            </span>
          </div>
        </div>

        <div className="space-y-4">
          <BilateralStatRow label="Total Shots" home={statistics.homeShots} away={statistics.awayShots} />
          <BilateralStatRow label="Shots on Target" home={statistics.homeShotsOnTarget} away={statistics.awayShotsOnTarget} />
          <BilateralStatRow label="Goalkeeper Saves" home={statistics.homeSaves} away={statistics.awaySaves} />
          <BilateralStatRow label="Corners" home={statistics.homeCorners} away={statistics.awayCorners} />
          <BilateralStatRow label="Fouls" home={statistics.homeFouls} away={statistics.awayFouls} invertBetter />
          <BilateralStatRow label="Offsides" home={statistics.homeOffsides} away={statistics.awayOffsides} invertBetter />
          <BilateralStatRow label="Yellow Cards" home={statistics.homeYellowCards} away={statistics.awayYellowCards} invertBetter />
          <BilateralStatRow label="Red Cards" home={statistics.homeRedCards} away={statistics.awayRedCards} invertBetter />
          <BilateralStatRow label="Substitutions Made" home={statistics.homeSubstitutions} away={statistics.awaySubstitutions} />
        </div>
      </div>

      {/* Half-by-Half Tactical Shift (if available from API) */}
      {comparison && (
        <div className="glass-panel border-navy-700/80 rounded-2xl p-6 sm:p-8 space-y-6">
          <div className="flex items-center gap-3 pb-4 border-b border-navy-700/80">
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
              <Activity className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">Half-by-Half Momentum Dynamics</h3>
              <p className="text-xs text-navy-300">Tactical adjustments and shift between 1st Half vs 2nd Half</p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* 1st Half Card */}
            <div className="bg-navy-900/80 border border-navy-700/80 rounded-xl p-5 space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-navy-700">
                <span className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-sky-400" />
                  1st Half (0' – 45+')
                </span>
                <span className="font-mono text-base font-bold text-white bg-navy-800 px-3 py-0.5 rounded-lg border border-navy-700">
                  {comparison.firstHalf.homeScore} – {comparison.firstHalf.awayScore}
                </span>
              </div>
              <div className="space-y-2 text-xs">
                <HalfStatItem label="Shots" home={comparison.firstHalf.homeShots} away={comparison.firstHalf.awayShots} />
                <HalfStatItem label="On Target" home={comparison.firstHalf.homeShotsOnTarget} away={comparison.firstHalf.awayShotsOnTarget} />
                <HalfStatItem label="Corners" home={comparison.firstHalf.homeCorners} away={comparison.firstHalf.awayCorners} />
                <HalfStatItem label="Fouls" home={comparison.firstHalf.homeFouls} away={comparison.firstHalf.awayFouls} />
              </div>
            </div>

            {/* 2nd Half Card */}
            <div className="bg-navy-900/80 border border-navy-700/80 rounded-xl p-5 space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-navy-700">
                <span className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-emerald-400" />
                  2nd Half (46' – FT)
                </span>
                <span className="font-mono text-base font-bold text-white bg-navy-800 px-3 py-0.5 rounded-lg border border-navy-700">
                  {comparison.secondHalf.homeScore} – {comparison.secondHalf.awayScore}
                </span>
              </div>
              <div className="space-y-2 text-xs">
                <HalfStatItem label="Shots" home={comparison.secondHalf.homeShots} away={comparison.secondHalf.awayShots} />
                <HalfStatItem label="On Target" home={comparison.secondHalf.homeShotsOnTarget} away={comparison.secondHalf.awayShotsOnTarget} />
                <HalfStatItem label="Corners" home={comparison.secondHalf.homeCorners} away={comparison.secondHalf.awayCorners} />
                <HalfStatItem label="Fouls" home={comparison.secondHalf.homeFouls} away={comparison.secondHalf.awayFouls} />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function SummaryMetric({
  title,
  homeValue,
  awayValue,
  icon: Icon,
}: {
  title: string;
  homeValue: number;
  awayValue: number;
  icon: any;
}) {
  return (
    <div className="glass-panel border-navy-700/80 rounded-xl p-5 hover:border-navy-600 transition">
      <div className="flex items-center justify-between text-navy-400 mb-2">
        <span className="text-xs uppercase tracking-wider font-semibold text-navy-300">{title}</span>
        <Icon className="w-4 h-4 text-football-400" />
      </div>
      <div className="flex items-baseline justify-between">
        <div className="text-center">
          <span className="text-2xl font-mono font-bold text-white">{homeValue}</span>
          <span className="text-[10px] block text-navy-400 uppercase mt-0.5 font-semibold">Home</span>
        </div>
        <span className="text-sm text-navy-500 font-mono font-light">vs</span>
        <div className="text-center">
          <span className="text-2xl font-mono font-bold text-white">{awayValue}</span>
          <span className="text-[10px] block text-navy-400 uppercase mt-0.5 font-semibold">Away</span>
        </div>
      </div>
    </div>
  );
}

function BilateralStatRow({
  label,
  home,
  away,
  invertBetter = false,
}: {
  label: string;
  home: number;
  away: number;
  invertBetter?: boolean;
}) {
  const total = home + away;
  const homePct = total === 0 ? 50 : Math.round((home / total) * 100);
  const awayPct = total === 0 ? 50 : 100 - homePct;

  const homeLead = invertBetter ? home < away : home > away;
  const awayLead = invertBetter ? away < home : away > home;

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-xs font-semibold">
        <span className={`font-mono text-sm ${homeLead ? 'text-football-400 font-bold' : 'text-navy-200'}`}>
          {home}
        </span>
        <span className="text-navy-300 uppercase tracking-wider text-[11px] font-medium">{label}</span>
        <span className={`font-mono text-sm ${awayLead ? 'text-football-400 font-bold' : 'text-navy-200'}`}>
          {away}
        </span>
      </div>

      <div className="w-full h-2 rounded-full bg-navy-950 flex overflow-hidden border border-navy-800">
        <div
          style={{ width: `${homePct}%` }}
          className={`h-full transition-all duration-500 ${
            homeLead ? 'bg-football-500' : 'bg-navy-600'
          }`}
        />
        <div
          style={{ width: `${awayPct}%` }}
          className={`h-full transition-all duration-500 ${
            awayLead ? 'bg-football-400' : 'bg-navy-700'
          }`}
        />
      </div>
    </div>
  );
}

function HalfStatItem({ label, home, away }: { label: string; home: number; away: number }) {
  return (
    <div className="flex items-center justify-between py-1 border-b border-navy-800 last:border-0">
      <span className="font-mono text-white font-medium">{home}</span>
      <span className="text-navy-400">{label}</span>
      <span className="font-mono text-white font-medium">{away}</span>
    </div>
  );
}

/* =========================================================================
   TIMELINE TAB (Event filtering, clock, rich badges, anchor jumping)
   ========================================================================= */
function TimelineTab({
  events,
  match,
  homeTheme,
  awayTheme,
}: {
  events: MatchEvent[];
  match: Match;
  homeTheme: any;
  awayTheme: any;
}) {
  const [filterType, setFilterType] = useState<string>('ALL');
  const [searchFilter, setSearchFilter] = useState('');

  const filteredEvents = useMemo(() => {
    return events.filter((e) => {
      if (filterType === 'GOALS' && !e.isGoal) return false;
      if (filterType === 'CARDS' && !e.isYellowCard && !e.isRedCard) return false;
      if (filterType === 'SHOTS' && !e.eventType.toLowerCase().includes('shot')) return false;
      if (filterType === 'SUBS' && !e.isSubstitution) return false;
      if (filterType === 'FOULS' && !e.eventType.toLowerCase().includes('foul')) return false;

      if (searchFilter.trim()) {
        const query = searchFilter.toLowerCase();
        const descMatch = e.description?.toLowerCase().includes(query);
        const playerMatch = e.playerName?.toLowerCase().includes(query);
        const teamMatch = e.teamName?.toLowerCase().includes(query);
        return descMatch || playerMatch || teamMatch;
      }
      return true;
    });
  }, [events, filterType, searchFilter]);

  return (
    <div className="glass-panel border-navy-700/80 rounded-2xl p-6 sm:p-8 space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 pb-5 border-b border-navy-700/80">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center gap-2">
            <Clock className="w-5 h-5 text-football-400" />
            <span>Match Event Timeline</span>
          </h3>
          <p className="text-xs text-navy-300">
            {events.length} chronological match records reconstructed from official tracking feed
          </p>
        </div>

        {/* Filter Pills */}
        <div className="flex flex-wrap items-center gap-1.5">
          {[
            { id: 'ALL', label: `All (${events.length})` },
            { id: 'GOALS', label: 'Goals ⚽' },
            { id: 'CARDS', label: 'Cards 🟨🟥' },
            { id: 'SHOTS', label: 'Shots 🎯' },
            { id: 'SUBS', label: 'Subs 🔄' },
            { id: 'FOULS', label: 'Fouls ⚠️' },
          ].map((pill) => (
            <button
              key={pill.id}
              onClick={() => setFilterType(pill.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition focus-visible:ring-2 focus-visible:ring-football-400 ${
                filterType === pill.id
                  ? 'bg-football-500 text-white font-bold shadow-md shadow-football-900/30'
                  : 'bg-navy-800 text-navy-300 hover:text-white hover:bg-navy-700 border border-navy-700'
              }`}
            >
              {pill.label}
            </button>
          ))}
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative">
        <Search className="w-4 h-4 text-navy-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
        <input
          type="text"
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
          placeholder="Filter timeline events by player, description, or action..."
          className="w-full bg-navy-900 border border-navy-700 rounded-xl pl-10 pr-4 py-2 text-xs text-white placeholder-navy-500 focus:outline-none focus:border-football-500 focus-visible:ring-2 focus-visible:ring-football-400"
        />
        {searchFilter && (
          <button
            onClick={() => setSearchFilter('')}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-navy-400 hover:text-white text-xs"
          >
            Clear
          </button>
        )}
      </div>

      {/* Events Stream */}
      <div className="space-y-3 max-h-[36rem] overflow-y-auto styled-scroll pr-1.5">
        {filteredEvents.length === 0 ? (
          <div className="text-center py-12 text-navy-400">
            <p className="text-sm">No events match the selected filter.</p>
          </div>
        ) : (
          filteredEvents.map((event) => {
            const isHome = event.teamId === match.homeTeamId;
            const teamBadge = isHome ? homeTheme : awayTheme;

            return (
              <div
                key={event.eventId}
                id={`event-${event.eventId}`}
                className="group relative flex items-start gap-4 p-4 rounded-xl bg-navy-900/90 hover:bg-navy-800/90 border border-navy-700/80 hover:border-navy-600 transition target:ring-2 target:ring-football-400 target:bg-navy-800"
              >
                {/* Clock & Period */}
                <div className="w-16 flex-shrink-0 text-center">
                  <span className="font-mono text-base font-extrabold text-white group-hover:text-football-400 transition">
                    {event.displayClock}
                  </span>
                  <span className="block text-[10px] uppercase font-mono tracking-wider text-navy-400 font-semibold mt-0.5">
                    {event.period}
                  </span>
                </div>

                {/* Vertical Divider Indicator */}
                <div className="w-1 self-stretch rounded-full bg-navy-700 group-hover:bg-football-500 transition-colors" />

                {/* Event Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2 mb-1.5">
                    {/* Event Tag */}
                    <span
                      className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        event.isGoal
                          ? 'bg-football-500 text-white shadow-sm shadow-football-500/30'
                          : event.isRedCard
                          ? 'bg-rose-500 text-white shadow-sm shadow-rose-500/30'
                          : event.isYellowCard
                          ? 'bg-amber-400 text-navy-950 font-black'
                          : event.isSubstitution
                          ? 'bg-purple-500/20 text-purple-300 border border-purple-500/40'
                          : 'bg-navy-800 text-navy-200 border border-navy-700'
                      }`}
                    >
                      {event.isGoal && '⚽ '}
                      {event.isRedCard && '🟥 '}
                      {event.isYellowCard && '🟨 '}
                      {event.isSubstitution && '🔄 '}
                      {event.eventType}
                    </span>

                    {/* Team tag if present */}
                    {event.teamName && (
                      <span className="inline-flex items-center gap-1.5 text-xs font-medium text-navy-200">
                        <span className={`w-2 h-2 rounded-full ${teamBadge.bg}`} />
                        <span>{event.teamName}</span>
                      </span>
                    )}

                    {/* Score State Badge */}
                    <span className="ml-auto font-mono text-xs font-bold text-navy-300 bg-navy-950 px-2 py-0.5 rounded border border-navy-800">
                      {event.scoreHomeAfterEvent} – {event.scoreAwayAfterEvent}
                    </span>
                  </div>

                  <p className="text-sm text-white font-medium leading-relaxed">{event.description}</p>

                  {/* Player attribution */}
                  {event.playerName && (
                    <div className="mt-2 flex items-center gap-3 text-xs text-navy-300">
                      <span className="flex items-center gap-1">
                        <Users className="w-3.5 h-3.5 text-navy-400" />
                        <span className="text-white font-medium">{event.playerName}</span>
                      </span>
                      {event.relatedPlayerName && (
                        <span className="text-navy-400">
                          (Related: <span className="text-navy-300">{event.relatedPlayerName}</span>)
                        </span>
                      )}
                      <span className="text-[10px] font-mono text-navy-500 ml-auto">
                        #{event.eventId}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

/* =========================================================================
   TURNING POINTS TAB (Ranked impact cards, impact dial, tactical rationale)
   ========================================================================= */
function TurningPointsTab({
  turningPoints,
  onSelectEvent,
}: {
  turningPoints: TurningPoint[];
  onSelectEvent: () => void;
}) {
  return (
    <div className="glass-panel border-navy-700/80 rounded-2xl p-6 sm:p-8 space-y-6">
      <div className="pb-4 border-b border-navy-700/80">
        <h3 className="text-lg font-bold text-white flex items-center gap-2">
          <TrendingUp className="w-5 h-5 text-football-400" />
          <span>Algorithmic Turning Points & Match Deciders</span>
        </h3>
        <p className="text-xs text-navy-300">
          Ranked moments where win probability, tactical momentum, and score trajectory swung decisive amounts
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4">
        {turningPoints.length === 0 ? (
          <p className="text-sm text-navy-400 py-8 text-center">No major turning points detected for this match.</p>
        ) : (
          turningPoints.map((tp, idx) => {
            const isTopDecider = idx === 0;

            return (
              <div
                key={tp.insightId}
                className={`rounded-2xl p-6 border transition-all ${
                  isTopDecider
                    ? 'bg-gradient-to-br from-football-950/40 via-navy-900 to-navy-900 border-football-500/50 shadow-lg shadow-football-950/50'
                    : 'bg-navy-900/80 border-navy-700/80 hover:border-navy-600'
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-navy-700/60">
                  <div className="flex items-center gap-2.5">
                    <span
                      className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-black ${
                        isTopDecider
                          ? 'bg-football-500 text-white shadow-md shadow-football-500/30'
                          : 'bg-navy-800 text-football-300 border border-navy-700'
                      }`}
                    >
                      <Award className="w-3.5 h-3.5" />
                      <span>#{idx + 1} {isTopDecider ? 'Decisive Game Changer' : 'Key Pivot'}</span>
                    </span>
                    <span className="text-sm font-semibold text-white">{tp.type}</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs px-2.5 py-1 rounded-lg bg-navy-950 border border-navy-800 text-white font-bold">
                      {tp.minute}' Minute
                    </span>
                    <span className="font-mono text-xs px-2.5 py-1 rounded-lg bg-football-500/10 border border-football-500/30 text-football-300 font-bold">
                      Score: {tp.scoreChange}
                    </span>
                  </div>
                </div>

                <div className="mt-4 space-y-3">
                  <p className="text-base text-white font-semibold leading-relaxed">{tp.explanation}</p>
                  <p className="text-sm text-navy-300 leading-relaxed">{tp.rankingRationale}</p>
                </div>

                {/* Grounding Evidence Links */}
                {tp.eventIds && tp.eventIds.length > 0 && (
                  <div className="mt-5 pt-4 border-t border-navy-700/60 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs uppercase tracking-wider font-semibold text-navy-400">
                        Grounding Evidence:
                      </span>
                      {tp.eventIds.map((id) => (
                        <a
                          key={id}
                          href={`#event-${id}`}
                          onClick={onSelectEvent}
                          className="font-mono text-xs px-2.5 py-1 rounded-lg bg-navy-800 hover:bg-football-500/20 text-football-300 hover:text-football-200 border border-navy-700 hover:border-football-500/50 transition"
                        >
                          Event #{id}
                        </a>
                      ))}
                    </div>

                    <span className="text-xs font-mono text-navy-400">
                      Impact Weight: <strong className="text-football-400">{tp.score.toFixed(1)}/10.0</strong>
                    </span>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

/* =========================================================================
   SYNTHETIC REPLAY TAB (Full pitch visualizer, clock scrub, playback controls)
   ========================================================================= */
function ReplayTab({
  match,
  matchId,
  events,
  replay,
  setReplay,
  playing,
  setPlaying,
  playbackMs,
  setPlaybackMs,
  homeTheme,
  awayTheme,
}: {
  match: Match;
  matchId: string;
  events: MatchEvent[];
  replay: ReplayState | null;
  setReplay: (v: ReplayState) => void;
  playing: boolean;
  setPlaying: (v: boolean) => void;
  playbackMs: number;
  setPlaybackMs: (v: number) => void;
  homeTheme: any;
  awayTheme: any;
}) {
  const loadSequence = async (seq: number) => {
    try {
      const state = await apiClient.getReplay(matchId, seq);
      setReplay(state);
    } catch (e) {
      console.error(e);
    }
  };

  const currentEvent = replay?.events && replay.events.length > 0
    ? replay.events[replay.events.length - 1]
    : null;

  const currentSequence = replay?.eventSequence ?? 0;
  const totalEvents = events.length;
  const progressPct = totalEvents === 0 ? 0 : Math.round((currentSequence / totalEvents) * 100);

  return (
    <div className="glass-panel border-navy-700/80 rounded-2xl p-6 sm:p-8 space-y-6">
      {/* Header and Controls */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 pb-5 border-b border-navy-700/80">
        <div>
          <h3 className="text-lg font-bold text-white flex items-center gap-2">
            <Play className="w-5 h-5 text-football-400" />
            <span>Synthetic Tactical Match Replay</span>
          </h3>
          <p className="text-xs text-navy-300">
            Event-by-event chronological match replay with live score transitions and tactical insight overlays
          </p>
        </div>

        {/* Playback Controls */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Speed Selector */}
          <select
            aria-label="Replay speed"
            value={playbackMs}
            onChange={(e) => setPlaybackMs(Number(e.target.value))}
            className="px-3 py-2 bg-navy-800 border border-navy-700 text-white text-xs font-semibold rounded-xl focus-visible:ring-2 focus-visible:ring-football-400"
          >
            <option value={1400}>0.5× Speed</option>
            <option value={700}>1.0× Speed</option>
            <option value={350}>2.0× Speed</option>
          </select>

          {/* Reset */}
          <button
            onClick={() => {
              setPlaying(false);
              loadSequence(0);
            }}
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-navy-800 hover:bg-navy-700 border border-navy-700 text-white text-xs font-semibold rounded-xl transition focus-visible:ring-2 focus-visible:ring-football-400"
            aria-label="Reset replay"
          >
            <RotateCcw className="w-4 h-4 text-navy-300" />
            <span>Reset</span>
          </button>

          {/* Step */}
          <button
            onClick={() => {
              setPlaying(false);
              loadSequence(Math.min(currentSequence + 1, totalEvents));
            }}
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-navy-800 hover:bg-navy-700 border border-navy-700 text-white text-xs font-semibold rounded-xl transition focus-visible:ring-2 focus-visible:ring-football-400"
            aria-label="Step forward one event"
          >
            <SkipForward className="w-4 h-4 text-navy-300" />
            <span>Step</span>
          </button>

          {/* Play/Pause Button */}
          <button
            onClick={() => setPlaying(!playing)}
            className={`inline-flex items-center gap-2 px-5 py-2 rounded-xl text-xs font-bold text-white transition shadow-lg ${
              playing
                ? 'bg-amber-600 hover:bg-amber-500 shadow-amber-900/30'
                : 'bg-football-500 hover:bg-football-600 shadow-football-900/30'
            }`}
            aria-label={playing ? 'Pause replay' : 'Play replay'}
          >
            {playing ? (
              <>
                <Pause className="w-4 h-4" />
                <span>Pause</span>
              </>
            ) : (
              <>
                <Play className="w-4 h-4 fill-current" />
                <span>Play Simulation</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Replay Scrubber Progress Bar */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs font-mono">
          <span className="text-football-400 font-bold">
            Simulated Minute: {replay?.simulatedMinute ?? 0}'
          </span>
          <span className="text-navy-300">
            Event {currentSequence} of {totalEvents} ({progressPct}%)
          </span>
        </div>
        <input
          type="range"
          min={0}
          max={totalEvents}
          value={currentSequence}
          onChange={(e) => {
            setPlaying(false);
            loadSequence(Number(e.target.value));
          }}
          className="w-full h-2 bg-navy-800 rounded-lg appearance-none cursor-pointer accent-football-500 focus-visible:ring-2 focus-visible:ring-football-400"
          aria-label="Replay scrub slider"
        />
      </div>

      {/* Visual Simulated Football Pitch */}
      <div className="relative rounded-2xl overflow-hidden bg-gradient-to-b from-emerald-950 via-emerald-900/60 to-emerald-950 border-2 border-emerald-800/60 p-6 min-h-[18rem] flex flex-col justify-between shadow-2xl">
        {/* Pitch Field Markings (Center line, circle, penalty boxes) */}
        <div className="absolute inset-x-0 top-1/2 -translate-y-1/2 border-t-2 border-emerald-600/30" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-32 h-32 rounded-full border-2 border-emerald-600/30 pointer-events-none" />
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-48 h-20 border-b-2 border-x-2 border-emerald-600/30 rounded-b-xl pointer-events-none" />
        <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-48 h-20 border-t-2 border-x-2 border-emerald-600/30 rounded-t-xl pointer-events-none" />

        {/* Pitch Top Bar: Simulated Score & Minute Billboard */}
        <div className="relative z-10 flex items-center justify-between">
          <div className="flex items-center gap-2 bg-navy-950/90 border border-navy-700 px-4 py-2 rounded-xl backdrop-blur-md">
            <span className={`w-3 h-3 rounded-full ${homeTheme.bg}`} />
            <span className="text-xs font-bold text-white">{match.homeTeamName}</span>
            <span className="font-mono text-base font-extrabold text-football-400 px-1">
              {replay?.homeScore ?? 0} : {replay?.awayScore ?? 0}
            </span>
            <span className="text-xs font-bold text-white">{match.awayTeamName}</span>
            <span className={`w-3 h-3 rounded-full ${awayTheme.bg}`} />
          </div>

          <div className="bg-navy-950/90 border border-navy-700 px-3.5 py-1.5 rounded-xl font-mono text-xs font-bold text-emerald-300">
            ⏱ {replay?.simulatedMinute ?? 0}' MINUTE
          </div>
        </div>

        {/* Center Live Event Overlay */}
        <div className="relative z-10 my-6 max-w-xl mx-auto w-full">
          {currentEvent ? (
            <div className="glass-panel border-football-500/40 bg-navy-950/85 p-5 rounded-2xl shadow-2xl space-y-2 backdrop-blur-md">
              <div className="flex items-center justify-between">
                <span
                  className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-bold ${
                    currentEvent.isGoal
                      ? 'bg-football-500 text-white'
                      : currentEvent.isRedCard
                      ? 'bg-rose-500 text-white'
                      : currentEvent.isYellowCard
                      ? 'bg-amber-400 text-black'
                      : 'bg-navy-800 text-navy-200 border border-navy-700'
                  }`}
                >
                  {currentEvent.isGoal && '⚽ '}
                  {currentEvent.isRedCard && '🟥 '}
                  {currentEvent.isYellowCard && '🟨 '}
                  {currentEvent.eventType}
                </span>
                <span className="font-mono text-xs text-navy-300">{currentEvent.displayClock}</span>
              </div>
              <p className="text-sm font-semibold text-white leading-snug">{currentEvent.description}</p>
              {currentEvent.playerName && (
                <p className="text-xs text-navy-300">
                  Player: <strong className="text-white">{currentEvent.playerName}</strong> ({currentEvent.teamName})
                </p>
              )}
            </div>
          ) : (
            <div className="text-center py-6 bg-navy-950/70 border border-navy-800 rounded-2xl backdrop-blur-md">
              <p className="text-xs font-medium text-navy-300">Press Play or Step to begin the match simulation</p>
            </div>
          )}
        </div>

        {/* Pitch Bottom Status */}
        <div className="relative z-10 flex items-center justify-between text-[11px] text-emerald-200/80 font-mono">
          <span>MatchMind Synthetic Engine</span>
          <span>Feed: Event Stream Sequence {currentSequence}</span>
        </div>
      </div>

      {/* Active Insight Highlights during replay */}
      {replay?.insights && replay.insights.length > 0 && (
        <div className="space-y-3">
          <h4 className="text-xs uppercase tracking-wider font-bold text-navy-300">
            Detected Turning Points at this Replay Step:
          </h4>
          {replay.insights.map((i) => (
            <div
              key={i.insightId}
              className="p-4 rounded-xl bg-navy-900 border-l-4 border-football-500 border border-navy-700"
            >
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold text-football-400">⚡ {i.type}</span>
                <span className="font-mono text-navy-300">{i.minute}'</span>
              </div>
              <p className="text-white text-sm font-medium mt-1">{i.explanation}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/* =========================================================================
   AI MATCH INTELLIGENCE TAB (Fan/Analyst mode, recap, natural Q&A)
   ========================================================================= */
function AITab({
  recap,
  mode,
  onModeChange,
  match,
  matchId,
  preference,
  setPreference,
  onJumpToEvent,
}: {
  recap: MatchRecap;
  mode: 'FAN' | 'ANALYST';
  onModeChange: (mode: 'FAN' | 'ANALYST') => void;
  match: Match;
  matchId: string;
  preference: string;
  setPreference: (value: string) => void;
  onJumpToEvent: () => void;
}) {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState<string | null>(null);
  const [evidenceIds, setEvidenceIds] = useState<string[]>([]);
  const [responseAiMode, setResponseAiMode] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const suggestedQuestions = [
    'Who won the match and what was the score?',
    'Who scored the goals and at what minutes?',
    'What was the key turning point in the match?',
    'How did the momentum shift in the second half?',
    'Were there any red cards or controversial moments?',
    `How did ${match.homeTeamName} perform overall?`,
  ];

  const askQuestion = async (customQ?: string) => {
    const q = (customQ !== undefined ? customQ : question).trim();
    if (!q) return;
    if (customQ !== undefined) setQuestion(customQ);
    try {
      setLoading(true);
      const response = await apiClient.askQuestion(matchId, { question: q, mode, preference });
      setAnswer(response.answer);
      setEvidenceIds(response.evidenceEventIds || []);
      setResponseAiMode(response.aiMode);
    } catch (err) {
      console.error('Failed to ask question:', err);
      setAnswer('Sorry, we could not generate an answer at this time. Please check your backend connection.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Explanation Mode Switcher Bar */}
      <div className="glass-panel border-navy-700/80 rounded-2xl p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <span className="text-xs uppercase tracking-wider font-bold text-navy-400 block mb-1">
            Explainability Persona
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => onModeChange('FAN')}
              className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-football-400 ${
                mode === 'FAN'
                  ? 'bg-football-500 text-white shadow-md shadow-football-900/30'
                  : 'bg-navy-800 text-navy-300 hover:text-white border border-navy-700'
              }`}
            >
              <span>🎉</span>
              <span>Fan Persona</span>
            </button>
            <button
              onClick={() => onModeChange('ANALYST')}
              className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-football-400 ${
                mode === 'ANALYST'
                  ? 'bg-football-500 text-white shadow-md shadow-football-900/30'
                  : 'bg-navy-800 text-navy-300 hover:text-white border border-navy-700'
              }`}
            >
              <span>📊</span>
              <span>Analyst Persona</span>
            </button>
          </div>
          <p className="text-xs text-navy-400 mt-2">
            {mode === 'FAN'
              ? 'High-energy storytelling, emotion-focused narrative, and accessible football terminology'
              : 'Data-grounded tactical phases, defensive block analysis, and expected impact metrics'}
          </p>
        </div>

        {/* Engine Status Badge & Preference Input */}
        <div className="w-full md:w-auto flex flex-col items-start md:items-end gap-2">
          <div className="flex items-center gap-2 text-xs font-semibold bg-navy-950/80 px-3 py-1.5 rounded-full border border-navy-700">
            <span
              className={`w-2.5 h-2.5 rounded-full ${
                recap.aiMode === 'GEMINI' ? 'bg-emerald-400' : 'bg-amber-400'
              }`}
            />
            <span className="text-navy-200">
              Intelligence: <strong className="text-white">{recap.aiMode}</strong>
            </span>
          </div>

          <div className="w-full flex items-center gap-2">
            <input
              value={preference}
              onChange={(e) => setPreference(e.target.value)}
              placeholder="Focus on player or team..."
              className="text-xs bg-navy-900 border border-navy-700 rounded-xl px-3 py-1.5 text-white placeholder-navy-500 focus:outline-none focus:border-football-500 focus-visible:ring-2 focus-visible:ring-football-400 w-full sm:w-48"
            />
          </div>
        </div>
      </div>

      {/* Match Recap Narrative */}
      <div className="glass-panel border-navy-700/80 rounded-2xl p-6 sm:p-8 space-y-6">
        <div className="flex items-center gap-3 pb-4 border-b border-navy-700/80">
          <div className="w-9 h-9 rounded-xl bg-football-500/10 border border-football-500/30 flex items-center justify-center text-football-400">
            <FileText className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">Match Narrative Recap</h3>
            <p className="text-xs text-navy-300">Executive match synopsis grounded in event chronology</p>
          </div>
        </div>

        <p className="text-white text-base leading-relaxed font-normal">{recap.recap}</p>

        {/* Persona Specific Take */}
        {mode === 'FAN' && recap.fanExplanation && (
          <div className="bg-gradient-to-r from-football-950/40 to-navy-900 border border-football-500/40 rounded-xl p-5 space-y-1.5">
            <span className="text-xs uppercase tracking-wider font-bold text-football-400">
              Fan Perspective:
            </span>
            <p className="text-sm text-navy-100 leading-relaxed">{recap.fanExplanation}</p>
          </div>
        )}

        {mode === 'ANALYST' && recap.analystExplanation && (
          <div className="bg-gradient-to-r from-navy-900 to-navy-950 border border-navy-700 rounded-xl p-5 space-y-1.5">
            <span className="text-xs uppercase tracking-wider font-bold text-sky-400">
              Tactical Analyst Breakdown:
            </span>
            <p className="text-sm text-navy-100 leading-relaxed">{recap.analystExplanation}</p>
          </div>
        )}

        {/* Key Moments */}
        {recap.keyMoments && (
          <div className="pt-4 border-t border-navy-700/80">
            <span className="text-xs uppercase tracking-wider font-bold text-navy-400 block mb-2">
              Key Decisive Moments:
            </span>
            <p className="text-sm text-navy-200 leading-relaxed">{recap.keyMoments}</p>
          </div>
        )}
      </div>

      {/* Natural Language Question & Answer Assistant */}
      <div className="glass-panel border-navy-700/80 rounded-2xl p-6 sm:p-8 space-y-5">
        <div className="flex items-center gap-3 pb-3 border-b border-navy-700/80">
          <div className="w-9 h-9 rounded-xl bg-football-500/10 border border-football-500/30 flex items-center justify-center text-football-400">
            <MessageSquare className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">Ask MatchMind AI</h3>
            <p className="text-xs text-navy-300">
              Query goals, momentum swings, red cards, half-time shifts, or player performance
            </p>
          </div>
        </div>

        {/* Suggested Prompts */}
        <div className="space-y-2">
          <span className="text-xs font-semibold text-navy-400">Prompt Suggestions:</span>
          <div className="flex flex-wrap gap-2">
            {suggestedQuestions.map((sq, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => askQuestion(sq)}
                className="text-xs bg-navy-800 hover:bg-navy-700 text-navy-200 hover:text-white border border-navy-700 rounded-full px-3 py-1.5 transition text-left focus-visible:ring-2 focus-visible:ring-football-400"
              >
                {sq}
              </button>
            ))}
          </div>
        </div>

        {/* Input Box */}
        <div className="space-y-3">
          <div className="relative">
            <textarea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  askQuestion();
                }
              }}
              placeholder="Ask anything about match results, goals, turning points, or player stats..."
              className="w-full bg-navy-900 border border-navy-700 rounded-xl p-4 text-white text-sm placeholder-navy-500 focus:outline-none focus:border-football-500 focus-visible:ring-2 focus-visible:ring-football-400 resize-none min-h-[5rem]"
              rows={3}
            />
            <button
              onClick={() => askQuestion()}
              disabled={loading || !question.trim()}
              className="absolute right-3 bottom-4 inline-flex items-center gap-1.5 px-4 py-2 bg-football-500 hover:bg-football-600 text-white text-xs font-bold rounded-lg transition disabled:opacity-40 disabled:cursor-not-allowed shadow-md"
            >
              {loading ? (
                <>
                  <Activity className="w-3.5 h-3.5 animate-spin" />
                  <span>Thinking…</span>
                </>
              ) : (
                <>
                  <Send className="w-3.5 h-3.5" />
                  <span>Ask</span>
                </>
              )}
            </button>
          </div>
          <span className="text-[11px] text-navy-400 block">
            Tip: Press <kbd className="px-1.5 py-0.5 rounded bg-navy-800 border border-navy-700 text-navy-200">Enter</kbd> to submit, <kbd className="px-1.5 py-0.5 rounded bg-navy-800 border border-navy-700 text-navy-200">Shift + Enter</kbd> for new line.
          </span>
        </div>

        {/* Answer Display */}
        {answer && (
          <div className="rounded-2xl bg-gradient-to-br from-navy-900 via-navy-900 to-navy-950 border border-football-500/30 p-6 space-y-4 shadow-xl">
            <div className="flex items-center justify-between pb-3 border-b border-navy-800">
              <span className="text-xs uppercase tracking-wider font-extrabold text-football-400 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5" />
                <span>AI Grounded Response</span>
              </span>
              {responseAiMode && (
                <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-navy-950 text-navy-300 border border-navy-700">
                  Engine: {responseAiMode}
                </span>
              )}
            </div>

            <p className="text-white text-sm sm:text-base leading-relaxed whitespace-pre-line">
              {answer}
            </p>

            {/* Clickable Grounding Evidence */}
            {evidenceIds.length > 0 && (
              <div className="pt-3 border-t border-navy-800 space-y-2">
                <span className="text-xs font-semibold text-navy-300 flex items-center gap-1.5">
                  <Shield className="w-3.5 h-3.5 text-football-400" />
                  <span>Grounding Evidence ({evidenceIds.length} verifying events):</span>
                </span>
                <div className="flex flex-wrap gap-2">
                  {evidenceIds.map((id) => (
                    <a
                      key={id}
                      href={`#event-${id}`}
                      onClick={onJumpToEvent}
                      className="font-mono text-xs px-2.5 py-1 rounded-lg bg-navy-950 hover:bg-football-500/20 text-football-300 hover:text-football-200 border border-navy-800 hover:border-football-500/50 transition"
                      title={`Jump to timeline event ${id}`}
                    >
                      Event #{id}
                    </a>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
