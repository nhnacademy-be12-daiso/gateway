package com.nhnacademy.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("========================================");
        log.info("[Global Filter] Request: {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath());
        log.info("[Global Filter] Headers: {}", exchange.getRequest().getHeaders());
        log.info("========================================");

        return chain.filter(exchange).then(Mono.fromRunnable(() -> log.info("[Global Filter] Response Status: {}", exchange.getResponse().getStatusCode())));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 가장 먼저 실행
    }
}

