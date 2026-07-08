<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faHouse, faUser, faBookmark, faGear, faCircleQuestion, faBell, faShieldHalved } from '@fortawesome/free-solid-svg-icons';
import { faPenToSquare } from '@fortawesome/free-regular-svg-icons';
import { getFollowing } from '../api/follow';
import type { UserSimple } from '../api/follow';
import { useAuth } from '../composables/useAuth';
import { resolveAssetUrl } from '../composables/useAssetUrl';

const props = defineProps<{ activeSection: string }>();
const emit = defineEmits<{
  navigate: [section: string];
  'go-profile': [userId: number];
  'go-drafts': [];
  'go-admin': [];
}>();

const { user, initFromStorage } = useAuth();
const isAdmin = computed(() => user.value?.role === 'ADMIN');

const followingList = ref<UserSimple[]>([]);
const isLoadingFollowing = ref(false);

const byPrefixAndName = {
  far: {
    'pen-to-square': faPenToSquare,
  },
  fas: {
    house: faHouse,
    user: faUser,
    bookmark: faBookmark,
    gear: faGear,
    'circle-question': faCircleQuestion,
    bell: faBell,
    'shield-halved': faShieldHalved,
  },
};

function selectSection(section: string) {
  emit('navigate', section);
}

function goHome() {
  selectSection('home');
}

function goProfile() {
  selectSection('profile');
}

function goToUserProfile(userId: number) {
  if (userId) {
    emit('go-profile', userId);
  }
}

async function loadFollowingList() {
  initFromStorage();
  if (!user.value) {
    followingList.value = [];
    return;
  }

  isLoadingFollowing.value = true;
  try {
    const res = await getFollowing(user.value.id);
    if (res.data.code === 0) {
      followingList.value = res.data.data?.items || [];
    }
  } catch {
    // 静默失败
  } finally {
    isLoadingFollowing.value = false;
  }
}

onMounted(() => {
  loadFollowingList();
});

defineExpose({ loadFollowingList });
</script>

<template>
  <aside class="left-panel" @click.stop>
    <button
      :class="['left-nav-btn', activeSection === 'home' ? 'active' : '']"
      type="button"
      @click="goHome"
    >
      <FontAwesomeIcon :icon="byPrefixAndName.fas.house" />
      <span>首页</span>
    </button>
    <button
      :class="['left-nav-btn', activeSection === 'profile' ? 'active' : '']"
      type="button"
      @click="goProfile"
    >
      <FontAwesomeIcon :icon="byPrefixAndName.fas.user" />
      <span>个人主页</span>
    </button>
    <button
      :class="['left-nav-btn', activeSection === 'favorite' ? 'active' : '']"
      type="button"
      @click="selectSection('favorite')"
    >
      <FontAwesomeIcon :icon="byPrefixAndName.fas.bookmark" />
      <span>收藏</span>
    </button>
    <button
      :class="['left-nav-btn', activeSection === 'drafts' ? 'active' : '']"
      type="button"
      @click="emit('go-drafts')"
    >
      <FontAwesomeIcon :icon="byPrefixAndName.far['pen-to-square']" />
      <span>草稿箱</span>
    </button>
    <button
      v-if="isAdmin"
      :class="['left-nav-btn', activeSection === 'admin' ? 'active' : '']"
      type="button"
      @click="emit('go-admin')"
    >
      <FontAwesomeIcon :icon="byPrefixAndName.fas['shield-halved']" />
      <span>管理</span>
    </button>
    <div class="left-follow-divider"></div>
    <section class="follow-section" aria-label="Following list">
      <p class="follow-title">Following</p>

      <div v-if="isLoadingFollowing" class="follow-loading">加载中...</div>

      <div v-else-if="followingList.length === 0" class="follow-empty">
        <p>暂无关注</p>
      </div>

      <template v-else>
        <button
          v-for="item in followingList"
          :key="item.id"
          class="follow-item"
          type="button"
          @click="goToUserProfile(item.id)"
        >
          <img v-if="resolveAssetUrl(item.avatar)" :src="resolveAssetUrl(item.avatar)" loading="lazy" class="follow-avatar" alt="头像" />
          <span v-else class="follow-dot">{{ (item.nickname || item.username || '?')[0] }}</span>
          <span>{{ item.nickname || item.username }}</span>
        </button>
      </template>
    </section>
  </aside>
</template>

<style scoped>
.left-panel {
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.11);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transform: translateX(0);
  transform-origin: left center;
  overflow: hidden;
  will-change: transform, opacity;
  transition: opacity 0.26s ease, transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.left-nav-btn {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.22);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: #1b1b1b;
  font-size: 0.95rem;
  font-weight: 600;
  padding: 11px 14px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.left-nav-btn:hover {
  transform: translateX(2px);
  background: #fff8de;
  box-shadow: 0 8px 14px rgba(0, 0, 0, 0.12);
}

.left-nav-btn.active {
  border-color: rgba(0, 0, 0, 0.45);
  background: #fff1bf;
}

.left-nav-btn :deep(svg) {
  font-size: 1rem;
}

.left-follow-divider {
  width: 100%;
  height: 1px;
  margin: 4px 0 2px;
  background: rgba(0, 0, 0, 0.16);
}

.follow-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.follow-title {
  margin: 0;
  font-size: 0.86rem;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.62);
}

.follow-item {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.88);
  color: #232323;
  font-size: 0.9rem;
  padding: 9px 10px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.follow-item:hover {
  background: #fff7dd;
  transform: translateX(2px);
}

.follow-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #2a2a2a;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
}

.follow-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.follow-loading,
.follow-empty {
  text-align: center;
  padding: 12px 8px;
  color: #999;
  font-size: 0.85rem;
}

.follow-empty p {
  margin: 0;
}
</style>
