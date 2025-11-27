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

package com.nhnacademy.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class CorsConfig {

    // 만약 배포할 때 Nginx 같은 웹 서버를 맨 앞에 두고, 주소를 완벽하게 하나로 합친다면 CORS 설정 자체가 필요 없습니다.
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 배포 환경에서는 보안을 위해 "*" 대신 실제 프론트엔드 도메인을 넣습니다.
        corsConfig.setAllowedOriginPatterns(Collections.singletonList("*"));

        // 허용할 HTTP 메서드 (GET, POST, PUT, DELETE 등)
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 요청 헤더 (Content-Type, Authorization 등 클라이언트가 보내는 헤더)
        corsConfig.setAllowedHeaders(List.of("*"));

        // 브라우저가 읽을 수 있게 허용할 응답 헤더
        corsConfig.addExposedHeader("Authorization");
        corsConfig.addExposedHeader("X-User-Id");
        corsConfig.addExposedHeader("X-Role");

        // 이 설정을 모든 경로("/**")에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

}
