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

package com.nhnacademy.gateway.jwt.util;

import com.nhnacademy.gateway.jwt.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // JWT 서명에 사용할 비밀키
    private final Key key;

    @Value("${jwt.access-token-expiration}")
    private final long expirationTime;

    @Value("${jwt.token-prefix}")
    private final String tokenPrefix;

    public JwtUtil(JwtProperties jwtProperties) {
        // 비밀키를 HMAC SHA 알고리즘용 Key 객체로 변환
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        // 초 단위로 설정되어 있는 expirationTime을 ms 단위로 변환
        this.expirationTime = jwtProperties.getExpirationTime() * 1000L;
        this.tokenPrefix = jwtProperties.getTokenPrefix();
    }

    // 토큰에서 Claims(payload 부분의 데이터 == 토큰에 담긴 정보) 추출 (내부용)
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)  // JWS 서명을 검증하면서 Claims 추출
                .getBody();
    }

    // 토큰 유효성 검증: 서명 검증 + 만료 여부
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);

            return !claims.getExpiration().before(new Date());

        } catch (Exception e) {
            return false;
        }
    }

    // 토큰에서 로그인 ID 추출
    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    // 토큰에서 권한 추출
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 토큰 남은 만료 시간 계산: 로그아웃 시 redis 블랙리스트 저장용으로 사용
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = parseClaims(token);
            long expirationTime = claims.getExpiration().getTime();
            long now = new Date().getTime();

            return expirationTime - now;

        } catch (Exception e) {
            return 0;
        }
    }

}
