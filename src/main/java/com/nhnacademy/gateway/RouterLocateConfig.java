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
                .route("team3-auth",
                        p->p.path("/auth/**").and()
                        .uri("lb://TEAM3-AUTH"))
                .route("team3-coupon",
                        p -> p.path("/api/coupons/**").and()
                                .uri("lb://TEAM3-COUPON"))
                .route("team3-user",
                        p -> p.path("/api/users/**").and()
                                .uri("lb://TEAM3-USER"))
                .build();

    }
}