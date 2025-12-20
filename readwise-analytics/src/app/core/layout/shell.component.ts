import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar.component';
import { HlmSidebarImports } from '@spartan-ng/helm/sidebar';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, ...HlmSidebarImports],
  template: `
    <hlm-sidebar-wrapper>
      <app-sidebar />
      <main hlmSidebarInset class="min-h-screen">
        <router-outlet />
      </main>
    </hlm-sidebar-wrapper>
  `,
})
export class ShellComponent {}
