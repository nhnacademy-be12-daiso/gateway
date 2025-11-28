package com.nhnacademy.gateway.filter;

import com.nhnacademy.gateway.jwt.properties.JwtProperties;
import com.nhnacademy.gateway.jwt.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

    public record Config(String role) {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            log.info("========================================");
            log.info("[Gateway Filter] 요청 경로: {} {}", request.getMethod(), request.getPath());
            log.info("========================================");

            String token = null;

            // 1. Authorization 헤더에서 토큰 확인 (기존 방식 - API 테스트용)
            if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authorizationHeader != null && !authorizationHeader.trim().isEmpty()) {
                    log.info("[Gateway Filter] Authorization 헤더에서 토큰 추출");
                    token = authorizationHeader.replace(jwtProperties.getTokenPrefix() + " ", "");
                }
            }

            // 2. Cookie에서 토큰 확인 (HttpOnly Cookie 방식)
            if (token == null) {
                token = extractTokenFromCookie(request, "accessToken");
                if (token != null) {
                    log.info("[Gateway Filter] Cookie에서 토큰 추출");
                }
            }

            // 토큰이 없으면 인증 실패
            if (token == null || token.trim().isEmpty()) {
                log.warn("[Gateway Filter] 토큰이 없습니다 (헤더 및 Cookie 모두 확인).");
                return onError(exchange, "No token found", HttpStatus.UNAUTHORIZED);
            }

            log.info("[Gateway Filter] 토큰 추출 완료 (길이: {})", token.length());

            // Redis 블랙리스트 확인 (Auth Server와 동일한 키 형식 사용)
            String blacklistKey = "blacklist:" + token;
            String isLogout = stringRedisTemplate.opsForValue().get(blacklistKey);
            log.info("[Gateway Filter] Redis 블랙리스트 확인: key={}, result={}", blacklistKey.substring(0, Math.min(30, blacklistKey.length())) + "...", isLogout);

            if (isLogout != null && isLogout.equals("logout")) {
                log.warn("[Gateway Filter] 블랙리스트에 등록된 토큰입니다.");
                return onError(exchange, "Token is in blacklist", HttpStatus.UNAUTHORIZED);
            }

            // 토큰 유효성 검증, Claims 추출
            if (!jwtUtil.isTokenValid(token)) {
                log.error("[Gateway Filter] JWT 토큰이 유효하지 않습니다.");
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            log.info("[Gateway Filter] JWT 토큰 검증 성공");

            String userId = jwtUtil.getLoginId(token);
            String role = jwtUtil.getRole(token);

            log.info("[Gateway Filter] Claims 추출 완료 - userId: {}, role: {}", userId, role);

            // WebFlux의 요청 객체는 불변이므로 직접 수정 불가
            // 대신 mutate()를 써서 헤더가 추가된 복제본을 새로 생성
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)    // API들한테 넘겨줄 ID
                    .header("X-Role", role)         // API들한테 넘겨줄 권한
                    .build();

            log.info("[Gateway Filter] 헤더 추가 완료");
            log.info("[Gateway Filter]   - X-User-Id: {}", userId);
            log.info("[Gateway Filter]   - X-Role: {}", role);
            log.info("[Gateway Filter] MSA 서비스로 요청 전달: {}", request.getPath());
            log.info("========================================");

            // 다음 필터에게 넘어갈 때, 원본이 아니라 헤더가 추가된 복제본을 쥐어줌
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    // WebFlux 방식으로 에러 응답 처리
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();

        log.error("[Gateway Filter] 인증 실패: {} (상태 코드: {})", err, httpStatus);
        log.error("========================================");

        response.setStatusCode(httpStatus);

        return response.setComplete();
    }

    // Cookie에서 토큰 추출 헬퍼 메서드
    private String extractTokenFromCookie(ServerHttpRequest request, String cookieName) {
        request.getCookies();
        if (request.getCookies().get(cookieName) == null) {
            return null;
        }

        var cookies = request.getCookies().get(cookieName);
        if (cookies != null && !cookies.isEmpty()) {
            return cookies.getFirst().getValue();
        }

        return null;
    }

}
