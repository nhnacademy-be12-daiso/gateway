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

package com.nhnacademy.gateway.jwt.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    // JWT 서명에 사용할 비밀 키
    @NotBlank
    @Value("${JWT.SECRET}")
    private String secret;

    // 토큰 만료 시간
    @NotNull
    private Long expirationTime;

    // Authorization 헤더에 붙는 접두사 (현재: Bearer)
    @NotBlank
    private String tokenPrefix;

    // JWT 토큰이 담기는 HTTP 헤더 이름 (현재: Authorization)
    @NotBlank
    private String header;

    // 로그인 요청 URL
    @NotBlank
    private String loginUrl;

}
