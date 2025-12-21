import { Component, input, output } from '@angular/core';
import { TopDocumentDto } from '../../../../core/models/api.models';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideFileText, lucidePenSquare } from '@ng-icons/lucide';

@Component({
  selector: 'app-most-highlighted',
  standalone: true,
  imports: [...HlmCardImports, ...HlmAvatarImports, ...HlmBadgeImports, ...HlmIconImports],
  providers: [provideIcons({ lucideFileText, lucidePenSquare })],
  template: `
    <section hlmCard class="h-full gap-0 p-5">
      <header hlmCardHeader class="mb-4 flex items-center justify-between p-0">
        <h3 hlmCardTitle class="text-lg font-semibold">Most Highlighted</h3>
        <button
          class="text-xs text-brand hover:underline"
          (click)="viewAllClick.emit()"
        >
          View all
        </button>
      </header>

      <div hlmCardContent class="p-0">
        @if (documents() && documents()!.length > 0) {
          <div class="space-y-3">
            @for (doc of documents()!.slice(0, 5); track doc.documentId) {
              <div
                class="flex items-center gap-3 rounded-lg p-2 transition-colors hover:bg-muted/50"
              >
                <!-- Thumbnail -->
                <hlm-avatar class="h-12 w-12 flex-shrink-0 rounded-lg">
                  @if (doc.imageUrl) {
                    <img hlmAvatarImage [src]="doc.imageUrl" [alt]="doc.title ?? 'Document thumbnail'" />
                  }
                  <span hlmAvatarFallback class="rounded-lg bg-muted">
                    <ng-icon hlm name="lucideFileText" class="text-muted-foreground" />
                  </span>
                </hlm-avatar>

                <!-- Content -->
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium text-foreground">
                    {{ doc.title ?? 'Untitled' }}
                  </p>
                  <p class="text-xs text-muted-foreground capitalize">
                    {{ doc.category ?? 'article' }}
                  </p>
                </div>

                <!-- Badges -->
                <div class="flex flex-shrink-0 items-center gap-2">
                  @if (doc.hasNotes) {
                    <div
                      class="rounded-full bg-blue-500/10 p-1 text-blue-600 dark:text-blue-400"
                      title="Has notes"
                    >
                      <ng-icon hlm name="lucidePenSquare" size="sm" />
                    </div>
                  }
                  <span
                    hlmBadge
                    variant="outline"
                    class="rounded-full bg-brand/10 text-brand border-transparent"
                  >
                    {{ doc.highlightCount }}
                  </span>
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
    </section>
  `,
})
export class MostHighlightedComponent {
  readonly documents = input<readonly TopDocumentDto[] | null>();
  readonly viewAllClick = output<void>();
}
