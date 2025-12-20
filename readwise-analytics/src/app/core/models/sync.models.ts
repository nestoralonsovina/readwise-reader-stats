// Sync state types
export type SyncPhase = 'DOCUMENTS' | 'HIGHLIGHTS' | 'NOTES';

export type SyncStatus =
  | 'idle'
  | 'running'
  | 'rate_limited'
  | 'completed'
  | 'failed'
  | 'cancelled';

export interface RateLimitInfo {
  readonly retryAfter: number;
  readonly attempt: number;
  readonly maxAttempts: number;
}

export interface PhaseCounts {
  readonly documents: number;
  readonly highlights: number;
  readonly notes: number;
}

export interface SyncState {
  readonly status: SyncStatus;
  readonly syncId: string | null;
  readonly currentPhase: SyncPhase | null;
  readonly completedPhases: number;
  readonly totalPhases: number;
  readonly overallPercent: number;
  readonly phaseCounts: PhaseCounts;
  readonly rateLimit: RateLimitInfo | null;
  readonly startedAt: Date | null;
  readonly error: string | null;
}

export const INITIAL_SYNC_STATE: SyncState = {
  status: 'idle',
  syncId: null,
  currentPhase: null,
  completedPhases: 0,
  totalPhases: 3,
  overallPercent: 0,
  phaseCounts: { documents: 0, highlights: 0, notes: 0 },
  rateLimit: null,
  startedAt: null,
  error: null,
};

// Log entry types
export type LogEntryType =
  | 'info'
  | 'phase'
  | 'progress'
  | 'success'
  | 'warning'
  | 'error'
  | 'complete';

export interface LogEntry {
  readonly id: number;
  readonly type: LogEntryType;
  readonly timestamp: string;
  readonly message: string;
}

// SSE event types (matching backend SyncProgressEvent)
export type SseEventType =
  | 'started'
  | 'phase_started'
  | 'progress'
  | 'rate_limited'
  | 'rate_limit_cleared'
  | 'phase_completed'
  | 'completed'
  | 'error'
  | 'cancelled';

export interface SseStartedEvent {
  readonly type: 'started';
  readonly syncId: string;
  readonly timestamp: string;
}

export interface SsePhaseStartedEvent {
  readonly type: 'phase_started';
  readonly phase: SyncPhase;
  readonly phaseNumber: number;
  readonly totalPhases: number;
  readonly timestamp: string;
}

export interface SseProgressEvent {
  readonly type: 'progress';
  readonly phase: SyncPhase;
  readonly processed: number;
  readonly timestamp: string;
}

export interface SseRateLimitedEvent {
  readonly type: 'rate_limited';
  readonly retryAfter: number;
  readonly attempt: number;
  readonly maxAttempts: number;
  readonly timestamp: string;
}

export interface SseRateLimitClearedEvent {
  readonly type: 'rate_limit_cleared';
  readonly timestamp: string;
}

export interface SsePhaseCompletedEvent {
  readonly type: 'phase_completed';
  readonly phase: SyncPhase;
  readonly count: number;
  readonly timestamp: string;
}

export interface SseCompletedEvent {
  readonly type: 'completed';
  readonly summary: {
    readonly documents: number;
    readonly highlights: number;
    readonly notes: number;
  };
  readonly duration: string;
  readonly timestamp: string;
}

export interface SseErrorEvent {
  readonly type: 'error';
  readonly phase: SyncPhase | null;
  readonly message: string;
  readonly timestamp: string;
}

export interface SseCancelledEvent {
  readonly type: 'cancelled';
  readonly phase: SyncPhase | null;
  readonly timestamp: string;
}

export type SseEvent =
  | SseStartedEvent
  | SsePhaseStartedEvent
  | SseProgressEvent
  | SseRateLimitedEvent
  | SseRateLimitClearedEvent
  | SsePhaseCompletedEvent
  | SseCompletedEvent
  | SseErrorEvent
  | SseCancelledEvent;

// API response types
export interface SyncStartResponse {
  readonly syncId: string;
  readonly status: string;
  readonly streamUrl: string;
}

export interface SyncConflictResponse {
  readonly activeSyncId: string;
}

export interface ActiveSyncResponse {
  readonly active: boolean;
  readonly syncId: string | null;
  readonly status: string | null;
  readonly currentPhase: SyncPhase | null;
  readonly startedAt: string | null;
}
