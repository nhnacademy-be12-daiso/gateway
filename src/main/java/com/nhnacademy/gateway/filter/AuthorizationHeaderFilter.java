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

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    // Gateway 필터는 Factory를 만들어야만 스프링이 인식함
    // 이 클래스는 필터를 만드는 공장이고 설정은 Config 클래스를 쓸 거라고 선언

    private final JwtUtil jwtUtil;

    private final JwtProperties jwtProperties;

    private final StringRedisTemplate stringRedisTemplate;

    public AuthorizationHeaderFilter(JwtUtil jwtUtil, JwtProperties jwtProperties,
                                     StringRedisTemplate stringRedisTemplate) {
        super(Config.class);    // 이 라인이 있어야만 application.yml에서 설정을 읽어올 수 있음
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public static class Config {
        // 설정 값이 필요하면 여기에 추가
        // 특정 라우트마다 권한을 다르게 검사하고 싶을 때
    }

    @Override
    public GatewayFilter apply(Config config) { // 딱 한 번 실행되어 실제 필터 로직(GatewayFilter)을 생성 후 반환
        return (exchange, chain) -> {
            // ServerWebExchange = HttpServletRequest + HttpServletResponse
            // GatewayFilterChain = 다음 필터로 넘기라고 알려주는 역할
            ServerHttpRequest request = exchange.getRequest();

            // 헤더 포함 여부 확인
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);
            }
            String token = authorizationHeader.replace(jwtProperties.getTokenPrefix() + " ", "");

            // Redis 블랙리스트 확인 (Auth Server와 동일한 키 형식 사용)
            String isLogout = stringRedisTemplate.opsForValue().get("blacklist:" + token);

            if (isLogout != null && isLogout.equals("logout")) {
                return onError(exchange, "Token is in blacklist", HttpStatus.UNAUTHORIZED);
            }

            // 토큰 유효성 검증, Claims 추출
            if (!jwtUtil.isTokenValid(token)) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            String userId = jwtUtil.getLoginId(token);
            String role = jwtUtil.getRole(token);

            // WebFlux의 요청 객체는 불변이므로 직접 수정 불가
            // 대신 mutate()를 써서 헤더가 추가된 복제본을 새로 생성
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)    // API들한테 넘겨줄 ID
                    .header("X-Role", role)         // API들한테 넘겨줄 권한
                    .build();

            // 다음 필터에게 넘어갈 때, 원본이 아니라 헤더가 추가된 복제본을 쥐어줌
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    // WebFlux 방식으로 에러 응답 처리
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        // Mono는 결과가 0개 또는 1개인 약속
        // Mono<Void>는 반환할 데이터는 없고 작업이 끝났는지 알려주는 역할
        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(httpStatus);

        // 더 이상 처리하지 말고 지금 당장 클라이언트에게 응답을 보내라는 의미
        return response.setComplete();
    }

}
