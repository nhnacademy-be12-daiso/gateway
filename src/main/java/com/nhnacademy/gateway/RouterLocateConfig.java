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

    @Bean
    public RouteLocator myRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-public",
                        p -> p.path("/users/signup", "/users/login", "/users/verify/**", "/users/check/**")
                                .uri("lb://TEAM3-USER"))

                .route("user-protected",
                        p -> p.path("/users/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                                .uri("lb://TEAM3-USER"))

                .route("coupon-public",
                        p -> p.path("/coupons/welcome/**")
                                .uri("lb://TEAM3-COUPON"))

                .route("coupon-protected",
                        p -> p.path("/coupons/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                                .uri("lb://TEAM3-COUPON"))

                .route("order-payment",
                        p -> p.path("/order-payment/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                                .uri("lb://TEAM3-ORDER-PAYMENT"))

                .route("books-search",
                        p -> p.path("/books/**")
                                .uri("lb://TEAM3-BOOKSEARCH"))
                .build();
    }
}
