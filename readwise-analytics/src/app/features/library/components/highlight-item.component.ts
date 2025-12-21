import { Component, input, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideStickyNote } from '@ng-icons/lucide';
import { HighlightDto } from '../../../core/models/api.models';

@Component({
  selector: 'app-highlight-item',
  standalone: true,
  imports: [DatePipe, ...HlmIconImports],
  providers: [provideIcons({ lucideStickyNote })],
  template: `
    <article class="px-6 py-4">
      <!-- Highlight text with quote styling -->
      <div class="flex gap-3">
        <div class="w-1 shrink-0 rounded-full bg-chart-1"></div>
        <div class="flex-1">
          <blockquote class="text-foreground">
            "{{ highlight().text }}"
          </blockquote>

          <!-- Date -->
          @if (highlight().createdAt) {
            <p class="mt-2 text-xs text-muted-foreground">
              {{ highlight().createdAt | date: 'MMM d, yyyy' }}
            </p>
          }

          <!-- Note (if exists) -->
          @if (highlight().note) {
            <div
              class="mt-3 rounded-lg border border-border bg-muted/50 p-3"
            >
              <div class="mb-1 flex items-center gap-1.5 text-xs text-muted-foreground">
                <ng-icon hlm name="lucideStickyNote" size="xs" />
                <span>Note</span>
              </div>
              <p class="text-sm text-foreground">{{ highlight().note }}</p>
            </div>
          }
        </div>
      </div>
    </article>
  `,
})
export class HighlightItemComponent {
  readonly highlight = input.required<HighlightDto>();
}
