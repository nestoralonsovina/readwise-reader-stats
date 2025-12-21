import { Component, input, computed } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HlmProgressImports } from '@spartan-ng/helm/progress';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { provideIcons } from '@ng-icons/core';
import { lucideFileText, lucideBook, lucideFileType } from '@ng-icons/lucide';
import {
  DrillDownType,
  DrillDownDocument,
  WordsReadDocument,
  CompletedDocument,
  BacklogDocument,
} from '../../../core/models/api.models';

const CATEGORY_GRADIENTS: Record<string, string> = {
  article: 'bg-gradient-to-br from-blue-500 to-indigo-600',
  book: 'bg-gradient-to-br from-amber-500 to-orange-600',
  pdf: 'bg-gradient-to-br from-red-500 to-rose-600',
  tweet: 'bg-gradient-to-br from-cyan-500 to-blue-600',
  default: 'bg-gradient-to-br from-gray-500 to-slate-600',
};

const CATEGORY_ICONS: Record<string, string> = {
  article: 'lucideFileText',
  book: 'lucideBook',
  pdf: 'lucideFileType',
  default: 'lucideFileText',
};

@Component({
  selector: 'app-document-row',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ...HlmProgressImports, ...HlmIconImports],
  providers: [provideIcons({ lucideFileText, lucideBook, lucideFileType })],
  template: `
    <div
      class="flex cursor-pointer items-center gap-4 border-b border-border px-6 py-4 transition-colors hover:bg-muted/50"
    >
      <!-- Cover image or gradient fallback -->
      <div
        class="flex h-14 w-10 shrink-0 items-center justify-center overflow-hidden rounded"
        [class]="coverUrl() ? '' : gradientClass()"
      >
        @if (coverUrl(); as url) {
          <img [src]="url" [alt]="title()" class="h-full w-full object-cover" />
        } @else {
          <ng-icon hlm [name]="categoryIcon()" size="sm" class="text-white/80" />
        }
      </div>

      <!-- Title and source -->
      <div class="min-w-0 flex-1">
        <p class="truncate font-medium text-foreground">
          {{ title() ?? 'Untitled' }}
        </p>
        <p class="truncate text-sm text-muted-foreground">{{ source() }}</p>

        <!-- Progress bar for words-read type -->
        @if (type() === 'words' && readingProgress() !== null) {
          <div class="mt-1.5">
            <hlm-progress class="h-1.5" [value]="readingProgress()">
              <hlm-progress-indicator class="bg-success" />
            </hlm-progress>
          </div>
        }
      </div>

      <!-- Value column (type-dependent) -->
      <div class="shrink-0 text-right">
        @switch (type()) {
          @case ('words') {
            <p class="font-medium text-foreground">
              {{ wordsRead() | number }}
            </p>
            <p class="text-xs text-muted-foreground">
              @if (readingProgress() === 100) {
                Done
              } @else {
                {{ readingProgress() }}%
              }
            </p>
          }
          @case ('completed') {
            <p class="text-sm text-muted-foreground">
              {{ completedAt() | date: 'MMM d' }}
            </p>
          }
          @case ('backlog') {
            <p class="font-medium text-foreground">{{ daysWaiting() }}d</p>
            <p class="text-xs text-muted-foreground">waiting</p>
          }
        }
      </div>
    </div>
  `,
})
export class DocumentRowComponent {
  readonly document = input.required<DrillDownDocument>();
  readonly type = input.required<DrillDownType>();

  readonly coverUrl = computed(() => this.document().coverUrl);
  readonly title = computed(() => this.document().title);
  readonly source = computed(() => this.document().source);
  readonly category = computed(() => this.document().category ?? 'default');

  readonly gradientClass = computed(
    () => CATEGORY_GRADIENTS[this.category()] ?? CATEGORY_GRADIENTS['default']
  );

  readonly categoryIcon = computed(
    () => CATEGORY_ICONS[this.category()] ?? CATEGORY_ICONS['default']
  );

  // Type-specific computed values with type narrowing
  readonly wordsRead = computed(() => {
    const doc = this.document();
    return isWordsReadDocument(doc) ? doc.wordsRead : 0;
  });

  readonly readingProgress = computed(() => {
    const doc = this.document();
    return isWordsReadDocument(doc) ? doc.readingProgress : null;
  });

  readonly completedAt = computed(() => {
    const doc = this.document();
    return isCompletedDocument(doc) ? doc.completedAt : null;
  });

  readonly daysWaiting = computed(() => {
    const doc = this.document();
    return isBacklogDocument(doc) ? doc.daysWaiting : 0;
  });
}

// Type guards
function isWordsReadDocument(doc: DrillDownDocument): doc is WordsReadDocument {
  return 'wordsRead' in doc;
}

function isCompletedDocument(doc: DrillDownDocument): doc is CompletedDocument {
  return 'completedAt' in doc;
}

function isBacklogDocument(doc: DrillDownDocument): doc is BacklogDocument {
  return 'daysWaiting' in doc;
}
