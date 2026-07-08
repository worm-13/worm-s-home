<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { getPostDetail, deletePost as deletePostApi } from '../api/post';
import type { PostDetail } from '../api/post';
import { likePost, unlikePost, favoritePost, unfavoritePost } from '../api/interaction';
import { follow, unfollow, getFollowStatus } from '../api/follow';
import { getCommentsByPostId, createComment } from '../api/comment';
import type { CommentVO } from '../api/comment';
import { useAuth } from '../composables/useAuth';
import { resolveAssetUrl } from '../composables/useAssetUrl';

const props = defineProps<{ visible: boolean; articleId: number | null }>();
const emit = defineEmits<{ close: []; deleted: [] }>();

const router = useRouter();
const { user } = useAuth();

// 文章详情状态
const isLoadingArticleDetail = ref(false);
const articleDetail = ref<PostDetail | null>(null);
const articleError = ref('');
const articleDetailFollowStatus = ref(false);

// 点赞收藏状态
const postLikes = ref<Map<number, boolean>>(new Map());
const postFavorites = ref<Map<number, boolean>>(new Map());

// 删除状态
const isDeletingPost = ref(false);

// 评论相关
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

// 解析内容块
const articleDetailBlocks = computed(() => parseContentBlocks(articleDetail.value?.content || ''));

// watch articleId 变化触发加载
watch(() => props.articleId, async (newId) => {
  if (newId !== null && props.visible) {
    await loadArticleDetail(newId);
  }
});

watch(() => props.visible, async (visible) => {
  if (visible && props.articleId !== null) {
    await loadArticleDetail(props.articleId);
  } else if (!visible) {
    cleanup();
  }
});

// 监听 sentinel 元素引用变化
watch(commentSentinelRef, (el) => {
  if (el && commentObserver) {
    commentObserver.observe(el);
  }
});

onUnmounted(() => {
  cleanup();
});

function cleanup() {
  articleDetail.value = null;
  articleError.value = '';
  commentObserver?.disconnect();
  commentObserver = null;
}

function formatDateKey(value?: string) {
  if (!value) return '未知日期';
  const date = new Date(value.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return value.slice(0, 10);
  const y = date.getFullYear();
  const m = `${date.getMonth() + 1}`.padStart(2, '0');
  const d = `${date.getDate()}`.padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function formatDateTime(value?: string) {
  if (!value) return '';
  const date = new Date(value.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return value;
  return `${formatDateKey(value)} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`;
}

function parseContentBlocks(content: string) {
  const lines = content.split(/\r?\n/);
  const blocks: Array<{ type: 'text' | 'image'; value: string }> = [];
  const imageUrlPattern = /^https?:\/\/\S+\.(png|jpg|jpeg|gif|webp)(\?.*)?$/i;
  const markdownImagePattern = /^!\[[^\]]*\]\((https?:\/\/[^)]+)\)$/i;

  let textBuffer: string[] = [];
  const flushText = () => {
    const merged = textBuffer.join('\n').trim();
    if (merged) {
      blocks.push({ type: 'text', value: merged });
    }
    textBuffer = [];
  };

  for (const line of lines) {
    const trimmed = line.trim();
    const markdownMatch = trimmed.match(markdownImagePattern);
    if (markdownMatch?.[1]) {
      flushText();
      blocks.push({ type: 'image', value: markdownMatch[1] });
      continue;
    }

    if (imageUrlPattern.test(trimmed)) {
      flushText();
      blocks.push({ type: 'image', value: trimmed });
      continue;
    }

    textBuffer.push(line);
  }

  flushText();
  return blocks;
}

// 加载文章详情
async function loadArticleDetail(id: number) {
  isLoadingArticleDetail.value = true;
  articleError.value = '';
  articleDetailFollowStatus.value = false;
  try {
    const res = await getPostDetail(id);
    if (res.data.code === 0) {
      const detail = res.data.data;
      if (detail) {
        articleDetail.value = {
          ...detail,
          likesCount: detail.likesCount ?? 0,
          favoritesCount: detail.favoritesCount ?? 0
        };
        postLikes.value.set(id, !!articleDetail.value.isLiked);
        postFavorites.value.set(id, !!articleDetail.value.isFavorited);

        // 加载关注状态（仅登录用户且非本人文章）
        if (user.value && articleDetail.value.userId !== user.value.id) {
          try {
            const followRes = await getFollowStatus(articleDetail.value.userId);
            if (followRes.data.code === 0) {
              articleDetailFollowStatus.value = followRes.data.data?.isFollowing || false;
            }
          } catch {
            // 静默失败
          }
        }
      }
      await loadComments(id);
      setupCommentObserver();
      return;
    }
    articleError.value = res.data.message || '加载文章详情失败。';
  } catch (error) {
    articleError.value = error instanceof Error ? error.message : '加载文章详情失败。';
  } finally {
    isLoadingArticleDetail.value = false;
  }
}

function closeArticleDetail() {
  emit('close');
}

// 评论相关
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
  if (!commentHasMore.value || isLoadingMoreComments.value || commentsLoading.value || !articleDetail.value) return;
  void loadComments(articleDetail.value.id, commentPage.value + 1, true);
}

