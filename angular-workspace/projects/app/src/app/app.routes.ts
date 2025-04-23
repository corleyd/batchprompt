import { Routes } from '@angular/router';
import { AuthGuard } from '@auth0/auth0-angular';

export const routes: Routes = [
  { 
    path: '', 
    redirectTo: 'home', 
    pathMatch: 'full' 
  },
  { 
    path: 'home', 
    loadComponent: () => import('./home/home.component').then(m => m.HomeComponent) 
  },
  { 
    path: 'profile', 
    loadComponent: () => import('./profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AuthGuard]
  },
  { 
    path: 'dashboard', 
    loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard]
  },
  { 
    path: 'prompts', 
    loadComponent: () => import('./prompts/prompt-list/prompt-list.component').then(m => m.PromptListComponent),
    canActivate: [AuthGuard]
  },
  { 
    path: 'prompts/new', 
    loadComponent: () => import('./prompts/prompt-edit/prompt-edit.component').then(m => m.PromptEditComponent),
    canActivate: [AuthGuard]
  },
  { 
    path: 'prompts/:id', 
    loadComponent: () => import('./prompts/prompt-edit/prompt-edit.component').then(m => m.PromptEditComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'files',
    loadChildren: () => import('./files/files.module').then(m => m.FilesModule),
    canActivate: [AuthGuard]
  },
  { 
    path: '**', 
    redirectTo: 'home' 
  }
];
