package com.example.catalog.vmazz.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * v-Mazz security: stateless JWT.
 *
 * Only the v-Mazz endpoints are protected. The existing AI / product-catalog endpoints
 * (/api/v1/products, /api/v1/ai, /api/v1/admin/products) stay open exactly as before -
 * protect them in a later step by removing the final permitAll rule.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityHandlers handlers)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())                       // stateless API, no cookies
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))   // H2 console
                .authorizeHttpRequests(auth -> auth
                        // public: sign in / register, browse the storefront
                        .requestMatchers(
                                antMatcher("/api/v1/auth/login"),
                                antMatcher("/api/v1/auth/register-customer"),
                                antMatcher("/api/v1/auth/register-company")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/v1/catalog/**")).permitAll()
                        .requestMatchers(antMatcher("/api/v1/auth/me"),
                                antMatcher("/api/v1/profile/**")).authenticated()

                        // company administrator only
                        .requestMatchers(
                                antMatcher("/api/v1/admin/users/**"),
                                antMatcher("/api/v1/admin/company/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/v1/admin/items/**")).hasRole("ADMIN")

                        // company administrator or sub user
                        .requestMatchers(
                                antMatcher("/api/v1/admin/items/**"),
                                antMatcher("/api/v1/admin/orders/**")).hasAnyRole("ADMIN", "SUB_ADMIN")

                        // buyers
                        .requestMatchers(
                                antMatcher("/api/v1/cart/**"),
                                antMatcher("/api/v1/orders/**")).hasRole("CUSTOMER")

                        // everything else (existing AI endpoints, Swagger, H2 console) stays open for now
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> {
                    oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()));
                    oauth.authenticationEntryPoint(handlers);
                    oauth.accessDeniedHandler(handlers);
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Turns the token's "role" claim (ADMIN, SUB_ADMIN, CUSTOMER) into ROLE_ authorities. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
