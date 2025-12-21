import { Component, input, computed, inject } from '@angular/core';
import { PipelineResponse } from '../../../../core/models/api.models';
import { ChartColorsService } from '../../../../core/services/chart-colors.service';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmProgressImports } from '@spartan-ng/helm/progress';

interface PipelineItem {
  readonly label: string;
  readonly count: number;
  readonly percentage: number;
  readonly color: string;
}

@Component({
  selector: 'app-pipeline-card',
  standalone: true,
  imports: [...HlmCardImports, ...HlmProgressImports],
  template: `
    <section hlmCard class="h-full gap-0 p-5">
      <header hlmCardHeader class="mb-4 flex items-center justify-between p-0">
        <h3 hlmCardTitle class="text-lg font-semibold">Content Pipeline</h3>
        @if (periodLabel()) {
          <span class="text-xs text-muted-foreground">{{ periodLabel() }}</span>
        }
      </header>

      <div hlmCardContent class="p-0">
        @if (data(); as d) {
          <div class="space-y-4">
            @for (item of pipelineItems(); track item.label) {
              <div>
                <div class="mb-1 flex items-center justify-between text-sm">
                  <div class="flex items-center gap-2">
                    <span
                      class="h-2 w-2 rounded-full"
                      [style.backgroundColor]="item.color"
                    ></span>
                    <span class="text-muted-foreground">{{ item.label }}</span>
                  </div>
                  <span class="font-medium text-foreground">{{ item.count }}</span>
                </div>
                <hlm-progress [value]="item.percentage" [max]="100" class="h-2 bg-muted">
                  <hlm-progress-indicator [style.backgroundColor]="item.color" />
                </hlm-progress>
              </div>
            }
          </div>

          <!-- Queue latency -->
          @if (d.period.averageQueueLatencyHours !== null) {
            <div class="mt-4 rounded-lg bg-muted/50 p-3">
              <p class="text-sm text-muted-foreground">
                Avg. queue time:
                <span class="font-medium text-foreground">
                  {{ formatLatency(d.period.averageQueueLatencyHours) }}
                </span>
              </p>
            </div>
          }
        } @else {
          <div class="flex h-48 items-center justify-center text-muted-foreground">
            No data available
          </div>
        }
      </div>
    </section>
  `,
})
export class PipelineCardComponent {
  private readonly chartColors = inject(ChartColorsService);

  readonly data = input<PipelineResponse | null>();
  readonly periodLabel = input<string>();

  private readonly locationColors = computed(() => ({
    new: this.chartColors.chart1(),
    later: this.chartColors.chart2(),
    shortlist: this.chartColors.chart3(),
    archive: this.chartColors.chart4(),
    feed: this.chartColors.chart5(),
  }));

  private readonly locationLabels: Record<string, string> = {
    new: 'New',
    later: 'Later',
    shortlist: 'Shortlist',
    archive: 'Archive',
    feed: 'Feed',
  };

  readonly pipelineItems = computed<PipelineItem[]>(() => {
    const breakdown = this.data()?.breakdown?.byLocation ?? [];
    const colors = this.locationColors();

    return breakdown.map((loc) => ({
      label: this.locationLabels[loc.location] ?? loc.location,
      count: loc.count,
      percentage: loc.percentage,
      color: colors[loc.location as keyof typeof colors] ?? this.chartColors.mutedForeground(),
    }));
  });

  formatLatency(hours: number | null): string {
    if (hours === null) {
      return 'N/A';
    }
    if (hours < 24) {
      return `${Math.round(hours)} hours`;
    }
    const days = Math.round(hours / 24);
    return `${days} day${days === 1 ? '' : 's'}`;
  }
}
