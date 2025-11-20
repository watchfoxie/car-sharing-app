package com.usarbcs.authen.service.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class TokenHandlerTest {

    private TokenHandler tokenHandler;

    @BeforeEach
    void setUp() {
        tokenHandler = new TokenHandler();
    ReflectionTestUtils.setField(tokenHandler, "jwtSigningKey", "U3VwZXJTZWNyZXRLZXlGb3JKd3RTaWduVGVzdHMxMjM=");
    }

    @Test
    void shouldGenerateAndValidateToken() {
        UserDetails userDetails = User.withUsername("token-user")
                .password("s3cret")
                .authorities("ROLE_USER")
                .build();

        String token = tokenHandler.generateToken(userDetails);

        Assertions.assertThat(token).isNotBlank();
        Assertions.assertThat(tokenHandler.isTokenValid(token, userDetails)).isTrue();
        Assertions.assertThat(tokenHandler.extractUserName(token)).isEqualTo(userDetails.getUsername());
    }
}
