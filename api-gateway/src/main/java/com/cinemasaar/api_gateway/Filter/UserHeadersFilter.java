package com.cinemasaar.api_gateway.Filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * After the token is validated, passes the user to downstream services as headers:
 *
 *   X-User-Id     userId claim  (MongoDB id)
 *   X-User-Role   USER | ADMIN
 *   X-User-Email  sub claim
 *
 * Any X-User-* headers sent by the client are removed first, so nobody can
 * pretend to be another user by setting them in Postman or the browser.
 */
@Component
public class UserHeadersFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String USER_EMAIL = "X-User-Email";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> withUserHeaders(exchange, auth.getToken()))
                .switchIfEmpty(Mono.fromSupplier(() -> withUserHeaders(exchange, null))) // public endpoint, no user
                .flatMap(chain::filter);
    }

    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID);
                    headers.remove(USER_ROLE);
                    headers.remove(USER_EMAIL);
                    if (jwt != null) {
                        setIfPresent(headers, USER_ID, jwt.getClaimAsString("userId"));
                        setIfPresent(headers, USER_ROLE, jwt.getClaimAsString("role"));
                        setIfPresent(headers, USER_EMAIL, jwt.getSubject());
                    }
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private void setIfPresent(HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
