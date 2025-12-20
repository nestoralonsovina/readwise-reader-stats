import { Component, input, computed, inject } from '@angular/core';
import { NgApexchartsModule } from 'ng-apexcharts';
import { ThemeService } from '../../../../core/services/theme.service';
import { HighlightResponse } from '../../../../core/models/api.models';
import { FormatNumberPipe } from '../../../../shared/pipes/format-number.pipe';
import {
  ApexChart,
  ApexNonAxisChartSeries,
  ApexPlotOptions,
  ApexLegend,
  ApexDataLabels,
  ApexTooltip,
} from 'ng-apexcharts';

@Component({
  selector: 'app-highlights-card',
  standalone: true,
  imports: [NgApexchartsModule, FormatNumberPipe],
  template: `
    <div class="rounded-xl border border-border bg-card p-5">
      <h3 class="mb-4 text-lg font-semibold text-foreground">Highlights</h3>

      @if (data(); as d) {
        <div class="flex items-center gap-4">
          <!-- Donut chart -->
          <div class="relative">
            <apx-chart
              [series]="series()"
              [chart]="chartOptions()"
              [labels]="labels()"
              [colors]="chartColors()"
              [plotOptions]="plotOptions"
              [legend]="legendOptions"
              [dataLabels]="dataLabels"
              [tooltip]="tooltipOptions()"
            />
            <!-- Center text -->
            <div class="absolute inset-0 flex items-center justify-center">
              <div class="text-center">
                <p class="text-2xl font-bold text-foreground">
                  {{ d.summary.total | formatNumber }}
                </p>
                <p class="text-xs text-muted-foreground">Total</p>
              </div>
            </div>
          </div>

          <!-- Color list -->
          <div class="flex-1 space-y-2">
            @for (color of d.colorDistribution; track color.color) {
              <div class="flex items-center justify-between text-sm">
                <div class="flex items-center gap-2">
                  <div
                    class="h-3 w-3 rounded-full"
                    [style.backgroundColor]="getColorHex(color.color)"
                  ></div>
                  <span class="capitalize text-muted-foreground">{{ color.color }}</span>
                </div>
                <span class="font-medium text-foreground">{{ color.count }}</span>
              </div>
            }
          </div>
        </div>

        <!-- Period stats -->
        <div class="mt-4 rounded-lg bg-muted/50 p-3">
          <p class="text-sm text-muted-foreground">
            This period:
            <span class="font-medium text-foreground">{{ d.summary.thisPeriod }}</span>
            highlights
          </p>
        </div>
      } @else {
        <div class="flex h-48 items-center justify-center text-muted-foreground">
          No data available
        </div>
      }
    </div>
  `,
})
export class HighlightsCardComponent {
  private readonly themeService = inject(ThemeService);

  readonly data = input<HighlightResponse | null>();

  private readonly colorMap: Record<string, string> = {
    yellow: '#facc15',
    blue: '#3b82f6',
    green: '#22c55e',
    orange: '#f97316',
    pink: '#ec4899',
    purple: '#a855f7',
    red: '#ef4444',
  };

  readonly series = computed<ApexNonAxisChartSeries>(() => {
    const distribution = this.data()?.colorDistribution ?? [];
    return distribution.map((c) => c.count);
  });

  readonly labels = computed(() => {
    const distribution = this.data()?.colorDistribution ?? [];
    return distribution.map((c) => c.color);
  });

  readonly chartColors = computed(() => {
    const distribution = this.data()?.colorDistribution ?? [];
    return distribution.map((c) => this.getColorHex(c.color));
  });

  readonly chartOptions = computed<ApexChart>(() => ({
    type: 'donut',
    height: 180,
    width: 180,
  }));

  readonly plotOptions: ApexPlotOptions = {
    pie: {
      donut: {
        size: '70%',
        labels: {
          show: false,
        },
      },
    },
  };

  readonly legendOptions: ApexLegend = {
    show: false,
  };

  readonly dataLabels: ApexDataLabels = {
    enabled: false,
  };

  readonly tooltipOptions = computed<ApexTooltip>(() => ({
    theme: this.themeService.isDark() ? 'dark' : 'light',
  }));

  getColorHex(color: string): string {
    return this.colorMap[color] ?? '#9ca3af';
  }
}
