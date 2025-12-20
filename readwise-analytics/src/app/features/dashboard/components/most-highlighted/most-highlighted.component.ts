import { Component, input } from '@angular/core';
import { TopDocumentDto } from '../../../../core/models/api.models';

@Component({
  selector: 'app-most-highlighted',
  standalone: true,
  template: `
    <div class="rounded-xl border border-border bg-card p-5">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-lg font-semibold text-foreground">Most Highlighted</h3>
      </div>

      @if (documents() && documents()!.length > 0) {
        <div class="space-y-3">
          @for (doc of documents()!.slice(0, 5); track doc.documentId) {
            <div
              class="flex items-center gap-3 rounded-lg p-2 transition-colors hover:bg-muted/50"
            >
              <!-- Thumbnail placeholder -->
              <div
                class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-lg bg-muted"
              >
                <svg
                  class="h-6 w-6 text-muted-foreground"
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
              </div>

              <!-- Content -->
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium text-foreground">
                  {{ doc.title ?? 'Untitled' }}
                </p>
                <p class="text-xs text-muted-foreground capitalize">
                  {{ doc.category ?? 'article' }}
                </p>
              </div>

              <!-- Highlight count badge -->
              <div
                class="flex-shrink-0 rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-600 dark:text-amber-400"
              >
                {{ doc.highlightCount }}
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="flex h-48 items-center justify-center text-muted-foreground">
          No highlights yet
        </div>
      }
    </div>
  `,
})
export class MostHighlightedComponent {
  readonly documents = input<readonly TopDocumentDto[] | null>();
}
