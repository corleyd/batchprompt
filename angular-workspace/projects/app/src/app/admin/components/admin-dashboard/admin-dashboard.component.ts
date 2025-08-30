import { Component } from '@angular/core';

interface StatsCard {
  title: string;
  value: string;
  icon: string;
  trend: string;
  color: string;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent {
  // Admin dashboard statistics (these would normally be fetched from a service)
  statsCards: StatsCard[] = [
  ];
}