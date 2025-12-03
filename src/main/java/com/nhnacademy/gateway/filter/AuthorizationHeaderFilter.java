package com.nhnacademy.gateway.filter;

import com.nhnacademy.gateway.jwt.properties.JwtProperties;
import com.nhnacademy.gateway.jwt.util.JwtUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthorizationHeaderFilter(JwtUtil jwtUtil, JwtProperties jwtProperties,
                                     StringRedisTemplate stringRedisTemplate) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @NoArgsConstructor
    public static class Config {
        private String role;

        public Config(String role) {
            this.role = role;
        }

        public String getRole() {
            return role;
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            try {
                ServerHttpRequest request = exchange.getRequest();

                String token = resolveToken(request);
                if (token == null) {
                    return onError(exchange, "No authorization token", HttpStatus.UNAUTHORIZED);
                }

                if (stringRedisTemplate.hasKey("blacklist:" + token)) {
                    return onError(exchange, "Token is in blacklist", HttpStatus.UNAUTHORIZED);
                }

                if (!jwtUtil.isTokenValid(token)) {
                    return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
                }

                String userId = jwtUtil.getLoginId(token);
                String userRole = jwtUtil.getRole(token);

                if (config.getRole() != null) {
                    if (userRole == null || !userRole.equals(config.getRole())) {
                        log.warn("[Gateway] 권한 부족: required={}, actual={}", config.getRole(), userRole);
                        return onError(exchange, "Forbidden: Insufficient permissions", HttpStatus.FORBIDDEN);
                    }
                }

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-Role", userRole)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("[Gateway] Filter Error: {}", e.getMessage(), e);
                return onError(exchange, "Internal Server Error in Filter", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private String resolveToken(ServerHttpRequest request) {
        if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith(jwtProperties.getTokenPrefix())) {
                return authHeader.replace(jwtProperties.getTokenPrefix() + " ", "");
            }
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        log.error("[Gateway] Error: {} ({})", err, httpStatus);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}