import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink, ActivatedRoute, NavigationEnd } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { IconsModule } from '../../../icons/icons.module';
import { map, filter } from 'rxjs/operators';
import { Observable, Subscription } from 'rxjs';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-toolbar',
  standalone: true,
  imports: [CommonModule, RouterLink, IconsModule],
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent implements OnInit, OnDestroy {
  title = 'BatchPrompt';
  showUserMenu = false;
  showExamplesDropdown = false;
  isAdmin$: Observable<boolean>;
  dropdownCloseTimeout: any;
  private routerSubscription: Subscription = new Subscription();

  constructor(
    public auth: AuthService, 
    private router: Router,
    private userService: UserService,
    private activatedRoute: ActivatedRoute
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
    
    // Check if current URL is '/signup' and redirect to Auth0 signup
    if (this.router.url === '/signup') {
      this.signup();
    }
  }

  ngOnInit(): void {
    // Subscribe to router events to detect navigation to /signup
    this.routerSubscription = this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      if (event.url === '/signup') {
        this.signup();
      }
    });

    // Check current URL in case component initializes with /signup
    if (this.router.url === '/signup') {
      this.signup();
    }
  }

  ngOnDestroy(): void {
    // Clean up subscription when component is destroyed
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  private checkSignupRoute() {
    // Check if the current route is the signup route
    const isSignupRoute = this.router.url.includes('/signup');

    if (isSignupRoute) {
      // Trigger the signup function if on the signup route
      this.signup();
    }
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

  joinWaitlist() {
    this.router.navigate(['/request-access']);
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

  setDropdownCloseTimeout() {
    this.dropdownCloseTimeout = setTimeout(() => {
      this.showExamplesDropdown = false;
    }, 180);
  }

  clearDropdownCloseTimeout() {
    if (this.dropdownCloseTimeout) {
      clearTimeout(this.dropdownCloseTimeout);
      this.dropdownCloseTimeout = null;
    }
  }
}

