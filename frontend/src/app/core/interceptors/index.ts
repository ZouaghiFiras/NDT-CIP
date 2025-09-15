// Re-export all interceptors
export * from './auth.interceptor';
export * from './error.interceptor';
export * from './jwt.interceptor';
export * from './loading.interceptor';

// Provider token for all interceptors
export const HTTP_INTERCEPTORS_PROVIDER = {
  provide: HTTP_INTERCEPTORS,
  useFactory: (authInterceptor: AuthInterceptor, 
               jwtInterceptor: JwtInterceptor, 
               errorInterceptor: ErrorInterceptor, 
               loadingInterceptor: LoadingInterceptor) => [
    authInterceptor, 
    jwtInterceptor, 
    errorInterceptor, 
    loadingInterceptor
  ],
  deps: [AuthInterceptor, JwtInterceptor, ErrorInterceptor, LoadingInterceptor],
  multi: true
};
