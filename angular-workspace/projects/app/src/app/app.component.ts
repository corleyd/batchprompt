import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'BatchPrompt';

  constructor(public auth: AuthService) {}

  login() {
    this.auth.loginWithRedirect();
  }

  logout() {
    this.auth.logout({ logoutParams: { returnTo: window.location.origin } });
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
