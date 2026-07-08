import axios from 'axios';

export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type PageResponse<T> = {
  items: T[];
  total: number;
  page: number;
  size: number;
  hasMore: boolean;
};

const http = axios.create({
  timeout: 10_000
});

let refreshPromise: Promise<string> | null = null;

http.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem('accessToken') || localStorage.getItem('token');
  const tokenType = localStorage.getItem('tokenType') || 'Bearer';
  if (accessToken) {
    config.headers.Authorization = `${tokenType} ${accessToken}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.includes('/refresh-token')) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          if (!refreshPromise) {
            refreshPromise = axios.post('/api/auth/refresh-token', { refreshToken })
              .then(res => {
                const { accessToken, refreshToken: newRefreshToken } = res.data.data;
                localStorage.setItem('accessToken', accessToken);
                if (newRefreshToken) {
                  localStorage.setItem('refreshToken', newRefreshToken);
                }
                return accessToken;
              })
              .finally(() => {
                refreshPromise = null;
              });
          }

          const newToken = await refreshPromise;
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return http(originalRequest);
        } catch (refreshError) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('tokenType');
          localStorage.removeItem('expiresIn');
          localStorage.removeItem('userInfo');
          window.location.href = '/';
          return Promise.reject(refreshError);
        }
      }
    }

    return Promise.reject(error);
  }
);

export default http;
