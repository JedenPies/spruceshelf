import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/entry/entry.component').then((m) => m.EntryComponent),
  },
  {
    path: 'cataloging-session/:sessionId',
    loadComponent: () =>
      import('./components/cataloging-session/cataloging-session.component').then(
        (m) => m.CatalogingSessionComponent,
      ),
  },
  {
    path: 'cataloging-session/:sessionId/scanner',
    loadComponent: () =>
      import('./components/cataloging-session/scanner/scanner.component').then(
        (m) => m.ScannerComponent,
      ),
  },
];
