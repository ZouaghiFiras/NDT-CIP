package com.network.digitaltwin.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.network.digitaltwin.service.security.UserDetailsServiceImpl;

import java.util.Arrays;

/**
 * Security configuration for the application.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, 
                         JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                         JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/devices/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/devices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/devices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/devices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/topology/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/topology/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/topology/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/topology/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/simulation/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/simulation/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/simulation/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/simulation/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/risk/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/risk/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/risk/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/risk/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/policy/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/policy/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/policy/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/policy/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/parametric/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/parametric/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/parametric/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/parametric/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/threats/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/threats/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/threats/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/threats/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/logs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/logs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/logs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/logs/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://your-frontend-domain.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
