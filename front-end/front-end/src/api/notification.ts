import http from './http';
import type { ApiResponse, PageResponse } from './http';

export type NotificationItem = {
    id: number;
    type: 'COMMENT' | 'REPLY' | 'LIKE' | 'FOLLOW';
    targetId?: number;
    isRead: boolean;
    createdAt: string;
    senderId: number;
    senderName: string;
    senderAvatar?: string;
    postTitle?: string;
};

/** 获取通知列表 */
export function getNotifications(page = 1, size = 20) {
    return http.get<ApiResponse<PageResponse<NotificationItem>>>(`/api/notifications?page=${page}&size=${size}`);
}

/** 获取未读数量 */
export function getUnreadCount() {
    return http.get<ApiResponse<{ count: number }>>('/api/notifications/unread-count');
}

/** 标记单条已读 */
export function markAsRead(id: number) {
    return http.put<ApiResponse<null>>(`/api/notifications/${id}/read`);
}

/** 标记全部已读 */
export function markAllAsRead() {
    return http.put<ApiResponse<null>>('/api/notifications/read-all');
}
