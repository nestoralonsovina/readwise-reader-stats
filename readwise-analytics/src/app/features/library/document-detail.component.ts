import { Component, inject, signal, computed, DestroyRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap } from 'rxjs';

import { DocumentService } from '../../core/services/document.service';
import { DocumentDetailResponse } from '../../core/models/api.models';

import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { HlmProgressImports } from '@spartan-ng/helm/progress';
import { HlmSpinnerImports } from '@spartan-ng/helm/spinner';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { HlmCheckboxImports } from '@spartan-ng/helm/checkbox';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { provideIcons } from '@ng-icons/core';
import {
  lucideArrowLeft,
  lucideExternalLink,
  lucideBookOpen,
  lucideHighlighter,
  lucideStickyNote,
  lucideClock,
  lucideFileText,
  lucideBook,
  lucideFileType,
} from '@ng-icons/lucide';

import { HighlightListComponent } from './components/highlight-list.component';
import { ReadingTimelineComponent } from '../../shared/components/reading-timeline/reading-timeline.component';
import { FormatNumberPipe } from '../../shared/pipes/format-number.pipe';

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
  selector: 'app-document-detail',
  standalone: true,
  imports: [
    RouterLink,
    FormatNumberPipe,
    HighlightListComponent,
    ReadingTimelineComponent,
    ...HlmButtonImports,
    ...HlmBadgeImports,
    ...HlmProgressImports,
    ...HlmSpinnerImports,
    ...HlmIconImports,
    ...HlmCheckboxImports,
    ...HlmLabelImports,
  ],
  providers: [
    provideIcons({
      lucideArrowLeft,
      lucideExternalLink,
      lucideBookOpen,
      lucideHighlighter,
      lucideStickyNote,
      lucideClock,
      lucideFileText,
      lucideBook,
      lucideFileType,
    }),
  ],
  template: `
    <div class="min-h-screen bg-background">
      @if (loading()) {
        <div class="flex h-96 items-center justify-center">
          <hlm-spinner size="lg" />
        </div>
      } @else if (error()) {
        <div class="mx-auto max-w-4xl px-6 py-8">
          <div
            class="rounded-lg border border-red-200 bg-red-50 p-6 text-red-700 dark:border-red-800 dark:bg-red-900/20 dark:text-red-400"
          >
            <p class="font-medium">Failed to load document</p>
            <p class="mt-1 text-sm">{{ error() }}</p>
            <a routerLink="/dashboard" hlmBtn variant="link" class="mt-4 p-0">
              <ng-icon hlm name="lucideArrowLeft" size="sm" class="mr-1" />
              Back to Dashboard
            </a>
          </div>
        </div>
      } @else if (document(); as doc) {
        <!-- Header -->
        <header class="border-b border-border bg-card">
          <div class="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
            <!-- Breadcrumb -->
            <nav class="flex items-center gap-2 text-sm text-muted-foreground">
              <a routerLink="/dashboard" class="hover:text-foreground">Dashboard</a>
              <span>/</span>
              <span class="text-foreground">{{ doc.title ?? 'Untitled' }}</span>
            </nav>

            <!-- Open in Reader -->
            <a
              [href]="doc.sourceUrl"
              target="_blank"
              rel="noopener noreferrer"
              hlmBtn
              variant="outline"
              size="sm"
            >
              <ng-icon hlm name="lucideExternalLink" size="sm" class="mr-1.5" />
              Open in Reader
            </a>
          </div>
        </header>

        <main class="mx-auto max-w-4xl space-y-6 px-6 py-8">
          <!-- Document Header -->
          <div class="flex gap-6">
            <!-- Cover image -->
            <div
              class="flex h-32 w-24 shrink-0 items-center justify-center overflow-hidden rounded-lg"
              [class]="doc.coverUrl ? '' : gradientClass()"
            >
              @if (doc.coverUrl) {
                <img
                  [src]="doc.coverUrl"
                  [alt]="doc.title ?? 'Document cover'"
                  class="h-full w-full object-cover"
                />
              } @else {
                <ng-icon hlm [name]="categoryIcon()" size="xl" class="text-white/80" />
              }
            </div>

            <!-- Title, author, source -->
            <div class="flex-1">
              <h1 class="text-2xl font-bold text-foreground">
                {{ doc.title ?? 'Untitled' }}
              </h1>
              @if (doc.author) {
                <p class="mt-1 text-muted-foreground">by {{ doc.author }}</p>
              }
              <div class="mt-2 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                <span>{{ doc.source }}</span>
                @if (doc.location) {
                  <span hlmBadge variant="secondary" class="capitalize">{{ doc.location }}</span>
                }
                @if (doc.category) {
                  <span hlmBadge variant="outline" class="capitalize">{{ doc.category }}</span>
                }
              </div>

              <!-- Progress bar -->
              <div class="mt-4">
                <div class="mb-1 flex items-center justify-between text-sm">
                  <span class="text-muted-foreground">Reading Progress</span>
                  <span class="font-medium">{{ doc.readingProgress }}%</span>
                </div>
                <hlm-progress class="h-2" [value]="doc.readingProgress">
                  <hlm-progress-indicator class="bg-success" />
                </hlm-progress>
              </div>

              <!-- Tags -->
              @if (doc.tags.length > 0) {
                <div class="mt-3 flex flex-wrap gap-2">
                  @for (tag of doc.tags; track tag) {
                    <span hlmBadge variant="secondary" class="text-xs">#{{ tag }}</span>
                  }
                </div>
              }
            </div>
          </div>

          <!-- Stats Row -->
          <div class="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div class="rounded-lg border border-border bg-card p-4 text-center">
              <div class="flex items-center justify-center gap-1 text-muted-foreground">
                <ng-icon hlm name="lucideBookOpen" size="sm" />
              </div>
              <p class="mt-1 text-2xl font-bold">{{ doc.wordCount | formatNumber }}</p>
              <p class="text-xs text-muted-foreground">Words</p>
            </div>
            <div class="rounded-lg border border-border bg-card p-4 text-center">
              <div class="flex items-center justify-center gap-1 text-muted-foreground">
                <ng-icon hlm name="lucideHighlighter" size="sm" />
              </div>
              <p class="mt-1 text-2xl font-bold">{{ doc.stats.highlightCount }}</p>
              <p class="text-xs text-muted-foreground">Highlights</p>
            </div>
            <div class="rounded-lg border border-border bg-card p-4 text-center">
              <div class="flex items-center justify-center gap-1 text-muted-foreground">
                <ng-icon hlm name="lucideStickyNote" size="sm" />
              </div>
              <p class="mt-1 text-2xl font-bold">{{ doc.stats.notesCount }}</p>
              <p class="text-xs text-muted-foreground">Notes</p>
            </div>
            <div class="rounded-lg border border-border bg-card p-4 text-center">
              <div class="flex items-center justify-center gap-1 text-muted-foreground">
                <ng-icon hlm name="lucideClock" size="sm" />
              </div>
              <p class="mt-1 text-2xl font-bold">{{ doc.stats.estimatedReadingTime }}m</p>
              <p class="text-xs text-muted-foreground">Reading Time</p>
            </div>
          </div>

          <!-- Reading Timeline -->
          <app-reading-timeline
            [savedAt]="doc.savedAt"
            [firstOpenedAt]="doc.firstOpenedAt"
            [lastOpenedAt]="doc.lastOpenedAt"
            [completedAt]="doc.readingProgress === 100 ? doc.lastOpenedAt : null"
          />

          <!-- Highlights -->
          <app-highlight-list
            [highlights]="doc.highlights"
            [notesOnly]="notesOnly()"
            (notesOnlyChange)="notesOnly.set($event)"
          />
        </main>
      }
    </div>
  `,
})
export class DocumentDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly documentService = inject(DocumentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly document = signal<DocumentDetailResponse | null>(null);
  readonly notesOnly = signal(false);

  readonly gradientClass = computed(() => {
    const category = this.document()?.category ?? 'default';
    return CATEGORY_GRADIENTS[category] ?? CATEGORY_GRADIENTS['default'];
  });

  readonly categoryIcon = computed(() => {
    const category = this.document()?.category ?? 'default';
    return CATEGORY_ICONS[category] ?? CATEGORY_ICONS['default'];
  });

  constructor() {
    // Load document when route changes
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const id = params.get('id');
          if (!id) {
            this.error.set('Document ID is required');
            return of(null);
          }
          this.loading.set(true);
          this.error.set(null);
          return this.documentService.getDocument(id).pipe(
            catchError((err: unknown) => {
              const message = err instanceof Error ? err.message : 'Failed to load document';
              this.error.set(message);
              return of(null);
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (doc) => {
          this.document.set(doc);
          this.loading.set(false);
        },
      });
  }
}
