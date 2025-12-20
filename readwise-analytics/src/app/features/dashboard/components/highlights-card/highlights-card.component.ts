import { Component, input } from '@angular/core';
import { HighlightResponse } from '../../../../core/models/api.models';
import { FormatNumberPipe } from '../../../../shared/pipes/format-number.pipe';
import { DecimalPipe } from '@angular/common';
import { HlmCardImports } from '@spartan-ng/helm/card';

@Component({
  selector: 'app-highlights-card',
  standalone: true,
  imports: [FormatNumberPipe, DecimalPipe, ...HlmCardImports],
  template: `
    <section hlmCard class="gap-0 p-5">
      <header hlmCardHeader class="mb-4 p-0">
        <h3 hlmCardTitle class="text-lg font-semibold">Highlights</h3>
      </header>

      <div hlmCardContent class="p-0">
        @if (data(); as d) {
          <!-- Main stats grid -->
          <div class="grid grid-cols-2 gap-4">
            <!-- Total highlights -->
            <div class="rounded-lg bg-muted/50 p-3">
              <p class="text-2xl font-bold text-foreground">
                {{ d.summary.total | formatNumber }}
              </p>
              <p class="text-xs text-muted-foreground">Total Highlights</p>
            </div>

            <!-- With notes -->
            <div class="rounded-lg bg-muted/50 p-3">
              <p class="text-2xl font-bold text-foreground">
                {{ d.summary.withNotes | formatNumber }}
              </p>
              <p class="text-xs text-muted-foreground">
                With Notes ({{ d.summary.notePercentage | number: '1.0-1' }}%)
              </p>
            </div>
          </div>

          <!-- Period comparison -->
          <div class="mt-4 rounded-lg border border-border p-3">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-muted-foreground">This Period</p>
                <p class="text-xl font-semibold text-foreground">
                  {{ d.summary.thisPeriod }}
                </p>
              </div>
              <div class="text-right">
                @if (d.summary.periodChangePercent !== null) {
                  <p
                    class="text-sm font-medium"
                    [class.text-green-600]="d.summary.periodChange >= 0"
                    [class.text-red-600]="d.summary.periodChange < 0"
                  >
                    {{ d.summary.periodChange >= 0 ? '+' : '' }}{{ d.summary.periodChange }}
                    ({{ d.summary.periodChange >= 0 ? '+' : '' }}{{ d.summary.periodChangePercent | number: '1.0-1' }}%)
                  </p>
                }
                <p class="text-xs text-muted-foreground">
                  vs previous ({{ d.summary.previousPeriod }})
                </p>
              </div>
            </div>
          </div>

          <!-- Average per document -->
          <div class="mt-3 text-center text-sm text-muted-foreground">
            Average: {{ d.summary.averagePerDocument | number: '1.1-1' }} highlights per document
          </div>
        } @else {
          <div class="flex h-48 items-center justify-center text-muted-foreground">
            No data available
          </div>
        }
      </div>
    </section>
  `,
})
export class HighlightsCardComponent {
  readonly data = input<HighlightResponse | null>();
}
