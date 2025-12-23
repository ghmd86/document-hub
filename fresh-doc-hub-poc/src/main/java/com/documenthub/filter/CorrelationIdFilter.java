package com.documenthub.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * WebFlux filter that extracts or generates correlation ID and propagates it via MDC.
 * Ensures correlation ID is available in all log statements across reactive boundaries.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-correlation-id";
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String REQUESTOR_ID_KEY = "requestorId";
    public static final String REQUESTOR_TYPE_KEY = "requestorType";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Extract or generate correlation ID
        String correlationId = extractOrGenerateCorrelationId(request);
        String requestorId = request.getHeaders().getFirst("X-requestor-id");
        String requestorType = request.getHeaders().getFirst("X-requestor-type");

        // Add correlation ID to response headers for tracing
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        // Set MDC values for the current thread
        MDC.put(CORRELATION_ID_KEY, correlationId);
        if (requestorId != null) {
            MDC.put(REQUESTOR_ID_KEY, requestorId);
        }
        if (requestorType != null) {
            MDC.put(REQUESTOR_TYPE_KEY, requestorType);
        }

        return chain.filter(exchange)
                .contextWrite(Context.of(
                        CORRELATION_ID_KEY, correlationId,
                        REQUESTOR_ID_KEY, requestorId != null ? requestorId : "",
                        REQUESTOR_TYPE_KEY, requestorType != null ? requestorType : ""
                ))
                .doFinally(signalType -> {
                    // Clean up MDC after request completes
                    MDC.remove(CORRELATION_ID_KEY);
                    MDC.remove(REQUESTOR_ID_KEY);
                    MDC.remove(REQUESTOR_TYPE_KEY);
                });
    }

    private String extractOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
