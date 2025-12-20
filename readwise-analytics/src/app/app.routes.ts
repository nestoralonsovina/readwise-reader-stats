import { Routes } from '@angular/router';
import { ShellComponent } from './core/layout/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent
          ),
      },
      // Placeholder routes for "Coming Soon" pages
      {
        path: 'reading-stats',
        redirectTo: 'dashboard',
      },
      {
        path: 'pipeline',
        redirectTo: 'dashboard',
      },
      {
        path: 'highlights',
        redirectTo: 'dashboard',
      },
      {
        path: 'library',
        redirectTo: 'dashboard',
      },
      {
        path: 'settings',
        redirectTo: 'dashboard',
      },
    ],
  },
];
