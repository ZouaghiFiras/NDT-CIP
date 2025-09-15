import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from '../../auth/services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { ApiResponse } from '../../shared/models/api-response';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(
    private authService: AuthService,
    private tokenStorageService: TokenStorageService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.tokenStorageService.getToken();

    // Add JWT token to the request headers if available
    if (token) {
      request = this.addTokenHeader(request, token);
    }

    return next.handle(request).pipe(
      catchError(error => {
        // Handle 401 Unauthorized errors
        if (error.status === 401) {
          return this.handle401Error(request, next);
        }

        // Handle other errors
        return throwError(error);
      })
    );
  }

  private addTokenHeader(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((token: string) => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(token);
          this.tokenStorageService.saveToken(token);

          // Retry the original request with the new token
          return next.handle(this.addTokenHeader(request, token));
        }),
        catchError(err => {
          this.isRefreshing = false;

          // If refresh token fails, logout the user
          this.authService.logout();
          return throwError(err);
        })
      );
    } else {
      // If we're already refreshing, wait for the new token and retry the request
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => next.handle(this.addTokenHeader(request, token)))
      );
    }
  }
}