function setupCommentObserver() {
  commentObserver?.disconnect();
  commentObserver = new IntersectionObserver((entries) => {
    if (entries[0]?.isIntersecting) {
      void loadMoreComments();
    }
  }, { rootMargin: '100px' });
}

async function submitComment() {
  if (!articleDetail.value) return;

  const content = commentInput.value.trim();
  if (!content) {
    commentError.value = '请输入评论内容';
    return;
  }

  isSubmittingComment.value = true;
  commentError.value = '';

  try {
    const res = await createComment(articleDetail.value.id, content);
    if (res.data.code === 0) {
      commentInput.value = '';
      await loadComments(articleDetail.value.id);
    } else {
      commentError.value = res.data.message || '评论失败';
    }
  } catch {
    commentError.value = '评论失败，请重试';
  } finally {
    isSubmittingComment.value = false;
  }
}

// 点赞
async function toggleLike(postId: number) {
  if (!user.value) return;

  const isLiked = postLikes.value.get(postId) || false;

  try {
    const res = isLiked ? await unlikePost(postId) : await likePost(postId);
    if (res.data.code === 0) {
      postLikes.value.set(postId, !isLiked);
      if (articleDetail.value && articleDetail.value.id === postId) {
        articleDetail.value.likesCount = (articleDetail.value.likesCount || 0) + (!isLiked ? 1 : -1);
      }
    }
  } catch {
    // 静默失败
  }
}

// 收藏
async function toggleFavorite(postId: number) {
  if (!user.value) return;

  const isFavorited = postFavorites.value.get(postId) || false;

  try {
    const res = isFavorited ? await unfavoritePost(postId) : await favoritePost(postId);
    if (res.data.code === 0) {
      postFavorites.value.set(postId, !isFavorited);
      if (articleDetail.value && articleDetail.value.id === postId) {
        articleDetail.value.favoritesCount = (articleDetail.value.favoritesCount || 0) + (!isFavorited ? 1 : -1);
      }
    }
  } catch {
    // 静默失败
  }
}

// 删除文章
async function deletePost(postId: number) {
  if (!user.value) return;

  if (!confirm('确定要删除这篇文章吗？')) return;

  isDeletingPost.value = true;

  try {
    const res = await deletePostApi(postId);
    if (res.data.code === 0) {
      emit('deleted');
      emit('close');
    } else {
      articleError.value = res.data.message || '删除失败';
    }
  } catch {
    articleError.value = '删除失败，请稍后重试';
  } finally {
    isDeletingPost.value = false;
  }
}

// 跳转到用户主页
function goToUserProfile(userId: number) {
  if (userId) {
    router.push(`/user/${userId}`);
  }
}

// 文章详情页关注/取消关注
async function toggleArticleDetailFollow(userId: number) {
  if (!user.value) return;

  try {
    if (articleDetailFollowStatus.value) {
      const res = await unfollow(userId);
      if (res.data.code === 0) {
        articleDetailFollowStatus.value = false;
      }
    } else {
      const res = await follow(userId);
      if (res.data.code === 0) {
        articleDetailFollowStatus.value = true;
      }
    }
  } catch {
    // 静默失败
  }
}
</script>

