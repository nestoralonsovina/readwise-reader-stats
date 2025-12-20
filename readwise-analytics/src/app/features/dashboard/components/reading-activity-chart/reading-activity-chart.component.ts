import { Component, input, computed, inject } from '@angular/core';
import { NgApexchartsModule } from 'ng-apexcharts';
import { ThemeService } from '../../../../core/services/theme.service';
import { ReadingStatsResponse } from '../../../../core/models/api.models';
import {
  ApexChart,
  ApexAxisChartSeries,
  ApexXAxis,
  ApexYAxis,
  ApexStroke,
  ApexFill,
  ApexDataLabels,
  ApexTooltip,
  ApexGrid,
  ApexLegend,
} from 'ng-apexcharts';

@Component({
  selector: 'app-reading-activity-chart',
  standalone: true,
  imports: [NgApexchartsModule],
  template: `
    <div class="rounded-xl border border-border bg-card p-5">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="font-semibold text-foreground">Reading Activity</h3>
        <div class="flex items-center gap-4 text-sm">
          <div class="flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-full bg-amber-500"></span>
            <span class="text-muted-foreground">Words</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-full bg-blue-500"></span>
            <span class="text-muted-foreground">Articles</span>
          </div>
        </div>
      </div>
      @if (data()) {
        <apx-chart
          [series]="series()"
          [chart]="chartOptions()"
          [xaxis]="xaxis()"
          [yaxis]="yaxis()"
          [stroke]="strokeOptions"
          [fill]="fillOptions"
          [dataLabels]="dataLabels"
          [tooltip]="tooltipOptions()"
          [grid]="gridOptions()"
          [legend]="legendOptions()"
          [colors]="colors"
        />
      } @else {
        <div class="flex h-64 items-center justify-center text-muted-foreground">
          No data available
        </div>
      }
    </div>
  `,
})
export class ReadingActivityChartComponent {
  private static readonly CHART_HEIGHT = 256;
  private static readonly STROKE_WIDTH = 3;
  private static readonly GRADIENT_OPACITY_FROM = 0.4;
  private static readonly GRADIENT_OPACITY_TO = 0.1;
  private static readonly GRID_DASH_ARRAY = 4;

  private static readonly COLORS = {
    WORDS: '#f59e0b',
    ARTICLES: '#3b82f6',
    LABEL_DARK: '#9ca3af',
    LABEL_LIGHT: '#6b7280',
    GRID_DARK: '#374151',
    GRID_LIGHT: '#e5e7eb',
  } as const;

  private readonly themeService = inject(ThemeService);

  readonly data = input<ReadingStatsResponse | null>();

  readonly series = computed<ApexAxisChartSeries>(() => {
    const stats = this.data()?.stats ?? [];
    return [
      {
        name: 'Words Read',
        type: 'area',
        data: stats.map((s) => s.wordsRead),
      },
      {
        name: 'Articles Completed',
        type: 'line',
        data: stats.map((s) => s.articlesCompleted),
      },
    ];
  });

  readonly chartOptions = computed<ApexChart>(() => ({
    type: 'line',
    height: ReadingActivityChartComponent.CHART_HEIGHT,
    toolbar: { show: false },
    background: 'transparent',
    fontFamily: 'inherit',
  }));

  readonly xaxis = computed<ApexXAxis>(() => {
    const stats = this.data()?.stats ?? [];
    const isDark = this.themeService.isDark();
    return {
      categories: stats.map((s) => this.formatDate(s.date)),
      labels: {
        style: {
          colors: isDark
            ? ReadingActivityChartComponent.COLORS.LABEL_DARK
            : ReadingActivityChartComponent.COLORS.LABEL_LIGHT,
        },
      },
      axisBorder: { show: false },
      axisTicks: { show: false },
    };
  });

  readonly yaxis = computed<ApexYAxis[]>(() => {
    const isDark = this.themeService.isDark();
    const labelColor = isDark
      ? ReadingActivityChartComponent.COLORS.LABEL_DARK
      : ReadingActivityChartComponent.COLORS.LABEL_LIGHT;

    return [
      {
        title: {
          text: 'Words Read',
          style: { fontWeight: 500, color: labelColor },
        },
        labels: {
          formatter: (val: number): string => this.formatNumber(val),
          style: { colors: labelColor },
        },
      },
      {
        opposite: true,
        title: {
          text: 'Articles',
          style: { fontWeight: 500, color: labelColor },
        },
        labels: {
          formatter: (val: number): string => Math.round(val).toString(),
          style: { colors: labelColor },
        },
      },
    ];
  });

  readonly strokeOptions: ApexStroke = {
    curve: 'smooth',
    width: [0, ReadingActivityChartComponent.STROKE_WIDTH],
  };

  readonly fillOptions: ApexFill = {
    type: ['gradient', 'solid'],
    gradient: {
      shadeIntensity: 1,
      opacityFrom: ReadingActivityChartComponent.GRADIENT_OPACITY_FROM,
      opacityTo: ReadingActivityChartComponent.GRADIENT_OPACITY_TO,
    },
  };

  readonly colors = [
    ReadingActivityChartComponent.COLORS.WORDS,
    ReadingActivityChartComponent.COLORS.ARTICLES,
  ];

  readonly dataLabels: ApexDataLabels = { enabled: false };

  readonly tooltipOptions = computed<ApexTooltip>(() => ({
    theme: this.themeService.isDark() ? 'dark' : 'light',
  }));

  readonly gridOptions = computed<ApexGrid>(() => ({
    borderColor: this.themeService.isDark()
      ? ReadingActivityChartComponent.COLORS.GRID_DARK
      : ReadingActivityChartComponent.COLORS.GRID_LIGHT,
    strokeDashArray: ReadingActivityChartComponent.GRID_DASH_ARRAY,
  }));

  readonly legendOptions = computed<ApexLegend>(() => ({
    show: false,
  }));

  private formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) {
      return dateStr;
    }
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  private formatNumber(val: number): string {
    if (val >= 1_000_000) return `${(val / 1_000_000).toFixed(1)}M`;
    if (val >= 1_000) return `${(val / 1_000).toFixed(1)}K`;
    return val.toString();
  }
}
