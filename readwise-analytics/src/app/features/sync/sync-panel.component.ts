import { Component, inject, computed } from '@angular/core';
import { SyncService } from '../../core/services/sync.service';
import { PhaseStepperComponent } from './components/phase-stepper.component';
import { RateLimitBannerComponent } from './components/rate-limit-banner.component';
import { ActivityLogComponent } from './components/activity-log.component';
import { SyncFooterComponent } from './components/sync-footer.component';
import { HlmSheetImports } from '@spartan-ng/helm/sheet';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';

@Component({
  selector: 'app-sync-panel',
  standalone: true,
  imports: [
    PhaseStepperComponent,
    RateLimitBannerComponent,
    ActivityLogComponent,
    SyncFooterComponent,
    ...HlmSheetImports,
    ...HlmBadgeImports,
  ],
  template: `
    <hlm-sheet-header class="border-b border-border px-6 py-4">
      <div class="flex items-center gap-3">
        <h2 hlmSheetTitle class="text-lg font-semibold">{{ panelTitle() }}</h2>
        <span hlmBadge variant="secondary" [class]="statusBadgeClasses()">
          {{ statusBadgeText() }}
        </span>
      </div>
    </hlm-sheet-header>

    <!-- Phase Stepper -->
    <app-phase-stepper [state]="syncService.state()" />

    <!-- Rate Limit Banner -->
    <app-rate-limit-banner [rateLimit]="syncService.state().rateLimit" />

    <!-- Activity Log -->
    <app-activity-log [logs]="syncService.logs()" />

    <!-- Footer -->
    <hlm-sheet-footer class="mt-auto border-t border-border p-0">
      <app-sync-footer
        class="w-full"
        [state]="syncService.state()"
        (startClick)="onStart()"
        (cancelClick)="onCancel()"
        (retryClick)="onRetry()"
      />
    </hlm-sheet-footer>
  `,
  styles: [
    `
      :host {
        display: flex;
        flex-direction: column;
        height: 100%;
        overflow: hidden;
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
      case 'running':
      case 'rate_limited':
        return 'Syncing with Readwise';
      default:
        return 'Sync with Readwise';
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
        return 'Ready';
    }
  });

  readonly statusBadgeClasses = computed(() => {
    const status = this.syncService.state().status;
    switch (status) {
      case 'running':
        return 'bg-chart-1/10 text-chart-1';
      case 'rate_limited':
        return 'bg-warning/10 text-warning';
      case 'completed':
        return 'bg-success/10 text-success';
      case 'failed':
      case 'cancelled':
        return 'bg-destructive/10 text-destructive';
      default:
        return 'bg-muted text-muted-foreground';
    }
  });

  onStart(): void {
    this.syncService.startSync();
  }

  onCancel(): void {
    this.syncService.cancelSync();
  }

  onRetry(): void {
    this.syncService.startSync();
  }
}
