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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    // JWT 서명에 사용할 비밀키
    private final Key key;

    public JwtUtil(JwtProperties jwtProperties) {
        log.info("========================================");
        log.info("[JwtUtil] Initializing with secret: {}...",
                jwtProperties.getSecret() != null ? jwtProperties.getSecret().substring(0, Math.min(10, jwtProperties.getSecret().length())) + "..." : "NULL");
        log.info("[JwtUtil] Token prefix: {}", jwtProperties.getTokenPrefix());
        log.info("========================================");

        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes)");
        }
        // 비밀키를 HMAC SHA 알고리즘용 Key 객체로 변환
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());

        log.info("[JwtUtil] JWT Secret Key initialized successfully");
    }

    // 토큰에서 Claims(payload 부분의 데이터 == 토큰에 담긴 정보) 추출 (내부용)
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
            return -1;
        }
    }

}
