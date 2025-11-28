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
                        p -> p.path("/auth/**")
                                .uri("lb://TEAM3-AUTH"))

                .route("team3-user-public",
                        p -> p.path("/api/users/signup", "/api/users/login",
                                        "/api/users/verify/**", "/api/users/check/**")
                                .uri("lb://TEAM3-USER"))

                .route("team3-user-protected",
                        p -> p.path("/api/users/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                                .uri("lb://TEAM3-USER"))

                .route("team3-coupon-public",
                        p -> p.path("/api/coupons/welcome/**")
                                .uri("lb://TEAM3-COUPON"))

                .route("team3-coupon-protected",
                        p -> p.path("/api/coupons/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                                .uri("lb://TEAM3-COUPON"))

                .route("team3-order-payment",
                        p -> p.path("/api/order-payment/**", "/api/orders/**", "/api/payments/**", "/api/carts/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                                .uri("lb://TEAM3-ORDER-PAYMENT"))

                .route("team3-booksearch",
                        p -> p.path("/api/books/**", "/api/search/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config())))
                                .uri("lb://TEAM3-BOOKSEARCH"))
                .build();

    }
}