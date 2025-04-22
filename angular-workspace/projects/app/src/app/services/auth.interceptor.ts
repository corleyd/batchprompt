import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, from, lastValueFrom } from 'rxjs';
import { AuthService } from '@auth0/auth0-angular';
import { mergeMap } from 'rxjs/operators';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  
  constructor(private auth: AuthService) {}
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Get the auth token from the service
    return from(this.getToken()).pipe(
      mergeMap(token => {
        // Clone the request and replace the original headers with
        // cloned headers, updated with the authorization
        if (token) {
          const authReq = req.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          });
          // Send the newly created request
          return next.handle(authReq);
        }
        // If no token, proceed with the original request
        return next.handle(req);
      })
    );
  }

  private async getToken(): Promise<string | null> {
    try {
      return await lastValueFrom(this.auth.getAccessTokenSilently());
    } catch (error) {
      console.error('Error getting access token', error);
      return null;
    }
  }
}