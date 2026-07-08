<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faPenToSquare } from '@fortawesome/free-regular-svg-icons';
import { faEnvelope, faGear, faCircleQuestion } from '@fortawesome/free-solid-svg-icons';
import { useAuth } from '../composables/useAuth';
import { useNotification } from '../composables/useNotification';
import { resolveAssetUrl } from '../composables/useAssetUrl';
import type { PostDetail } from '../api/post';

import LeftNav from '../components/LeftNav.vue';
import ArticleFeed from '../components/ArticleFeed.vue';
import ArticleDetailModal from '../components/ArticleDetailModal.vue';
import PostFormModal from '../components/PostFormModal.vue';
import DraftPanel from '../components/DraftPanel.vue';
import NotificationPanel from '../components/NotificationPanel.vue';
import UserProfileSection from '../components/UserProfileSection.vue';

const router = useRouter();
const { user, initFromStorage, refreshUserInfo, logout } = useAuth();
const {
  unreadMessageCount,
  connectWebSocket,
  disconnectWebSocket,
  loadUnreadCount,
  loadUnreadMessageCount,
} = useNotification();

/* ---- 状态 ---- */
const activeSection = ref<'home' | 'profile' | 'favorite' | 'drafts'>('home');
const isLeftNavCollapsed = ref(false);
const showUserMenu = ref(false);
const showPostModal = ref(false);
const draftToEdit = ref<PostDetail | null>(null);
const articleDetailVisible = ref(false);
const articleDetailId = ref<number | null>(null);

/* ---- 计算属性 ---- */
const avatarImageUrl = computed(() => resolveAssetUrl(user.value?.avatar));
const hasAvatar = computed(() => Boolean(avatarImageUrl.value));
const userInitial = computed(() => {
  const name = (user.value?.nickname || user.value?.username || '').trim();
  return name ? name.charAt(0).toUpperCase() : '?';
});
const maskedEmail = computed(() => {
  const source = user.value?.email?.trim();
  if (!source) return '未设置邮箱';
  const [local = '', domain = ''] = source.split('@');
  return `${(local.slice(0, 2) || 'us')}******@${domain}`;
});

/* ---- 生命周期 ---- */
onMounted(() => {
  if (!initFromStorage()) {
    router.replace('/');
    return;
  }
  void refreshUserInfo();
  void loadUnreadCount();
  void loadUnreadMessageCount();
  connectWebSocket();
});

onUnmounted(() => {
  disconnectWebSocket();
});

/* ---- 导航 ---- */
function handleNavigate(section: string) {
  activeSection.value = section as typeof activeSection.value;
}

function handleGoDrafts() {
  activeSection.value = 'drafts';
}

/* ---- 文章 ---- */
function openArticleDetail(id: number) {
  articleDetailId.value = id;
  articleDetailVisible.value = true;
}

function closeArticleDetail() {
  articleDetailVisible.value = false;
  articleDetailId.value = null;
}

function startArticleEditing() {
  draftToEdit.value = null;
  showPostModal.value = true;
}

function handleEditDraft(draft: PostDetail) {
  draftToEdit.value = draft;
  showPostModal.value = true;
}

function handlePublished() {
  showPostModal.value = false;
  draftToEdit.value = null;
}

function handleArticleDeleted() {
  closeArticleDetail();
}

function handleNotificationOpen(id: number) {
  openArticleDetail(id);
}

/* ---- 用户菜单 ---- */
function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value;
}

function closeUserMenu() {
  showUserMenu.value = false;
}

function toggleLeftNav() {
  isLeftNavCollapsed.value = !isLeftNavCollapsed.value;
}

function goProfile() {
  activeSection.value = 'profile';
  showUserMenu.value = false;
}

function goToUserProfile(userId: number) {
  if (userId) router.push(`/user/${userId}`);
}

function openSettings() {
  activeSection.value = 'profile';
  showUserMenu.value = false;
}

function handleLogout() {
  logout();
}
</script>

