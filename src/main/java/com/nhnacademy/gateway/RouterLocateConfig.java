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
                .route("team3-auth",
                        p -> p.path("/auth/**")
                                .uri(AUTH_LB_URL))

                .route("team3-user-public",
                        p -> p.path("/api/users/signup")
                                .uri(USER_LB_URL))
                .route("team3-user-protected",
                        p -> p.path("/api/users/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_USER))))
                                .uri(USER_LB_URL))
                .route("team3-user-admin",
                        p -> p.path("/api/admin/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_ADMIN))))
                                .uri((USER_LB_URL))
                )
                .route("team3-coupon-protected",
                        p -> p.path("/api/coupons/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_USER))))
                                .uri(COUPON_LB_URL))
                .route("team3-order-payment",
                        p -> p.path("/api/order-payment/**", "/api/orders/**", "/api/payments/**", "/api/carts/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_USER))))
                                .uri(ORDER_PAYMENT_LB_URL))

                .route("team3-booksearch",
                        p -> p.path("/api/books/**", "/api/search/**")
                                .filters(f -> f.filter(
                                        authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_USER))))
                                .uri(BOOKSEARCH_LB_URL))
                .build();

    }
}