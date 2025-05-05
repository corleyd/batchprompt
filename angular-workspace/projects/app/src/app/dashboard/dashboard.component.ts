import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { NavigationLayoutComponent, NavigationLink } from '../shared/components/navigation-layout/navigation-layout.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NavigationLayoutComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  dashboardLinks: NavigationLink[] = [
    { path: 'home', label: 'Home', icon: 'Home' },
    { path: 'files/upload', label: 'Upload New File', icon: 'Upload' },
    { path: 'files', label: 'My Files', exactMatch: true, icon: 'file-text' },
    { path: 'jobs', label: 'Jobs', icon: 'Briefcase' },
    { path: 'prompts', label: 'Prompts', icon: 'message-square' }
  ];
  
  sidebarCollapsed = false;

  constructor(public auth: AuthService) {}
  
  onSidebarToggled(collapsed: boolean) {
    this.sidebarCollapsed = collapsed;
    // You could save this preference to localStorage or a user service
  }
}