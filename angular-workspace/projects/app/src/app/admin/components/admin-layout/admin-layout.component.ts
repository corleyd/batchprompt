import { Component } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { CommonModule } from '@angular/common'; 
import { RouterModule } from '@angular/router';
import { NavigationLayoutComponent, NavigationLink } from '../../../shared/components/navigation-layout/navigation-layout.component';

@Component({
  selector: 'app-admin-layout',
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss'],
  imports: [CommonModule, RouterModule, NavigationLayoutComponent],
  standalone: true
})
export class AdminLayoutComponent {
  adminLinks: NavigationLink[] = [
    { path: 'dashboard', label: 'Dashboard', icon: 'BarChart2' },
    { path: 'users', label: 'Users', icon: 'Users' },
    { path: 'jobs', label: 'Jobs', icon: 'Briefcase' },
    { path: 'waitlist', label: 'Waitlist', icon: 'Clock' },
    { path: 'settings', label: 'Settings', icon: 'Settings' },
    { path: 'reports', label: 'Reports', icon: 'BarChart2' }
  ];
  
  sidebarCollapsed = false;

  constructor(public auth: AuthService) {}
  
  onSidebarToggled(collapsed: boolean) {
    this.sidebarCollapsed = collapsed;
    // You could save this preference to localStorage or a user service
  }
}