package com.cinemasaar.api_gateway.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Logs every request that reaches the gateway, including ones rejected
 * by security (401/403) or with no matching route (404):
 *
 *   POST /api/auth/login -> 200 (85 ms)
 *   GET /api/admin/audit -> 403 (3 ms)
 *
 * It's a WebFilter (not a GlobalFilter) so it runs before security
 * and also sees rejected requests.
 */
@Component
public class RequestLoggingFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long start = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        return chain.filter(exchange).doFinally(signal -> {
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            log.info("{} {} -> {} ({} ms)",
                    request.getMethod(),
                    request.getURI().getRawPath(),
                    status != null ? status.value() : "-",
                    System.currentTimeMillis() - start);
        });
    }

    @Override
    public int getOrder() {
        // Run before Spring Security's filter chain (order -100)
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
