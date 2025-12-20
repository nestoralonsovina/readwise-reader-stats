import { Component, input, computed } from '@angular/core';
import { SyncState, SyncPhase } from '../../../core/models/sync.models';
import { ProgressBarComponent } from './progress-bar.component';

interface PhaseDisplay {
  readonly name: string;
  readonly phase: SyncPhase;
  readonly number: number;
}

const PHASES: readonly PhaseDisplay[] = [
  { name: 'Documents', phase: 'DOCUMENTS', number: 1 },
  { name: 'Highlights', phase: 'HIGHLIGHTS', number: 2 },
  { name: 'Notes', phase: 'NOTES', number: 3 },
];

@Component({
  selector: 'app-phase-stepper',
  standalone: true,
  imports: [ProgressBarComponent],
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
                <svg
                  class="h-5 w-5 text-white"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M5 13l4 4L19 7"
                  />
                </svg>
              } @else if (isPhaseActive(phase)) {
                <svg
                  class="h-5 w-5 animate-spin text-white"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                  />
                </svg>
              } @else if (isPhaseFailed(phase)) {
                <svg
                  class="h-5 w-5 text-white"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
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

      <app-progress-bar
        [percent]="state().overallPercent"
        [isAnimated]="isRunning()"
        [isError]="isFailed()"
      />
    </div>
  `,
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
      return 'bg-emerald-500';
    }
    if (this.isPhaseFailed(phase)) {
      return 'bg-destructive';
    }
    if (this.isPhaseActive(phase)) {
      return 'bg-amber-500 animate-pulse';
    }
    return 'bg-muted';
  }

  getPhaseLabelClasses(phase: PhaseDisplay): string {
    if (this.isPhaseCompleted(phase)) {
      return 'text-emerald-600 dark:text-emerald-400';
    }
    if (this.isPhaseFailed(phase)) {
      return 'text-destructive';
    }
    if (this.isPhaseActive(phase)) {
      return 'text-amber-600 dark:text-amber-400';
    }
    return 'text-muted-foreground';
  }

  getConnectorClasses(phase: PhaseDisplay): string {
    if (this.isPhaseCompleted(phase)) {
      return 'bg-emerald-500';
    }
    return 'bg-muted';
  }

  getPhaseStatus(phase: PhaseDisplay): string {
    const counts = this.state().phaseCounts;
    const count = counts[phase.phase.toLowerCase() as keyof typeof counts];

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
