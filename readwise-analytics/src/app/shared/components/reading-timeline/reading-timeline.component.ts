import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideCircleCheck } from '@ng-icons/lucide';

@Component({
  selector: 'app-reading-timeline',
  standalone: true,
  imports: [DatePipe, ...HlmIconImports],
  providers: [provideIcons({ lucideCircleCheck })],
  template: `
    <section class="rounded-lg border border-border bg-card p-5">
      <h3 class="mb-4 font-semibold">Reading Timeline</h3>
      <div class="grid grid-cols-2 gap-6 text-sm md:grid-cols-4">
        <div>
          <div class="mb-1 text-muted-foreground">Saved</div>
          <div class="font-medium">
            @if (savedAt(); as date) { {{ date | date: 'MMM d, y' }} } @else { — }
          </div>
        </div>
        <div>
          <div class="mb-1 text-muted-foreground">First Opened</div>
          <div class="font-medium">
            @if (firstOpenedAt(); as date) { {{ date | date: 'MMM d, y' }} } @else { — }
          </div>
        </div>
        <div>
          <div class="mb-1 text-muted-foreground">Last Read</div>
          <div class="font-medium">
            @if (lastReadAt(); as date) { {{ date | date: 'MMM d, y' }} } @else { — }
          </div>
        </div>
        <div>
          <div class="mb-1 text-muted-foreground">Completed</div>
          <div class="flex items-center gap-1.5 font-medium">
            @if (completedAt(); as date) {
              <ng-icon hlm name="lucideCircleCheck" size="sm" class="text-success" />
              {{ date | date: 'MMM d, y' }}
            } @else {
              —
            }
          </div>
        </div>
      </div>
    </section>
  `,
})
export class ReadingTimelineComponent {
  readonly savedAt = input<string | null>(null);
  readonly firstOpenedAt = input<string | null>(null);
  readonly lastReadAt = input<string | null>(null);
  readonly completedAt = input<string | null>(null);
}
