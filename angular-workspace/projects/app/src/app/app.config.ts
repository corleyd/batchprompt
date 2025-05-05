import { ApplicationConfig, importProvidersFrom, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AuthModule, AuthService } from '@auth0/auth0-angular';
import { provideHttpClient, withInterceptors, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { from, Observable, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';

// Auth interceptor function
const authInterceptor = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const auth = inject(AuthService);
  
  return from(auth.getAccessTokenSilently()).pipe(
    catchError(error => {
      console.error('Error getting access token', error);
      return of(null);
    }),
    switchMap(token => {
      console.log('Token:', token); // This should now be hit for every request
      if (token) {
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
        return next(authReq);
      }
      return next(req);
    })
  );
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
    importProvidersFrom(
      AuthModule.forRoot({
        domain: 'auth.batchprompt.ai',
        clientId: 'YWtTkEee1GvY2vHz7jM5RmkaVU2Gc7NT',
        authorizationParams: {
          redirect_uri: window.location.origin,
          audience: 'https://api.batchprompt.ai'
        }
      })
    ),
    provideAnimations()
  ]
};
