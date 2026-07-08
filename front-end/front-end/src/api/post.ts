import http from './http';
import type { ApiResponse, PageResponse } from './http';

export type CreatePostPayload = {
  title: string;
  content: string;
  summary?: string;
  coverImage?: string;
  status?: number;
  scheduledAt?: string;
};

export type UpdatePostPayload = {
  title?: string;
  content?: string;
  summary?: string;
  coverImage?: string;
  status?: number;
  scheduledAt?: string;
};

export type PostListItem = {
  id: number;
  userId: number;
  authorName: string;
  authorAvatar?: string;
  title: string;
  summary?: string;
  coverImage?: string;
  status?: number;
  scheduledAt?: string;
  createdAt: string;
  updatedAt?: string;
};

export type PostDetail = {
  id: number;
  userId: number;
  authorName: string;
  authorAvatar?: string;
  title: string;
  content: string;
  summary?: string;
  coverImage?: string;
  status?: number;
  scheduledAt?: string;
  createdAt: string;
  updatedAt?: string;
  likesCount?: number;
  favoritesCount?: number;
  isLiked?: boolean;
  isFavorited?: boolean;
};

/** 发布文章 */
export function createPost(payload: CreatePostPayload) {
  return http.post<ApiResponse<{ postId: number }>>('/api/posts', payload);
}

/** 保存草稿 */
export function saveDraft(payload: CreatePostPayload) {
  return http.post<ApiResponse<{ postId: number }>>('/api/posts/draft', payload);
}

/** 更新文章 */
export function updatePost(postId: number, payload: UpdatePostPayload) {
  return http.put<ApiResponse<null>>(`/api/posts/${postId}`, payload);
}

/** 删除文章 */
export function deletePost(postId: number) {
  return http.delete<ApiResponse<null>>(`/api/posts/${postId}`);
}

/** 上传封面图片 */
export function uploadPostCover(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return http.post<ApiResponse<{ url: string }>>('/api/posts/cover-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

/** 首页已发布文章列表（分页） */
export function getPostList(page = 1, size = 10) {
  return http.get<ApiResponse<PageResponse<PostListItem>>>(`/api/posts?page=${page}&size=${size}`);
}

/** 已发布文章详情 */
export function getPostDetail(id: number) {
  return http.get<ApiResponse<PostDetail>>(`/api/posts/${id}`);
}

/** 发布草稿/定时文章为已发布 */
export function publishPost(postId: number) {
  return http.post<ApiResponse<null>>(`/api/posts/${postId}/publish`);
}

/** 查看当前用户所有文章（含草稿、定时） */
export function getMyPosts() {
  return http.get<ApiResponse<PostListItem[]>>('/api/posts/my');
}

/** 查看当前用户草稿列表（分页） */
export function getMyDrafts(page = 1, size = 10) {
  return http.get<ApiResponse<PageResponse<PostListItem>>>(`/api/posts/my/drafts?page=${page}&size=${size}`);
}

/** 查看自己的文章详情（草稿/定时也可查看） */
export function getMyPostDetail(id: number) {
  return http.get<ApiResponse<PostDetail>>(`/api/posts/my/${id}`);
}

/** 查看指定用户已发布文章列表（分页） */
export function getUserPosts(userId: number, page = 1, size = 50) {
  return http.get<ApiResponse<PageResponse<PostListItem>>>(`/api/posts/user/${userId}?page=${page}&size=${size}`);
}
