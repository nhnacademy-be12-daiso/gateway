package com.nhnacademy.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouterLocateConfig {

    @Bean
    public RouteLocator myRoute(RouteLocatorBuilder builder){
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
