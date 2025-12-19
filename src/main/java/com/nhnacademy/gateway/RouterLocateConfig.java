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

package com.nhnacademy.gateway;

import com.nhnacademy.gateway.filter.AuthorizationHeaderFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@RequiredArgsConstructor
@Configuration
public class RouterLocateConfig {

    private final AuthorizationHeaderFilter authorizationHeaderFilter;
    private final KeyResolver userKeyResolver;

    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String AUTH_LB_URL = "lb://TEAM3-AUTH";
    private static final String USER_LB_URL = "lb://TEAM3-USER";
    private static final String COUPON_LB_URL = "lb://TEAM3-COUPON";
    private static final String ORDER_PAYMENT_LB_URL = "lb://TEAM3-ORDER-PAYMENT";
    private static final String BOOKSEARCH_LB_URL = "lb://TEAM3-BOOKSEARCH";

    @Bean
    @Primary
    public RedisRateLimiter commonRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    @Bean
    public RedisRateLimiter strictRateLimiter() {
        return new RedisRateLimiter(5, 10);
    }

    @Bean
    public RedisRateLimiter searchRateLimiter() {
        return new RedisRateLimiter(30, 60);
    }

    @Bean
    public RouteLocator myRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                // [Public] 인증 서비스
                .route("team3-auth",
                        p -> p.path("/auth/**")
                                .filters(f -> f.requestRateLimiter(c -> c
                                        .setRateLimiter(commonRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                                .uri(AUTH_LB_URL))

                // [Public] 유저 서비스 공개 API
                .route("team3-user-public",
                        p -> p.path("/api/users/signup", "/api/users/check-id",
                                        "/api/users/find-id", "/api/users/find-password")
                                .filters(f -> f.requestRateLimiter(c -> c
                                        .setRateLimiter(commonRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                                .uri(USER_LB_URL))

                // [Protected] 유저 서비스
                .route("team3-user-protected",
                        p -> p.path("/api/users/**")
                                .filters(f -> f
                                        .filter(authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_USER)))
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(strictRateLimiter())
                                                .setKeyResolver(userKeyResolver)))
                                .uri(USER_LB_URL))

                // [Protected] 관리자 API
                .route("team3-user-admin",
                        p -> p.path("/api/admin/**")
                                .filters(f -> f
                                        .filter(authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_ADMIN)))
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(commonRateLimiter())
                                                .setKeyResolver(userKeyResolver)))
                                .uri(USER_LB_URL))

                // [Protected] 배송비 정책
                .route("team3-delivery",
                        p -> p.path("/api/admin/deliveries/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_ADMIN))))
                                .uri(ORDER_PAYMENT_LB_URL))

                // [Protected] 포장 정책
                .route("team3-packaging",
                        p -> p.path("/api/admin/packagings/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_ADMIN))))
                                .uri(ORDER_PAYMENT_LB_URL))

                // [Protected] 쿠폰 관리 (ROLE_ADMIN)
                .route("team3-coupon-protected-admin",
                        p -> p.path("/api/coupons/policies/**")
                                .filters(f -> f
                                        .filter(authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_ADMIN)))
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(commonRateLimiter())
                                                .setKeyResolver(userKeyResolver)))
                                .uri(COUPON_LB_URL))

                // [Protected] 쿠폰 발급/사용 (ROLE_USER)
                .route("team3-coupon-public",
                        p -> p.path("/api/coupons/**")
                                .filters(f -> f
                                        .filter(authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_USER)))
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(strictRateLimiter())
                                                .setKeyResolver(userKeyResolver)))
                                .uri(COUPON_LB_URL))

                // [Public] 장바구니/주문/결제
                .route("team3-order-payment-public",
                        p -> p.path("/api/guest/**", "/api/carts/**",
                                        "/api/orders/**", "/api/payments/**")
                                .filters(f -> f.requestRateLimiter(c -> c
                                        .setRateLimiter(commonRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                                .uri(ORDER_PAYMENT_LB_URL))

                // [Public] 도서 검색
                .route("team3-booksearch",
                        p -> p.path("/api/books/**", "/api/search/**", "/api/reviews/**", "/api/likes/**",
                                        "/api/v2/books/**")
                                .filters(f -> f.requestRateLimiter(c -> c
                                        .setRateLimiter(searchRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                                .uri(BOOKSEARCH_LB_URL))

                .route("zipkin-ui",
                        p -> p.path("/zipkin/**")
                                .uri("http://zipkin:9411"))

                .build();
    }
}