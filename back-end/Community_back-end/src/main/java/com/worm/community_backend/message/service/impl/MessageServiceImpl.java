package com.worm.community_backend.message.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.config.WsMessage;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.message.entity.Message;
import com.worm.community_backend.message.mapper.MessageMapper;
import com.worm.community_backend.message.service.MessageService;
import com.worm.community_backend.message.vo.ConversationVO;
import com.worm.community_backend.message.vo.MessageVO;
import com.worm.community_backend.notification.websocket.NotificationWebSocketHandler;
import com.worm.community_backend.user.mapper.UserFollowMapper;
import com.worm.community_backend.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final ObjectMapper objectMapper;

    @Override
    public MessageVO sendMessage(Long senderId, Long receiverId, String content) {
        if (receiverId == null) throw new BusinessException(ResultCode.MESSAGE_RECEIVER_NOT_FOUND);
        if (content == null || content.isBlank()) throw new BusinessException(ResultCode.MESSAGE_CONTENT_REQUIRED);
        if (senderId.equals(receiverId)) throw new BusinessException(ResultCode.BAD_REQUEST);

        // 校验接收者存在
        if (userMapper.selectById(receiverId) == null) {
            throw new BusinessException(ResultCode.MESSAGE_RECEIVER_NOT_FOUND);
        }

        // 校验互相关注
        if (userFollowMapper.countByFollowerAndFollowing(senderId, receiverId) == 0
                || userFollowMapper.countByFollowerAndFollowing(receiverId, senderId) == 0) {
            throw new BusinessException(ResultCode.NOT_MUTUAL_FOLLOW);
        }

        // 插入消息
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setIsRead(0);
        messageMapper.insert(message);

        // 构建 VO
        MessageVO vo = buildMessageVO(message);

        // WebSocket 推送
        try {
            WsMessage wsMsg = new WsMessage("MESSAGE", vo);
            String json = objectMapper.writeValueAsString(wsMsg);
            NotificationWebSocketHandler.sendToUser(receiverId, json);
        } catch (Exception e) {
            log.warn("WebSocket 推送私信失败, receiverId={}", receiverId, e);
        }

        return vo;
    }

    @Override
    public List<ConversationVO> getConversations(Long userId) {
        return messageMapper.selectConversationList(userId);
    }

    @Override
    public List<MessageVO> getConversation(Long userId, Long otherUserId, int page, int size) {
        int offset = (page - 1) * size;
        List<MessageVO> list = messageMapper.selectConversation(userId, otherUserId, size, offset);
        // 反转为时间正序
        java.util.Collections.reverse(list);
        return list;
    }

    @Override
    public int countUnread(Long userId) {
        return messageMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long userId, Long otherUserId) {
        messageMapper.markAsRead(otherUserId, userId);
    }

    private MessageVO buildMessageVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead() == 1);
        vo.setCreatedAt(message.getCreatedAt());
        // 查询发送者信息
        var sender = userMapper.selectById(message.getSenderId());
        if (sender != null) {
            vo.setSenderName(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
            vo.setSenderAvatar(sender.getAvatar());
        }
        return vo;
    }
}
