import { Component, input, computed } from '@angular/core';
import { NgApexchartsModule } from 'ng-apexcharts';
import { FormatNumberPipe } from '../../pipes/format-number.pipe';
import { ComingSoonBadgeComponent } from '../coming-soon-badge/coming-soon-badge.component';
import {
  ApexChart,
  ApexStroke,
  ApexFill,
  ApexTooltip,
  ApexAxisChartSeries,
} from 'ng-apexcharts';

export type KpiIconType = 'document' | 'checkmark' | 'flame' | 'inbox';

interface ChangeIndicator {
  readonly value: number;
  readonly direction: 'up' | 'down';
}


@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [NgApexchartsModule, FormatNumberPipe, ComingSoonBadgeComponent],
  template: `
    <div class="rounded-xl border border-border bg-card p-5 transition-colors">
      <div class="mb-3 flex items-start justify-between">
        <div class="flex items-center gap-2">
          <!-- Icon badge -->
          @if (icon()) {
            <div
              class="flex h-8 w-8 items-center justify-center rounded-lg"
              [class]="iconBgClass()"
            >
              @switch (icon()) {
                @case ('document') {
                  <svg
                    class="h-4 w-4"
                    [class]="iconColorClass()"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                    />
                  </svg>
                }
                @case ('checkmark') {
                  <svg
                    class="h-4 w-4"
                    [class]="iconColorClass()"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                }
                @case ('flame') {
                  <svg
                    class="h-4 w-4"
                    [class]="iconColorClass()"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z"
                    />
                  </svg>
                }
                @case ('inbox') {
                  <svg
                    class="h-4 w-4"
                    [class]="iconColorClass()"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                    />
                  </svg>
                }
              }
            </div>
          }
          <span class="text-sm text-muted-foreground">{{ title() }}</span>
        </div>

        <!-- Sparkline -->
        @if (sparklineData().length > 0) {
          <div class="h-8 w-16">
            <apx-chart
              [series]="series()"
              [chart]="chartOptions"
              [stroke]="strokeOptions"
              [fill]="fillOptions"
              [tooltip]="tooltipOptions"
              [colors]="[sparklineColor()]"
            />
          </div>
        }
      </div>

      <!-- Value and change indicator -->
      <div class="flex items-baseline gap-2">
        <span class="text-2xl font-bold text-foreground">
          {{ value() | formatNumber }}
        </span>

        @if (showComingSoon()) {
          <app-coming-soon-badge />
        } @else if (change(); as c) {
          <span
            class="flex items-center gap-0.5 text-xs font-medium"
            [class.text-emerald-500]="c.direction === 'up'"
            [class.text-red-500]="c.direction === 'down'"
          >
            @if (c.direction === 'up') {
              <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M5 10l7-7m0 0l7 7m-7-7v18"
                />
              </svg>
            } @else {
              <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M19 14l-7 7m0 0l-7-7m7 7V3"
                />
              </svg>
            }
            {{ c.value }}%
          </span>
        }
      </div>

      <!-- Subtitle -->
      @if (subtitleText()) {
        <p class="mt-1 text-xs text-muted-foreground">{{ subtitleText() }}</p>
      }
    </div>
  `,
})
export class KpiCardComponent {
  readonly title = input.required<string>();
  readonly value = input.required<number | null>();
  readonly subtitle = input<string>();
  readonly sparklineData = input<number[]>([]);
  readonly sparklineColor = input<string>('#f59e0b');
  readonly icon = input<KpiIconType>();
  readonly iconBgClass = input<string>('bg-blue-100 dark:bg-blue-900/30');
  readonly iconColorClass = input<string>('text-blue-600 dark:text-blue-400');
  readonly change = input<ChangeIndicator | null>(null);
  readonly showComingSoon = input<boolean>(false);

  /** Determines what subtitle text to display based on inputs */
  readonly subtitleText = computed<string | null>(() => {
    const customSubtitle = this.subtitle();
    if (customSubtitle) {
      return customSubtitle;
    }

    const hasChange = this.change() !== null;
    const isComingSoon = this.showComingSoon();
    if (hasChange && !isComingSoon) {
      return 'vs last month';
    }

    return null;
  });

  readonly series = computed<ApexAxisChartSeries>(() => [
    {
      name: this.title(),
      data: this.sparklineData(),
    },
  ]);

  readonly chartOptions: ApexChart = {
    type: 'area',
    height: 32,
    width: 64,
    sparkline: { enabled: true },
    animations: { enabled: false },
  };

  readonly strokeOptions: ApexStroke = {
    curve: 'smooth',
    width: 2,
  };

  readonly fillOptions: ApexFill = {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.4,
      opacityTo: 0.1,
      stops: [0, 100],
    },
  };

  readonly tooltipOptions: ApexTooltip = {
    enabled: false,
  };
}
