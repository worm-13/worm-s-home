package com.worm.community_backend.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTests {

    @Test
    void shouldGenerateAndParseToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setExpirationSeconds(3600);

        JwtTokenProvider provider = new JwtTokenProvider(properties);
        String token = provider.generateAccessToken(53827L, "USER");

        Assertions.assertTrue(provider.isValid(token));
        Assertions.assertEquals(53827L, provider.getUserId(token));
        Assertions.assertEquals("USER", provider.getRole(token));
        Assertions.assertEquals(3600, provider.getExpirationSeconds());
    }

    @Test
    void shouldFallbackToPlainTextWhenSecretIsNotBase64() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("not_base64_secret_with_@_symbol");
        properties.setExpirationSeconds(3600);

        JwtTokenProvider provider = new JwtTokenProvider(properties);
        String token = provider.generateAccessToken(12345L, "ADMIN");

        Assertions.assertTrue(provider.isValid(token));
        Assertions.assertEquals(12345L, provider.getUserId(token));
        Assertions.assertEquals("ADMIN", provider.getRole(token));
    }
}

