import { Component, input, output, computed, ElementRef, ViewChild } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HlmSheetImports } from '@spartan-ng/helm/sheet';
import { BrnSheetImports } from '@spartan-ng/brain/sheet';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { HlmSpinnerImports } from '@spartan-ng/helm/spinner';
import { provideIcons } from '@ng-icons/core';
import { lucideX } from '@ng-icons/lucide';
import { DocumentRowComponent } from '../document-row/document-row.component';
import {
  DrillDownType,
  DrillDownSummary,
  DrillDownDocument,
} from '../../../core/models/api.models';

const TITLE_MAP: Record<DrillDownType, string> = {
  words: 'Words Read',
  completed: 'Articles Completed',
  backlog: 'Reading Backlog',
};

const UNIT_MAP: Record<DrillDownType, string> = {
  words: 'words',
  completed: 'articles',
  backlog: 'documents',
};

@Component({
  selector: 'app-stat-drill-down-sheet',
  standalone: true,
  imports: [
    DecimalPipe,
    DocumentRowComponent,
    ...HlmSheetImports,
    ...BrnSheetImports,
    ...HlmButtonImports,
    ...HlmIconImports,
    ...HlmSpinnerImports,
  ],
  providers: [provideIcons({ lucideX })],
  template: `
    <hlm-sheet side="right">
      <!-- Hidden trigger for programmatic opening -->
      <button #trigger brnSheetTrigger class="hidden" aria-hidden="true"></button>

      <hlm-sheet-content *brnSheetContent="let ctx" class="flex w-full max-w-md flex-col p-0">
        <!-- Header -->
        <hlm-sheet-header class="border-b border-border px-6 py-4">
          <h2 hlmSheetTitle class="text-lg font-semibold">{{ title() }}</h2>
        </hlm-sheet-header>

        @if (loading()) {
          <!-- Loading state -->
          <div class="flex flex-1 items-center justify-center">
            <hlm-spinner size="lg" />
          </div>
        } @else {
          <!-- Summary -->
          @if (summary(); as s) {
            <div class="border-b border-border px-6 py-4">
              <div class="text-3xl font-bold text-foreground">
                {{ s.total | number }}
              </div>
              <div class="text-sm text-muted-foreground">
                Total {{ unitLabel() }}
                @if (s.changePercent !== null) {
                  <span
                    [class.text-success]="s.changePercent > 0"
                    [class.text-destructive]="s.changePercent < 0"
                  >
                    ({{ s.changePercent > 0 ? '+' : '' }}{{ s.changePercent | number: '1.1-1' }}%)
                  </span>
                }
              </div>
            </div>
          }

          <!-- Document count -->
          @if (documents().length > 0) {
            <div class="border-b border-border px-6 py-3 text-sm text-muted-foreground">
              {{ documents().length }}{{ hasMore() ? '+' : '' }} documents
            </div>
          }

          <!-- Document list -->
          <div class="flex-1 overflow-y-auto">
            @for (doc of documents(); track doc.id) {
              <app-document-row
                [document]="doc"
                [type]="type()"
                (click)="onDocumentClick(doc.id)"
              />
            } @empty {
              <div class="px-6 py-8 text-center text-muted-foreground">
                No documents found
              </div>
            }
          </div>

          <!-- Footer with load more -->
          @if (hasMore()) {
            <hlm-sheet-footer class="border-t border-border p-4">
              <button
                hlmBtn
                variant="outline"
                class="w-full"
                (click)="loadMore.emit()"
                [disabled]="loadingMore()"
              >
                @if (loadingMore()) {
                  <hlm-spinner size="sm" class="mr-2" />
                }
                Load more
              </button>
            </hlm-sheet-footer>
          }
        }
      </hlm-sheet-content>
    </hlm-sheet>
  `,
})
export class StatDrillDownSheetComponent {
  @ViewChild('trigger') private readonly trigger!: ElementRef<HTMLButtonElement>;

  readonly type = input.required<DrillDownType>();
  readonly summary = input<DrillDownSummary | null>(null);
  readonly documents = input<readonly DrillDownDocument[]>([]);
  readonly hasMore = input<boolean>(false);
  readonly loading = input<boolean>(false);
  readonly loadingMore = input<boolean>(false);

  readonly loadMore = output<void>();
  readonly documentSelect = output<string>();

  readonly title = computed(() => TITLE_MAP[this.type()]);
  readonly unitLabel = computed(() => UNIT_MAP[this.type()]);

  open(): void {
    this.trigger.nativeElement.click();
  }

  onDocumentClick(documentId: string): void {
    this.documentSelect.emit(documentId);
  }
}
