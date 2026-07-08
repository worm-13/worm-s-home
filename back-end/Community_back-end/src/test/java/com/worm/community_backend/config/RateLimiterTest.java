package com.worm.community_backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    @InjectMocks
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter.setEnabled(true);
        rateLimiter.setLoginMaxAttempts(5);
        rateLimiter.setLoginWindowSeconds(300);
        rateLimiter.setRegisterMaxAttempts(3);
        rateLimiter.setRegisterWindowSeconds(3600);
    }

    @Test
    void isLoginAllowed_FirstAttempt() {
        assertTrue(rateLimiter.isLoginAllowed("192.168.1.1"));
    }

    @Test
    void isLoginAllowed_UnderLimit() {
        String ip = "192.168.1.1";
        for (int i = 0; i < 4; i++) {
            rateLimiter.recordLoginAttempt(ip);
        }
        assertTrue(rateLimiter.isLoginAllowed(ip));
    }

    @Test
    void isLoginAllowed_AtLimit() {
        String ip = "192.168.1.1";
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordLoginAttempt(ip);
        }
        assertFalse(rateLimiter.isLoginAllowed(ip));
    }

    @Test
    void isRegisterAllowed_FirstAttempt() {
        assertTrue(rateLimiter.isRegisterAllowed("192.168.1.1"));
    }

    @Test
    void isRegisterAllowed_AtLimit() {
        String ip = "192.168.1.1";
        for (int i = 0; i < 3; i++) {
            rateLimiter.recordRegisterAttempt(ip);
        }
        assertFalse(rateLimiter.isRegisterAllowed(ip));
    }

    @Test
    void resetLoginAttempts_Success() {
        String ip = "192.168.1.1";
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordLoginAttempt(ip);
        }
        assertFalse(rateLimiter.isLoginAllowed(ip));
        
        rateLimiter.resetLoginAttempts(ip);
        assertTrue(rateLimiter.isLoginAllowed(ip));
    }

    @Test
    void resetRegisterAttempts_Success() {
        String ip = "192.168.1.1";
        for (int i = 0; i < 3; i++) {
            rateLimiter.recordRegisterAttempt(ip);
        }
        assertFalse(rateLimiter.isRegisterAllowed(ip));
        
        rateLimiter.resetRegisterAttempts(ip);
        assertTrue(rateLimiter.isRegisterAllowed(ip));
    }

    @Test
    void disabledRateLimiter_AllowsAll() {
        rateLimiter.setEnabled(false);
        
        String ip = "192.168.1.1";
        for (int i = 0; i < 100; i++) {
            rateLimiter.recordLoginAttempt(ip);
        }
        assertTrue(rateLimiter.isLoginAllowed(ip));
    }

    @Test
    void differentIPs_Independent() {
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";
        
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordLoginAttempt(ip1);
        }
        
        assertFalse(rateLimiter.isLoginAllowed(ip1));
        assertTrue(rateLimiter.isLoginAllowed(ip2));
    }
}
