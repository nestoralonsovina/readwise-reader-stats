import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { HlmSidebarImports } from '@spartan-ng/helm/sidebar';
import { HlmIconImports } from '@spartan-ng/helm/icon';
import { HlmBadgeImports } from '@spartan-ng/helm/badge';
import { provideIcons } from '@ng-icons/core';
import {
  lucideLayoutDashboard,
  lucideBarChart3,
  lucideInbox,
  lucideSparkles,
  lucideBookOpen,
  lucideSettings,
} from '@ng-icons/lucide';

interface NavItem {
  readonly label: string;
  readonly route: string;
  readonly iconName: string;
  readonly comingSoon: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    ...HlmSidebarImports,
    ...HlmIconImports,
    ...HlmBadgeImports,
  ],
  providers: [
    provideIcons({
      lucideLayoutDashboard,
      lucideBarChart3,
      lucideInbox,
      lucideSparkles,
      lucideBookOpen,
      lucideSettings,
    }),
  ],
  template: `
    <hlm-sidebar collapsible="icon">
      <!-- Header with logo -->
      <hlm-sidebar-header class="border-b border-sidebar-border p-4">
        <div class="flex items-center gap-3">
          <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand">
            <ng-icon name="lucideBookOpen" class="text-white" size="sm" />
          </div>
          <span class="text-lg font-semibold group-data-[collapsible=icon]:hidden">
            Reader Analytics
          </span>
        </div>
      </hlm-sidebar-header>

      <!-- Main content -->
      <hlm-sidebar-content>
        <hlm-sidebar-group>
          <ul hlmSidebarMenu>
            @for (item of mainNavItems; track item.route) {
              <li hlmSidebarMenuItem>
                <a
                  hlmSidebarMenuButton
                  [routerLink]="item.route"
                  routerLinkActive
                  #rla="routerLinkActive"
                  [isActive]="rla.isActive"
                  [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }"
                >
                  <ng-icon [name]="item.iconName" />
                  <span>{{ item.label }}</span>
                </a>
                @if (item.comingSoon) {
                  <span
                    hlmSidebarMenuBadge
                    class="bg-muted text-muted-foreground text-xs group-data-[collapsible=icon]:hidden"
                  >
                    Soon
                  </span>
                }
              </li>
            }
          </ul>
        </hlm-sidebar-group>

        <hlm-sidebar-separator />

        <hlm-sidebar-group>
          <ul hlmSidebarMenu>
            @for (item of secondaryNavItems; track item.route) {
              <li hlmSidebarMenuItem>
                <a
                  hlmSidebarMenuButton
                  [routerLink]="item.route"
                  routerLinkActive
                  #rla="routerLinkActive"
                  [isActive]="rla.isActive"
                >
                  <ng-icon [name]="item.iconName" />
                  <span>{{ item.label }}</span>
                </a>
                @if (item.comingSoon) {
                  <span
                    hlmSidebarMenuBadge
                    class="bg-muted text-muted-foreground text-xs group-data-[collapsible=icon]:hidden"
                  >
                    Soon
                  </span>
                }
              </li>
            }
          </ul>
        </hlm-sidebar-group>
      </hlm-sidebar-content>

      <!-- Footer with settings -->
      <hlm-sidebar-footer class="border-t border-sidebar-border">
        <ul hlmSidebarMenu>
          <li hlmSidebarMenuItem>
            <a hlmSidebarMenuButton routerLink="/settings">
              <ng-icon name="lucideSettings" />
              <span>Settings</span>
            </a>
            <span
              hlmSidebarMenuBadge
              class="bg-muted text-muted-foreground text-xs group-data-[collapsible=icon]:hidden"
            >
              Soon
            </span>
          </li>
        </ul>
      </hlm-sidebar-footer>
    </hlm-sidebar>
  `,
})
export class SidebarComponent {
  private readonly router = inject(Router);

  readonly mainNavItems: readonly NavItem[] = [
    {
      label: 'Dashboard',
      route: '/dashboard',
      iconName: 'lucideLayoutDashboard',
      comingSoon: false,
    },
    {
      label: 'Reading Stats',
      route: '/reading-stats',
      iconName: 'lucideBarChart3',
      comingSoon: true,
    },
    {
      label: 'Content Pipeline',
      route: '/pipeline',
      iconName: 'lucideInbox',
      comingSoon: true,
    },
    {
      label: 'Highlights',
      route: '/highlights',
      iconName: 'lucideSparkles',
      comingSoon: true,
    },
  ];

  readonly secondaryNavItems: readonly NavItem[] = [
    {
      label: 'Library',
      route: '/library',
      iconName: 'lucideBookOpen',
      comingSoon: true,
    },
  ];
}
