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
                        p->p.path("/auth/**")
                        .uri("lb://TEAM3-AUTH")).build();


//                .route("coupon",
//                p->p.path("/coupons/**")
//                        .uri("lb://COUPON"))
//                .route("shop-service",
//                        p->p.path("/shop-service/**")
//                                .uri("lb://SHOP-SERVICE"))
    }
}
