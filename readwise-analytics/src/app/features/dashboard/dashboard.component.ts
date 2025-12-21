import { Component, inject, signal, effect, computed, DestroyRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, of, catchError, Observable } from 'rxjs';

import { AnalyticsService } from '../../core/services/analytics.service';
import { SyncService } from '../../core/services/sync.service';
import { DrillDownService } from '../../core/services/drill-down.service';
import { ChartColorsService } from '../../core/services/chart-colors.service';
import {
  Period,
  FixedPeriod,
  CustomDateRange,
  isCustomPeriod,
  periodToGranularity,
  periodToDateRange,
  DashboardResponse,
  ReadingStatsResponse,
  StreakResponse,
  PipelineResponse,
  HighlightResponse,
  DrillDownType,
  DrillDownSummary,
  DrillDownDocument,
  DrillDownResponse,
  WordsReadDocument,
  CompletedDocument,
  BacklogDocument,
} from '../../core/models/api.models';

import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { StreakBarComponent } from '../../shared/components/streak-bar/streak-bar.component';
import { StatDrillDownSheetComponent } from '../../shared/components/stat-drill-down-sheet/stat-drill-down-sheet.component';

import { DashboardHeaderComponent } from './components/dashboard-header/dashboard-header.component';
import { ReadingActivityChartComponent } from './components/reading-activity-chart/reading-activity-chart.component';
import { PipelineCardComponent } from './components/pipeline-card/pipeline-card.component';
import { HighlightsCardComponent } from './components/highlights-card/highlights-card.component';
import { MostHighlightedComponent } from './components/most-highlighted/most-highlighted.component';
import { DashboardFooterComponent } from './components/dashboard-footer/dashboard-footer.component';
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
    StatDrillDownSheetComponent,
    DashboardHeaderComponent,
    ReadingActivityChartComponent,
    PipelineCardComponent,
    HighlightsCardComponent,
    MostHighlightedComponent,
    DashboardFooterComponent,
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
            [sparklineColor]="chartColors.chart1()"
            icon="document"
            iconBgClass="bg-chart-1/10"
            iconColorClass="text-chart-1"
            [clickable]="true"
            (cardClick)="openDrillDown('words')"
          />
          <app-kpi-card
            title="Completed"
            [value]="articlesCompleted()"
            [sparklineData]="articlesSparkline()"
            [sparklineColor]="chartColors.chart4()"
            icon="checkmark"
            iconBgClass="bg-chart-4/10"
            iconColorClass="text-chart-4"
            [clickable]="true"
            (cardClick)="openDrillDown('completed')"
          />
          <app-streak-bar
            [current]="currentStreak()"
            [longest]="longestStreak()"
          />
          <app-kpi-card
            title="Backlog"
            [value]="backlogSize()"
            subtitle="articles to read"
            [sparklineColor]="chartColors.chart3()"
            icon="inbox"
            iconBgClass="bg-chart-3/10"
            iconColorClass="text-chart-3"
            [clickable]="true"
            (cardClick)="openDrillDown('backlog')"
          />
        </div>

        <!-- Reading Activity Chart (full width) -->
        <app-reading-activity-chart [data]="readingStats()" (zoomChange)="onChartZoom($event)" />

        <!-- Bottom Row: Pipeline, Highlights, Most Highlighted -->
        <div class="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
          <app-pipeline-card [data]="pipeline()" [periodLabel]="periodLabelText()" />
          <app-highlights-card [data]="highlights()" [periodLabel]="periodLabelText()" />
          <app-most-highlighted [documents]="topDocuments()" />
        </div>

        <app-dashboard-footer
          [pipeline]="pipeline()"
          [highlights]="highlights()"
        />
      </main>

      <!-- Drill-down sheet -->
      @if (drillDownType(); as type) {
        <app-stat-drill-down-sheet
          [type]="type"
          [summary]="drillDownSummary()"
          [documents]="drillDownDocuments()"
          [hasMore]="drillDownHasMore()"
          [loading]="drillDownLoading()"
          [loadingMore]="drillDownLoadingMore()"
          (loadMore)="loadMoreDrillDown()"
          (documentSelect)="navigateToDocument($event)"
        />
      }

      <!-- Loading overlay -->
      @if (loading()) {
        <div
          class="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm"
        >
          <div class="flex flex-col items-center gap-4">
            <hlm-spinner class="size-8 text-brand" aria-label="Loading analytics" />
            <p class="text-sm text-muted-foreground">Loading analytics...</p>
          </div>
        </div>
      }

    </div>
  `,
})
export class DashboardComponent {
  @ViewChild(StatDrillDownSheetComponent) private drillDownSheet?: StatDrillDownSheetComponent;

  private readonly analyticsService = inject(AnalyticsService);
  private readonly syncService = inject(SyncService);
  private readonly drillDownService = inject(DrillDownService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly chartColors = inject(ChartColorsService);

  readonly period = signal<Period>(30);
  readonly loading = signal(true);
  readonly lastSynced = signal<Date | null>(null);
  readonly data = signal<DashboardData | null>(null);
  readonly error = signal<string | null>(null);

  // Drill-down state
  readonly drillDownType = signal<DrillDownType | null>(null);
  readonly drillDownSummary = signal<DrillDownSummary | null>(null);
  readonly drillDownDocuments = signal<readonly DrillDownDocument[]>([]);
  readonly drillDownHasMore = signal(false);
  readonly drillDownNextCursor = signal<string | null>(null);
  readonly drillDownLoading = signal(false);
  readonly drillDownLoadingMore = signal(false);

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

  readonly periodLabelText = computed(() => {
    const currentPeriod = this.period();
    if (isCustomPeriod(currentPeriod)) {
      return `${currentPeriod.startDate} – ${currentPeriod.endDate}`;
    }
    switch (currentPeriod) {
      case 7:
        return 'Last 7 days';
      case 30:
        return 'Last 30 days';
      case 365:
        return 'Last year';
    }
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

  onChartZoom(customRange: CustomDateRange): void {
    this.period.set(customRange);
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

  openDrillDown(type: DrillDownType): void {
    // Reset state for new drill-down
    this.drillDownType.set(type);
    this.drillDownSummary.set(null);
    this.drillDownDocuments.set([]);
    this.drillDownHasMore.set(false);
    this.drillDownNextCursor.set(null);
    this.drillDownLoading.set(true);

    // Open the sheet first, then load data
    setTimeout(() => this.drillDownSheet?.open());

    this.loadDrillDownData(type);
  }

  loadMoreDrillDown(): void {
    const type = this.drillDownType();
    const cursor = this.drillDownNextCursor();
    if (!type || !cursor) return;

    this.drillDownLoadingMore.set(true);
    this.loadDrillDownData(type, cursor);
  }

  navigateToDocument(documentId: string): void {
    void this.router.navigate(['/library', documentId]);
  }

  private loadDrillDownData(type: DrillDownType, cursor?: string): void {
    const { startDate, endDate } = periodToDateRange(this.period());
    const params = { startDate, endDate, cursor };

    // Create correctly typed observable based on drill-down type
    type AnyDrillDownResponse = DrillDownResponse<
      WordsReadDocument | CompletedDocument | BacklogDocument
    >;

    let request$: Observable<AnyDrillDownResponse>;

    switch (type) {
      case 'words':
        request$ = this.drillDownService.getWordsRead(params);
        break;
      case 'completed':
        request$ = this.drillDownService.getCompleted(params);
        break;
      case 'backlog':
        request$ = this.drillDownService.getBacklog(params);
        break;
    }

    request$
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(() => of(null))
      )
      .subscribe((result) => {
        if (result) {
          this.drillDownSummary.set(result.summary);
          // Append documents if loading more, replace otherwise
          if (cursor) {
            this.drillDownDocuments.set([...this.drillDownDocuments(), ...result.documents]);
          } else {
            this.drillDownDocuments.set(result.documents);
          }
          this.drillDownHasMore.set(result.hasMore);
          this.drillDownNextCursor.set(result.nextCursor);
        }
        this.drillDownLoading.set(false);
        this.drillDownLoadingMore.set(false);
      });
  }
}
