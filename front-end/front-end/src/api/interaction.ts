import http from './http';
import type { ApiResponse } from './http';

export function likePost(postId: number) {
  return http.post<ApiResponse<null>>(`/api/posts/${postId}/like`);
}

export function unlikePost(postId: number) {
  return http.delete<ApiResponse<null>>(`/api/posts/${postId}/like`);
}

export function favoritePost(postId: number) {
  return http.post<ApiResponse<null>>(`/api/posts/${postId}/favorite`);
}

export function unfavoritePost(postId: number) {
  return http.delete<ApiResponse<null>>(`/api/posts/${postId}/favorite`);
}

export function getInteractionStatus(postId: number) {
  return http.get<ApiResponse<{ isLiked: boolean; isFavorited: boolean }>>(
    `/api/posts/${postId}/interaction-status`
  );
}

export function getMyFavorites() {
  return http.get<ApiResponse<any[]>>('/api/users/me/favorites');
}
