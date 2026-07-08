package com.worm.community_backend.moderation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SensitiveWordFilterTest {

    @InjectMocks
    private SensitiveWordFilter sensitiveWordFilter;

    @BeforeEach
    void setUp() {
        sensitiveWordFilter.init();
    }

    @Test
    void containsSensitiveWord_Found() {
        assertTrue(sensitiveWordFilter.containsSensitiveWord("This is 垃圾 content"));
    }

    @Test
    void containsSensitiveWord_NotFound() {
        assertFalse(sensitiveWordFilter.containsSensitiveWord("This is normal content"));
    }

    @Test
    void containsSensitiveWord_CaseInsensitive() {
        sensitiveWordFilter.addWord("Spam");
        assertTrue(sensitiveWordFilter.containsSensitiveWord("This is SPAM content"));
    }

    @Test
    void containsSensitiveWord_NullText() {
        assertFalse(sensitiveWordFilter.containsSensitiveWord(null));
    }

    @Test
    void containsSensitiveWord_EmptyText() {
        assertFalse(sensitiveWordFilter.containsSensitiveWord(""));
    }

    @Test
    void filter_MaskSensitiveWords() {
        String result = sensitiveWordFilter.filter("This is 垃圾 content");
        assertEquals("This is ** content", result);
    }

    @Test
    void filter_NoSensitiveWords() {
        String result = sensitiveWordFilter.filter("This is normal content");
        assertEquals("This is normal content", result);
    }

    @Test
    void filter_NullText() {
        assertNull(sensitiveWordFilter.filter(null));
    }

    @Test
    void addWord_Success() {
        sensitiveWordFilter.addWord("newbadword");
        assertTrue(sensitiveWordFilter.containsSensitiveWord("This contains newbadword"));
    }

    @Test
    void removeWord_Success() {
        sensitiveWordFilter.addWord("tempword");
        assertTrue(sensitiveWordFilter.containsSensitiveWord("This contains tempword"));
        
        sensitiveWordFilter.removeWord("tempword");
        assertFalse(sensitiveWordFilter.containsSensitiveWord("This contains tempword"));
    }
}
