import { Component, inject } from '@angular/core';
import { NgSwitch, NgSwitchCase } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { ComingSoonBadgeComponent } from '../coming-soon-badge/coming-soon-badge.component';

interface NavItem {
  readonly label: string;
  readonly route: string;
  readonly iconType: IconType;
  readonly comingSoon: boolean;
}

type IconType =
  | 'dashboard'
  | 'stats'
  | 'pipeline'
  | 'highlights'
  | 'library'
  | 'settings';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [NgSwitch, NgSwitchCase, RouterLink, RouterLinkActive, ComingSoonBadgeComponent],
  template: `
    <aside
      class="fixed inset-y-0 left-0 z-20 flex w-56 flex-col border-r border-border bg-card"
    >
      <!-- Logo -->
      <div class="border-b border-border p-4">
        <div class="flex items-center gap-3">
          <div
            class="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500"
          >
            <svg
              class="h-5 w-5 text-white"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
              />
            </svg>
          </div>
          <span class="text-lg font-semibold text-foreground">Reader Analytics</span>
        </div>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 space-y-1 p-3">
        @for (item of mainNavItems; track item.route) {
          <a
            [routerLink]="item.route"
            routerLinkActive="bg-amber-50 text-amber-700 dark:bg-amber-900/20 dark:text-amber-400 font-medium"
            [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <ng-container [ngSwitch]="item.iconType">
              <ng-container *ngSwitchCase="'dashboard'">
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z"/>
                </svg>
              </ng-container>
              <ng-container *ngSwitchCase="'stats'">
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
                </svg>
              </ng-container>
              <ng-container *ngSwitchCase="'pipeline'">
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
                </svg>
              </ng-container>
              <ng-container *ngSwitchCase="'highlights'">
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"/>
                </svg>
              </ng-container>
              <ng-container *ngSwitchCase="'library'">
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
                </svg>
              </ng-container>
              <ng-container *ngSwitchCase="'settings'">
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                </svg>
              </ng-container>
            </ng-container>
            <span class="flex-1">{{ item.label }}</span>
            @if (item.comingSoon) {
              <app-coming-soon-badge variant="muted" />
            }
          </a>
        }

        <!-- Separator -->
        <div class="my-4 border-t border-border"></div>

        <!-- Library -->
        @for (item of secondaryNavItems; track item.route) {
          <a
            [routerLink]="item.route"
            routerLinkActive="bg-amber-50 text-amber-700 dark:bg-amber-900/20 dark:text-amber-400"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <ng-container [ngSwitch]="item.iconType">
              <ng-container *ngSwitchCase="'library'">
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
                </svg>
              </ng-container>
            </ng-container>
            <span class="flex-1">{{ item.label }}</span>
            @if (item.comingSoon) {
              <app-coming-soon-badge variant="muted" />
            }
          </a>
        }
      </nav>

      <!-- Settings -->
      <div class="border-t border-border p-3">
        <a
          routerLink="/settings"
          class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
            />
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
            />
          </svg>
          <span class="flex-1">Settings</span>
          <app-coming-soon-badge variant="muted" />
        </a>
      </div>
    </aside>
  `,
})
export class SidebarComponent {
  private readonly router = inject(Router);

  readonly mainNavItems: readonly NavItem[] = [
    {
      label: 'Dashboard',
      route: '/dashboard',
      iconType: 'dashboard',
      comingSoon: false,
    },
    {
      label: 'Reading Stats',
      route: '/reading-stats',
      iconType: 'stats',
      comingSoon: true,
    },
    {
      label: 'Content Pipeline',
      route: '/pipeline',
      iconType: 'pipeline',
      comingSoon: true,
    },
    {
      label: 'Highlights',
      route: '/highlights',
      iconType: 'highlights',
      comingSoon: true,
    },
  ];

  readonly secondaryNavItems: readonly NavItem[] = [
    {
      label: 'Library',
      route: '/library',
      iconType: 'library',
      comingSoon: true,
    },
  ];
}
