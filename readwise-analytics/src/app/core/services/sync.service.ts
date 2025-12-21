import { Injectable, inject, signal, computed, NgZone } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  SyncState,
  SyncStatus,
  SyncPhase,
  LogEntry,
  LogEntryType,
  SseEvent,
  SyncStartResponse,
  ActiveSyncResponse,
  INITIAL_SYNC_STATE,
} from '../models/sync.models';

// Type-safe mapping from SyncPhase to PhaseCounts keys
const PHASE_TO_COUNT_KEY: Record<SyncPhase, 'fetched' | 'documents' | 'highlights' | 'notes'> = {
  FETCHING: 'fetched',
  DOCUMENTS: 'documents',
  HIGHLIGHTS: 'highlights',
  NOTES: 'notes',
} as const;

@Injectable({ providedIn: 'root' })
export class SyncService {
  private readonly http = inject(HttpClient);
  private readonly ngZone = inject(NgZone);

  private eventSource: EventSource | null = null;
  private logIdCounter = 0;
  private lastEventId: string | null = null;
  private reconnectTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private isReconnecting = false;

  // Signals for reactive state
  readonly state = signal<SyncState>(INITIAL_SYNC_STATE);
  readonly logs = signal<readonly LogEntry[]>([]);

  // Computed values
  readonly isRunning = computed(() => {
    const status = this.state().status;
    return status === 'running' || status === 'rate_limited';
  });

  readonly isCompleted = computed(() => this.state().status === 'completed');
  readonly isFailed = computed(() => this.state().status === 'failed');

