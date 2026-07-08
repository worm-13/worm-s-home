<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faArrowLeft, faPaperPlane } from '@fortawesome/free-solid-svg-icons';
import {
  getConversations,
  getConversation,
  sendMessage as apiSendMessage,
  markConversationAsRead,
  type ConversationItem,
  type MessageItem
} from '../api/message';
import { resolveAssetUrl } from '../composables/useAssetUrl';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const currentUser = ref<{ id: number; username: string; nickname?: string | null; avatar?: string | null } | null>(null);
const conversations = ref<ConversationItem[]>([]);
const activeUserId = ref<number | null>(null);
const messages = ref<MessageItem[]>([]);
const inputText = ref('');
const isLoadingConversations = ref(false);
const isLoadingMessages = ref(false);
const isSending = ref(false);
const chatBodyRef = ref<HTMLDivElement | null>(null);
const notMutualFollow = ref(false);
let wsConnection: WebSocket | null = null;

function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
}

function formatMessageTime(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
    }
  });
}

async function loadConversations() {
  isLoadingConversations.value = true;
  try {
    const res = await getConversations();
    if (res.data.code === 0) {
      conversations.value = res.data.data ?? [];
    }
  } catch { /* ignore */ } finally {
    isLoadingConversations.value = false;
  }
}

async function selectConversation(userId: number) {
  if (activeUserId.value === userId) return;
  activeUserId.value = userId;
  notMutualFollow.value = false;
  await loadMessages(userId);
  try { await markConversationAsRead(userId); } catch { /* ignore */ }
  // 清除本地未读
  const conv = conversations.value.find(c => c.userId === userId);
  if (conv) conv.unreadCount = 0;
}

async function loadMessages(userId: number) {
  isLoadingMessages.value = true;
  messages.value = [];
  try {
    const res = await getConversation(userId);
    if (res.data.code === 0) {
      messages.value = res.data.data ?? [];
    }
  } catch { /* ignore */ } finally {
    isLoadingMessages.value = false;
    scrollToBottom();
  }
}

