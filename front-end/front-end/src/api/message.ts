import http from './http';
import type { ApiResponse } from './http';

export type MessageItem = {
    id: number;
    senderId: number;
    receiverId: number;
    content: string;
    isRead: boolean;
    createdAt: string;
    senderName: string;
    senderAvatar?: string;
};

export type ConversationItem = {
    userId: number;
    username: string;
    nickname?: string;
    avatar?: string;
    lastMessage: string;
    lastMessageTime: string;
    unreadCount: number;
};

export function sendMessage(receiverId: number, content: string) {
    return http.post<ApiResponse<MessageItem>>('/api/messages', { receiverId, content });
}

export function getConversations() {
    return http.get<ApiResponse<ConversationItem[]>>('/api/messages/conversations');
}

export function getConversation(userId: number, page = 1, size = 50) {
    return http.get<ApiResponse<MessageItem[]>>(`/api/messages/conversations/${userId}?page=${page}&size=${size}`);
}

export function getUnreadMessageCount() {
    return http.get<ApiResponse<{ count: number }>>('/api/messages/unread-count');
}

export function markConversationAsRead(userId: number) {
    return http.put<ApiResponse<null>>(`/api/messages/conversations/${userId}/read`);
}
