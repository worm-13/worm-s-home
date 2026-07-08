<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { getPostList } from '../api/post';
import type { PostListItem } from '../api/post';
import { resolveAssetUrl } from '../composables/useAssetUrl';

const articles = ref<PostListItem[]>([]);
const isLoadingArticles = ref(false);
const articleError = ref('');
const articlePage = ref(1);
const articleHasMore = ref(true);
const isLoadingMoreArticles = ref(false);
const ARTICLE_PAGE_SIZE = 10;
const articleSentinelRef = ref<HTMLDivElement | null>(null);
let articleObserver: IntersectionObserver | null = null;

const emit = defineEmits<{
  'open-detail': [id: number];
}>();

const groupedArticles = computed(() => {
  const map = new Map<string, PostListItem[]>();
  for (const item of articles.value) {
    const key = formatDateKey(item.createdAt);
    const group = map.get(key) || [];
    group.push(item);
    map.set(key, group);
  }
  return Array.from(map.entries()).map(([date, items]) => ({ date, items }));
});

function formatDateKey(value?: string) {
  if (!value) {
    return '未知日期';
  }
  const date = new Date(value.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 10);
  }
  const y = date.getFullYear();
  const m = `${date.getMonth() + 1}`.padStart(2, '0');
  const d = `${date.getDate()}`.padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function formatDateTime(value?: string) {
  if (!value) {
    return '';
  }
  const date = new Date(value.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return `${formatDateKey(value)} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`;
}

function buildPlainTextPreview(raw?: string) {
  if (!raw) {
    return '';
  }

  const withoutMarkdownImages = raw.replace(/!\[[^\]]*\]\((https?:\/\/[^)]+)\)/gi, ' ');
  const withoutImageUrls = withoutMarkdownImages.replace(/https?:\/\/\S+\.(png|jpg|jpeg|gif|webp)(\?\S*)?/gi, ' ');
  const compact = withoutImageUrls.replace(/\s+/g, ' ').trim();
  if (!compact) {
    return '';
  }
  return compact.length > 120 ? `${compact.slice(0, 120)}...` : compact;
}

async function loadArticles(page = 1, append = false) {
  if (page === 1) {
    isLoadingArticles.value = true;
  } else {
    isLoadingMoreArticles.value = true;
  }
  articleError.value = '';
  try {
    const res = await getPostList(page, ARTICLE_PAGE_SIZE);
    if (res.data.code === 0) {
      const pageData = res.data.data;
      const items = pageData?.items ?? [];
      if (append) {
        articles.value.push(...items);
      } else {
        articles.value = items;
      }
      articlePage.value = page;
      articleHasMore.value = pageData?.hasMore ?? false;
      return;
    }
    articleError.value = res.data.message || '加载文章列表失败。';
  } catch (error) {
    articleError.value = error instanceof Error ? error.message : '加载文章列表失败。';
  } finally {
    isLoadingArticles.value = false;
    isLoadingMoreArticles.value = false;
  }
}

async function loadMoreArticles() {
  if (!articleHasMore.value || isLoadingMoreArticles.value || isLoadingArticles.value) return;
  await loadArticles(articlePage.value + 1, true);
}

function setupArticleObserver() {
  articleObserver = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) {
      void loadMoreArticles();
    }
  }, { rootMargin: '200px' });
}

watch(articleSentinelRef, (el) => {
  if (el && articleObserver) {
    articleObserver.observe(el);
  }
});

function openArticleDetail(id: number) {
  emit('open-detail', id);
}

onMounted(() => {
  void loadArticles();
  setupArticleObserver();
});

onUnmounted(() => {
  articleObserver?.disconnect();
  articleObserver = null;
});
</script>

<template>
  <div class="main-panel">
    <section class="article-feed">
      <header class="article-feed-header">
        <h1>首页文章</h1>
        <p>按发布时间倒序展示</p>
      </header>

      <p v-if="isLoadingArticles" class="article-feed-tip">文章加载中...</p>
      <p v-else-if="articleError" class="article-feed-error">{{ articleError }}</p>
      <p v-else-if="groupedArticles.length === 0" class="article-feed-tip">还没有文章，点击右上角开始发布。</p>

      <section v-else class="article-group-list">
        <article v-for="group in groupedArticles" :key="group.date" class="article-group">
          <h2>{{ group.date }}</h2>
          <button
            v-for="item in group.items"
            :key="item.id"
            class="article-card"
            type="button"
            @click="openArticleDetail(item.id)"
          >
            <div class="article-card-main">
              <div class="article-card-meta">
                <span>{{ item.authorName || '匿名用户' }} · {{ formatDateTime(item.createdAt) }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ buildPlainTextPreview(item.summary) || '暂无摘要' }}</p>
            </div>
            <img v-if="item.coverImage" :src="resolveAssetUrl(item.coverImage)" loading="lazy" alt="文章封面" />
          </button>
        </article>
        <div v-if="isLoadingMoreArticles" class="article-feed-tip">加载更多文章...</div>
        <div v-else-if="!articleHasMore && articles.length > 0" class="article-feed-tip">已加载全部文章</div>
        <div ref="articleSentinelRef" class="article-scroll-sentinel"></div>
      </section>
    </section>
  </div>
</template>

<style scoped>
.article-feed {
  width: min(980px, 100%);
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.1);
  padding: 18px;
  align-self: stretch;
  overflow: auto;
}

.article-feed-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.article-feed-header h1 {
  margin: 0;
  color: #161616;
  font-size: 1.2rem;
}

.article-feed-header p {
  margin: 0;
  color: #666;
  font-size: 0.86rem;
}

.article-feed-tip,
.article-feed-error {
  margin: 12px 0 0;
  font-size: 0.92rem;
}

.article-feed-tip {
  color: #595959;
}

.article-feed-error {
  color: #a32929;
}

.article-scroll-sentinel {
  height: 1px;
  width: 100%;
}

.article-group-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.article-group h2 {
  margin: 0 0 8px;
  font-size: 0.9rem;
  color: #5a5a5a;
  letter-spacing: 0.02em;
}

.article-card {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.13);
  border-radius: 12px;
  background: #fff;
  padding: 10px;
  margin-bottom: 8px;
  display: flex;
  align-items: stretch;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.article-card-main {
  flex: 1;
  min-width: 0;
}

.article-card-meta {
  margin: 0;
  color: #6f6f6f;
  font-size: 0.8rem;
  display: flex;
  align-items: center;
  gap: 6px;
}

.article-author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.article-card h3 {
  margin: 6px 0 4px;
  font-size: 1rem;
  color: #1f1f1f;
}

.article-card p {
  margin: 0;
  color: #4f4f4f;
  line-height: 1.5;
}

.article-card img {
  width: 160px;
  height: 96px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}
</style>
