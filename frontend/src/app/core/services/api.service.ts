import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  /**
   * Generic GET request
   * @param endpoint API endpoint
   * @param params HTTP parameters
   * @returns Observable with response data
   */
  get<T>(endpoint: string, params?: HttpParams): Observable<T> {
    return this.http.get<ApiResponse<T>>(`${this.apiUrl}/${endpoint}`, { params })
      .pipe(
        map(response => this.unwrapResponse(response))
      );
  }

  /**
   * Generic POST request
   * @param endpoint API endpoint
   * @param payload Request body
   * @returns Observable with response data
   */
  post<T>(endpoint: string, payload: any): Observable<T> {
    return this.http.post<ApiResponse<T>>(`${this.apiUrl}/${endpoint}`, payload)
      .pipe(
        map(response => this.unwrapResponse(response))
      );
  }

  /**
   * Generic PUT request
   * @param endpoint API endpoint
   * @param payload Request body
   * @returns Observable with response data
   */
  put<T>(endpoint: string, payload: any): Observable<T> {
    return this.http.put<ApiResponse<T>>(`${this.apiUrl}/${endpoint}`, payload)
      .pipe(
        map(response => this.unwrapResponse(response))
      );
  }

  /**
   * Generic DELETE request
   * @param endpoint API endpoint
   * @returns Observable with response data
   */
  delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<ApiResponse<T>>(`${this.apiUrl}/${endpoint}`)
      .pipe(
        map(response => this.unwrapResponse(response))
      );
  }

  /**
   * Generic PATCH request
   * @param endpoint API endpoint
   * @param payload Request body
   * @returns Observable with response data
   */
  patch<T>(endpoint: string, payload: any): Observable<T> {
    return this.http.patch<ApiResponse<T>>(`${this.apiUrl}/${endpoint}`, payload)
      .pipe(
        map(response => this.unwrapResponse(response))
      );
  }

  /**
   * Generic GET request with full response
   * @param endpoint API endpoint
   * @param params HTTP parameters
   * @returns Observable with full HTTP response
   */
  getFull<T>(endpoint: string, params?: HttpParams): Observable<HttpResponse<T>> {
    return this.http.get<ApiResponse<T>>(`${this.apiUrl}/${endpoint}`, { 
      observe: 'response', 
      params 
    });
  }

  /**
   * Unwrap API response to extract data
   * @param response API response
   * @returns Response data
   */
  private unwrapResponse<T>(response: ApiResponse<T>): T {
    if (response.status) {
      return response.data;
    } else {
      throw new Error(response.message || 'API request failed');
    }
  }

  /**
   * Upload file
   * @param endpoint API endpoint
   * @param file File to upload
   * @param additionalData Additional data to send with the file
   * @returns Observable with response data
   */
  upload<T>(endpoint: string, file: File, additionalData: any = {}): Observable<T> {
    const formData: FormData = new FormData();
    formData.append('file', file);

    // Add additional data
    Object.keys(additionalData).forEach(key => {
      formData.append(key, additionalData[key]);
    });

    return this.http.post<ApiResponse<T>>(`${this.apiUrl}/${endpoint}`, formData)
      .pipe(
        map(response => this.unwrapResponse(response))
      );
  }

  /**
   * Download file
   * @param endpoint API endpoint
   * @param params HTTP parameters
   * @param filename Filename to save as
   */
  download(endpoint: string, params?: HttpParams, filename?: string): void {
    this.getFull(endpoint, params).subscribe({
      next: (response) => {
        const contentDisposition = response.headers.get('content-disposition');
        let defaultFilename = 'download';

        // Extract filename from content-disposition header if available
        if (contentDisposition) {
          const filenameRegex = /filename[^;=
]*=((['"]).*?|[^;
]*)/;
          const matches = filenameRegex.exec(contentDisposition);
          if (matches && matches[1]) {
            defaultFilename = matches[1].replace(/['"]/g, '');
          }
        }

        // Create blob from response
        const blob = new Blob([response.body], { 
          type: response.headers.get('content-type') || 'application/octet-stream' 
        });

        // Create download link
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename || defaultFilename;
        document.body.appendChild(a);
        a.click();

        // Clean up
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      },
      error: (error) => {
        console.error('Download failed:', error);
      }
    });
  }
}
