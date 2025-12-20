import { Component, inject, computed, HostListener } from '@angular/core';
import { SyncService } from '../../core/services/sync.service';
import { PhaseStepperComponent } from './components/phase-stepper.component';
import { RateLimitBannerComponent } from './components/rate-limit-banner.component';
import { ActivityLogComponent } from './components/activity-log.component';
import { SyncFooterComponent } from './components/sync-footer.component';

@Component({
  selector: 'app-sync-panel',
  standalone: true,
  imports: [
    PhaseStepperComponent,
    RateLimitBannerComponent,
    ActivityLogComponent,
    SyncFooterComponent,
  ],
  template: `
    @if (syncService.isPanelOpen()) {
      <div class="fixed inset-0 z-50">
        <!-- Backdrop -->
        <div
          class="fixed inset-0 bg-gray-900/50 transition-opacity dark:bg-black/60"
          (click)="close()"
        ></div>

        <!-- Panel -->
        <div
          class="fixed inset-y-0 right-0 flex w-full max-w-lg flex-col bg-card shadow-xl"
        >
          <!-- Header -->
          <div
            class="flex items-center justify-between border-b border-border px-6 py-4"
          >
            <div class="flex items-center gap-3">
              <h2 class="text-lg font-semibold">{{ panelTitle() }}</h2>
              <span
                class="rounded-full px-2 py-0.5 text-xs font-medium"
                [class]="statusBadgeClasses()"
              >
                {{ statusBadgeText() }}
              </span>
            </div>
            <button
              type="button"
              class="rounded-lg p-2 text-muted-foreground hover:bg-muted"
              (click)="close()"
            >
              <svg
                class="h-5 w-5"
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
            </button>
          </div>

          <!-- Phase Stepper -->
          <app-phase-stepper [state]="syncService.state()" />

          <!-- Rate Limit Banner -->
          <app-rate-limit-banner [rateLimit]="syncService.state().rateLimit" />

          <!-- Activity Log -->
          <app-activity-log [logs]="syncService.logs()" />

          <!-- Footer -->
          <app-sync-footer
            [state]="syncService.state()"
            (cancelClick)="onCancel()"
            (doneClick)="close()"
            (retryClick)="onRetry()"
          />
        </div>
      </div>
    }
  `,
  styles: [
    `
      :host {
        display: contents;
      }
    `,
  ],
})
export class SyncPanelComponent {
  readonly syncService = inject(SyncService);

  readonly panelTitle = computed(() => {
    const status = this.syncService.state().status;
    switch (status) {
      case 'completed':
        return 'Sync Complete';
      case 'failed':
        return 'Sync Failed';
      case 'cancelled':
        return 'Sync Cancelled';
      default:
        return 'Syncing with Readwise';
    }
  });

  readonly statusBadgeText = computed(() => {
    const status = this.syncService.state().status;
    switch (status) {
      case 'running':
        return 'Running';
      case 'rate_limited':
        return 'Rate Limited';
      case 'completed':
        return 'Completed';
      case 'failed':
        return 'Failed';
      case 'cancelled':
        return 'Cancelled';
      default:
        return 'Idle';
    }
  });

  readonly statusBadgeClasses = computed(() => {
    const status = this.syncService.state().status;
    switch (status) {
      case 'running':
        return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
      case 'rate_limited':
        return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
      case 'completed':
        return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400';
      case 'failed':
      case 'cancelled':
        return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
      default:
        return 'bg-muted text-muted-foreground';
    }
  });

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.close();
  }

  close(): void {
    this.syncService.closePanel();
  }

  onCancel(): void {
    this.syncService.cancelSync();
  }

  onRetry(): void {
    this.syncService.startSync();
  }
}
