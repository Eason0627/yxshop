package com.yxshop.Module.Message.Service;

import com.yxshop.Module.Message.Dto.MessageSendDto;
import com.yxshop.Module.Message.Entity.MessageConversationEntity;

public interface MessageConversationService {
    /** 当前用户会话列表 */
    Object listConversations(Long userId, Integer page, Integer size);

    /** 会话消息记录 */
    Object getMessages(Long userId, Long conversationId, Integer page, Integer size);

    /** 发送消息 */
    Object sendMessage(Long userId, MessageSendDto dto);

    /** 标记会话已读 */
    Object markRead(Long userId, Long conversationId);

    /** 获取未读消息总数 */
    Object unreadCount(Long userId);

    /** 删除/归档会话 */
    Object deleteConversation(Long userId, Long conversationId);

    /** 创建系统会话（供业务事件调用，如订单通知） */
    MessageConversationEntity ensureSystemConversation(Long userId, String conversationType, String groupType,
                                                        String name, String avatar, Long targetId);
}
