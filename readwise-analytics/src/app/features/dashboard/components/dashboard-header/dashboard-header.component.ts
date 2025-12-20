import { Component, input, output, inject, computed } from '@angular/core';
import { PeriodToggleComponent } from '../../../../shared/components/period-toggle/period-toggle.component';
import { ThemeService } from '../../../../core/services/theme.service';
import { SyncService } from '../../../../core/services/sync.service';
import { Period } from '../../../../core/models/api.models';

@Component({
  selector: 'app-dashboard-header',
  standalone: true,
  imports: [PeriodToggleComponent],
  template: `
    <header class="border-b border-border bg-card">
      <div class="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        <!-- Last synced -->
        <span class="text-sm text-muted-foreground">
          {{ lastSyncedText() }}
        </span>

        <!-- Controls -->
        <div class="flex items-center gap-4">
          <app-period-toggle
            [period]="period()"
            (periodChange)="periodChange.emit($event)"
          />

          <!-- Theme toggle -->
          <button
            type="button"
            class="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-foreground"
            (click)="themeService.toggle()"
            [attr.aria-label]="
              themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'
            "
          >
            @if (themeService.isDark()) {
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"
                />
              </svg>
            } @else {
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"
                />
              </svg>
            }
          </button>

          <!-- Sync button -->
          <button
            type="button"
            class="inline-flex items-center gap-2 rounded-lg bg-amber-500 px-4 py-2 text-sm font-medium text-white hover:bg-amber-600 disabled:opacity-50"
            (click)="onSyncClick()"
          >
            <svg
              class="h-4 w-4"
              [class.animate-spin]="syncService.isRunning()"
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
            {{ syncService.isRunning() ? 'Syncing...' : 'Sync' }}
          </button>
        </div>
      </div>
    </header>
  `,
})
export class DashboardHeaderComponent {
  readonly themeService = inject(ThemeService);
  readonly syncService = inject(SyncService);

  readonly period = input.required<Period>();
  readonly lastSynced = input<Date | null>(null);

  readonly periodChange = output<Period>();

  readonly lastSyncedText = computed(() => {
    const date = this.lastSynced();
    if (!date) {
      return 'Not synced yet';
    }

    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) {
      return 'Last synced: just now';
    }
    if (diffMins === 1) {
      return 'Last synced: 1 minute ago';
    }
    if (diffMins < 60) {
      return `Last synced: ${diffMins} minutes ago`;
    }

    const diffHours = Math.floor(diffMins / 60);
    if (diffHours === 1) {
      return 'Last synced: 1 hour ago';
    }
    if (diffHours < 24) {
      return `Last synced: ${diffHours} hours ago`;
    }

    const diffDays = Math.floor(diffHours / 24);
    if (diffDays === 1) {
      return 'Last synced: 1 day ago';
    }
    return `Last synced: ${diffDays} days ago`;
  });

  onSyncClick(): void {
    if (this.syncService.isRunning()) {
      // If already running, just open the panel
      this.syncService.openPanel();
    } else {
      // Start a new sync
      this.syncService.startSync();
    }
  }
}
