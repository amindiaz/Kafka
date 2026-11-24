import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'donaciones' },
  {
    path: 'donaciones',
    canActivate: [authGuard],
    loadComponent: () => import('./features/donaciones/donaciones').then(m => m.Donaciones),
  },
  {
    path: 'necesidades',
    canActivate: [authGuard],
    loadComponent: () => import('./features/necesidades/necesidades').then(m => m.Necesidades),
  },
  {
    path: 'envios',
    canActivate: [authGuard],
    loadComponent: () => import('./features/envios/envios').then(m => m.Envios),
  },
  {
    path: 'rabbit',
    canActivate: [authGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () => import('./features/rabbit-admin/rabbit-admin').then(m => m.RabbitAdmin),
  },
  { path: '**', redirectTo: 'donaciones' },
];
