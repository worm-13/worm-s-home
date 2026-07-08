import { ref } from 'vue';
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, type NotificationItem } from '../api/notification';
import { getUnreadMessageCount } from '../api/message';

const notifications = ref<NotificationItem[]>([]);
const unreadCount = ref(0);
const unreadMessageCount = ref(0);
const showNotificationPanel = ref(false);

let wsConnection: WebSocket | null = null;
let retryCount = 0;
const MAX_RETRIES = 5;

export function useNotification() {
  function connectWebSocket(onNotification?: () => void, onMessage?: () => void) {
    const token = localStorage.getItem('accessToken');
    if (!token) return;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/notifications`;
    wsConnection = new WebSocket(wsUrl, [`auth.${token}`]);
    wsConnection.onopen = () => { retryCount = 0; };
    wsConnection.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg.wsType === 'NOTIFICATION') {
          notifications.value.unshift(msg.data);
          unreadCount.value++;
          onNotification?.();
        } else if (msg.wsType === 'MESSAGE') {
          unreadMessageCount.value++;
          onMessage?.();
        }
      } catch { /* ignore parse errors */ }
    };
    wsConnection.onclose = () => {
      wsConnection = null;
      if (retryCount < MAX_RETRIES) {
        retryCount++;
        setTimeout(() => connectWebSocket(onNotification, onMessage), 5000);
      }
    };
    wsConnection.onerror = () => {
      wsConnection?.close();
    };
  }

  function disconnectWebSocket() {
    wsConnection?.close();
    wsConnection = null;
    retryCount = MAX_RETRIES; // 阻止重连
  }

  async function loadUnreadCount() {
    try {
      const res = await getUnreadCount();
      if (res.data.code === 0) unreadCount.value = res.data.data?.count ?? 0;
    } catch { /* ignore */ }
  }

  async function loadUnreadMessageCount() {
    try {
      const res = await getUnreadMessageCount();
      if (res.data.code === 0) unreadMessageCount.value = res.data.data?.count ?? 0;
    } catch { /* ignore */ }
  }

  async function loadNotifications() {
    try {
      const res = await getNotifications();
      if (res.data.code === 0) notifications.value = res.data.data?.items ?? [];
    } catch { /* ignore */ }
  }

  function toggleNotificationPanel() {
    showNotificationPanel.value = !showNotificationPanel.value;
    if (showNotificationPanel.value && notifications.value.length === 0) {
      void loadNotifications();
    }
  }

  function notificationText(n: NotificationItem): string {
    switch (n.type) {
      case 'COMMENT': return `评论了你的文章《${n.postTitle || ''}》`;
      case 'REPLY': return `回复了你在《${n.postTitle || ''}》中的评论`;
      case 'LIKE': return `赞了你的文章《${n.postTitle || ''}》`;
      case 'FOLLOW': return '关注了你';
      default: return '';
    }
  }

  async function handleNotificationClick(n: NotificationItem, onOpenArticle?: (id: number) => void) {
    if (!n.isRead) {
      try { await markAsRead(n.id); } catch { /* ignore */ }
      n.isRead = true;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
    if (n.targetId) onOpenArticle?.(n.targetId);
    showNotificationPanel.value = false;
  }

  async function handleMarkAllRead() {
    try { await markAllAsRead(); } catch { /* ignore */ }
    notifications.value.forEach(n => { n.isRead = true; });
    unreadCount.value = 0;
  }

  return {
    notifications,
    unreadCount,
    unreadMessageCount,
    showNotificationPanel,
    connectWebSocket,
    disconnectWebSocket,
    loadUnreadCount,
    loadUnreadMessageCount,
    loadNotifications,
    toggleNotificationPanel,
    notificationText,
    handleNotificationClick,
    handleMarkAllRead,
  };
}
