package com.nhnacademy.gateway.filter;

import com.nhnacademy.gateway.jwt.properties.JwtProperties;
import com.nhnacademy.gateway.jwt.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationHeaderFilterTest {

    private AuthorizationHeaderFilter filter;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private GatewayFilterChain chain;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String USER_ID = "29";
    private static final String ROLE = "USER";

    @BeforeEach
    void setUp() {
        filter = new AuthorizationHeaderFilter(jwtUtil, jwtProperties, stringRedisTemplate);

        lenient().when(jwtProperties.getTokenPrefix()).thenReturn("Bearer");
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("X-User-Id 헤더 추가 성공 - Authorization 헤더 사용")
    void addXUserIdHeader_Success_WithAuthorizationHeader() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(valueOperations.get("blacklist:" + VALID_TOKEN)).willReturn(null);
        given(jwtUtil.isTokenValid(VALID_TOKEN)).willReturn(true);
        given(jwtUtil.getLoginId(VALID_TOKEN)).willReturn(USER_ID);
        given(jwtUtil.getRole(VALID_TOKEN)).willReturn(ROLE);

        given(chain.filter(any(ServerWebExchange.class))).willAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            ServerHttpRequest modifiedRequest = modifiedExchange.getRequest();

            // X-User-Id와 X-Role 헤더가 추가되었는지 검증
            assertThat(modifiedRequest.getHeaders().getFirst("X-User-Id")).isEqualTo(USER_ID);
            assertThat(modifiedRequest.getHeaders().getFirst("X-Role")).isEqualTo(ROLE);

            return Mono.empty();
        });

        // when
        Mono<Void> result = filter.apply(new AuthorizationHeaderFilter.Config("ROLE_USER"))
                .filter(exchange, chain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil).isTokenValid(VALID_TOKEN);
        verify(jwtUtil).getLoginId(VALID_TOKEN);
        verify(jwtUtil).getRole(VALID_TOKEN);
    }

    @Test
    @DisplayName("X-User-Id 헤더 추가 성공 - Cookie 사용")
    void addXUserIdHeader_Success_WithCookie() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .cookie(new HttpCookie("accessToken", VALID_TOKEN))
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(valueOperations.get("blacklist:" + VALID_TOKEN)).willReturn(null);
        given(jwtUtil.isTokenValid(VALID_TOKEN)).willReturn(true);
        given(jwtUtil.getLoginId(VALID_TOKEN)).willReturn(USER_ID);
        given(jwtUtil.getRole(VALID_TOKEN)).willReturn(ROLE);

        given(chain.filter(any(ServerWebExchange.class))).willAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            ServerHttpRequest modifiedRequest = modifiedExchange.getRequest();

            // X-User-Id와 X-Role 헤더가 추가되었는지 검증
            assertThat(modifiedRequest.getHeaders().getFirst("X-User-Id")).isEqualTo(USER_ID);
            assertThat(modifiedRequest.getHeaders().getFirst("X-Role")).isEqualTo(ROLE);

            return Mono.empty();
        });

        // when
        Mono<Void> result = filter.apply(new AuthorizationHeaderFilter.Config("ROLE_USER"))
                .filter(exchange, chain);

        // then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("토큰 없음 - 401 Unauthorized")
    void noToken_Returns401() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = filter.apply(new AuthorizationHeaderFilter.Config("ROLE_USER"))
                .filter(exchange, chain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("블랙리스트 토큰 - 401 Unauthorized")
    void blacklistedToken_Returns401() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(valueOperations.get("blacklist:" + VALID_TOKEN)).willReturn("logout");

        // when
        Mono<Void> result = filter.apply(new AuthorizationHeaderFilter.Config("ROLE_USER"))
                .filter(exchange, chain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("유효하지 않은 토큰 - 401 Unauthorized")
    void invalidToken_Returns401() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(valueOperations.get("blacklist:" + VALID_TOKEN)).willReturn(null);
        given(jwtUtil.isTokenValid(VALID_TOKEN)).willReturn(false);

        // when
        Mono<Void> result = filter.apply(new AuthorizationHeaderFilter.Config("ROLE_USER"))
                .filter(exchange, chain);

        // then
        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("X-User-Id 값 검증")
    void verifyXUserIdValue() {
        // given
        String expectedUserId = "123";
        String expectedRole = "ADMIN";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(valueOperations.get(anyString())).willReturn(null);
        given(jwtUtil.isTokenValid(VALID_TOKEN)).willReturn(true);
        given(jwtUtil.getLoginId(VALID_TOKEN)).willReturn(expectedUserId);
        given(jwtUtil.getRole(VALID_TOKEN)).willReturn(expectedRole);

        given(chain.filter(any(ServerWebExchange.class))).willAnswer(invocation -> {
            ServerWebExchange modifiedExchange = invocation.getArgument(0);
            ServerHttpRequest modifiedRequest = modifiedExchange.getRequest();

            // 정확한 값이 전달되는지 검증
            String actualUserId = modifiedRequest.getHeaders().getFirst("X-User-Id");
            String actualRole = modifiedRequest.getHeaders().getFirst("X-Role");

            assertThat(actualUserId).isEqualTo(expectedUserId);
            assertThat(actualRole).isEqualTo(expectedRole);

            return Mono.empty();
        });

        // when
        Mono<Void> result = filter.apply(new AuthorizationHeaderFilter.Config("ROLE_USER"))
                .filter(exchange, chain);

        // then
        StepVerifier.create(result)
                .verifyComplete();
    }
}

