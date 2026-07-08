package com.worm.community_backend.comment.service.impl;

import com.worm.community_backend.comment.dto.CommentCreateDTO;
import com.worm.community_backend.comment.entity.Comment;
import com.worm.community_backend.comment.mapper.CommentMapper;
import com.worm.community_backend.comment.service.CommentService;
import com.worm.community_backend.comment.vo.CommentVO;
import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.moderation.service.SensitiveWordFilter;
import com.worm.community_backend.notification.service.NotificationService;
import com.worm.community_backend.post.entity.Post;
import com.worm.community_backend.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int COMMENT_MAX_LENGTH = 500;
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final NotificationService notificationService;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long userId, CommentCreateDTO dto) {
        if (userId == null || dto == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BusinessException(ResultCode.COMMENT_CONTENT_REQUIRED);
        }
        
        String content = dto.getContent().trim();
        
        // 服务端长度限制
        if (content.length() > COMMENT_MAX_LENGTH) {
            throw new BusinessException(1050, "comment exceeds maximum length of " + COMMENT_MAX_LENGTH);
        }
        
        // 敏感词过滤
        if (sensitiveWordFilter.containsSensitiveWord(content)) {
            throw new BusinessException(1051, "comment contains sensitive words");
        }
        
        if (postMapper.selectById(dto.getPostId()) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Comment comment = new Comment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent().trim());

        if (commentMapper.insert(comment) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
        commentMapper.increasePostCommentCount(dto.getPostId());

        // 发送通知
        if (dto.getParentId() != null) {
            Comment parent = commentMapper.selectById(dto.getParentId());
            if (parent != null) {
                notificationService.createNotification(parent.getUserId(), userId, "REPLY", dto.getPostId());
            }
        } else {
            Post post = postMapper.selectById(dto.getPostId());
            if (post != null) {
                notificationService.createNotification(post.getUserId(), userId, "COMMENT", dto.getPostId());
            }
        }

        return comment.getId();
    }

    @Override
    public List<CommentVO> getCommentsByPostId(Long postId) {
        if (postId == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        List<CommentVO> all = commentMapper.selectByPostId(postId);
        return buildTree(all);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        if (userId == null || commentId == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        if (commentMapper.softDelete(commentId) != 1) {
            throw new BusinessException(ResultCode.COMMENT_DELETE_FAILED);
        }
        commentMapper.decreasePostCommentCount(comment.getPostId());
    }

    @Override
    public PageResponse<CommentVO> getCommentsByPostIdPage(Long postId, int page, int size) {
        if (postId == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        int offset = (page - 1) * size;
        long total = commentMapper.countByPostId(postId);
        List<CommentVO> flat = commentMapper.selectByPostIdPage(postId, offset, size);
        List<CommentVO> tree = buildTree(flat);
        boolean hasMore = offset + size < total;
        return new PageResponse<>(tree, total, page, size, hasMore);
    }

    private List<CommentVO> buildTree(List<CommentVO> flatList) {
        Map<Long, CommentVO> map = new LinkedHashMap<>();
        List<CommentVO> roots = new ArrayList<>();

        for (CommentVO vo : flatList) {
            vo.setReplies(new ArrayList<>());
            map.put(vo.getId(), vo);
        }

        for (CommentVO vo : flatList) {
            if (vo.getParentId() == null || !map.containsKey(vo.getParentId())) {
                roots.add(vo);
            } else {
                map.get(vo.getParentId()).getReplies().add(vo);
            }
        }
        return roots;
    }
}
