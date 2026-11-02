package com.cinemasaar.api_gateway.Config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Gateway security. The gateway runs on WebFlux, so this uses ServerHttpSecurity
 * (not the HttpSecurity used in auth-service).
 *
 * Rules:
 *   - /api/auth/**                     public (register / login)
 *   - GET on the movie catalogue       public (movies, showtimes, theaters, screens, seats)
 *   - GET seat maps and add-on list    public
 *   - catalogue writes, OMDb lookups   ADMIN only
 *   - /api/admin/**, /api/v1/admin/**  ADMIN only
 *   - everything else                  any valid token
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /** movies-service paths: GET is public, every other method requires ADMIN. */
    private static final String[] CATALOGUE = {
            "/api/movies/**", "/api/showtimes/**", "/api/theaters/**", "/api/screens/**", "/api/seats/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveJwtDecoder jwtDecoder) {
        ServerAuthenticationEntryPoint unauthorized = (exchange, ex) ->
                writeError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        ServerAccessDeniedHandler forbidden = (exchange, ex) ->
                writeError(exchange, HttpStatus.FORBIDDEN, "You don't have permission to access this resource");

        http
                // Stateless JWT API: no CSRF, sessions, login form or basic auth
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                .cors(Customizer.withDefaults()) // uses the corsConfigurationSource bean below

                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()          // CORS preflight
                        .pathMatchers("/api/auth/**").permitAll()
                        // OMDb lookups spend the shared API quota: admins only
                        .pathMatchers("/api/movies/omdb/**").hasRole("ADMIN")
                        // Browsing the catalogue needs no login
                        .pathMatchers(HttpMethod.GET, CATALOGUE).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/showtimes/*/seats", "/api/v1/addons/**").permitAll()
                        // Any other method on the catalogue (import, create, edit, delete) is admin-only
                        .pathMatchers(CATALOGUE).hasRole("ADMIN")
                        .pathMatchers("/api/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated()
                )

                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden));

        return http.build();
    }

    /**
     * Validates tokens with jjwt, the same library and secret auth-service uses to sign them,
     * then hands Spring Security a Jwt object.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) must be at least 32 characters");
        }
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        JwtParser parser = Jwts.parser().verifyWith(key).build();

        return token -> {
            try {
                Jws<Claims> jws = parser.parseSignedClaims(token);
                Claims claims = jws.getPayload();

                Jwt.Builder builder = Jwt.withTokenValue(token)
                        .headers(h -> h.putAll(jws.getHeader()))
                        .claims(c -> c.putAll(claims));
                if (claims.getIssuedAt() != null) builder.issuedAt(claims.getIssuedAt().toInstant());
                if (claims.getExpiration() != null) builder.expiresAt(claims.getExpiration().toInstant());

                return Mono.just(builder.build());
            } catch (JwtException | IllegalArgumentException e) {
                return Mono.error(new BadJwtException("Invalid or expired token", e));
            }
        };
    }

    /** Turns the "role" claim (USER / ADMIN) into ROLE_USER / ROLE_ADMIN for hasRole(). */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /** CORS: lets the frontend (another port) call the API from the browser. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Same JSON error shape as auth-service. */
    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), status.value(), status.getReasonPhrase(), message,
                exchange.getRequest().getPath().value());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
