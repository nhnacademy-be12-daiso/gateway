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
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class RouterLocateConfig {

    private final AuthorizationHeaderFilter authorizationHeaderFilter;
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String AUTH_LB_URL = "lb://TEAM3-AUTH";
    private static final String USER_LB_URL = "lb://TEAM3-USER";
    private static final String COUPON_LB_URL = "lb://TEAM3-COUPON";
    private static final String ORDER_PAYMENT_LB_URL = "lb://TEAM3-ORDER-PAYMENT";
    private static final String BOOKSEARCH_LB_URL = "lb://TEAM3-BOOKSEARCH";

    @Bean
    public RouteLocator myRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                // 아무나 접속할 수 있음(로그인, 회원가입)
                .route("team3-auth",
                        p -> p.path("/auth/**")
                                .uri(AUTH_LB_URL))
                .route("team3-user-public",
                        p -> p.path("/api/users/signup", "/api/users/find-id", "/api/users/find-password")
                                .uri(USER_LB_URL))

                // user
                .route("team3-user-protected",
                        p -> p.path("/api/users/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_USER))))
                                .uri(USER_LB_URL))
                .route("team3-user-admin",
                        p -> p.path("/api/admin/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_ADMIN))))
                                .uri((USER_LB_URL))
                )

                // coupon
                .route("team3-coupon-protected-admin",
                        p -> p.path("/api/coupons/policies/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_ADMIN))
                                ))
                                .uri(COUPON_LB_URL))
                .route("team3-coupon-public",
                        p -> p.path("/api/coupons/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_USER))))
                                .uri(COUPON_LB_URL))

                .route("team3-order-payment-public",
                        p -> p.path("/api/carts/**")
                                .uri("lb://TEAM3-ORDER-PAYMENT"))

                // order-payment
                .route("team3-order-payment",
                        p -> p.path("/api/order-payment/**", "/api/orders/**", "/api/payments/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(
                                                new AuthorizationHeaderFilter.Config(ROLE_USER))))
                                .uri(ORDER_PAYMENT_LB_URL))

                // booksearch
                .route("team3-booksearch",
                        p -> p.path("/api/books/**", "/api/search/**")
//                                .filters(f -> f.filter(
//                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_USER))))
                                .uri(BOOKSEARCH_LB_URL))
                .build();

    }
}
