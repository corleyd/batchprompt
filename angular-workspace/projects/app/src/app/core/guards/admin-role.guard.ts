import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router, UrlTree } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { Observable, map, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminRoleGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    return this.auth.user$.pipe(
      map(user => {
        // Check if user is authenticated and has admin role
        // First check if user roles exist
        if (!user) {
          return this.router.parseUrl('/dashboard/home');
        }
        
        // Check for admin role
        // This assumes your Auth0 assigns roles in the "http://batchprompt.ai/roles" namespace
        // Adjust this according to your actual Auth0 configuration
        const roles = user['https://batchprompt.ai/roles'] as string[] || [];
        const isAdmin = roles.includes('admin');
        
        if (isAdmin) {
          return true;
        }
        
        // If not admin, redirect to regular dashboard
        return this.router.parseUrl('/dashboard/home');
      }),
      tap(result => {
        if (result !== true) {
          console.log('User does not have admin role. Redirecting to dashboard.');
        }
      })
    );
  }
}