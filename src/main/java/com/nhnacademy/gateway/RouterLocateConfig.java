package com.nhnacademy.gateway;

import com.nhnacademy.gateway.filter.AuthorizationHeaderFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public RouteLocator myRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                // 1. [Public] 인증 서비스 (로그인, 회원가입 등)
                // - 로그인 시도는 넉넉하게, 혹은 IP 기반 제한
                .route("team3-auth",
                        p -> p.path("/auth/**")
                                .filters(f -> f.requestRateLimiter(c -> c
                                        .setRateLimiter(new RedisRateLimiter(20, 40)) // 초당 20개
                                        .setKeyResolver(userKeyResolver)))
                                .uri(AUTH_LB_URL))

                // 2. [Public] 유저 서비스 공개 API (회원가입, ID찾기 등)
                .route("team3-user-public",
                        p -> p.path("/api/users/signup", "/api/users/check-id",
                                        "/api/users/find-id", "/api/users/find-password")
                                .filters(f -> f.requestRateLimiter(c -> c
                                        .setRateLimiter(new RedisRateLimiter(10, 20))
                                        .setKeyResolver(userKeyResolver)))
                                .uri(USER_LB_URL))

                // 3. [Protected] 유저 서비스 (ROLE_USER) -> 유저 ID 기준 제한
                .route("team3-user-protected",
                        p -> p.path("/api/users/**")
                                .filters(f -> f
                                        // (1) 인증 필터 먼저 실행 (X-User-Id 헤더 생성)
                                        .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_USER)))
                                        // (2) 그 다음 Rate Limiter 실행 (유저 ID로 카운팅)
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(new RedisRateLimiter(5, 10)) // 초당 5개
                                                .setKeyResolver(userKeyResolver)))
                                .uri(USER_LB_URL))

                // 4. [Protected] 관리자 API (ROLE_ADMIN)
                .route("team3-user-admin",
                        p -> p.path("/api/admin/**")
                                .filters(f -> f
                                        .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_ADMIN)))
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(new RedisRateLimiter(10, 20))
                                                .setKeyResolver(userKeyResolver)))
                                .uri(USER_LB_URL)
                )

                // 5. [Protected] 쿠폰 관리 (ROLE_ADMIN)
                .route("team3-coupon-protected-admin",
                        p -> p.path("/api/coupons/policies/**")
                                .filters(f -> f
                                        .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_ADMIN)))
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(new RedisRateLimiter(10, 20))
                                                .setKeyResolver(userKeyResolver)))
                                .uri(COUPON_LB_URL))

                // 6. [Protected] 쿠폰 발급/사용 (ROLE_USER)
                // - 쿠폰 발급은 트래픽이 몰릴 수 있으므로 정책에 따라 빡빡하게 설정 가능
                .route("team3-coupon-public",
                        p -> p.path("/api/coupons/**")
                                .filters(f -> f
                                        .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_USER)))
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(new RedisRateLimiter(5, 10))
                                                .setKeyResolver(userKeyResolver)))
                                .uri(COUPON_LB_URL))

                // 7. [Public] 장바구니/주문 준비 (비로그인 허용 구간으로 보임)
                .route("team3-order-payment-public",
                        p -> p.path("/api/carts/**", "/api/orders/prepare")
                                .filters(f -> f.requestRateLimiter(c -> c
                                        .setRateLimiter(new RedisRateLimiter(20, 40))
                                        .setKeyResolver(userKeyResolver)))
                                .uri(ORDER_PAYMENT_LB_URL))

                // 8. [Protected] 주문/결제 (ROLE_USER)
                .route("team3-order-payment",
                        p -> p.path("/api/order-payment/**", "/api/orders/**", "/api/payments/**")
                                .filters(f -> f
                                        .filter(authorizationHeaderFilter.apply(new AuthorizationHeaderFilter.Config(ROLE_USER)))
                                        .requestRateLimiter(c -> c
                                                .setRateLimiter(new RedisRateLimiter(5, 10))
                                                .setKeyResolver(userKeyResolver)))
                                .uri(ORDER_PAYMENT_LB_URL))

                // 9. [Public] 도서 검색 (누구나 검색 가능)
                // - 트래픽이 많을 수 있으므로 IP 기반 제한 적용
                .route("team3-booksearch",
                        p -> p.path("/api/books/**", "/api/search/**", "/api/reviews/**", "/api/likes/**")
                                .filters(f -> f.requestRateLimiter(c -> c
                                        .setRateLimiter(new RedisRateLimiter(30, 60)) // 검색은 좀 더 넉넉하게
                                        .setKeyResolver(userKeyResolver)))
                                .uri(BOOKSEARCH_LB_URL))
                .build();
    }
}