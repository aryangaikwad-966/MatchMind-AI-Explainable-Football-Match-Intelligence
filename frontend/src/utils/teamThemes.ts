import { Flame, Zap, AlertCircle, Shield, Activity } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

export interface TeamTheme {
  bg: string;
  gradient: string;
  accent: string;
  text: string;
  border: string;
  badgeBg: string;
  initials: string;
}

export const TEAM_THEMES: Record<string, TeamTheme> = {
  'T-NBR': {
    bg: 'bg-emerald-600',
    gradient: 'from-emerald-600 to-teal-900',
    accent: '#10b981',
    text: 'text-emerald-200',
    border: 'border-emerald-500/50',
    badgeBg: 'bg-emerald-950/80',
    initials: 'NFC',
  },
  'T-KNG': {
    bg: 'bg-amber-600',
    gradient: 'from-amber-600 to-yellow-950',
    accent: '#f59e0b',
    text: 'text-amber-200',
    border: 'border-amber-500/50',
    badgeBg: 'bg-amber-950/80',
    initials: 'KNU',
  },
  'T-WHA': {
    bg: 'bg-sky-600',
    gradient: 'from-sky-600 to-blue-950',
    accent: '#0284c7',
    text: 'text-sky-200',
    border: 'border-sky-500/50',
    badgeBg: 'bg-sky-950/80',
    initials: 'WHA',
  },
  'T-RMC': {
    bg: 'bg-rose-600',
    gradient: 'from-rose-600 to-red-950',
    accent: '#e11d48',
    text: 'text-rose-200',
    border: 'border-rose-500/50',
    badgeBg: 'bg-rose-950/80',
    initials: 'RMC',
  },
  'T-EMR': {
    bg: 'bg-indigo-600',
    gradient: 'from-indigo-600 to-violet-950',
    accent: '#6366f1',
    text: 'text-indigo-200',
    border: 'border-indigo-500/50',
    badgeBg: 'bg-indigo-950/80',
    initials: 'EMR',
  },
  'T-ASH': {
    bg: 'bg-orange-600',
    gradient: 'from-orange-600 to-stone-900',
    accent: '#ea580c',
    text: 'text-orange-200',
    border: 'border-orange-500/50',
    badgeBg: 'bg-stone-950/80',
    initials: 'ASH',
  },
  'T-SLV': {
    bg: 'bg-slate-500',
    gradient: 'from-slate-500 to-slate-800',
    accent: '#64748b',
    text: 'text-slate-200',
    border: 'border-slate-400/50',
    badgeBg: 'bg-slate-900/80',
    initials: 'SLV',
  },
  'T-BRK': {
    bg: 'bg-cyan-600',
    gradient: 'from-cyan-600 to-teal-950',
    accent: '#06b6d4',
    text: 'text-cyan-200',
    border: 'border-cyan-500/50',
    badgeBg: 'bg-cyan-950/80',
    initials: 'BRK',
  },
  'T-STH': {
    bg: 'bg-purple-600',
    gradient: 'from-purple-600 to-indigo-950',
    accent: '#9333ea',
    text: 'text-purple-200',
    border: 'border-purple-500/50',
    badgeBg: 'bg-purple-950/80',
    initials: 'STH',
  },
  'T-CRS': {
    bg: 'bg-lime-600',
    gradient: 'from-lime-600 to-emerald-950',
    accent: '#65a30d',
    text: 'text-lime-200',
    border: 'border-lime-500/50',
    badgeBg: 'bg-lime-950/80',
    initials: 'CRS',
  },
};

export function getTeamBadge(teamId: string, teamName: string): TeamTheme {
  if (TEAM_THEMES[teamId]) {
    return TEAM_THEMES[teamId];
  }
  const initials = teamName
    ? teamName.split(' ').map(n => n[0]).join('').slice(0, 3).toUpperCase()
    : 'FC';

  return {
    bg: 'bg-navy-700',
    gradient: 'from-navy-700 to-navy-900',
    accent: '#38bdf8',
    text: 'text-white',
    border: 'border-navy-600',
    badgeBg: 'bg-navy-900/80',
    initials,
  };
}

export interface ScenarioMeta {
  label: string;
  icon: LucideIcon;
  color: string;
  bgGlow: string;
}

export function getScenarioMeta(scenario: string): ScenarioMeta {
  switch (scenario) {
    case 'LATE_COMEBACK_VICTORY':
      return {
        label: 'Late Comeback',
        icon: Flame,
        color: 'text-amber-400 bg-amber-500/10 border-amber-500/30',
        bgGlow: 'from-amber-500/10 to-transparent',
      };
    case 'NARROW_LATE_WINNER':
    case 'TWO_LATE_GOALS_CLOSE_MATCH':
      return {
        label: 'Late Drama',
        icon: Zap,
        color: 'text-emerald-400 bg-football-500/10 border-football-500/30',
        bgGlow: 'from-emerald-500/10 to-transparent',
      };
    case 'RED_CARD_TACTICAL_SHIFT':
      return {
        label: 'Red Card Shift',
        icon: AlertCircle,
        color: 'text-rose-400 bg-rose-500/10 border-rose-500/30',
        bgGlow: 'from-rose-500/10 to-transparent',
      };
    case 'GOALLESS_DRAW_WITH_CHANCES':
    case 'EARLY_GOAL_DEFENSIVE_HOLD':
      return {
        label: 'Defensive Battle',
        icon: Shield,
        color: 'text-sky-400 bg-sky-500/10 border-sky-500/30',
        bgGlow: 'from-sky-500/10 to-transparent',
      };
    case 'HIGH_SCORING_MOMENTUM_SWINGS':
      return {
        label: 'Goal Fest',
        icon: Flame,
        color: 'text-purple-400 bg-purple-500/10 border-purple-500/30',
        bgGlow: 'from-purple-500/10 to-transparent',
      };
    default:
      return {
        label: scenario ? scenario.replace(/_/g, ' ').toLowerCase() : 'Standard Match',
        icon: Activity,
        color: 'text-navy-300 bg-navy-800 border-navy-700',
        bgGlow: 'from-navy-800/20 to-transparent',
      };
  }
}
