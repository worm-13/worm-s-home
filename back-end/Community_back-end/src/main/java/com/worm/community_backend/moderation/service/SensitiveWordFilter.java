package com.worm.community_backend.moderation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * 敏感词过滤服务。
 */
@Slf4j
@Service
public class SensitiveWordFilter {

    private final Set<String> sensitiveWords = new HashSet<>();

    @PostConstruct
    public void init() {
        // 初始化敏感词库（实际项目中应从数据库或配置文件加载）
        // 这里只是一些示例敏感词
        sensitiveWords.add("垃圾");
        sensitiveWords.add("骗子");
        sensitiveWords.add("色情");
        sensitiveWords.add("赌博");
        sensitiveWords.add("暴力");
        log.info("Loaded {} sensitive words", sensitiveWords.size());
    }

    /**
     * 检查文本是否包含敏感词。
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String word : sensitiveWords) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤文本中的敏感词，用 * 替换。
     */
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        for (String word : sensitiveWords) {
            String replacement = "*".repeat(word.length());
            result = result.replaceAll("(?i)" + word, replacement);
        }
        return result;
    }

    /**
     * 添加敏感词。
     */
    public void addWord(String word) {
        sensitiveWords.add(word);
    }

    /**
     * 移除敏感词。
     */
    public void removeWord(String word) {
        sensitiveWords.remove(word);
    }
}
