import { Component } from '@angular/core';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent {
  // Admin dashboard statistics (these would normally be fetched from a service)
  statsCards = [
    { title: 'Total Users', value: '1,243', icon: 'users', trend: '+12%', color: '#4C51BF' },
    { title: 'Active Jobs', value: '57', icon: 'activity', trend: '+3%', color: '#38B2AC' },
    { title: 'Storage Used', value: '512 GB', icon: 'database', trend: '+18%', color: '#ED8936' },
    { title: 'API Requests', value: '98.3K', icon: 'server', trend: '+27%', color: '#667EEA' }
  ];
}