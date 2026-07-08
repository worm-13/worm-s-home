package com.worm.community_backend.message.mapper;

import com.worm.community_backend.message.entity.Message;
import com.worm.community_backend.message.vo.ConversationVO;
import com.worm.community_backend.message.vo.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    int insert(Message message);

    List<ConversationVO> selectConversationList(@Param("userId") Long userId);

    List<MessageVO> selectConversation(@Param("userId") Long userId,
                                       @Param("otherUserId") Long otherUserId,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    int countUnread(@Param("receiverId") Long receiverId);

    void markAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
