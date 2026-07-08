package com.worm.community_backend.message.service;

import com.worm.community_backend.message.vo.ConversationVO;
import com.worm.community_backend.message.vo.MessageVO;

import java.util.List;

public interface MessageService {

    MessageVO sendMessage(Long senderId, Long receiverId, String content);

    List<ConversationVO> getConversations(Long userId);

    List<MessageVO> getConversation(Long userId, Long otherUserId, int page, int size);

    int countUnread(Long userId);

    void markAsRead(Long userId, Long otherUserId);
}
