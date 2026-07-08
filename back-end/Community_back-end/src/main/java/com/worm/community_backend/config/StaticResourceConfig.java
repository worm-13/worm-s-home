package com.worm.community_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * 静态资源映射配置：将上传目录映射为可访问 URL。
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final String avatarUploadDir;
    private final String avatarPublicPath;
    private final String backgroundUploadDir;
    private final String backgroundPublicPath;
    private final String postCoverUploadDir;
    private final String postCoverPublicPath;

    public StaticResourceConfig(@Value("${app.avatar.upload-dir:uploads/avatars}") String avatarUploadDir,
                                @Value("${app.avatar.public-path:/avatars}") String avatarPublicPath,
                                @Value("${app.background.upload-dir:uploads/backgrounds}") String backgroundUploadDir,
                                @Value("${app.background.public-path:/backgrounds}") String backgroundPublicPath,
                                @Value("${app.post-cover.upload-dir:uploads/post-covers}") String postCoverUploadDir,
                                @Value("${app.post-cover.public-path:/post-covers}") String postCoverPublicPath) {
        this.avatarUploadDir = avatarUploadDir;
        this.avatarPublicPath = avatarPublicPath;
        this.backgroundUploadDir = backgroundUploadDir;
        this.backgroundPublicPath = backgroundPublicPath;
        this.postCoverUploadDir = postCoverUploadDir;
        this.postCoverPublicPath = postCoverPublicPath;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor())
                .addPathPatterns("/api/**");
    }

    @Override
    /** 注册头像、背景图、文章封面的资源映射规则。 */
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 头像资源
        String normalizedPublicPath = normalizePublicPath(avatarPublicPath);
        Path uploadPath = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }

        registry.addResourceHandler(normalizedPublicPath + "/**")
                .addResourceLocations(location)
                .setCacheControl(org.springframework.http.CacheControl.maxAge(7, TimeUnit.DAYS)
                        .cachePublic()
                        .mustRevalidate());

        // 背景图资源
        String normalizedBackgroundPublicPath = normalizePublicPath(backgroundPublicPath);
        Path backgroundPath = Paths.get(backgroundUploadDir).toAbsolutePath().normalize();
        String backgroundLocation = backgroundPath.toUri().toString();
        if (!backgroundLocation.endsWith("/")) {
            backgroundLocation = backgroundLocation + "/";
        }

        registry.addResourceHandler(normalizedBackgroundPublicPath + "/**")
            .addResourceLocations(backgroundLocation)
            .setCacheControl(org.springframework.http.CacheControl.maxAge(7, TimeUnit.DAYS)
                    .cachePublic()
                    .mustRevalidate());

        // 文章封面资源
        String normalizedPostCoverPublicPath = normalizePublicPath(postCoverPublicPath);
        Path postCoverPath = Paths.get(postCoverUploadDir).toAbsolutePath().normalize();
        String postCoverLocation = postCoverPath.toUri().toString();
        if (!postCoverLocation.endsWith("/")) {
            postCoverLocation = postCoverLocation + "/";
        }

        registry.addResourceHandler(normalizedPostCoverPublicPath + "/**")
            .addResourceLocations(postCoverLocation)
            .setCacheControl(org.springframework.http.CacheControl.maxAge(7, TimeUnit.DAYS)
                    .cachePublic()
                    .mustRevalidate());
    }

    private static String normalizePublicPath(String publicPath) {
        // 统一 publicPath 形态：以 / 开头且不以 / 结尾。
        String path = StringUtils.hasText(publicPath) ? publicPath.trim() : "/avatars";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}