<template>
  <main class="home-container" @click="closeUserMenu">
    <!-- 汉堡菜单 -->
    <button class="menu-toggle" type="button" aria-label="切换左侧导航" @click.stop="toggleLeftNav">
      <span></span>
      <span></span>
      <span></span>
    </button>

    <!-- 品牌名 -->
    <p class="home-brand" aria-label="网站名">Lumen Grove</p>

    <!-- 私信按钮 -->
    <button class="msg-icon-btn" type="button" aria-label="私信" @click="router.push('/messages')">
      <FontAwesomeIcon :icon="faEnvelope" />
      <span v-if="unreadMessageCount > 0" class="notification-badge">{{ unreadMessageCount > 99 ? '99+' : unreadMessageCount }}</span>
    </button>

    <!-- 通知铃铛 -->
    <NotificationPanel @open-article="handleNotificationOpen" />

    <!-- 编辑按钮 -->
    <button class="home-edit-btn" type="button" aria-label="开始编辑文章" @click="startArticleEditing">
      <FontAwesomeIcon :icon="faPenToSquare" />
    </button>

    <!-- 用户头像 -->
    <button
      class="home-avatar"
      type="button"
      :title="user?.username || '访客'"
      aria-label="用户头像"
      @click.stop="toggleUserMenu"
    >
      <img v-if="hasAvatar" class="home-avatar-image" :src="avatarImageUrl" alt="" />
      <span v-else>{{ userInitial }}</span>
    </button>

    <!-- 用户菜单 -->
    <section v-if="showUserMenu" class="user-menu" @click.stop>
      <div class="user-menu-section user-menu-profile">
        <span class="user-menu-avatar">
          <img v-if="hasAvatar" class="user-menu-avatar-image" :src="avatarImageUrl" alt="" />
          <span v-else>{{ userInitial }}</span>
        </span>
        <div class="user-menu-profile-text">
          <p class="user-menu-name">{{ user?.nickname || user?.username || '访客' }}</p>
          <button class="user-menu-sub-link" type="button" @click="goProfile">查看主页</button>
          <button class="user-menu-sub-link" type="button" @click="openSettings">更改头像</button>
        </div>
      </div>

      <div class="user-menu-section">
        <button class="user-menu-link" type="button" @click="openSettings">
          <FontAwesomeIcon :icon="faGear" />
          <span>设置</span>
        </button>
        <button class="user-menu-link" type="button">
          <FontAwesomeIcon :icon="faCircleQuestion" />
          <span>帮助</span>
        </button>
      </div>

      <div class="user-menu-divider"></div>

      <div class="user-menu-section">
        <button class="user-menu-signout" @click="handleLogout">退出登录</button>
        <p class="user-menu-email">{{ maskedEmail }}</p>
      </div>

      <div class="user-menu-divider"></div>

      <div class="user-menu-footer" role="navigation" aria-label="菜单底部链接">
        <button type="button">关于</button>
        <span class="user-menu-meta">贡献人员：虫子wormzzZ</span>
        <div class="github-home-block">
          <span class="user-menu-meta">开发者 GitHub 首页</span>
          <a class="github-url" href="https://github.com/worm-13" target="_blank" rel="noopener noreferrer">https://github.com/worm-13</a>
        </div>
      </div>
    </section>

    <!-- 内容区 -->
    <section :class="['content-shell', isLeftNavCollapsed ? 'collapsed' : '']">
      <LeftNav
        :active-section="activeSection"
        @navigate="handleNavigate"
        @go-profile="goToUserProfile"
        @go-drafts="handleGoDrafts"
      />

      <!-- 首页 -->
      <div v-if="activeSection === 'home'" class="main-panel">
        <ArticleFeed @open-detail="openArticleDetail" />
      </div>

      <!-- 个人主页 -->
      <div v-else-if="activeSection === 'profile'" class="main-panel">
        <UserProfileSection />
      </div>

      <!-- 收藏 -->
      <div v-else-if="activeSection === 'favorite'" class="main-panel">
        <div class="welcome-box">
          <h1>收藏</h1>
          <p>这里将展示你收藏的内容。</p>
        </div>
      </div>

      <!-- 草稿箱 -->
      <div v-else-if="activeSection === 'drafts'" class="main-panel">
        <DraftPanel
          @edit-draft="handleEditDraft"
          @published="handlePublished"
          @deleted="handlePublished"
        />
      </div>
    </section>

    <!-- 文章详情弹窗 -->
    <ArticleDetailModal
      :visible="articleDetailVisible"
      :article-id="articleDetailId"
      @close="closeArticleDetail"
      @deleted="handleArticleDeleted"
    />

    <!-- 发布/编辑文章弹窗 -->
    <PostFormModal
      :visible="showPostModal"
      :draft-to-edit="draftToEdit"
      @close="showPostModal = false"
      @published="handlePublished"
    />
  </main>
