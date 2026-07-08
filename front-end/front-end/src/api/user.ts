import http from './http';
import type { ApiResponse } from './http';

export type StoredUser = {
  id: number;
  username: string;
  nickname?: string | null;
  email?: string | null;
  avatar?: string | null;
  backgroundImage?: string | null;
  bio?: string | null;
  role?: string | null;
};

export function changePassword(oldPassword: string, newPassword: string) {
  return http.put<ApiResponse<null>>('/api/auth/users/me/password', {
    oldPassword,
    newPassword
  });
}

export function updateSignature(bio: string) {
  return http.put<ApiResponse<{ bio: string }>>('/api/auth/users/me/profile', {
    bio
  });
}

/** 获取指定用户信息 */
export function getUserInfo(userId: number) {
  return http.get<ApiResponse<StoredUser>>(`/api/auth/users/${userId}`);
}

/** 上传头像（multipart） */
export function uploadAvatar(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return http.post<ApiResponse<{ url: string }>>('/api/auth/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

/** 上传背景图（multipart） */
export function uploadBackgroundImage(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return http.post<ApiResponse<{ url: string }>>('/api/auth/users/me/background-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}