<template>
  <section v-if="visible" class="article-detail-backdrop" @click.self="closeArticleDetail">
    <div class="article-detail-modal" @click.stop>
      <button class="article-detail-close" type="button" @click="closeArticleDetail">×</button>

      <p v-if="isLoadingArticleDetail" class="article-feed-tip">详情加载中...</p>
      <p v-else-if="articleError" class="article-feed-error">{{ articleError }}</p>
      <template v-else-if="articleDetail">
        <div class="article-detail-meta">
          <img
            v-if="articleDetail.authorAvatar"
            class="article-author-avatar clickable-avatar"
            :src="resolveAssetUrl(articleDetail.authorAvatar)"
            loading="lazy"
            alt="作者头像"
            @click="goToUserProfile(articleDetail.userId)"
          />
          <span class="clickable-name" @click="goToUserProfile(articleDetail.userId)">{{ articleDetail.authorName || '匿名用户' }}</span>
          <span> · {{ formatDateTime(articleDetail.createdAt) }}</span>
          <button
            v-if="user && articleDetail.userId !== user.id"
            class="follow-author-btn"
            :class="{ 'following': articleDetailFollowStatus }"
            @click="toggleArticleDetailFollow(articleDetail.userId)"
          >
            {{ articleDetailFollowStatus ? '已关注' : '关注' }}
          </button>
        </div>
        <h2>{{ articleDetail.title }}</h2>
        <img
          v-if="articleDetail.coverImage"
          class="article-detail-cover"
          :src="resolveAssetUrl(articleDetail.coverImage)"
          loading="lazy"
          alt="文章封面"
        />
        <section class="article-detail-content">
          <template v-for="(block, index) in articleDetailBlocks" :key="`${block.type}-${index}`">
            <p v-if="block.type === 'text'" class="article-detail-paragraph">{{ block.value }}</p>
            <img v-else class="article-detail-inline-image" :src="block.value" loading="lazy" alt="正文图片" />
          </template>
        </section>

        <!-- 点赞收藏栏 -->
        <div class="article-detail-actions">
          <button class="action-btn" type="button" @click="toggleLike(articleDetail.id)">
            <span class="like-icon" :class="{ 'liked': postLikes.get(articleDetail.id) }">
              <svg v-if="!postLikes.get(articleDetail.id)" viewBox="0 0 24 24" width="20" height="20" stroke="#999" stroke-width="2" fill="none">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="#e53935">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
            </span>
            <span class="action-count">{{ articleDetail.likesCount }}</span>
          </button>
          <button class="action-btn" type="button" @click="toggleFavorite(articleDetail.id)">
            <span class="favorite-icon" :class="{ 'favorited': postFavorites.get(articleDetail.id) }">
              <svg v-if="!postFavorites.get(articleDetail.id)" viewBox="0 0 24 24" width="20" height="20" stroke="#999" stroke-width="2" fill="none">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="#ffc107">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
            </span>
            <span class="action-count">{{ articleDetail.favoritesCount }}</span>
          </button>
          <!-- 仅文章作者显示删除按钮 -->
          <button v-if="user && articleDetail.userId === user.id" class="delete-post-btn" type="button" :disabled="isDeletingPost" @click="deletePost(articleDetail.id)">
            {{ isDeletingPost ? '删除中...' : '删除文章' }}
          </button>
        </div>

        <!-- 评论区 -->
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
                class="comment-avatar clickable-avatar"
                :src="resolveAssetUrl(comment.avatar)"
                loading="lazy"
                alt="头像"
                @click="goToUserProfile(comment.userId)"
              />
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-author clickable-name" @click="goToUserProfile(comment.userId)">{{ comment.nickname || comment.username }}</span>
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
      <p v-else class="article-feed-tip">暂无文章详情。</p>
    </div>
  </section>
</template>

<style scoped>
.article-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 36;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
}

.article-detail-modal {
  width: min(900px, 100%);
  max-height: 92vh;
  overflow: auto;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.14);
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.22);
  padding: 16px;
  position: relative;
}

.article-detail-close {
  position: sticky;
  top: 0;
  margin-left: auto;
  display: block;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: #fff;
  cursor: pointer;
}

.article-detail-meta {
  margin: 6px 0;
  color: #666;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 8px;
}

.follow-author-btn {
  margin-left: auto;
  padding: 5px 16px;
  border-radius: 16px;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: #ff6b35;
  color: #fff;
  border: none;
}

.follow-author-btn:hover {
  background: #e55a2b;
}

.follow-author-btn.following {
  background: #fff;
  color: #666;
  border: 1px solid #ddd;
}

.follow-author-btn.following:hover {
  border-color: #e53935;
  color: #e53935;
}

.article-detail-modal h2 {
  margin: 0 0 10px;
  color: #1b1b1b;
}

.article-detail-cover {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  background: #f5f5f5;
  border-radius: 12px;
}

.article-detail-content {
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.article-detail-paragraph {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.75;
  color: #2a2a2a;
  font-size: 0.95rem;
}

.article-detail-inline-image {
  width: 100%;
  max-height: none;
  object-fit: contain;
  border-radius: 10px;
  background: #f5f5f5;
}

/* 点赞收藏栏 */
.article-detail-actions {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: none;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  transition: background-color 0.2s ease;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.05);
}

.action-btn:hover .like-icon svg {
  stroke: #e53935;
}

.action-btn:hover .favorite-icon svg {
  stroke: #ffc107;
}

.like-icon,
.favorite-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s ease;
}

.like-icon.liked,
.favorite-icon.favorited {
  transform: scale(1.1);
}

.action-count {
  font-size: 0.9rem;
  color: #666;
  font-weight: 500;
  transition: color 0.2s ease;
}

.like-icon.liked + .action-count {
  color: #e53935;
}

.favorite-icon.favorited + .action-count {
  color: #ffc107;
}

/* 删除文章按钮 */
.delete-post-btn {
  margin-left: auto;
  padding: 6px 14px;
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  background: #fff;
  color: #666;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.delete-post-btn:hover:not(:disabled) {
  background: #e53935;
  border-color: #e53935;
  color: #fff;
}

.delete-post-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 评论区 */
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

.clickable-avatar {
  cursor: pointer;
  transition: opacity 0.2s;
}

.clickable-avatar:hover {
  opacity: 0.8;
}

.clickable-name {
  cursor: pointer;
  transition: color 0.2s;
}

.clickable-name:hover {
  color: #ff6b35;
  text-decoration: underline;
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
  text-align: center;
  color: #999;
  font-size: 0.85rem;
  padding: 20px 0;
}

.article-feed-error {
  text-align: center;
  color: #e53935;
  font-size: 0.85rem;
  padding: 20px 0;
}

.article-author-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
</style>