  async startSync(): Promise<void> {
    // Prevent starting if already running
    if (this.isRunning()) {
      return;
    }

    // Reset state
    this.state.set({
      ...INITIAL_SYNC_STATE,
      status: 'running',
      startedAt: new Date(),
    });
    this.logs.set([]);
    this.logIdCounter = 0;
    this.lastEventId = null;
    this.cancelPendingReconnect();

    try {
      const response = await this.http
        .post<SyncStartResponse>('/sync', {})
        .toPromise();

      if (!response) {
        throw new Error('Empty response from sync endpoint');
      }

      this.state.update((s) => ({ ...s, syncId: response.syncId }));
      this.connectToStream(response.syncId);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 409) {
        // Sync already running - connect to existing stream
        const conflict = error.error as { activeSyncId: string };
        this.state.update((s) => ({ ...s, syncId: conflict.activeSyncId }));
        this.connectToStream(conflict.activeSyncId);
        this.addLog('info', 'Reconnecting to active sync...');
      } else {
        this.handleError('Failed to start sync');
      }
    }
  }

  private connectToStream(syncId: string): void {
    this.disconnect();

    const url = this.lastEventId
      ? `/sync/${syncId}/stream?lastEventId=${this.lastEventId}`
      : `/sync/${syncId}/stream`;

    this.eventSource = new EventSource(url);

    this.eventSource.onmessage = (event) => {
      this.ngZone.run(() => {
        this.lastEventId = event.lastEventId;
        this.handleEvent(JSON.parse(event.data) as SseEvent);
      });
    };

    this.eventSource.onerror = () => {
      this.ngZone.run(() => {
        // Attempt reconnection after brief delay, preventing concurrent attempts
        if (this.isRunning() && !this.isReconnecting) {
          this.isReconnecting = true;
          this.cancelPendingReconnect();
          this.reconnectTimeoutId = setTimeout(() => {
            this.isReconnecting = false;
            const currentSyncId = this.state().syncId;
            if (currentSyncId && this.isRunning()) {
              this.connectToStream(currentSyncId);
            }
          }, 2000);
        }
      });
    };
  }

  private cancelPendingReconnect(): void {
    if (this.reconnectTimeoutId !== null) {
      clearTimeout(this.reconnectTimeoutId);
      this.reconnectTimeoutId = null;
    }
    this.isReconnecting = false;
  }

  private handleEvent(event: SseEvent): void {
    switch (event.type) {
      case 'started':
        this.addLog('info', 'Sync started');
        break;

      case 'phase_started':
        this.state.update((s) => ({
          ...s,
          currentPhase: event.phase,
          status: 'running',
          rateLimit: null,
        }));
        this.addLog(
          'phase',
          `Phase ${event.phaseNumber}/${event.totalPhases}: Syncing ${event.phase.toLowerCase()}...`
        );
        break;

      case 'progress':
        this.updatePhaseCount(event.phase, event.processed);
        this.addLog(
          'progress',
          `${this.formatPhase(event.phase)}: ${event.processed} processed`
        );
        break;

      case 'page_fetched':
        this.state.update((s) => ({
          ...s,
          phaseCounts: {
            ...s.phaseCounts,
            fetched: event.totalItemsSoFar,
          },
        }));
        this.addLog(
          'progress',
          `Page ${event.pageNumber}: ${event.itemsInPage} items (${event.totalItemsSoFar} total)${event.hasMore ? '' : ' - Complete'}`
        );
        break;

      case 'rate_limited':
        this.state.update((s) => ({
          ...s,
          status: 'rate_limited',
          rateLimit: {
            retryAfter: event.retryAfter,
            attempt: event.attempt,
            maxAttempts: event.maxAttempts,
          },
        }));
        this.addLog(
          'warning',
          `Rate limit hit. Waiting ${event.retryAfter}s (attempt ${event.attempt}/${event.maxAttempts})`
        );
        break;

      case 'rate_limit_cleared':
        this.state.update((s) => ({
          ...s,
          status: 'running',
          rateLimit: null,
        }));
        this.addLog('info', 'Rate limit cleared, resuming...');
        break;

      case 'phase_completed':
        this.updatePhaseCount(event.phase, event.count);
        this.state.update((s) => ({
          ...s,
          completedPhases: s.completedPhases + 1,
          overallPercent: Math.round(((s.completedPhases + 1) / s.totalPhases) * 100),
        }));
        this.addLog(
          'success',
          `Phase completed: ${event.count} ${event.phase.toLowerCase()} synced`
        );
        break;

      case 'completed':
        this.state.update((s) => ({
          ...s,
          status: 'completed',
          completedPhases: 4,
          overallPercent: 100,
          currentPhase: null,
          phaseCounts: {
            ...s.phaseCounts,
            documents: event.summary.documents,
            highlights: event.summary.highlights,
            notes: event.summary.notes,
          },
        }));
        this.addLog('complete', `Sync completed successfully (${event.duration})`);
        this.disconnect();
        break;

      case 'error':
        this.state.update((s) => ({
          ...s,
          status: 'failed',
          error: event.message,
        }));
        this.addLog('error', event.message);
        this.disconnect();
        break;

      case 'cancelled':
        this.state.update((s) => ({
          ...s,
          status: 'cancelled',
        }));
        this.addLog('warning', 'Sync cancelled');
        this.disconnect();
        break;
    }
  }

  private updatePhaseCount(phase: SyncPhase, count: number): void {
    const key = PHASE_TO_COUNT_KEY[phase];
    this.state.update((s) => ({
      ...s,
      phaseCounts: {
        ...s.phaseCounts,
        [key]: count,
      },
    }));
  }

  private formatPhase(phase: SyncPhase): string {
    return phase.charAt(0) + phase.slice(1).toLowerCase();
  }

  private addLog(type: LogEntryType, message: string): void {
    const entry: LogEntry = {
      id: ++this.logIdCounter,
      type,
      timestamp: new Date().toLocaleTimeString('en-US', {
        hour12: false,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      }),
      message,
    };
    this.logs.update((logs) => [...logs, entry]);
  }

  private handleError(message: string): void {
    this.state.update((s) => ({
      ...s,
      status: 'failed',
      error: message,
    }));
    this.addLog('error', message);
    this.disconnect();
  }

  private disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  async cancelSync(): Promise<void> {
    const syncId = this.state().syncId;
    if (!syncId) return;

    try {
      await this.http.delete(`/sync/${syncId}`).toPromise();
      this.state.update((s) => ({ ...s, status: 'cancelled' }));
      this.addLog('warning', 'Sync cancelled by user');
      this.disconnect();
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 404) {
          this.addLog('warning', 'Sync already completed or not found');
        } else if (error.status >= 500) {
          this.addLog('error', `Server error while cancelling sync: ${error.status}`);
        } else {
          this.addLog('error', `Failed to cancel sync: ${error.message}`);
        }
      } else {
        this.addLog('error', 'Network error while cancelling sync');
      }
    }
  }

  async checkActiveSync(): Promise<void> {
    try {
      const response = await this.http
        .get<ActiveSyncResponse>('/sync/active')
        .toPromise();

      if (response?.active && response.syncId) {
        this.state.update((s) => ({
          ...s,
          status: 'running',
          syncId: response.syncId,
          currentPhase: response.currentPhase,
          startedAt: response.startedAt ? new Date(response.startedAt) : null,
        }));
        this.connectToStream(response.syncId);
      }
    } catch {
      // No active sync, ignore
    }
  }

  reset(): void {
    this.disconnect();
    this.state.set(INITIAL_SYNC_STATE);
    this.logs.set([]);
  }
}
