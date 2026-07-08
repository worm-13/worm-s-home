package com.worm.community_backend.post.controller;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.post.dto.PostCreateDTO;
import com.worm.community_backend.post.dto.PostUpdateDTO;
import com.worm.community_backend.post.service.PostService;
import com.worm.community_backend.post.vo.PostDetailVO;
import com.worm.community_backend.post.vo.PostListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final PostService postService;

    @Value("${app.post-cover.upload-dir:uploads/post-covers}")
    private String postCoverUploadDir;

    @Value("${app.post-cover.public-path:/post-covers}")
    private String postCoverPublicPath;

    @Value("${app.post-cover.max-size-bytes:20971520}")
    private long postCoverMaxSizeBytes;

    /** 发布文章 */
    @PostMapping
    public ApiResponse<Map<String, Long>> createPost(@RequestBody PostCreateDTO dto, Authentication auth) {
        Long postId = postService.createPost(getUserId(auth), dto);
        return ApiResponse.success(Map.of("postId", postId));
    }

    /** 保存草稿 */
    @PostMapping("/draft")
    public ApiResponse<Map<String, Long>> saveDraft(@RequestBody PostCreateDTO dto, Authentication auth) {
        Long postId = postService.saveDraft(getUserId(auth), dto);
        return ApiResponse.success(Map.of("postId", postId));
    }

    /** 更新文章（已发布/草稿/定时均可编辑） */
    @PutMapping("/{id}")
    public ApiResponse<Void> updatePost(@PathVariable Long id, @RequestBody PostUpdateDTO dto, Authentication auth) {
        postService.updatePost(getUserId(auth), id, dto);
        return ApiResponse.success(null);
    }

    /** 删除文章 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id, Authentication auth) {
        postService.deletePost(getUserId(auth), id);
        return ApiResponse.success(null);
    }

    /** 首页已发布文章列表（分页） */
    @GetMapping
    public ApiResponse<PageResponse<PostListItemVO>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.listPostsPage(page, size));
    }

    /** 已发布文章详情 */
    @GetMapping("/{id}")
    public ApiResponse<PostDetailVO> getPostDetail(@PathVariable Long id, Authentication auth) {
        Long currentUserId = auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
        return ApiResponse.success(postService.getPostDetail(id, currentUserId));
    }

    /** 发布草稿/定时文章为已发布 */
    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publishPost(@PathVariable Long id, Authentication auth) {
        postService.publishPost(getUserId(auth), id);
        return ApiResponse.success(null);
    }

    /** 查看当前用户所有文章（含草稿、定时，分页） */
    @GetMapping("/my")
    public ApiResponse<PageResponse<PostListItemVO>> listMyPosts(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.listMyPostsPage(getUserId(auth), page, size));
    }

    /** 查看当前用户草稿列表（分页） */
    @GetMapping("/my/drafts")
    public ApiResponse<PageResponse<PostListItemVO>> listMyDrafts(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.listMyDraftsPage(getUserId(auth), page, size));
    }

    /** 查看自己的文章详情（草稿/定时也可查看） */
    @GetMapping("/my/{id}")
    public ApiResponse<PostDetailVO> getMyPostDetail(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success(postService.getMyPostDetail(getUserId(auth), id));
    }

    /** 查看指定用户的已发布文章列表（分页，公开） */
    @GetMapping("/user/{userId}")
    public ApiResponse<PageResponse<PostListItemVO>> listUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.listUserPostsPage(userId, page, size));
    }

    /** 上传封面图片 */
    @PostMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.POST_COVER_FILE_REQUIRED);
        }
        if (file.getSize() > postCoverMaxSizeBytes) {
            throw new BusinessException(ResultCode.POST_COVER_SIZE_EXCEEDED);
        }

        String ext = resolveExtension(file);
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path uploadPath = Paths.get(postCoverUploadDir).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(fileName).normalize();

        if (!target.startsWith(uploadPath)) {
            throw new BusinessException(ResultCode.POST_COVER_UPLOAD_FAILED);
        }

        Files.createDirectories(uploadPath);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String publicPath = normalizePublicPath(postCoverPublicPath);
        return ApiResponse.success(Map.of("url", publicPath + "/" + fileName));
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return userId;
    }

    private String resolveExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name) || !name.contains(".")) {
            throw new BusinessException(ResultCode.POST_COVER_TYPE_INVALID);
        }
        String ext = name.substring(name.lastIndexOf('.') + 1).trim().toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.POST_COVER_TYPE_INVALID);
        }
        return ext;
    }

    private String normalizePublicPath(String path) {
        if (!StringUtils.hasText(path))
            return "/post-covers";
        path = path.trim();
        if (!path.startsWith("/"))
            path = "/" + path;
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        return path;
    }
}
