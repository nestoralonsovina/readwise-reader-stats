import { Component, input, output, inject, computed } from '@angular/core';
import { PeriodToggleComponent } from '../../../../shared/components/period-toggle/period-toggle.component';
import { SyncPanelComponent } from '../../../sync/sync-panel.component';
import { ThemeService } from '../../../../core/services/theme.service';
import { SyncService } from '../../../../core/services/sync.service';
import { Period } from '../../../../core/models/api.models';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { HlmSidebarTrigger } from '@spartan-ng/helm/sidebar';
import { HlmSheetImports } from '@spartan-ng/helm/sheet';
import { BrnSheetImports } from '@spartan-ng/brain/sheet';
import { provideIcons } from '@ng-icons/core';
import { lucideSun, lucideMoon, lucideRefreshCw, lucideMenu } from '@ng-icons/lucide';

@Component({
  selector: 'app-dashboard-header',
  standalone: true,
  imports: [
    PeriodToggleComponent,
    SyncPanelComponent,
    ...HlmButtonImports,
    ...HlmIconImports,
    ...HlmSheetImports,
    ...BrnSheetImports,
    HlmSidebarTrigger,
  ],
  providers: [provideIcons({ lucideSun, lucideMoon, lucideRefreshCw, lucideMenu })],
  template: `
    <header class="border-b border-border bg-card">
      <div class="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        <!-- Left side: mobile trigger + last synced -->
        <div class="flex items-center gap-3">
          <!-- Mobile sidebar trigger -->
          <button hlmSidebarTrigger class="md:hidden" aria-label="Toggle sidebar">
            <ng-icon hlm name="lucideMenu" size="lg" />
          </button>

          <!-- Last synced -->
          <span class="text-sm text-muted-foreground">
            {{ lastSyncedText() }}
          </span>
        </div>

        <!-- Controls -->
        <div class="flex items-center gap-4">
          <app-period-toggle
            [period]="period()"
            (periodChange)="periodChange.emit($event)"
          />

          <!-- Theme toggle -->
          <button
            hlmBtn
            variant="ghost"
            size="icon"
            (click)="themeService.toggle()"
            [attr.aria-label]="
              themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'
            "
          >
            @if (themeService.isDark()) {
              <ng-icon hlm name="lucideSun" size="lg" />
            } @else {
              <ng-icon hlm name="lucideMoon" size="lg" />
            }
          </button>

          <!-- Sync Sheet -->
          <hlm-sheet side="right">
            <button
              brnSheetTrigger
              hlmBtn
              class="bg-brand text-brand-foreground hover:bg-brand/90"
            >
              <ng-icon
                hlm
                name="lucideRefreshCw"
                size="sm"
                [class.animate-spin]="syncService.isRunning()"
              />
              {{ syncService.isRunning() ? 'Syncing...' : 'Sync' }}
            </button>

            <hlm-sheet-content *brnSheetContent="let ctx" class="w-full max-w-lg sm:max-w-lg">
              <app-sync-panel />
            </hlm-sheet-content>
          </hlm-sheet>
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
}
