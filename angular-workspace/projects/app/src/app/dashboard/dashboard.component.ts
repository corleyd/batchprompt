import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { NavigationLayoutComponent, NavigationLink } from '../shared/components/navigation-layout/navigation-layout.component';
import { NotificationIndicatorComponent } from '../shared/components/notification-indicator.component';
import { NotificationService } from '../services/notification.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NavigationLayoutComponent, NotificationIndicatorComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  dashboardLinks: NavigationLink[] = [
    { path: 'home', label: 'Home', icon: 'Home' },
    { path: 'files/upload', label: 'Upload New File', icon: 'Upload' },
    { path: 'files', label: 'Files', exactMatch: true, icon: 'file-text' },
    { path: 'jobs', label: 'Jobs', icon: 'Briefcase' },
    { path: 'prompts', label: 'Prompts', icon: 'message-square' }
  ];
  
  sidebarCollapsed = false;
  isAuthenticated = false;
  private destroy$ = new Subject<void>();

  constructor(
    public auth: AuthService,
    private notificationService: NotificationService
  ) {}
  
  ngOnInit(): void {
    // Track authentication state
    this.auth.isAuthenticated$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isAuthenticated => {
        this.isAuthenticated = isAuthenticated;
        if (isAuthenticated) {
          this.notificationService.connect();
        }
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  onSidebarToggled(collapsed: boolean) {
    this.sidebarCollapsed = collapsed;
    // You could save this preference to localStorage or a user service
  }
}