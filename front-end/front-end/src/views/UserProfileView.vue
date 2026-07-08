<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getFollowStatus, follow, unfollow } from '../api/follow';
import { getUserInfo } from '../api/user';
import { getUserPosts } from '../api/post';
import { resolveAssetUrl } from '../composables/useAssetUrl';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const userId = computed(() => Number(route.params.id));
const currentUser = computed(() => authStore.user);

const isOwnProfile = computed(() => currentUser.value?.id === userId.value);

// 用户信息
interface UserProfile {
  id: number;
  username: string;
  nickname?: string | null;
  avatar?: string | null;
  bio?: string | null;
  backgroundImage?: string | null;
  followersCount?: number;
  followingCount?: number;
  postsCount?: number;
}
const userInfo = ref<UserProfile | null>(null);
const isFollowing = ref(false);
const isFollowedBy = ref(false);
const isMutualFollow = computed(() => isFollowing.value && isFollowedBy.value);
const isLoading = ref(true);
const error = ref('');

// 背景图和头像URL
const backgroundImageUrl = computed(() => resolveAssetUrl(userInfo.value?.backgroundImage));
const canShowBackgroundImage = computed(() => Boolean(backgroundImageUrl.value));
const avatarImageUrl = computed(() => resolveAssetUrl(userInfo.value?.avatar));
const canShowAvatarImage = computed(() => Boolean(avatarImageUrl.value));

// 文章列表
interface UserPost {
  id: number;
  title: string;
  summary?: string;
  coverImage?: string;
  createdAt: string;
}
const userPosts = ref<UserPost[]>([]);
const isLoadingPosts = ref(false);

