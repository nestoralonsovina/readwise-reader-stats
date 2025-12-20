import { Component, input, computed } from '@angular/core';
import { PipelineResponse } from '../../../../core/models/api.models';
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
    <section hlmCard class="gap-0 p-5">
      <header hlmCardHeader class="mb-4 p-0">
        <h3 hlmCardTitle class="text-lg font-semibold">Content Pipeline</h3>
      </header>

      <div hlmCardContent class="p-0">
        @if (data(); as d) {
          <div class="space-y-4">
            @for (item of pipelineItems(); track item.label) {
              <div>
                <div class="mb-1 flex items-center justify-between text-sm">
                  <span class="text-muted-foreground">{{ item.label }}</span>
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
  readonly data = input<PipelineResponse | null>();

  private readonly locationColors: Record<string, string> = {
    new: '#3b82f6',
    later: '#f59e0b',
    shortlist: '#8b5cf6',
    archive: '#10b981',
    feed: '#6b7280',
  };

  private readonly locationLabels: Record<string, string> = {
    new: 'New',
    later: 'Later',
    shortlist: 'Shortlist',
    archive: 'Archive',
    feed: 'Feed',
  };

  readonly pipelineItems = computed<PipelineItem[]>(() => {
    const breakdown = this.data()?.breakdown?.byLocation ?? [];

    return breakdown.map((loc) => ({
      label: this.locationLabels[loc.location] ?? loc.location,
      count: loc.count,
      percentage: loc.percentage,
      color: this.locationColors[loc.location] ?? '#9ca3af',
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
