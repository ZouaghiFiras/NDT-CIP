import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip intercepting certain requests
    if (this.shouldSkipIntercept(request)) {
      return next.handle(request);
    }

    // Add correlation ID for request tracking
    const correlationId = this.generateCorrelationId();
    request = request.clone({
      setHeaders: {
        'X-Correlation-ID': correlationId
      }
    });

    // Add API version header
    request = request.clone({
      setHeaders: {
        'API-Version': '1.0'
      }
    });

    return next.handle(request);
  }

  private shouldSkipIntercept(request: HttpRequest<any>): boolean {
    // Skip intercepting certain URLs
    const skipUrls = [
      /\/assets\/.*/, // Static assets
      /\/api-docs\/.*/, // Swagger/OpenAPI docs
      /\/health$/, // Health check endpoint
    ];

    return skipUrls.some(url => url.test(request.url));
  }

  private generateCorrelationId(): string {
    // Generate a unique correlation ID for each request
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}
