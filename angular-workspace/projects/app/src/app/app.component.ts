import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { Subscription } from 'rxjs';
import { ToolbarComponent } from './shared/components/toolbar/toolbar.component';
import { ConfirmationDialogComponent, ConfirmationDialogData } from './shared/components/confirmation-dialog/confirmation-dialog.component';
import { ConfirmationDialogService, DialogState } from './shared/services/confirmation-dialog.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, ToolbarComponent, ConfirmationDialogComponent ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'BatchPrompt';
  
  // Dialog state
  dialogState: DialogState = {
    isVisible: false,
    data: {
      title: '',
      message: '',
      confirmText: 'Confirm',
      cancelText: 'Cancel',
      isDangerous: false
    }
  };
  
  private dialogSubscription?: Subscription;

  constructor(
    public auth: AuthService, 
    private router: Router,
    private confirmationDialogService: ConfirmationDialogService
  ) {}

  ngOnInit(): void {
    // Subscribe to dialog state changes
    this.dialogSubscription = this.confirmationDialogService.dialogState$.subscribe(
      (state: DialogState) => {
        this.dialogState = state;
      }
    );
  }

  ngOnDestroy(): void {
    if (this.dialogSubscription) {
      this.dialogSubscription.unsubscribe();
    }
  }

  onDialogConfirmed(): void {
    this.confirmationDialogService.handleConfirm();
  }

  onDialogCancelled(): void {
    this.confirmationDialogService.handleCancel();
  }

  signup() {
    this.auth.loginWithRedirect({
      authorizationParams: { screen_hint: 'signup' }
    });
  }

  login() {
    this.auth.loginWithRedirect();
  }

  logout() {
    this.auth.logout({ logoutParams: { returnTo: window.location.origin } });
  }

  navigateToDashboard() {
    this.router.navigate(['/dashboard/home']);
  }  

  /**
   * Check if the current route is the root path
   * @returns boolean indicating if the current URL is the root path
   */
  isRootPath(): boolean {
    return this.router.url === '/' || this.router.url === '';
  }

  /**
   * Gets user's initials from their full name
   * @param name Full name of the user
   * @returns First letter of first name and first letter of last name if available
   */
  getUserInitials(name?: string): string {
    if (!name) return '?';
    
    const names = name.trim().split(' ');
    if (names.length === 1) return names[0].charAt(0).toUpperCase();
    
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }
}
