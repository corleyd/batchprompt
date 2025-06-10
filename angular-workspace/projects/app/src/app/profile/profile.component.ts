import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { UserService } from '../services/user.service';
import { ConfirmationDialogService } from '../shared/services/confirmation-dialog.service';
import { Subject, takeUntil, combineLatest } from 'rxjs';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {
  userProfile: any = null;
  creditBalance: number = 0;
  loading = true;
  error: string | null = null;
  
  private destroy$ = new Subject<void>();

  constructor(
    public auth: AuthService,
    private userService: UserService,
    private confirmationDialogService: ConfirmationDialogService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUserData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadUserData(): void {
    this.loading = true;
    this.error = null;
    
    // Load user profile and account data
    combineLatest([
      this.auth.user$,
      this.userService.creditBalance$
    ]).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: ([profile, balance]) => {
        this.userProfile = profile;
        this.creditBalance = balance || 0;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user data:', error);
        this.error = 'Failed to load user information';
        this.loading = false;
      }
    });
  }

  getUserInitials(name?: string): string {
    if (!name) {
      return 'U';
    }
    
    const nameParts = name.split(' ');
    if (nameParts.length > 1) {
      // Get first letter of first and last name
      return (nameParts[0][0] + nameParts[nameParts.length - 1][0]).toUpperCase();
    } else {
      // If only one name, return the first letter
      return nameParts[0][0].toUpperCase();
    }
  }

  confirmDeleteAccount(): void {
    this.confirmationDialogService.confirmDelete('your account and all associated data')
      .then((confirmed) => {
        if (confirmed) {
          this.deleteAccount();
        }
      });
  }

  private deleteAccount(): void {
    if (!this.userProfile?.sub) {
      return;
    }

    this.userService.deleteUser(this.userProfile.sub)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          alert('Your account has been successfully deleted.');
          this.auth.logout({ 
            logoutParams: { 
              returnTo: window.location.origin 
            } 
          });
        },
        error: (error) => {
          console.error('Error deleting account:', error);
          alert('Failed to delete account. Please try again or contact support.');
        }
      });
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard/home']);
  }
}