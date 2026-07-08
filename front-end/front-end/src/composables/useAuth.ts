import { storeToRefs } from 'pinia';
import { useAuthStore } from '../stores/auth';

/**
 * @deprecated 直接使用 useAuthStore() 更简洁。保留此函数仅为向后兼容。
 */
export function useAuth() {
  const store = useAuthStore();
  const { user, isLoggedIn } = storeToRefs(store);

  return {
    user,
    isLoggedIn,
    initFromStorage: () => store.loadFromStorage(),
    refreshUserInfo: () => store.refreshUserInfo(),
    logout: () => store.logout(),
    updateLocalUser: (patch: Record<string, unknown>) => store.updateLocalUser(patch),
    resolveAssetUrl: store.resolveAssetUrl,
  };
}
