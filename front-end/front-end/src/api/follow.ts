import http from './http';
import type { ApiResponse, PageResponse } from './http';

export type UserSimple = {
  id: number;
  username: string;
  nickname?: string;
  avatar?: string;
  bio?: string;
};

export function follow(userId: number) {
  return http.post<ApiResponse<null>>(`/api/users/${userId}/follow`);
}

export function unfollow(userId: number) {
  return http.delete<ApiResponse<null>>(`/api/users/${userId}/follow`);
}

export function getFollowing(userId: number, page = 1, size = 20) {
  return http.get<ApiResponse<PageResponse<UserSimple>>>(`/api/users/${userId}/following?page=${page}&size=${size}`);
}

export function getFollowers(userId: number, page = 1, size = 20) {
  return http.get<ApiResponse<PageResponse<UserSimple>>>(`/api/users/${userId}/followers?page=${page}&size=${size}`);
}

export function getFollowStatus(userId: number) {
  return http.get<ApiResponse<{ isFollowing: boolean; isFollowedBy: boolean }>>(`/api/users/${userId}/follow-status`);
}
