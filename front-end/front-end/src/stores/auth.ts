import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { getUserInfo, type StoredUser } from '../api/user';
import { resolveAssetUrl } from '../composables/useAssetUrl';
import http from '../api/http';
import router from '../router';

export const useAuthStore = defineStore('auth', () => {
  const user = ref<StoredUser | null>(null);
  const accessToken = ref<string | null>(null);
  const refreshToken = ref<string | null>(null);

  const isLoggedIn = computed(() => !!accessToken.value);

  function loadFromStorage(): boolean {
    const token = localStorage.getItem('accessToken');
    const raw = localStorage.getItem('userInfo');
    if (!token || !raw) return false;
    try {
      accessToken.value = token;
      refreshToken.value = localStorage.getItem('refreshToken');
      user.value = JSON.parse(raw);
      return true;
    } catch {
      return false;
    }
  }

  function persistAuth(payload: {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
    user: StoredUser;
  }) {
    accessToken.value = payload.accessToken;
    refreshToken.value = payload.refreshToken;
    user.value = payload.user;
    localStorage.setItem('accessToken', payload.accessToken);
    localStorage.setItem('refreshToken', payload.refreshToken);
    localStorage.setItem('tokenType', payload.tokenType);
    localStorage.setItem('expiresIn', String(payload.expiresIn));
    localStorage.setItem('userInfo', JSON.stringify(payload.user));
  }

  function updateLocalUser(patch: Partial<StoredUser>) {
    if (user.value) {
      user.value = { ...user.value, ...patch };
      localStorage.setItem('userInfo', JSON.stringify(user.value));
    }
  }

  async function refreshUserInfo() {
    if (!user.value) return;
    try {
      const res = await getUserInfo(user.value.id);
      if (res.data.code === 0) {
        user.value = res.data.data;
        localStorage.setItem('userInfo', JSON.stringify(res.data.data));
      }
    } catch {
      // silent
    }
  }

  async function logout() {
    try {
      await http.post('/api/auth/logout');
    } catch {
      // silent
    }
    accessToken.value = null;
    refreshToken.value = null;
    user.value = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('tokenType');
    localStorage.removeItem('expiresIn');
    localStorage.removeItem('userInfo');
    router.replace('/');
  }

  return {
    user,
    accessToken,
    refreshToken,
    isLoggedIn,
    loadFromStorage,
    persistAuth,
    updateLocalUser,
    refreshUserInfo,
    logout,
    resolveAssetUrl,
  };
});
