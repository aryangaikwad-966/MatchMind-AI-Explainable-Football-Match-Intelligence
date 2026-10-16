import { describe, expect, it } from 'vitest';
import { nextReplaySequence } from './replay';

describe('synthetic replay controls', () => {
  it('steps chronologically and never exceeds the event stream', () => {
    expect(nextReplaySequence(0, 3)).toBe(1);
    expect(nextReplaySequence(3, 3)).toBe(3);
  });
});
