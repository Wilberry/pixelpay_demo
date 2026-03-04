package com.pixelwallet.config;

import com.pixelwallet.security.JwtAuthenticationFilter;
import com.pixelwallet.security.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig configures Spring Security for the PixelWallet application.
 * <p>
 * <strong>Security Strategy:</strong>
 * <ul>
 *   <li><strong>Authentication:</strong> JWT token-based (stateless, no server-side sessions)</li>
 *   <li><strong>Authorization:</strong> Role-based access control; most endpoints require authenticated user</li>
 *   <li><strong>Password Storage:</strong> BCrypt hashing with configurable strength</li>
 *   <li><strong>Session Management:</strong> STATELESS mode - each request independently authenticated</li>
 *   <li><strong>CSRF Protection:</strong> Disabled (stateless APIs don't need CSRF tokens)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Public Endpoints:</strong>
 * <ul>
 *   <li>GET / (homepage with UI)</li>
 *   <li>POST /api/auth/login</li>
 *   <li>POST /api/auth/register</li>
 *   <li>GET /api/auth/health</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Protected Endpoints:</strong> All other API endpoints require valid JWT Bearer token
 * sent via Authorization header: {@code Authorization: Bearer <token>}
 * </p>
 *
 * @author PixelWallet Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    /**
     * Constructs security configuration with required dependencies.
     *
     * @param jwtProvider JWT token generation and validation service
     * @param userDetailsService Service for loading user details by username
     */
    public SecurityConfig(JwtProvider jwtProvider, UserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Configures password encoder using BCrypt algorithm.
     * <p>
     * BCrypt benefits:
     * <ul>
     *   <li>Automatically salts passwords</li>
     *   <li>Computationally expensive (resistant to brute force)</li>
     *   <li>Strength is configurable (currently uses default of 10 rounds)</li>
     * </ul>
     * </p>
     *
     * @return BCryptPasswordEncoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures Data Access Object (DAO) authentication provider.
     * <p>
     * AuthenticationProvider that uses UserDetailsService and PasswordEncoder
     * to authenticate users against stored credentials.
     * </p>
     *
     * @return Configured DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Provides the AuthenticationManager bean for use throughout the application.
     * <p>
     * AuthenticationManager is used to authenticate credentials during login.
     * </p>
     *
     * @param config AuthenticationConfiguration from Spring Security
     * @return AuthenticationManager bean
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Creates JWT authentication filter bean.
     * <p>
     * Filter extracts JWT token from Authorization header, validates signature,
     * and populates Spring Security context with authenticated user.
     * </p>
     *
     * @return Configured JwtAuthenticationFilter
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userDetailsService);
    }

    /**
     * Configures security filter chain and HTTP security rules.
     * <p>
     * <strong>Security Configuration:</strong>
     * <ul>
     *   <li>CSRF disabled (stateless API, no session)</li>
     *   <li>Session creation set to STATELESS</li>
     *   <li>Exception handling configured with custom entry point</li>
     *   <li>Public endpoints permitted without authentication</li>
     *   <li>All other endpoints require valid JWT token</li>
     *   <li>JWT filter added before UsernamePasswordAuthenticationFilter</li>
     * </ul>
     * </p>
     *
     * @param http HttpSecurity object for configuring HTTP security
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF protection for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Custom exception handling for unauthorized requests
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized\"}");
                }))
            
            // Use STATELESS session management (no session cookies)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Define authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/").permitAll()
                .requestMatchers("/index.html", "/css/**", "/js/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Protected endpoints - authentication required
                .anyRequest().authenticated())
            
            // Use custom authentication provider and JWT filter
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
