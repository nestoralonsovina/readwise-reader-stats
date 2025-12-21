import { Component, input, computed, inject, output } from '@angular/core';
import { NgApexchartsModule } from 'ng-apexcharts';
import { ThemeService } from '../../../../core/services/theme.service';
import { ChartColorsService } from '../../../../core/services/chart-colors.service';
import { ReadingStatsResponse, CustomDateRange } from '../../../../core/models/api.models';
import { HlmCardImports } from '@spartan-ng/helm/card';
import {
  ApexChart,
  ApexAxisChartSeries,
  ApexXAxis,
  ApexYAxis,
  ApexStroke,
  ApexDataLabels,
  ApexTooltip,
  ApexGrid,
  ApexLegend,
} from 'ng-apexcharts';

interface ZoomEventAxis {
  readonly min: number;
  readonly max: number;
}

@Component({
  selector: 'app-reading-activity-chart',
  standalone: true,
  imports: [NgApexchartsModule, ...HlmCardImports],
  template: `
    <section hlmCard class="gap-0 p-5">
      <header hlmCardHeader class="mb-4 flex items-center justify-between p-0">
        <h3 hlmCardTitle class="font-semibold">Reading Activity</h3>
        <div class="flex items-center gap-4 text-sm">
          <div class="flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-full bg-chart-2"></span>
            <span class="text-muted-foreground">Words</span>
          </div>
          <div class="flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-full bg-chart-1"></span>
            <span class="text-muted-foreground">Articles</span>
          </div>
        </div>
      </header>
      <div hlmCardContent class="p-0">
        @if (data()) {
          <apx-chart
            [series]="series()"
            [chart]="chartOptions()"
            [xaxis]="xaxis()"
            [yaxis]="yaxis()"
            [stroke]="strokeOptions"
            [dataLabels]="dataLabels"
            [tooltip]="tooltipOptions()"
            [grid]="gridOptions()"
            [legend]="legendOptions()"
            [colors]="colors()"
          />
        } @else {
          <div class="flex h-64 items-center justify-center text-muted-foreground">
            No data available
          </div>
        }
      </div>
    </section>
  `,
})
export class ReadingActivityChartComponent {
  private static readonly CHART_HEIGHT = 256;
  private static readonly STROKE_WIDTH = 3;
  private static readonly GRID_DASH_ARRAY = 4;
  private static readonly MILLION_THRESHOLD = 1_000_000;
  private static readonly THOUSAND_THRESHOLD = 1_000;

  private readonly themeService = inject(ThemeService);
  private readonly chartColorsService = inject(ChartColorsService);

  readonly data = input<ReadingStatsResponse | null>();
  readonly zoomChange = output<CustomDateRange>();

  readonly series = computed<ApexAxisChartSeries>(() => {
    const stats = this.data()?.stats ?? [];
    return [
      {
        name: 'Words Read',
        type: 'line',
        data: stats.map((s) => ({ x: new Date(s.date).getTime(), y: s.wordsRead })),
      },
      {
        name: 'Articles Completed',
        type: 'line',
        data: stats.map((s) => ({ x: new Date(s.date).getTime(), y: s.articlesCompleted })),
      },
    ];
  });

  readonly chartOptions = computed<ApexChart>(() => ({
    type: 'line',
    height: ReadingActivityChartComponent.CHART_HEIGHT,
    toolbar: {
      show: true,
      tools: {
        download: false,
        selection: true,
        zoom: true,
        zoomin: true,
        zoomout: true,
        pan: false,
        reset: true,
      },
    },
    zoom: {
      enabled: true,
      type: 'x',
      autoScaleYaxis: true,
    },
    selection: {
      enabled: true,
      type: 'x',
    },
    background: 'transparent',
    fontFamily: 'inherit',
    events: {
      zoomed: (_chartContext: unknown, { xaxis }: { xaxis: ZoomEventAxis }) => {
        this.handleZoom(xaxis.min, xaxis.max);
      },
    },
  }));

  readonly xaxis = computed<ApexXAxis>(() => {
    const labelColor = this.chartColorsService.chartLabel();
    return {
      type: 'datetime',
      labels: {
        style: {
          colors: labelColor,
        },
        datetimeUTC: false,
      },
      axisBorder: { show: false },
      axisTicks: { show: false },
    };
  });

  private handleZoom(minTimestamp: number, maxTimestamp: number): void {
    const startDate = new Date(minTimestamp).toISOString().split('T')[0];
    const endDate = new Date(maxTimestamp).toISOString().split('T')[0];
    this.zoomChange.emit({ type: 'custom', startDate, endDate });
  }

  readonly yaxis = computed<ApexYAxis[]>(() => {
    const labelColor = this.chartColorsService.chartLabel();

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
    width: ReadingActivityChartComponent.STROKE_WIDTH,
  };

  readonly colors = computed(() => [
    this.chartColorsService.chart2(),
    this.chartColorsService.chart1(),
  ]);

  readonly dataLabels: ApexDataLabels = { enabled: false };

  readonly tooltipOptions = computed<ApexTooltip>(() => ({
    theme: this.themeService.isDark() ? 'dark' : 'light',
  }));

  readonly gridOptions = computed<ApexGrid>(() => ({
    borderColor: this.chartColorsService.chartGrid(),
    strokeDashArray: ReadingActivityChartComponent.GRID_DASH_ARRAY,
  }));

  readonly legendOptions = computed<ApexLegend>(() => ({
    show: false,
  }));

  private formatNumber(val: number): string {
    if (val >= ReadingActivityChartComponent.MILLION_THRESHOLD) {
      return `${(val / ReadingActivityChartComponent.MILLION_THRESHOLD).toFixed(1)}M`;
    }
    if (val >= ReadingActivityChartComponent.THOUSAND_THRESHOLD) {
      return `${(val / ReadingActivityChartComponent.THOUSAND_THRESHOLD).toFixed(1)}K`;
    }
    return val.toString();
  }
}
