import { Component, input } from '@angular/core';
import { PipelineResponse, HighlightResponse } from '../../../../core/models/api.models';
import { FormatNumberPipe } from '../../../../shared/pipes/format-number.pipe';
import { ComingSoonBadgeComponent } from '../../../../shared/components/coming-soon-badge/coming-soon-badge.component';

@Component({
  selector: 'app-dashboard-footer',
  standalone: true,
  imports: [FormatNumberPipe, ComingSoonBadgeComponent],
  template: `
    <footer class="mt-6 border-t border-border pt-6">
      <div class="flex items-center justify-center gap-6 text-sm text-muted-foreground">
        @if (pipeline(); as p) {
          <span>{{ p.current.total | formatNumber }} documents</span>
        }

        @if (highlights(); as h) {
          <span>{{ h.summary.total | formatNumber }} highlights</span>
        }

        <span class="flex items-center gap-1.5">
          <span class="text-muted-foreground/50">-- tags</span>
          <app-coming-soon-badge variant="muted" />
        </span>
      </div>
    </footer>
  `,
})
export class DashboardFooterComponent {
  readonly pipeline = input<PipelineResponse | null>();
  readonly highlights = input<HighlightResponse | null>();
}
