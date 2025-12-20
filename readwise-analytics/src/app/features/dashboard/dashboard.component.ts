import { Component, inject, signal, effect, computed, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of, catchError } from 'rxjs';

import { AnalyticsService } from '../../core/services/analytics.service';
import { SyncService } from '../../core/services/sync.service';
import {
  Period,
  periodToGranularity,
  periodToDateRange,
  DashboardResponse,
  ReadingStatsResponse,
  StreakResponse,
  PipelineResponse,
  HighlightResponse,
} from '../../core/models/api.models';

import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { StreakBarComponent } from '../../shared/components/streak-bar/streak-bar.component';

import { DashboardHeaderComponent } from './components/dashboard-header/dashboard-header.component';
import { ReadingActivityChartComponent } from './components/reading-activity-chart/reading-activity-chart.component';
import { PipelineCardComponent } from './components/pipeline-card/pipeline-card.component';
import { HighlightsCardComponent } from './components/highlights-card/highlights-card.component';
import { MostHighlightedComponent } from './components/most-highlighted/most-highlighted.component';
import { DashboardFooterComponent } from './components/dashboard-footer/dashboard-footer.component';
import { SyncPanelComponent } from '../sync/sync-panel.component';
import { HlmSpinnerImports } from '@spartan-ng/helm/spinner';

interface DashboardData {
  readonly dashboard: DashboardResponse;
  readonly readingStats: ReadingStatsResponse;
  readonly streak: StreakResponse;
  readonly pipeline: PipelineResponse;
  readonly highlights: HighlightResponse;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    KpiCardComponent,
    StreakBarComponent,
    DashboardHeaderComponent,
    ReadingActivityChartComponent,
    PipelineCardComponent,
    HighlightsCardComponent,
    MostHighlightedComponent,
    DashboardFooterComponent,
    SyncPanelComponent,
    ...HlmSpinnerImports,
  ],
  template: `
    <div class="min-h-screen bg-background">
      <app-dashboard-header
        [period]="period()"
        [lastSynced]="lastSynced()"
        (periodChange)="period.set($event)"
      />

      <main class="mx-auto max-w-7xl space-y-6 px-6 py-8">
        <!-- Error state -->
        @if (error()) {
          <div
            class="rounded-lg border border-red-200 bg-red-50 p-4 text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400"
          >
            <p class="font-medium">Failed to load analytics data</p>
            <p class="mt-1 text-sm">{{ error() }}</p>
            <button
              type="button"
              class="mt-2 text-sm font-medium underline"
              (click)="retryLoad()"
            >
              Try again
            </button>
          </div>
        }

        <!-- KPI Row -->
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <app-kpi-card
            title="Words Read"
            [value]="wordsRead()"
            [sparklineData]="wordsSparkline()"
            sparklineColor="#3b82f6"
            icon="document"
            iconBgClass="bg-blue-100 dark:bg-blue-900/30"
            iconColorClass="text-blue-600 dark:text-blue-400"
          />
          <app-kpi-card
            title="Completed"
            [value]="articlesCompleted()"
            [sparklineData]="articlesSparkline()"
            sparklineColor="#10b981"
            icon="checkmark"
            iconBgClass="bg-emerald-100 dark:bg-emerald-900/30"
            iconColorClass="text-emerald-600 dark:text-emerald-400"
          />
          <app-streak-bar
            [current]="currentStreak()"
            [longest]="longestStreak()"
          />
          <app-kpi-card
            title="Backlog"
            [value]="backlogSize()"
            subtitle="articles to read"
            sparklineColor="#8b5cf6"
            icon="inbox"
            iconBgClass="bg-violet-100 dark:bg-violet-900/30"
            iconColorClass="text-violet-600 dark:text-violet-400"
          />
        </div>

        <!-- Reading Activity Chart (full width) -->
        <app-reading-activity-chart [data]="readingStats()" />

        <!-- Bottom Row: Pipeline, Highlights, Most Highlighted -->
        <div class="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
          <app-pipeline-card [data]="pipeline()" />
          <app-highlights-card [data]="highlights()" />
          <app-most-highlighted [documents]="topDocuments()" />
        </div>

        <app-dashboard-footer
          [pipeline]="pipeline()"
          [highlights]="highlights()"
        />
      </main>

      <!-- Loading overlay -->
      @if (loading()) {
        <div
          class="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm"
        >
          <div class="flex flex-col items-center gap-4">
            <hlm-spinner class="size-8 text-amber-500" aria-label="Loading analytics" />
            <p class="text-sm text-muted-foreground">Loading analytics...</p>
          </div>
        </div>
      }

      <!-- Sync Panel (slide-over) -->
      <app-sync-panel />
    </div>
  `,
})
export class DashboardComponent {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly syncService = inject(SyncService);
  private readonly destroyRef = inject(DestroyRef);

  readonly period = signal<Period>(30);
  readonly loading = signal(true);
  readonly lastSynced = signal<Date | null>(null);
  readonly data = signal<DashboardData | null>(null);
  readonly error = signal<string | null>(null);

  // Computed values for template binding
  readonly wordsRead = computed(() => this.data()?.dashboard.summary.wordsRead ?? null);
  readonly articlesCompleted = computed(
    () => this.data()?.dashboard.summary.articlesCompleted ?? null
  );
  readonly backlogSize = computed(() => this.data()?.dashboard.summary.backlogSize ?? null);
  readonly currentStreak = computed(() => this.data()?.streak.current.days ?? null);
  readonly longestStreak = computed(() => this.data()?.streak.longest.days ?? null);
  readonly readingStats = computed(() => this.data()?.readingStats ?? null);
  readonly pipeline = computed(() => this.data()?.pipeline ?? null);
  readonly highlights = computed(() => this.data()?.highlights ?? null);
  readonly topDocuments = computed(() => this.data()?.highlights.topDocuments ?? null);

  readonly wordsSparkline = computed(() => {
    const stats = this.data()?.readingStats.stats ?? [];
    return stats.slice(-7).map((s) => s.wordsRead);
  });

  readonly articlesSparkline = computed(() => {
    const stats = this.data()?.readingStats.stats ?? [];
    return stats.slice(-7).map((s) => s.articlesCompleted);
  });

  constructor() {
    // React to period changes
    effect(() => {
      const currentPeriod = this.period();
      this.loadData(currentPeriod);
    });

    // Refresh data when sync completes
    effect(() => {
      if (this.syncService.isCompleted()) {
        this.lastSynced.set(new Date());
        this.loadData(this.period());
      }
    });
  }

  retryLoad(): void {
    this.loadData(this.period());
  }

  private loadData(period: Period): void {
    this.loading.set(true);
    this.error.set(null);

    const granularity = periodToGranularity(period);
    const { startDate, endDate } = periodToDateRange(period);

    forkJoin({
      dashboard: this.analyticsService.getDashboard(startDate, endDate),
      readingStats: this.analyticsService.getReadingStats(granularity, startDate, endDate),
      streak: this.analyticsService.getStreak(),
      pipeline: this.analyticsService.getPipeline(startDate, endDate),
      highlights: this.analyticsService.getHighlights(startDate, endDate),
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError((err: unknown) => {
          const message = err instanceof Error ? err.message : 'Unknown error occurred';
          this.error.set(message);
          return of(null);
        })
      )
      .subscribe({
        next: (result) => {
          if (result !== null) {
            this.data.set({
              dashboard: result.dashboard,
              readingStats: result.readingStats,
              streak: result.streak,
              pipeline: result.pipeline,
              highlights: result.highlights,
            });
          }
          this.loading.set(false);
        },
      });
  }
}
