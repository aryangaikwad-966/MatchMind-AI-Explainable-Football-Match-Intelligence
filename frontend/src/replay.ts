export function nextReplaySequence(current: number, total: number): number {
  return Math.max(0, Math.min(current + 1, total));
}
