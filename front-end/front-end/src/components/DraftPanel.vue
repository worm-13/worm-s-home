<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getMyDrafts, publishPost, deletePost, getMyPostDetail } from '../api/post';
import type { PostListItem, PostDetail } from '../api/post';

const emit = defineEmits<{
  'edit-draft': [draft: PostDetail];
  published: [];
  deleted: [];
}>();

const drafts = ref<PostListItem[]>([]);
const isLoadingDrafts = ref(false);
const draftError = ref('');
const draftPage = ref(1);
const draftHasMore = ref(true);
const isLoadingMoreDrafts = ref(false);
const DRAFT_PAGE_SIZE = 10;

onMounted(() => {
  loadDrafts();
});

async function loadDrafts(page = 1, append = false) {
  if (page === 1) {
    isLoadingDrafts.value = true;
  } else {
    isLoadingMoreDrafts.value = true;
  }
  draftError.value = '';
  try {
    const res = await getMyDrafts(page, DRAFT_PAGE_SIZE);
    if (res.data.code === 0) {
      const pageData = res.data.data;
      const items = pageData?.items ?? [];
      if (append) {
        drafts.value.push(...items);
      } else {
        drafts.value = items;
      }
      draftPage.value = page;
      draftHasMore.value = pageData?.hasMore ?? false;
    } else {
      draftError.value = res.data.message || '加载草稿列表失败。';
    }
  } catch (error) {
    draftError.value = error instanceof Error ? error.message : '加载草稿列表失败。';
  } finally {
    isLoadingDrafts.value = false;
    isLoadingMoreDrafts.value = false;
  }
}

function loadMoreDrafts() {
  if (!draftHasMore.value || isLoadingMoreDrafts.value || isLoadingDrafts.value) return;
  void loadDrafts(draftPage.value + 1, true);
}

async function openDraftForEdit(draftId: number) {
  try {
    const res = await getMyPostDetail(draftId);
    if (res.data.code === 0 && res.data.data) {
      emit('edit-draft', res.data.data);
    } else {
      draftError.value = '加载草稿详情失败。';
    }
  } catch (error) {
    draftError.value = error instanceof Error ? error.message : '加载草稿详情失败。';
  }
}

async function handlePublishDraft(draftId: number) {
  draftError.value = '';
  try {
    const res = await publishPost(draftId);
    if (res.data.code === 0) {
      await loadDrafts();
      emit('published');
    } else {
      draftError.value = res.data.message || '发布失败';
    }
  } catch (error) {
    draftError.value = '发布失败，请稍后重试';
  }
}

async function handleDeleteDraft(draftId: number) {
  if (!confirm('确定要删除这篇草稿吗？')) return;
  draftError.value = '';
  try {
    const res = await deletePost(draftId);
    if (res.data.code === 0) {
      await loadDrafts();
      emit('deleted');
    } else {
      draftError.value = res.data.message || '删除失败';
    }
  } catch (error) {
    draftError.value = '删除失败，请稍后重试';
  }
}

function getStatusLabel(status?: number) {
  if (status === 2) return '草稿';
  if (status === 3) return '定时发布';
  if (status === 1) return '已发布';
  return '';
}

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
</script>

<template>
  <div class="main-panel">
    <section class="article-feed">
      <header class="article-feed-header">
        <h1>草稿箱</h1>
        <p>管理你的草稿和定时发布文章</p>
      </header>

      <p v-if="isLoadingDrafts" class="article-feed-tip">草稿加载中...</p>
      <p v-else-if="draftError" class="article-feed-error">{{ draftError }}</p>
      <p v-else-if="drafts.length === 0" class="article-feed-tip">暂无草稿，点击右上角开始编辑文章并保存为草稿。</p>

      <section v-else class="article-group-list">
        <article v-for="draft in drafts" :key="draft.id" class="article-card draft-card">
          <div class="article-card-main">
            <div class="article-card-meta">
              <span class="draft-status-tag" :class="{ 'scheduled': draft.status === 3 }">{{ getStatusLabel(draft.status) }}</span>
              <span v-if="draft.status === 3 && draft.scheduledAt" class="draft-schedule-time">定时: {{ formatDateTime(draft.scheduledAt) }}</span>
              <span> · 更新于 {{ formatDateTime(draft.updatedAt || draft.createdAt) }}</span>
            </div>
            <h3>{{ draft.title }}</h3>
            <p>{{ buildPlainTextPreview(draft.summary) || '暂无摘要' }}</p>
          </div>
          <div class="draft-actions">
            <button class="draft-action-btn edit" type="button" @click.stop="openDraftForEdit(draft.id)">编辑</button>
            <button class="draft-action-btn publish" type="button" @click.stop="handlePublishDraft(draft.id)">发布</button>
            <button class="draft-action-btn delete" type="button" @click.stop="handleDeleteDraft(draft.id)">删除</button>
          </div>
        </article>
        <div v-if="isLoadingMoreDrafts" class="article-feed-tip">加载更多草稿...</div>
        <div v-else-if="!draftHasMore && drafts.length > 0" class="article-feed-tip">已加载全部草稿</div>
        <button v-else-if="draftHasMore" class="load-more-btn" type="button" @click="loadMoreDrafts">加载更多</button>
      </section>
    </section>
  </div>
</template>

<style scoped>
.draft-card {
  flex-direction: column;
  align-items: stretch;
}

.draft-status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #e8f0fe;
  color: #1a73e8;
}

.draft-status-tag.scheduled {
  background: #fff3e0;
  color: #e65100;
}

.draft-schedule-time {
  font-size: 0.78rem;
  color: #999;
}

.draft-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.draft-action-btn {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(0, 0, 0, 0.16);
  background: #fff;
  color: #333;
}

.draft-action-btn.edit:hover {
  background: #e8f0fe;
  border-color: #1a73e8;
  color: #1a73e8;
}

.draft-action-btn.publish {
  background: #1a73e8;
  border-color: #1a73e8;
  color: #fff;
}

.draft-action-btn.publish:hover {
  background: #1557b0;
}

.draft-action-btn.delete {
  border-color: rgba(229, 57, 53, 0.3);
  color: #e53935;
}

.draft-action-btn.delete:hover {
  background: #ffeaea;
  border-color: #e53935;
}

.load-more-btn {
  display: block;
  margin: 16px auto 0;
  padding: 8px 24px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #595959;
  font-size: 0.9rem;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}

.load-more-btn:hover {
  border-color: #1a1a1a;
  color: #1a1a1a;
}
</style>
