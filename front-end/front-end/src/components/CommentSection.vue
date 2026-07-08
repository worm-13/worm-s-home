<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { getCommentsByPostId, createComment } from '../api/comment';
import type { CommentVO } from '../api/comment';
import { resolveAssetUrl } from '../composables/useAssetUrl';

const props = defineProps<{ postId: number }>();
const emit = defineEmits<{ 'comment-added': [] }>();

const comments = ref<CommentVO[]>([]);
const commentsLoading = ref(false);
const commentInput = ref('');
const isSubmittingComment = ref(false);
const commentError = ref('');
const commentPage = ref(1);
const commentHasMore = ref(true);
const isLoadingMoreComments = ref(false);
const COMMENT_PAGE_SIZE = 20;
const commentSentinelRef = ref<HTMLDivElement | null>(null);
let commentObserver: IntersectionObserver | null = null;

function formatDateTime(value?: string) {
  if (!value) {
    return '';
  }
  const date = new Date(value.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const y = date.getFullYear();
  const m = `${date.getMonth() + 1}`.padStart(2, '0');
  const d = `${date.getDate()}`.padStart(2, '0');
  return `${y}-${m}-${d} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`;
}

async function loadComments(postId: number, page = 1, append = false) {
  if (page === 1) {
    commentsLoading.value = true;
  } else {
    isLoadingMoreComments.value = true;
  }
  try {
    const res = await getCommentsByPostId(postId, page, COMMENT_PAGE_SIZE);
    if (res.data.code === 0) {
      const pageData = res.data.data;
      const items = pageData?.items ?? [];
      if (append) {
        comments.value.push(...items);
      } else {
        comments.value = items;
      }
      commentPage.value = page;
      commentHasMore.value = pageData?.hasMore ?? false;
    } else {
      if (!append) comments.value = [];
    }
  } catch {
    if (!append) comments.value = [];
  } finally {
    commentsLoading.value = false;
    isLoadingMoreComments.value = false;
  }
}

function loadMoreComments() {
  if (!commentHasMore.value || isLoadingMoreComments.value || commentsLoading.value) return;
  void loadComments(props.postId, commentPage.value + 1, true);
}

function setupCommentObserver() {
  commentObserver?.disconnect();
  commentObserver = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) {
      void loadMoreComments();
    }
  }, { rootMargin: '100px' });
}

watch(commentSentinelRef, (el) => {
  if (el && commentObserver) {
    commentObserver.observe(el);
  }
});

async function submitComment() {
  const content = commentInput.value.trim();
  if (!content) {
    commentError.value = '请输入评论内容';
    return;
  }

  isSubmittingComment.value = true;
  commentError.value = '';

  try {
    const res = await createComment(props.postId, content);
    if (res.data.code === 0) {
      commentInput.value = '';
      await loadComments(props.postId);
      emit('comment-added');
    } else {
      commentError.value = res.data.message || '评论失败';
    }
  } catch {
    commentError.value = '评论失败，请重试';
  } finally {
    isSubmittingComment.value = false;
  }
}

onMounted(() => {
  void loadComments(props.postId);
  setupCommentObserver();
});

onUnmounted(() => {
  commentObserver?.disconnect();
  commentObserver = null;
});
</script>

<template>
  <section class="article-comments">
    <h3>评论 ({{ comments.length }})</h3>

    <!-- 评论输入 -->
    <div class="comment-input-box">
      <textarea
        v-model="commentInput"
        class="comment-input"
        placeholder="写下你的评论..."
        rows="3"
        maxlength="500"
      ></textarea>
      <div class="comment-actions">
        <span class="comment-char-count">{{ commentInput.length }}/500</span>
        <button class="comment-submit-btn" type="button" :disabled="!commentInput.trim() || isSubmittingComment" @click="submitComment">
          {{ isSubmittingComment ? '提交中...' : '发表评论' }}
        </button>
      </div>
      <p v-if="commentError" class="comment-error">{{ commentError }}</p>
    </div>

    <!-- 评论列表 -->
    <p v-if="commentsLoading" class="article-feed-tip">评论加载中...</p>
    <div v-else-if="comments.length === 0" class="article-feed-tip">暂无评论，来发表第一条评论吧！</div>
    <div v-else class="comment-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <img
          v-if="comment.avatar"
          class="comment-avatar"
          :src="resolveAssetUrl(comment.avatar)"
          loading="lazy"
          alt="头像"
        />
        <div class="comment-content">
          <div class="comment-header">
            <span class="comment-author">{{ comment.nickname || comment.username }}</span>
            <span class="comment-time">{{ formatDateTime(comment.createdAt) }}</span>
          </div>
          <p class="comment-text">{{ comment.content }}</p>
        </div>
      </div>
      <div v-if="isLoadingMoreComments" class="article-feed-tip" style="padding: 8px 0;">加载更多评论...</div>
      <div v-else-if="!commentHasMore" class="article-feed-tip" style="padding: 8px 0;">已加载全部评论</div>
      <div ref="commentSentinelRef" class="article-scroll-sentinel"></div>
    </div>
  </section>
</template>

<style scoped>
.article-comments {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.article-comments h3 {
  margin: 0 0 12px;
  font-size: 1rem;
  color: #1a1a1a;
}

.comment-input-box {
  margin-bottom: 16px;
}

.comment-input {
  width: 100%;
  min-height: 80px;
  padding: 12px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 10px;
  font-size: 0.9rem;
  resize: vertical;
  box-sizing: border-box;
}

.comment-input:focus {
  outline: none;
  border-color: #1a73e8;
}

.comment-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.comment-char-count {
  font-size: 0.75rem;
  color: #999;
}

.comment-submit-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: #1a73e8;
  color: #fff;
  font-size: 0.85rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.comment-submit-btn:hover:not(:disabled) {
  background: #1557b0;
}

.comment-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.comment-error {
  margin: 8px 0 0;
  color: #e53935;
  font-size: 0.8rem;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.comment-author {
  font-size: 0.85rem;
  font-weight: 500;
  color: #1a1a1a;
}

.comment-time {
  font-size: 0.75rem;
  color: #999;
}

.comment-text {
  margin: 0;
  font-size: 0.9rem;
  color: #333;
  line-height: 1.6;
}

.article-feed-tip {
  color: #595959;
  margin: 12px 0 0;
  font-size: 0.92rem;
}

.article-scroll-sentinel {
  height: 1px;
  width: 100%;
}
</style>
