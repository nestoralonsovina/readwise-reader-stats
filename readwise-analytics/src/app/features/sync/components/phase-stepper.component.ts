import { Component, input, computed } from '@angular/core';
import { SyncState, SyncPhase, PhaseCounts } from '../../../core/models/sync.models';
import { HlmProgressImports } from '@spartan-ng/helm/progress';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideCheck, lucideRefreshCw, lucideX } from '@ng-icons/lucide';

interface PhaseDisplay {
  readonly name: string;
  readonly phase: SyncPhase;
  readonly number: number;
}

const PHASES: readonly PhaseDisplay[] = [
  { name: 'Fetching', phase: 'FETCHING', number: 1 },
  { name: 'Documents', phase: 'DOCUMENTS', number: 2 },
  { name: 'Highlights', phase: 'HIGHLIGHTS', number: 3 },
  { name: 'Notes', phase: 'NOTES', number: 4 },
];

// Type-safe mapping from SyncPhase to PhaseCounts keys
const PHASE_TO_COUNT_KEY: Record<SyncPhase, keyof PhaseCounts> = {
  FETCHING: 'fetched',
  DOCUMENTS: 'documents',
  HIGHLIGHTS: 'highlights',
  NOTES: 'notes',
} as const;

@Component({
  selector: 'app-phase-stepper',
  standalone: true,
  imports: [...HlmProgressImports, ...HlmIconImports],
  providers: [provideIcons({ lucideCheck, lucideRefreshCw, lucideX })],
  template: `
    <div class="border-b border-border bg-muted/50 px-6 py-5">
      <div class="flex items-center justify-between">
        @for (phase of phases; track phase.phase; let i = $index) {
          <!-- Phase -->
          <div class="flex flex-1 flex-col items-center">
            <div
              class="mb-2 flex h-10 w-10 items-center justify-center rounded-full"
              [class]="getPhaseIconClasses(phase)"
            >
              @if (isPhaseCompleted(phase)) {
                <ng-icon
                  hlm
                  name="lucideCheck"
                  class="text-white"
                  aria-label="Phase completed"
                />
              } @else if (isPhaseActive(phase)) {
                <ng-icon
                  hlm
                  name="lucideRefreshCw"
                  class="animate-spin text-white"
                  aria-label="Phase in progress"
                />
              } @else if (isPhaseFailed(phase)) {
                <ng-icon
                  hlm
                  name="lucideX"
                  class="text-white"
                  aria-label="Phase failed"
                />
              } @else {
                <span class="text-sm font-medium text-muted-foreground">
                  {{ phase.number }}
                </span>
              }
            </div>
            <span class="text-xs font-medium" [class]="getPhaseLabelClasses(phase)">
              {{ phase.name }}
            </span>
            <span class="text-xs text-muted-foreground">
              {{ getPhaseStatus(phase) }}
            </span>
          </div>

          <!-- Connector -->
          @if (i < phases.length - 1) {
            <div
              class="-mt-6 h-0.5 flex-1"
              [class]="getConnectorClasses(phase)"
            ></div>
          }
        }
      </div>

      <!-- Progress Bar -->
      <div class="mt-5">
        <div class="mb-1.5 flex items-center justify-between">
          <span class="text-xs text-muted-foreground">Overall Progress</span>
          <span class="text-xs font-medium">{{ state().overallPercent }}%</span>
        </div>
        <hlm-progress [value]="state().overallPercent" [max]="100" class="h-2">
          <hlm-progress-indicator
            [class]="isFailed() ? 'bg-destructive' : 'bg-brand'"
            [class.progress-animated]="isRunning()"
          />
        </hlm-progress>
      </div>
    </div>
  `,
  styles: [
    `
      @keyframes progress-animation {
        0% {
          background-position: 1rem 0;
        }
        100% {
          background-position: 0 0;
        }
      }

      .progress-animated {
        background-image: linear-gradient(
          -45deg,
          rgba(255, 255, 255, 0.15) 25%,
          transparent 25%,
          transparent 50%,
          rgba(255, 255, 255, 0.15) 50%,
          rgba(255, 255, 255, 0.15) 75%,
          transparent 75%,
          transparent
        );
        background-size: 1rem 1rem;
        animation: progress-animation 1s linear infinite;
      }
    `,
  ],
})
export class PhaseStepperComponent {
  readonly state = input.required<SyncState>();

  readonly phases = PHASES;

  readonly isRunning = computed(() => {
    const status = this.state().status;
    return status === 'running' || status === 'rate_limited';
  });

  readonly isFailed = computed(() => this.state().status === 'failed');

  isPhaseCompleted(phase: PhaseDisplay): boolean {
    return phase.number <= this.state().completedPhases;
  }

  isPhaseActive(phase: PhaseDisplay): boolean {
    return this.state().currentPhase === phase.phase && this.isRunning();
  }

  isPhaseFailed(phase: PhaseDisplay): boolean {
    return (
      this.state().status === 'failed' &&
      this.state().currentPhase === phase.phase
    );
  }

  isPhasePending(phase: PhaseDisplay): boolean {
    return (
      !this.isPhaseCompleted(phase) &&
      !this.isPhaseActive(phase) &&
      !this.isPhaseFailed(phase)
    );
  }

  getPhaseIconClasses(phase: PhaseDisplay): string {
    if (this.isPhaseCompleted(phase)) {
      return 'bg-success';
    }
    if (this.isPhaseFailed(phase)) {
      return 'bg-destructive';
    }
    if (this.isPhaseActive(phase)) {
      return 'bg-brand animate-pulse';
    }
    return 'bg-muted';
  }

  getPhaseLabelClasses(phase: PhaseDisplay): string {
    if (this.isPhaseCompleted(phase)) {
      return 'text-success';
    }
    if (this.isPhaseFailed(phase)) {
      return 'text-destructive';
    }
    if (this.isPhaseActive(phase)) {
      return 'text-brand';
    }
    return 'text-muted-foreground';
  }

  getConnectorClasses(phase: PhaseDisplay): string {
    if (this.isPhaseCompleted(phase)) {
      return 'bg-success';
    }
    return 'bg-muted';
  }

  getPhaseStatus(phase: PhaseDisplay): string {
    const counts = this.state().phaseCounts;

    if (phase.phase === 'FETCHING') {
      if (this.isPhaseCompleted(phase)) {
        return `${counts.fetched} items`;
      }
      if (this.isPhaseActive(phase)) {
        return `${counts.fetched} fetched`;
      }
      return 'Pending';
    }

    const key = PHASE_TO_COUNT_KEY[phase.phase];
    const count = counts[key];

    if (this.isPhaseCompleted(phase)) {
      return `${count} synced`;
    }
    if (this.isPhaseFailed(phase)) {
      return 'Failed';
    }
    if (this.isPhaseActive(phase)) {
      return `${count} processed`;
    }
    return 'Pending';
  }
}
