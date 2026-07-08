<script setup lang="ts">
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useNotification } from '../composables/useNotification';
import { resolveAssetUrl } from '../composables/useAssetUrl';
import type { NotificationItem } from '../api/notification';

const {
  notifications,
  unreadCount,
  showNotificationPanel,
  toggleNotificationPanel,
  notificationText,
  handleNotificationClick,
  handleMarkAllRead,
} = useNotification();

const emit = defineEmits<{
  'open-article': [id: number];
}>();

function formatDateTime(value?: string) {
  if (!value) return '';
  const date = new Date(value.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return value;
  const y = date.getFullYear();
  const m = `${date.getMonth() + 1}`.padStart(2, '0');
  const d = `${date.getDate()}`.padStart(2, '0');
  return `${y}-${m}-${d} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`;
}

function onNotificationClick(n: NotificationItem) {
  handleNotificationClick(n, (id) => emit('open-article', id));
}
</script>

<template>
  <button class="notification-bell-btn" type="button" aria-label="消息通知" @click.stop="toggleNotificationPanel">
    <FontAwesomeIcon :icon="['fas', 'bell']" />
    <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
  </button>
  <div v-if="showNotificationPanel" class="notification-panel" @click.stop>
    <header class="notification-panel-header">
      <span>消息通知</span>
      <button type="button" @click="handleMarkAllRead">全部已读</button>
    </header>
    <div v-if="notifications.length === 0" class="notification-empty">暂无通知</div>
    <div v-else class="notification-list">
      <div v-for="n in notifications" :key="n.id" :class="['notification-item', { unread: !n.isRead }]" @click="onNotificationClick(n)">
        <img v-if="n.senderAvatar" :src="resolveAssetUrl(n.senderAvatar)" loading="lazy" class="notification-avatar" />
        <span v-else class="notification-avatar-placeholder">{{ (n.senderName || '?').charAt(0) }}</span>
        <div class="notification-body">
          <p><span class="notification-sender">{{ n.senderName }}</span> <span class="notification-text">{{ notificationText(n) }}</span></p>
          <span class="notification-time">{{ formatDateTime(n.createdAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 通知面板 */
.notification-panel {
  position: absolute;
  top: calc(1.8rem + 52px);
  right: calc(3rem + 52px + 20px + 44px + 12px - 160px);
  width: 360px;
  max-height: 420px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
  z-index: 100;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.notification-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eee;
  font-weight: 600;
  font-size: 0.95rem;
}

.notification-panel-header button {
  background: none;
  border: none;
  color: #1a73e8;
  font-size: 0.82rem;
  cursor: pointer;
  font-weight: 500;
}

.notification-panel-header button:hover {
  text-decoration: underline;
}

.notification-empty {
  padding: 40px 16px;
  text-align: center;
  color: #999;
  font-size: 0.9rem;
}

.notification-list {
  overflow-y: auto;
  max-height: 360px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-left: 3px solid transparent;
}

.notification-item:hover {
  background: #f8f9fa;
}

.notification-item.unread {
  background: #f0f6ff;
  border-left-color: #1a73e8;
}

.notification-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.notification-avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e8f0fe;
  color: #1a73e8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
  flex-shrink: 0;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-body p {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.4;
  color: #333;
}

.notification-sender {
  font-weight: 600;
}

.notification-text {
  color: #666;
}

.notification-time {
  font-size: 0.75rem;
  color: #999;
  margin-top: 2px;
  display: block;
}

.notification-bell-btn {
  position: relative;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  color: inherit;
  padding: 0;
}

.notification-badge {
  position: absolute;
  top: -4px;
  right: -6px;
  background: #e53935;
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 999px;
  min-width: 14px;
  text-align: center;
  line-height: 1.4;
}
</style>
