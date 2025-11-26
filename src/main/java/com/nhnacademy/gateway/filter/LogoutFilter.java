/*
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * + Copyright 2025. NHN Academy Corp. All rights reserved.
 * + * While every precaution has been taken in the preparation of this resource,  assumes no
 * + responsibility for errors or omissions, or for damages resulting from the use of the information
 * + contained herein
 * + No part of this resource may be reproduced, stored in a retrieval system, or transmitted, in any
 * + form or by any means, electronic, mechanical, photocopying, recording, or otherwise, without the
 * + prior written permission.
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

package com.nhnacademy.gateway.filter;

import com.nhnacademy.gateway.jwt.properties.JwtProperties;
import com.nhnacademy.gateway.jwt.util.JwtUtil;
import java.util.Objects;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class LogoutFilter extends AbstractGatewayFilterFactory<LogoutFilter.Config> {

    private final JwtUtil jwtUtil;

    private final JwtProperties jwtProperties;

    private final StringRedisTemplate stringRedisTemplate;

    public LogoutFilter(JwtUtil jwtUtil, JwtProperties jwtProperties,
                        StringRedisTemplate stringRedisTemplate) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authorizationHeader = Objects.requireNonNull(
                    request.getHeaders().get(HttpHeaders.AUTHORIZATION)).getFirst();
            String token = authorizationHeader.replace(jwtProperties.getTokenPrefix() + " ", "");

            // 토큰 남은 시간 계산
            long remainingTime = jwtUtil.getRemainingExpiration(token);

            // Redis 블랙리스트 등록
            if (remainingTime > 0) {
                stringRedisTemplate.opsForValue().set(token, "logout", remainingTime);
            }

            // API로 요청을 넘기지 않고 여기서 200 OK 주고 끝냄
            ServerHttpResponse response = exchange.getResponse();

            response.setStatusCode(HttpStatus.OK);

            return response.setComplete();
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(httpStatus);

        return response.setComplete();
    }

}
