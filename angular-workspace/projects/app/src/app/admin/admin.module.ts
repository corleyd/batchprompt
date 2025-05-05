import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AdminRoutingModule } from './admin-routing.module';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { UsersComponent } from './pages/users/users.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { ReportsComponent } from './pages/reports/reports.component';
import { AdminJobsComponent } from './components/admin-jobs/admin-jobs.component';
import { GenericTableModule } from '../shared/components/generic-table/generic-table.module';

// Import Angular Material modules
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { UserDetailsComponent } from './pages/user-details/user-details.component';

@NgModule({
  declarations: [
    AdminDashboardComponent,
    UsersComponent,
    SettingsComponent,
    ReportsComponent,
    AdminJobsComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    AdminRoutingModule,
    GenericTableModule,
    // Add Angular Material modules
    MatTabsModule,
    MatIconModule,
    MatButtonModule,
    // Import standalone component
    UserDetailsComponent
  ]
})
export class AdminModule { }