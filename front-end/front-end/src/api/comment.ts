import http from './http';
import type { ApiResponse, PageResponse } from './http';

export type CommentVO = {
  id: number;
  postId: number;
  userId: number;
  username: string;
  nickname?: string;
  avatar?: string;
  parentId?: number;
  content: string;
  createdAt: string;
  replies?: CommentVO[];
};

export function createComment(postId: number, content: string, parentId?: number) {
  return http.post<ApiResponse<{ commentId: number }>>('/api/comments', {
    postId,
    content,
    parentId
  });
}

export function getCommentsByPostId(postId: number, page = 1, size = 20) {
  return http.get<ApiResponse<PageResponse<CommentVO>>>(`/api/comments/post/${postId}?page=${page}&size=${size}`);
}

export function deleteComment(commentId: number) {
  return http.delete<ApiResponse<null>>(`/api/comments/${commentId}`);
}
