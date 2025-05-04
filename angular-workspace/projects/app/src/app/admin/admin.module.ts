import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AdminRoutingModule } from './admin-routing.module';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { AdminLayoutComponent } from './components/admin-layout/admin-layout.component';
import { UsersComponent } from './pages/users/users.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { ReportsComponent } from './pages/reports/reports.component';
import { AdminJobsComponent } from './components/admin-jobs/admin-jobs.component';

@NgModule({
  declarations: [
    AdminDashboardComponent,
    AdminLayoutComponent,
    UsersComponent,
    SettingsComponent,
    ReportsComponent,
    AdminJobsComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    AdminRoutingModule
  ]
})
export class AdminModule { }