</template>

<style scoped>
/* ========== 布局骨架 ========== */
.home-container {
  position: relative;
  min-height: 100vh;
  background-color: #f5f5f5;
  font-family: sans-serif;
  padding: 7.2rem 1.5rem 1.5rem;
  overflow: hidden;
}

.content-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
  min-height: calc(100vh - 8.7rem);
  transition: grid-template-columns 0.3s ease, gap 0.3s ease;
  align-items: stretch;
}

.content-shell.collapsed {
  grid-template-columns: 0 minmax(0, 1fr);
  gap: 0;
}

.main-panel {
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 0;
  transition: transform 0.3s ease;
}

/* ========== 顶部导航栏 ========== */
.menu-toggle {
  position: absolute;
  top: 2.15rem;
  left: 1.05rem;
  width: 42px;
  height: 42px;
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.9);
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.12);
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.menu-toggle:hover {
  background: #fff5d0;
  transform: translateY(-1px);
}

.menu-toggle span {
  width: 19px;
  height: 2.2px;
  border-radius: 999px;
  background: #101010;
}

.home-brand {
  position: absolute;
  top: 1.25rem;
  left: 7rem;
  margin: 0;
  padding: 0.48rem 0.95rem;
  border-radius: 999px;
  border: 1px solid rgba(226, 195, 118, 0.58);
  background: linear-gradient(135deg, #fff7c9 0%, #ffefad 100%);
  box-shadow:
    0 10px 22px rgba(232, 196, 102, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
  color: #000;
  font-size: clamp(1.55rem, 2.7vw, 2.3rem);
  font-weight: 800;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  text-shadow:
    0 0 1px rgba(0, 0, 0, 0.9),
    0 0 10px rgba(18, 212, 255, 0.42),
    0 0 22px rgba(18, 212, 255, 0.32),
    0 0 36px rgba(18, 212, 255, 0.24);
  animation: electric-flicker 2.6s infinite;
}

.home-brand::after {
  content: '';
  position: absolute;
  top: calc(100% + 14px);
  left: -7rem;
  width: 100vw;
  height: 2px;
  transform: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.45);
}

.msg-icon-btn {
  position: absolute;
  top: 1.8rem;
  right: calc(3rem + 52px + 20px + 44px + 12px + 44px + 12px);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  color: #333;
  transition: all 0.2s;
  z-index: 10;
}

.msg-icon-btn:hover {
  background: #f0f0f0;
  transform: scale(1.05);
}

.notification-badge {
  position: absolute;
  top: -4px;
  right: -4px;
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
  line-height: 1;
}

.home-edit-btn {
  position: absolute;
  top: 1.8rem;
  right: calc(3rem + 52px + 20px);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1.5px solid rgba(0, 0, 0, 0.5);
  background: #fff;
  color: #111;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.16);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.home-edit-btn::after {
  content: '编辑文章';
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%) translateY(-4px);
  padding: 5px 10px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.84);
  color: #fff;
  font-size: 0.78rem;
  line-height: 1;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.home-edit-btn:hover {
  transform: translateY(-1px);
  background-color: #fff7dd;
  box-shadow: 0 10px 18px rgba(0, 0, 0, 0.2);
}