async function handleSend() {
  const text = inputText.value.trim();
  if (!text || !activeUserId.value || isSending.value) return;
  isSending.value = true;
  notMutualFollow.value = false;
  try {
    const res = await apiSendMessage(activeUserId.value, text);
    if (res.data.code === 0) {
      messages.value.push(res.data.data!);
      inputText.value = '';
      scrollToBottom();
      // 更新会话列表
      const conv = conversations.value.find(c => c.userId === activeUserId.value);
      if (conv) {
        conv.lastMessage = text;
        conv.lastMessageTime = new Date().toISOString();
      }
    }
  } catch (err: any) {
    const code = err?.response?.data?.code;
    if (code === 1038) {
      notMutualFollow.value = true;
    }
  } finally {
    isSending.value = false;
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
}

function connectWS() {
  const token = authStore.accessToken;
  if (!token) return;
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/ws/notifications?token=${token}`;
  wsConnection = new WebSocket(wsUrl);
  wsConnection.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data);
      if (msg.wsType === 'MESSAGE') {
        const newMsg: MessageItem = msg.data;
        // 如果是当前对话的消息
        if (activeUserId.value && (newMsg.senderId === activeUserId.value || newMsg.receiverId === activeUserId.value)) {
          messages.value.push(newMsg);
          scrollToBottom();
          // 标记已读
          if (newMsg.senderId === activeUserId.value) {
            try { markConversationAsRead(activeUserId.value); } catch { /* ignore */ }
          }
        }
        // 更新会话列表
        const conv = conversations.value.find(c => c.userId === newMsg.senderId || c.userId === newMsg.receiverId);
        if (conv) {
          conv.lastMessage = newMsg.content;
          conv.lastMessageTime = newMsg.createdAt;
          if (newMsg.senderId !== currentUser.value?.id && activeUserId.value !== newMsg.senderId) {
            conv.unreadCount++;
          }
        } else {
          // 新会话，重新加载列表
          loadConversations();
        }
      }
    } catch { /* ignore */ }
  };
  wsConnection.onclose = () => {
    wsConnection = null;
    if (currentUser.value) setTimeout(connectWS, 5000);
  };
  wsConnection.onerror = () => {
    wsConnection?.close();
  };
}

function goBack() {
  router.push('/home');
}

onMounted(async () => {
  if (authStore.user) currentUser.value = authStore.user;
  await loadConversations();
  connectWS();
  // 支持 URL 参数直接打开对话
  const targetUserId = Number(route.query.userId);
  if (targetUserId) {
    await selectConversation(targetUserId);
  }
});

onUnmounted(() => {
  wsConnection?.close();
  wsConnection = null;
});
</script>

<template>
  <div class="messages-page">
    <!-- 顶部导航 -->
    <header class="messages-header">
      <button class="back-btn" @click="goBack">
        <FontAwesomeIcon :icon="faArrowLeft" />
        <span>返回</span>
      </button>
      <h1 class="messages-title">私信</h1>
      <div style="width: 60px"></div>
    </header>

    <div class="messages-body">
      <!-- 左侧会话列表 -->
      <aside class="conversation-list">
        <div v-if="isLoadingConversations" class="conv-loading">加载中...</div>
        <div v-else-if="conversations.length === 0" class="conv-empty">暂无会话</div>
        <div
          v-for="conv in conversations"
          :key="conv.userId"
          :class="['conv-item', { active: activeUserId === conv.userId }]"
          @click="selectConversation(conv.userId)"
        >
          <div class="conv-avatar-wrap">
            <img v-if="conv.avatar" :src="resolveAssetUrl(conv.avatar)" loading="lazy" class="conv-avatar" />
            <span v-else class="conv-avatar-fallback">{{ (conv.nickname || conv.username || '?').charAt(0) }}</span>
            <span v-if="conv.unreadCount > 0" class="conv-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
          </div>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">{{ conv.nickname || conv.username }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <p class="conv-preview">{{ conv.lastMessage }}</p>
          </div>
        </div>
      </aside>

      <!-- 右侧聊天窗口 -->
      <section class="chat-panel">
        <template v-if="activeUserId">
          <div ref="chatBodyRef" class="chat-body">
            <div v-if="isLoadingMessages" class="chat-loading">加载中...</div>
            <template v-else>
              <div v-for="msg in messages" :key="msg.id" :class="['chat-bubble-row', { mine: msg.senderId === currentUser?.id }]">
                <div class="chat-bubble">
                  <p class="chat-bubble-text">{{ msg.content }}</p>
                  <span class="chat-bubble-time">{{ formatMessageTime(msg.createdAt) }}</span>
                </div>
              </div>
            </template>
          </div>
          <div class="chat-input-area">
            <p v-if="notMutualFollow" class="chat-error-tip">双方互相关注后才能发送私信</p>
            <div class="chat-input-row">
              <textarea
                v-model="inputText"
                class="chat-input"
                placeholder="输入消息..."
                rows="1"
                :disabled="notMutualFollow"
                @keydown="handleKeydown"
              ></textarea>
              <button class="chat-send-btn" :disabled="!inputText.trim() || isSending || notMutualFollow" @click="handleSend">
                <FontAwesomeIcon :icon="faPaperPlane" />
              </button>
            </div>
          </div>
        </template>
        <div v-else class="chat-placeholder">
          <p>选择一个会话开始聊天</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.messages-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.messages-header {
  background: #fff;
  padding: 12px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: #666;
  font-size: 0.95rem;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #333;
}

.messages-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

.messages-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧会话列表 */
.conversation-list {
  width: 320px;
  background: #fff;
  border-right: 1px solid rgba(0, 0, 0, 0.08);
  overflow-y: auto;
  flex-shrink: 0;
}

.conv-loading,
.conv-empty {
  padding: 40px 16px;
  text-align: center;
  color: #999;
  font-size: 0.9rem;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.conv-item:hover {
  background: #f8f8f8;
}

.conv-item.active {
  background: #e8f0fe;
}

.conv-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.conv-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.conv-avatar-fallback {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  font-weight: 600;
  color: #555;
}

.conv-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #e53935;
  color: #fff;
  font-size: 0.65rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conv-name {
  font-size: 0.95rem;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 0.75rem;
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}

.conv-preview {
  margin: 0;
  font-size: 0.82rem;
  color: #888;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 右侧聊天窗口 */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f9f9f9;
  min-width: 0;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-loading {
  text-align: center;
  padding: 40px;
  color: #999;
}

.chat-bubble-row {
  display: flex;
  justify-content: flex-start;
}

.chat-bubble-row.mine {
  justify-content: flex-end;
}

.chat-bubble {
  max-width: 65%;
  padding: 10px 14px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: relative;
}

.chat-bubble-row.mine .chat-bubble {
  background: #1a73e8;
  color: #fff;
}

.chat-bubble-text {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-bubble-time {
  display: block;
  font-size: 0.7rem;
  margin-top: 4px;
  opacity: 0.6;
  text-align: right;
}

.chat-input-area {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  padding: 12px 16px;
}

.chat-error-tip {
  margin: 0 0 8px;
  font-size: 0.82rem;
  color: #e53935;
  text-align: center;
}

.chat-input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 10px 16px;
  font-size: 0.95rem;
  resize: none;
  outline: none;
  font-family: inherit;
  max-height: 120px;
  transition: border-color 0.2s;
}

.chat-input:focus {
  border-color: #1a73e8;
}

.chat-input:disabled {
  background: #f5f5f5;
  color: #999;
}

.chat-send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: #1a73e8;
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.chat-send-btn:hover:not(:disabled) {
  background: #1557b0;
}

.chat-send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 1rem;
}

/* 响应式 */
@media (max-width: 768px) {
  .conversation-list {
    width: 100%;
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 5;
  }

  .chat-panel {
    width: 100%;
  }

  .messages-body {
    position: relative;
  }
}
</style>
