import { Component, input, output, computed } from '@angular/core';
import { HlmCheckboxImports } from '@spartan-ng/helm/checkbox';
import { HlmLabelImports } from '@spartan-ng/helm/label';
import { HighlightDto } from '../../../core/models/api.models';
import { HighlightItemComponent } from './highlight-item.component';

@Component({
  selector: 'app-highlight-list',
  standalone: true,
  imports: [...HlmCheckboxImports, ...HlmLabelImports, HighlightItemComponent],
  template: `
    <section class="rounded-lg border border-border bg-card">
      <!-- Header with filter -->
      <header class="flex items-center justify-between border-b border-border px-6 py-4">
        <h2 class="text-lg font-semibold">Highlights</h2>
        <label hlmLabel class="flex cursor-pointer items-center gap-2 text-sm">
          <hlm-checkbox
            [checked]="notesOnly()"
            (checkedChange)="notesOnlyChange.emit($event)"
          />
          With notes only
        </label>
      </header>

      <!-- Highlights list -->
      <div class="divide-y divide-border">
        @for (highlight of filteredHighlights(); track highlight.id) {
          <app-highlight-item [highlight]="highlight" />
        } @empty {
          <div class="px-6 py-8 text-center text-muted-foreground">
            @if (notesOnly()) {
              No highlights with notes
            } @else {
              No highlights yet
            }
          </div>
        }
      </div>
    </section>
  `,
})
export class HighlightListComponent {
  readonly highlights = input.required<readonly HighlightDto[]>();
  readonly notesOnly = input<boolean>(false);

  readonly notesOnlyChange = output<boolean>();

  readonly filteredHighlights = computed(() => {
    const all = this.highlights();
    if (!this.notesOnly()) {
      return all;
    }
    return all.filter((h) => h.note !== null);
  });
}
