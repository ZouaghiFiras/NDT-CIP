import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { NotificationService } from '../services/notification.service';
import { ApiResponse } from '../../shared/models/api-response';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private notificationService: NotificationService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError(error => {
        let errorMessage = 'An unknown error occurred';

        // Handle API response errors
        if (error.error) {
          // Check if it's our standardized API response
          if (error.error.status !== undefined && error.error.message !== undefined) {
            errorMessage = error.error.message;

            // Show notification for API errors
            this.notificationService.error(errorMessage);

            // For 401 errors, redirect to login
            if (error.status === 401) {
              // The JWT interceptor will handle the token refresh
              // If refresh fails, the user will be logged out
            }

            // For 403 errors, show permission denied message
            if (error.status === 403) {
              errorMessage = 'You do not have permission to perform this action';
              this.notificationService.error(errorMessage);
            }

            // For 500 errors, show server error message
            if (error.status >= 500) {
              errorMessage = 'Server error. Please try again later';
              this.notificationService.error(errorMessage);
            }
          } else {
            // Handle non-standard error responses
            errorMessage = error.error.message || error.error || errorMessage;
          }
        } else {
          // Handle HTTP errors without error body
          switch (error.status) {
            case 400:
              errorMessage = 'Bad request. Please check your input';
              break;
            case 401:
              errorMessage = 'Unauthorized. Please login again';
              break;
            case 403:
              errorMessage = 'Forbidden. You do not have permission';
              break;
            case 404:
              errorMessage = 'Resource not found';
              break;
            case 429:
              errorMessage = 'Too many requests. Please try again later';
              break;
            default:
              errorMessage = `Error: ${error.status}`;
          }
        }

        // Log the error for debugging
        console.error('API Error:', error);

        // Show error notification
        this.notificationService.error(errorMessage);

        // Return the error to propagate it to the component
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
