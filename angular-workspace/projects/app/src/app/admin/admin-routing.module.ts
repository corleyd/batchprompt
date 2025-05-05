import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { AdminLayoutComponent } from './components/admin-layout/admin-layout.component';
import { UsersComponent } from './pages/users/users.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { ReportsComponent } from './pages/reports/reports.component';
import { AdminRoleGuard } from '../core/guards/admin-role.guard';
import { AdminJobsComponent } from './components/admin-jobs/admin-jobs.component';
import { UserDetailsComponent } from './pages/user-details/user-details.component';

const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [AdminRoleGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        component: AdminDashboardComponent
      },
      {
        path: 'users',
        component: UsersComponent
      },
      {
        path: 'users/:userId',
        component: UserDetailsComponent
      },
      {
        path: 'jobs',
        component: AdminJobsComponent
      },
      {
        path: 'settings',
        component: SettingsComponent
      },
      {
        path: 'reports',
        component: ReportsComponent
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }