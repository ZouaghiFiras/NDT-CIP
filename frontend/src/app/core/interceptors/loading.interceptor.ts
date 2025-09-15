import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize, tap } from 'rxjs/operators';
import { LoadingService } from '../services/loading.service';

@Injectable()
export class LoadingInterceptor implements HttpInterceptor {
  private totalRequests = 0;
  private pendingRequests = 0;

  constructor(private loadingService: LoadingService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    this.totalRequests++;
    this.pendingRequests++;
    this.loadingService.setLoading(true);

    return next.handle(request).pipe(
      tap(
        () => {},
        (error) => {
          this.decrementPendingRequests();
        }
      ),
      finalize(() => {
        this.decrementPendingRequests();
      })
    );
  }

  private decrementPendingRequests() {
    this.pendingRequests--;
    if (this.pendingRequests === 0) {
      this.totalRequests = 0;
      this.loadingService.setLoading(false);
    }
  }
}
