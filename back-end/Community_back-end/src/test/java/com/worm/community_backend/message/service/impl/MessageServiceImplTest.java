package com.worm.community_backend.message.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.message.entity.Message;
import com.worm.community_backend.message.mapper.MessageMapper;
import com.worm.community_backend.message.vo.ConversationVO;
import com.worm.community_backend.message.vo.MessageVO;
import com.worm.community_backend.user.entity.User;
import com.worm.community_backend.user.mapper.UserFollowMapper;
import com.worm.community_backend.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserFollowMapper userFollowMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User testSender;
    private User testReceiver;
    private final Long senderId = 100L;
    private final Long receiverId = 200L;

    @BeforeEach
    void setUp() {
        testSender = new User();
        testSender.setId(senderId);
        testSender.setUsername("sender");
        testSender.setNickname("Sender Nick");
        testSender.setAvatar("/avatars/sender.jpg");

        testReceiver = new User();
        testReceiver.setId(receiverId);
        testReceiver.setUsername("receiver");
        testReceiver.setNickname("Receiver Nick");
    }

    @Test
    void sendMessage_Success() throws Exception {
        when(userMapper.selectById(receiverId)).thenReturn(testReceiver);
        when(userFollowMapper.countByFollowerAndFollowing(senderId, receiverId)).thenReturn(1);
        when(userFollowMapper.countByFollowerAndFollowing(receiverId, senderId)).thenReturn(1);
        doAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(1L);
            msg.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(messageMapper).insert(any(Message.class));
        when(userMapper.selectById(senderId)).thenReturn(testSender);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        MessageVO result = messageService.sendMessage(senderId, receiverId, "Hello!");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(senderId, result.getSenderId());
        assertEquals(receiverId, result.getReceiverId());
        assertEquals("Hello!", result.getContent());
        verify(messageMapper).insert(any(Message.class));
    }

    @Test
    void sendMessage_ReceiverNull() {
        assertThrows(BusinessException.class, () -> messageService.sendMessage(senderId, null, "Hello!"));
    }

    @Test
    void sendMessage_ContentBlank() {
        assertThrows(BusinessException.class, () -> messageService.sendMessage(senderId, receiverId, "  "));
        assertThrows(BusinessException.class, () -> messageService.sendMessage(senderId, receiverId, null));
    }

    @Test
    void sendMessage_SelfMessage() {
        assertThrows(BusinessException.class, () -> messageService.sendMessage(senderId, senderId, "Hello!"));
    }

    @Test
    void sendMessage_ReceiverNotFound() {
        when(userMapper.selectById(receiverId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> messageService.sendMessage(senderId, receiverId, "Hello!"));
    }

    @Test
    void sendMessage_NotMutualFollow() {
        when(userMapper.selectById(receiverId)).thenReturn(testReceiver);
        when(userFollowMapper.countByFollowerAndFollowing(senderId, receiverId)).thenReturn(0);

        assertThrows(BusinessException.class, () -> messageService.sendMessage(senderId, receiverId, "Hello!"));
    }

    @Test
    void getConversations_Success() {
        ConversationVO vo = new ConversationVO();
        vo.setUserId(receiverId);
        vo.setUsername("receiver");
        vo.setNickname("Receiver");

        when(messageMapper.selectConversationList(senderId)).thenReturn(Arrays.asList(vo));

        List<ConversationVO> conversations = messageService.getConversations(senderId);

        assertNotNull(conversations);
        assertEquals(1, conversations.size());
        assertEquals(receiverId, conversations.get(0).getUserId());
    }

    @Test
    void getConversation_Success() {
        MessageVO vo = new MessageVO();
        vo.setId(1L);
        vo.setContent("Hello!");

        when(messageMapper.selectConversation(senderId, receiverId, 20, 0)).thenReturn(Arrays.asList(vo));

        List<MessageVO> messages = messageService.getConversation(senderId, receiverId, 1, 20);

        assertNotNull(messages);
        assertEquals(1, messages.size());
    }

    @Test
    void countUnread_Success() {
        when(messageMapper.countUnread(senderId)).thenReturn(5);

        int count = messageService.countUnread(senderId);

        assertEquals(5, count);
    }

    @Test
    void markAsRead_Success() {
        messageService.markAsRead(senderId, receiverId);

        verify(messageMapper).markAsRead(receiverId, senderId);
    }
}
