package com.nhnacademy.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;
import java.util.Objects;

@Configuration
public class RateLimiterConfig {
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            return Mono.just(
                    Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                            .getAddress().getHostAddress()
            );
        };
    }
}