.home-edit-btn:hover::after {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.home-edit-btn :deep(svg) {
  font-size: 1.15rem;
}

/* ========== 用户头像 & 菜单 ========== */
.home-avatar {
  position: absolute;
  top: 1.4rem;
  right: 2rem;
  width: 52px;
  height: 52px;
  padding: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.35rem;
  font-weight: 800;
  color: #101010;
  background: radial-gradient(circle at 30% 28%, #fffef3 0%, #f3ead0 40%, #e4d5ad 100%);
  border: 2px solid rgba(0, 0, 0, 0.5);
  box-shadow:
    0 8px 16px rgba(0, 0, 0, 0.18),
    inset 0 2px 5px rgba(255, 255, 255, 0.72);
  text-transform: uppercase;
  cursor: pointer;
  overflow: hidden;
  appearance: none;
  -webkit-appearance: none;
  flex-shrink: 0;
}

.home-avatar-image,
.user-menu-avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: 50%;
  aspect-ratio: 1 / 1;
  flex-shrink: 0;
}

.user-menu {
  position: absolute;
  top: calc(1.9rem + 52px + 10px);
  right: 1.5rem;
  width: 290px;
  padding: 12px 0;
  border-radius: 6px;
  background: #f6f6f6;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.2);
  z-index: 10;
}

.user-menu-section {
  padding: 0 20px;
}

.user-menu-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.user-menu-avatar {
  width: 56px;
  height: 56px;
  min-width: 56px;
  min-height: 56px;
  padding: 0;
  border-radius: 50%;
  background: #d8ccb2;
  color: #2a2a2a;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
  font-weight: 700;
  text-transform: uppercase;
  overflow: hidden;
  flex-shrink: 0;
}

.user-menu-profile-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-menu-name {
  margin: 0;
  color: #404040;
  font-size: 1.04rem;
  font-weight: 600;
}

.user-menu-link {
  width: 100%;
  border: 0;
  background: transparent;
  color: #4c4c4c;
  font-size: 1.02rem;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
  text-align: left;
}

.user-menu-link:hover {
  color: #1e1e1e;
}

.user-menu-link :deep(svg) {
  width: 20px;
  color: #6c6c6c;
}

.user-menu-divider {
  height: 1px;
  margin: 10px 0;
  background: rgba(0, 0, 0, 0.08);
}

.user-menu-sub-link {
  border: 0;
  background: transparent;
  color: #6b6b6b;
  font-size: 0.98rem;
  padding: 0;
  cursor: pointer;
  text-align: left;
}

.user-menu-sub-link:hover {
  color: #313131;
}

.user-menu-signout {
  width: 100%;
  border: 0;
  background: transparent;
  color: #4f4f4f;
  font-size: 2rem;
  padding: 2px 0;
  text-align: left;
  cursor: pointer;
}

.user-menu-signout:hover {
  color: #292929;
}

.user-menu-email {
  margin: 2px 0 0;
  color: #777;
  font-size: 0.92rem;
}

.user-menu-footer {
  padding: 0 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 12px;
}

.user-menu-footer button {
  border: 0;
  background: transparent;
  color: #666;
  font-size: 0.95rem;
  cursor: pointer;
  padding: 0;
}

.user-menu-footer button:hover {
  color: #323232;
}

.user-menu-meta {
  color: #666;
  font-size: 0.95rem;
}

.github-home-block {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.github-url {
  font-size: 0.86rem;
  color: #9a9a9a;
  text-decoration: none;
  transition: color 0.2s ease;
}

.github-url:hover {
  color: #2d6cdf;
}

/* ========== 收藏占位 ========== */
.welcome-box {
  background: white;
  padding: 3rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  text-align: center;
  min-width: 300px;
  width: min(560px, 100%);
}

.welcome-box h1 {
  color: #42b883;
  margin-bottom: 1.5rem;
}

/* ========== 左侧导航（折叠态） ========== */
:deep(.content-shell.collapsed .left-panel) {
  opacity: 0;
  transform: translateX(-100%) scale(0.98);
  pointer-events: none;
}

/* ========== 品牌名闪烁动画 ========== */
@keyframes electric-flicker {
  0%,
  100% {
    opacity: 1;
    filter: brightness(1);
  }

  12% {
    opacity: 0.88;
    filter: brightness(1.1);
  }

  13% {
    opacity: 0.98;
    filter: brightness(1);
  }

  45% {
    opacity: 0.9;
    filter: brightness(1.12);
  }

  46% {
    opacity: 1;
    filter: brightness(1);
  }

  78% {
    opacity: 0.92;
    filter: brightness(1.08);
  }
}
</style>
