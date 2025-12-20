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
  ApexTheme,
  ApexLegend,
} from 'ng-apexcharts';

@Component({
  selector: 'app-reading-activity-chart',
  standalone: true,
  imports: [NgApexchartsModule],
  template: `
    <div class="rounded-xl border border-border bg-card p-5">
      <h3 class="mb-4 text-lg font-semibold text-foreground">Reading Activity</h3>
      @if (data()) {
        <apx-chart
          [series]="series()"
          [chart]="chartOptions()"
          [xaxis]="xaxis()"
          [yaxis]="yaxis"
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
    height: 256,
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
          colors: isDark ? '#9ca3af' : '#6b7280',
        },
      },
      axisBorder: { show: false },
      axisTicks: { show: false },
    };
  });

  readonly yaxis: ApexYAxis[] = [
    {
      title: { text: 'Words Read', style: { fontWeight: 500 } },
      labels: {
        formatter: (val) => this.formatNumber(val),
      },
    },
    {
      opposite: true,
      title: { text: 'Articles', style: { fontWeight: 500 } },
      labels: {
        formatter: (val) => Math.round(val).toString(),
      },
    },
  ];

  readonly strokeOptions: ApexStroke = {
    curve: 'smooth',
    width: [0, 3],
  };

  readonly fillOptions: ApexFill = {
    type: ['gradient', 'solid'],
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.4,
      opacityTo: 0.1,
    },
  };

  readonly colors = ['#f59e0b', '#3b82f6'];

  readonly dataLabels: ApexDataLabels = { enabled: false };

  readonly tooltipOptions = computed<ApexTooltip>(() => ({
    theme: this.themeService.isDark() ? 'dark' : 'light',
  }));

  readonly gridOptions = computed<ApexGrid>(() => ({
    borderColor: this.themeService.isDark() ? '#374151' : '#e5e7eb',
    strokeDashArray: 4,
  }));

  readonly legendOptions = computed<ApexLegend>(() => ({
    position: 'top',
    horizontalAlign: 'right',
    labels: {
      colors: this.themeService.isDark() ? '#9ca3af' : '#6b7280',
    },
  }));

  private formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  private formatNumber(val: number): string {
    if (val >= 1_000_000) return `${(val / 1_000_000).toFixed(1)}M`;
    if (val >= 1_000) return `${(val / 1_000).toFixed(1)}K`;
    return val.toString();
  }
}