// 加载用户信息
async function loadUserInfo() {
  isLoading.value = true;
  error.value = '';
  try {
    // 获取用户基本信息
    const userRes = await getUserInfo(userId.value);
    if (userRes.data.code === 0) {
      userInfo.value = userRes.data.data;
    } else {
      error.value = '用户不存在';
      return;
    }

    // 获取关注状态（仅登录用户且非本人）
    if (authStore.accessToken && !isOwnProfile.value) {
      const statusRes = await getFollowStatus(userId.value);
      if (statusRes.data.code === 0) {
        isFollowing.value = statusRes.data.data?.isFollowing || false;
        isFollowedBy.value = statusRes.data.data?.isFollowedBy || false;
      }
    }

    // 获取用户文章列表
    await loadUserPosts();
  } catch (e) {
    error.value = '加载失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
}

// 加载用户文章
async function loadUserPosts() {
  isLoadingPosts.value = true;
  try {
    const res = await getUserPosts(userId.value);
    if (res.data.code === 0) {
      userPosts.value = res.data.data?.items || [];
    }
  } catch (e) {
  } finally {
    isLoadingPosts.value = false;
  }
}

// 关注/取消关注
async function toggleFollow() {
  if (!authStore.accessToken) {
    router.push('/');
    return;
  }

  try {
    if (isFollowing.value) {
      const res = await unfollow(userId.value);
      if (res.data.code === 0) {
        isFollowing.value = false;
        if (userInfo.value) {
          userInfo.value.followersCount = Math.max(0, (userInfo.value.followersCount || 1) - 1);
        }
      }
    } else {
      const res = await follow(userId.value);
      if (res.data.code === 0) {
        isFollowing.value = true;
        if (userInfo.value) {
          userInfo.value.followersCount = (userInfo.value.followersCount || 0) + 1;
        }
      }
    }
  } catch (e) {
  }
}

// 格式化日期
function formatDate(dateStr: string) {
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
}

// 返回首页
function goBack() {
  router.push('/home');
}

onMounted(() => {
  loadUserInfo();
});
</script>

<template>
  <div class="user-profile-page">
    <!-- 顶部导航 -->
    <header class="profile-page-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回首页
      </button>
    </header>

    <!-- 加载中 -->
    <div v-if="isLoading" class="loading-state">
      <p>加载中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button @click="loadUserInfo">重试</button>
    </div>

    <!-- 用户信息 -->
    <div v-else class="profile-container">
      <div class="profile-box">
        <!-- 背景图 -->
        <div class="profile-hero">
          <img
            v-if="canShowBackgroundImage"
            class="profile-hero-image"
            :src="backgroundImageUrl"
            loading="lazy"
            alt="背景图"
          />
          <div v-else class="profile-hero-placeholder"></div>
          <div class="profile-hero-mask"></div>
        </div>

        <!-- 头像 -->
        <div class="profile-avatar-wrap profile-avatar-wrap--floating">
          <img
            v-if="canShowAvatarImage"
            class="profile-avatar-image"
            :src="avatarImageUrl"
            loading="lazy"
            alt="头像"
          />
          <span v-else class="profile-avatar-fallback">{{ userInfo?.nickname?.[0] || userInfo?.username?.[0] || '?' }}</span>
        </div>

        <div class="profile-main-info">
          <h2 class="profile-name">
            {{ userInfo?.nickname || userInfo?.username || '未知用户' }}
            <button v-if="!isOwnProfile && currentUser && isMutualFollow" class="profile-message-btn" @click="router.push('/messages?userId=' + route.params.id)">私信</button>
            <button v-if="!isOwnProfile && currentUser" class="follow-btn" :class="{ 'following': isFollowing }" @click="toggleFollow">
              {{ isFollowing ? '已关注' : '关注' }}
            </button>
          </h2>

          <!-- 个性签名框 -->
          <div class="profile-signature-box">
            <div class="profile-signature-header">
              <p class="profile-signature-label">个性签名</p>
            </div>
            <p v-if="userInfo?.bio" class="profile-signature">{{ userInfo.bio }}</p>
            <p v-else class="profile-signature-empty">
              <span v-if="isOwnProfile">还没设置个性签名，赶快设置吧</span>
              <span v-else>该用户未设置个性签名</span>
              <button v-if="isOwnProfile" class="set-signature-btn" @click="router.push('/home')">去设置</button>
            </p>
          </div>

          <!-- 统计信息 -->
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">{{ userInfo?.followingCount || 0 }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo?.followersCount || 0 }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo?.postsCount || userPosts.length }}</span>
              <span class="stat-label">文章</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 文章列表 -->
      <div class="profile-posts-section">
        <h2 class="section-title">文章</h2>

        <div v-if="isLoadingPosts" class="loading-posts">加载中...</div>

        <div v-else-if="userPosts.length === 0" class="empty-posts">
          <p>暂无文章</p>
        </div>

        <div v-else class="posts-list">
          <div v-for="post in userPosts" :key="post.id" class="post-card">
            <div class="post-main">
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-summary">{{ post.summary }}</p>
              <span class="post-date">{{ formatDate(post.createdAt) }}</span>
            </div>
            <img v-if="post.coverImage" :src="resolveAssetUrl(post.coverImage)" loading="lazy" alt="封面" class="post-cover" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 顶部导航 */
.profile-page-header {
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
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

/* 加载和错误状态 */
.loading-state,
.error-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.error-state button {
  margin-top: 12px;
  padding: 8px 20px;
  background: #ff6b35;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

/* 内容区域 */
.profile-container {
  max-width: 980px;
  margin: 0 auto;
  padding: 24px;
}

.profile-box {
  width: min(980px, 100%);
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  text-align: center;
}

/* 背景图 */
.profile-hero {
  position: relative;
  height: 380px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.profile-hero-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-hero-placeholder {
  width: 100%;
  height: 100%;
  background:
    radial-gradient(circle at 12% 20%, rgba(255, 236, 170, 0.8), transparent 45%),
    radial-gradient(circle at 82% 78%, rgba(147, 223, 255, 0.78), transparent 42%),
    linear-gradient(135deg, #fff4cc 0%, #ffe7a1 35%, #f5f7ff 100%);
}

.profile-hero-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.35) 0%, rgba(0, 0, 0, 0.05) 55%, rgba(0, 0, 0, 0) 100%);
}

/* 头像 */
.profile-avatar-wrap {
  margin: 0 auto 0.9rem;
  width: 112px;
  height: 112px;
  border-radius: 50%;
  border: 4px solid rgba(255, 255, 255, 0.95);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff4c8;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
}

.profile-avatar-wrap--floating {
  margin-top: -82px;
  position: relative;
  z-index: 2;
}

.profile-avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-fallback {
  font-size: 1.9rem;
  font-weight: 800;
  color: #1f1f1f;
}

/* 用户名 */
.profile-name {
  margin: 0;
  font-size: clamp(1.4rem, 2.5vw, 1.9rem);
  color: #111;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

/* 关注按钮 */
.follow-btn {
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: #ff6b35;
  color: #fff;
  border: none;
}

.follow-btn:hover {
  background: #e55a2b;
}

.follow-btn.following {
  background: #fff;
  color: #666;
  border: 1px solid #ddd;
}

.follow-btn.following:hover {
  border-color: #e53935;
  color: #e53935;
}

/* 私信按钮 */
.profile-message-btn {
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: #1a73e8;
  color: #fff;
  border: none;
}

.profile-message-btn:hover {
  background: #1557b0;
}

/* 主要信息区域 */
.profile-main-info {
  padding: 0 1.1rem 1.45rem;
}

/* 个性签名框 */
.profile-signature-box {
  margin: 0.9rem auto 0;
  width: min(820px, 100%);
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 12px;
  background: #fffdf3;
  padding: 0.72rem 0.9rem;
  text-align: left;
}

.profile-signature-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-signature-label {
  margin: 0;
  color: rgba(0, 0, 0, 0.56);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.profile-signature {
  margin: 0.36rem 0 0;
  max-width: 100%;
  color: #424242;
  font-size: 0.98rem;
  line-height: 1.6;
}

.profile-signature-empty {
  margin: 0.36rem 0 0;
  max-width: 100%;
  color: #999;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 12px;
}

.set-signature-btn {
  padding: 4px 12px;
  background: none;
  border: 1px solid #ff6b35;
  color: #ff6b35;
  border-radius: 12px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
}

.set-signature-btn:hover {
  background: #ff6b35;
  color: #fff;
}

/* 统计信息 */
.profile-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-top: 20px;
  padding: 20px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 1.3rem;
  font-weight: 700;
  color: #1f1f1f;
}

.stat-label {
  font-size: 0.85rem;
  color: #999;
  margin-top: 4px;
}

/* 文章列表 */
.profile-posts-section {
  margin-top: 24px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 1.2rem;
  color: #333;
  font-weight: 600;
}

.loading-posts,
.empty-posts {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s;
}

.post-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.post-main {
  flex: 1;
  min-width: 0;
}

.post-title {
  margin: 0 0 8px;
  font-size: 1rem;
  color: #1f1f1f;
  line-height: 1.4;
}

.post-summary {
  margin: 0 0 8px;
  font-size: 0.85rem;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-date {
  font-size: 0.75rem;
  color: #999;
}

.post-cover {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

/* 响应式 */
@media (max-width: 600px) {
  .profile-hero {
    height: 200px;
  }
  
  .profile-avatar-wrap {
    width: 80px;
    height: 80px;
  }
  
  .profile-avatar-wrap--floating {
    margin-top: -50px;
  }
  
  .profile-stats {
    gap: 24px;
  }
}
</style>
