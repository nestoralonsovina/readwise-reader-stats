import { Component, input, computed, output } from '@angular/core';
import { NgApexchartsModule } from 'ng-apexcharts';
import { FormatNumberPipe } from '../../pipes/format-number.pipe';
import { ComingSoonBadgeComponent } from '../coming-soon-badge/coming-soon-badge.component';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import {
  lucideFileText,
  lucideCheckCircle,
  lucideFlame,
  lucideInbox,
  lucideArrowUp,
  lucideArrowDown,
} from '@ng-icons/lucide';
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

const iconMap: Record<KpiIconType, string> = {
  document: 'lucideFileText',
  checkmark: 'lucideCheckCircle',
  flame: 'lucideFlame',
  inbox: 'lucideInbox',
};

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [
    NgApexchartsModule,
    FormatNumberPipe,
    ComingSoonBadgeComponent,
    ...HlmCardImports,
    ...HlmIconImports,
  ],
  providers: [
    provideIcons({
      lucideFileText,
      lucideCheckCircle,
      lucideFlame,
      lucideInbox,
      lucideArrowUp,
      lucideArrowDown,
    }),
  ],
  template: `
    <section
      hlmCard
      class="gap-0 p-5 transition-colors"
      [class.cursor-pointer]="clickable()"
      [class.hover:bg-muted/50]="clickable()"
      (click)="onClick()"
    >
      <header hlmCardHeader class="mb-3 flex items-start justify-between p-0">
        <div class="flex items-center gap-2">
          <!-- Icon badge -->
          @if (icon(); as iconType) {
            <div
              class="flex h-8 w-8 items-center justify-center rounded-lg"
              [class]="iconBgClass()"
            >
              <ng-icon hlm [name]="iconName()" size="sm" [class]="iconColorClass()" />
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
      </header>

      <div hlmCardContent class="p-0">
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
              [class.text-success]="c.direction === 'up'"
              [class.text-destructive]="c.direction === 'down'"
            >
              <ng-icon
                hlm
                [name]="c.direction === 'up' ? 'lucideArrowUp' : 'lucideArrowDown'"
                size="xs"
              />
              {{ c.value }}%
            </span>
          }
        </div>

        <!-- Subtitle -->
        @if (subtitleText()) {
          <p class="mt-1 text-xs text-muted-foreground">{{ subtitleText() }}</p>
        }
      </div>
    </section>
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
  readonly clickable = input<boolean>(false);

  readonly cardClick = output<void>();

  onClick(): void {
    if (this.clickable()) {
      this.cardClick.emit();
    }
  }

  readonly iconName = computed(() => {
    const iconType = this.icon();
    return iconType ? iconMap[iconType] : '';
  });

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
