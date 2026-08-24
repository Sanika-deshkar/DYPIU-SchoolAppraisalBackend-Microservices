package com.director_appraisal.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-otp",
            "/api/auth/mfa"
    );

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        // 0. Extract or generate Correlation ID
        String incomingCorrId = request.getHeaders().getFirst("X-Correlation-Id");
        if (incomingCorrId == null || incomingCorrId.isBlank()) {
            incomingCorrId = request.getHeaders().getFirst("X-Correlation-ID");
        }
        if (incomingCorrId == null || incomingCorrId.isBlank()) {
            incomingCorrId = request.getHeaders().getFirst("X-Request-Id");
        }
        final String correlationId = (incomingCorrId != null && !incomingCorrId.isBlank() && incomingCorrId.length() <= 64)
                ? incomingCorrId.trim()
                : java.util.UUID.randomUUID().toString();

        long startTime = System.currentTimeMillis();
        log.info("[GATEWAY_REQUEST_START] correlationId={} method={} path={} ip={}",
                correlationId, method, path, request.getRemoteAddress() != null ? request.getRemoteAddress().getHostString() : "unknown");

        // Attach correlationId to outgoing response
        exchange.getResponse().getHeaders().set("X-Correlation-Id", correlationId);

        // 1. Allow OPTIONS requests (CORS preflight)
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 2. Allow public auth endpoints
        if (isPublicEndpoint(path)) {
            ServerHttpRequest mutatedReq = request.mutate()
                    .header("X-Correlation-Id", correlationId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedReq).build())
                    .doFinally(signalType -> {
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("[GATEWAY_REQUEST_END] correlationId={} method={} path={} status={} durationMs={}",
                                correlationId, method, path, exchange.getResponse().getStatusCode(), duration);
                    });
        }

        // 3. Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Authorization token is missing.", HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_MISSING", correlationId);
        }

        String token = authHeader.substring(7).trim();

        // 4. Validate token
        if (!jwtUtil.validateToken(token)) {
            return onError(exchange, "Invalid or expired authorization token.", HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID", correlationId);
        }

        // 5. Extract verified user claims and attach safe downstream headers
        String userEmail = jwtUtil.extractEmail(token);
        String userRole = jwtUtil.extractRole(token);
        String userSchool = jwtUtil.extractSchool(token);
        String userName = jwtUtil.extractName(token);
        String universityId = jwtUtil.extractUniversityId(token);
        String universityCode = jwtUtil.extractUniversityCode(token);

        ServerHttpRequest.Builder reqBuilder = request.mutate();
        // Strip any spoofed headers from client
        reqBuilder.headers(httpHeaders -> {
            httpHeaders.remove("X-User-Email");
            httpHeaders.remove("X-User-Role");
            httpHeaders.remove("X-User-School");
            httpHeaders.remove("X-User-Name");
            httpHeaders.remove("X-University-Id");
            httpHeaders.remove("X-University-Code");
        });

        reqBuilder.header("X-Correlation-Id", correlationId);
        if (userEmail != null) reqBuilder.header("X-User-Email", userEmail);
        if (userRole != null) reqBuilder.header("X-User-Role", userRole);
        if (userSchool != null) reqBuilder.header("X-User-School", userSchool);
        if (userName != null) reqBuilder.header("X-User-Name", userName);
        if (universityId != null) reqBuilder.header("X-University-Id", universityId);
        if (universityCode != null) reqBuilder.header("X-University-Code", universityCode);

        return chain.filter(exchange.mutate().request(reqBuilder.build()).build())
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[GATEWAY_REQUEST_END] correlationId={} method={} path={} status={} durationMs={}",
                            correlationId, method, path, exchange.getResponse().getStatusCode(), duration);
                });
    }

    private boolean isPublicEndpoint(String path) {
        if (path == null) return false;
        if (path.startsWith("/api/auth/") || path.equals("/api/auth") || path.startsWith("/uploads/") || path.startsWith("/api/attachments/public/") || path.startsWith("/api/users/university/")) {
            return true;
        }
        return PUBLIC_ENDPOINTS.stream().anyMatch(endpoint -> path.equalsIgnoreCase(endpoint) || path.startsWith(endpoint + "/"));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus, String errorCode, String correlationId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("X-Correlation-Id", correlationId);

        String path = exchange.getRequest().getURI().getPath();
        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"code\":\"%s\",\"message\":\"%s\",\"service\":\"api-gateway\",\"path\":\"%s\",\"correlationId\":\"%s\"}",
                java.time.Instant.now().toString(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                errorCode,
                err.replace("\"", "\\\""),
                path,
                correlationId
        );
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Run before routing
    }
}

