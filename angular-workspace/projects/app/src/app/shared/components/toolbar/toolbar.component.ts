import { CommonModule } from '@angular/common';
import { Component, HostListener } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { IconsModule } from '../../../icons/icons.module';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-toolbar',
  standalone: true,
  imports: [CommonModule, RouterLink, IconsModule],
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent {
  title = 'BatchPrompt';
  showUserMenu = false;
  isAdmin$: Observable<boolean>;

  constructor(
    public auth: AuthService, 
    private router: Router,
    private userService: UserService
  ) {
    // Create an observable to check if the current user has admin role
    this.isAdmin$ = this.auth.user$.pipe(
      map(user => {
        if (!user) return false;
        // Check for admin role - adjust namespace according to your Auth0 setup
        const roles = user['https://batchprompt.ai/roles'] as string[] || [];
        return roles.includes('admin');
      })
    );
  }

  @HostListener('document:click', ['$event'])
  closeMenuOnClickOutside(event: MouseEvent) {
    const userButton = document.querySelector('.user-button');
    if (this.showUserMenu && userButton && !userButton.contains(event.target as Node)) {
      this.showUserMenu = false;
    }
  }

  toggleUserMenu(event: MouseEvent) {
    event.stopPropagation();
    this.showUserMenu = !this.showUserMenu;
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
    // Reset user validation state before logout
    this.userService.resetValidation();
    
    // Close user menu if open
    this.showUserMenu = false;
    
    // Perform Auth0 logout
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

