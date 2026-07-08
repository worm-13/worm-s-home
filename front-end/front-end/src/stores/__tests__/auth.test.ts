import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from '../auth';

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it('should initialize with no user', () => {
    const store = useAuthStore();
    expect(store.user).toBeNull();
    expect(store.accessToken).toBeNull();
    expect(store.isLoggedIn).toBe(false);
  });

  it('should persist auth data', () => {
    const store = useAuthStore();
    const payload = {
      accessToken: 'test-token',
      refreshToken: 'test-refresh',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        id: 1,
        username: 'testuser',
        nickname: 'Test',
        role: 'USER'
      }
    };

    store.persistAuth(payload);

    expect(store.accessToken).toBe('test-token');
    expect(store.refreshToken).toBe('test-refresh');
    expect(store.user?.username).toBe('testuser');
    expect(store.isLoggedIn).toBe(true);
    expect(localStorage.getItem('accessToken')).toBe('test-token');
  });

  it('should load from storage', () => {
    localStorage.setItem('accessToken', 'stored-token');
    localStorage.setItem('refreshToken', 'stored-refresh');
    localStorage.setItem('userInfo', JSON.stringify({
      id: 1,
      username: 'stored',
      role: 'ADMIN'
    }));

    const store = useAuthStore();
    const result = store.loadFromStorage();

    expect(result).toBe(true);
    expect(store.accessToken).toBe('stored-token');
    expect(store.user?.username).toBe('stored');
    expect(store.user?.role).toBe('ADMIN');
  });

  it('should logout and clear storage', async () => {
    const store = useAuthStore();
    store.persistAuth({
      accessToken: 'token',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: { id: 1, username: 'test', role: 'USER' }
    });

    await store.logout();

    expect(store.accessToken).toBeNull();
    expect(store.user).toBeNull();
    expect(localStorage.getItem('accessToken')).toBeNull();
  });

  it('should update local user', () => {
    const store = useAuthStore();
    store.persistAuth({
      accessToken: 'token',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: { id: 1, username: 'test', role: 'USER' }
    });

    store.updateLocalUser({ nickname: 'Updated' });

    expect(store.user?.nickname).toBe('Updated');
    expect(JSON.parse(localStorage.getItem('userInfo')!).nickname).toBe('Updated');
  });
});
