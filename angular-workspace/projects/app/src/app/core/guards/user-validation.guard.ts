import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, catchError, map, of, switchMap } from 'rxjs';
import { UserService } from '../../services/user.service';
import { AuthService } from '@auth0/auth0-angular';

@Injectable({
  providedIn: 'root'
})
export class UserValidationGuard {
  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    // First check if the user is already authenticated and validated
    if (this.userService.isUserValidated()) {
      return of(true);
    }

    // Check if the user is authenticated with Auth0
    return this.authService.isAuthenticated$.pipe(
      switchMap(isAuthenticated => {
        if (!isAuthenticated) {
          // If not authenticated, redirect to landing page
          return of(this.router.parseUrl('/'));
        }

        // User is authenticated, validate them
        return this.userService.validateUserOnLogin().pipe(
          map(user => {
            // User is validated, allow access
            return true;
          }),
          catchError(error => {
            console.error('Error in UserValidationGuard:', error);
            // If there's an error validating the user, redirect to landing page
            return of(this.router.parseUrl('/'));
          })
        );
      })
    );
  }
}