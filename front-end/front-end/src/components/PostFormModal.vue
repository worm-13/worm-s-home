<script setup lang="ts">
import { ref, watch } from 'vue';
import { createPost, saveDraft, updatePost, uploadPostCover } from '../api/post';
import type { PostDetail } from '../api/post';
import { resolveAssetUrl } from '../composables/useAssetUrl';

const MAX_IMAGE_SIZE = 20 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp']);

const props = defineProps<{
  visible: boolean;
  draftToEdit?: PostDetail | null;
}>();

const emit = defineEmits<{
  close: [];
  published: [];
}>();

const coverImageInput = ref<HTMLInputElement | null>(null);
const postImagesInput = ref<HTMLInputElement | null>(null);
const isSubmittingPost = ref(false);
const isUploadingCoverImage = ref(false);
const postError = ref('');
const postSuccess = ref('');
const postImages = ref<Array<{ id: string; url: string }>>([]);
const editingDraftId = ref<number | null>(null);

const postForm = ref({
  title: '',
  content: '',
  summary: '',
  coverImage: '',
  publishMode: 'now' as 'now' | 'draft' | 'scheduled',
  scheduledAt: ''
});

watch(() => props.visible, (val) => {
  if (val) {
    postError.value = '';
    postSuccess.value = '';
    if (props.draftToEdit) {
      editingDraftId.value = props.draftToEdit.id;
      postForm.value = {
        title: props.draftToEdit.title || '',
        content: props.draftToEdit.content || '',
        summary: props.draftToEdit.summary || '',
        coverImage: props.draftToEdit.coverImage || '',
        publishMode: 'draft',
        scheduledAt: ''
      };
      postImages.value = [];
    } else {
      editingDraftId.value = null;
      postForm.value = {
        title: '',
        content: '',
        summary: '',
        coverImage: '',
        publishMode: 'now',
        scheduledAt: ''
      };
      postImages.value = [];
    }
  } else {
    postError.value = '';
    postSuccess.value = '';
    postImages.value = [];
    editingDraftId.value = null;
  }
});

function closePostModal() {
  if (isSubmittingPost.value || isUploadingCoverImage.value) {
    return;
  }
  emit('close');
}

function triggerPostImagesSelect() {
  if (isUploadingCoverImage.value) {
    return;
  }
  postImagesInput.value?.click();
}

function removePostImage(id: string) {
  postImages.value = postImages.value.filter((item) => item.id !== id);
}

function movePostImage(index: number, direction: -1 | 1) {
  const nextIndex = index + direction;
  if (nextIndex < 0 || nextIndex >= postImages.value.length) {
    return;
  }
  const cloned = [...postImages.value];
  const [target] = cloned.splice(index, 1);
  cloned.splice(nextIndex, 0, target);
  postImages.value = cloned;
}

function setImageAsCover(url: string) {
  postForm.value.coverImage = url;
}

async function handlePostImagesSelect(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);

  if (files.length === 0) {
    return;
  }

  postError.value = '';
  postSuccess.value = '';
  isUploadingCoverImage.value = true;

  try {
    for (const file of files) {
      if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
        postError.value = `文件 ${file.name} 格式不支持，仅支持 jpg、jpeg、png、gif 或 webp。`;
        continue;
      }

      if (file.size > MAX_IMAGE_SIZE) {
        postError.value = `文件 ${file.name} 超过 20MB 限制。`;
        continue;
      }

      const res = await uploadPostCover(file);
      if (res.data.code === 0 && res.data.data?.url) {
        postImages.value.push({
          id: `${Date.now()}_${Math.random().toString(36).slice(2)}`,
          url: res.data.data.url
        });
      } else {
        postError.value = res.data.message || `文件 ${file.name} 上传失败。`;
      }
    }

    if (!postForm.value.coverImage && postImages.value.length > 0) {
      postForm.value.coverImage = postImages.value[0].url;
    }
  } catch (error) {
    const err = error as { response?: { data?: { message?: string } }; message?: string };
    postError.value = err.response?.data?.message || err.message || '图片上传失败，请稍后重试。';
  } finally {
    isUploadingCoverImage.value = false;
    input.value = '';
  }
}

function triggerCoverImageSelect() {
  if (isUploadingCoverImage.value) {
    return;
  }
  coverImageInput.value?.click();
}

async function handleCoverImageSelect(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) {
    return;
  }

  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    postError.value = '封面仅支持 jpg、jpeg、png、gif 或 webp 格式。';
    input.value = '';
    return;
  }

  if (file.size > MAX_IMAGE_SIZE) {
    postError.value = '封面图片不能超过 20MB。';
    input.value = '';
    return;
  }

  postError.value = '';
  postSuccess.value = '';
  isUploadingCoverImage.value = true;

  try {
    const res = await uploadPostCover(file);
    if (res.data.code === 0 && res.data.data?.url) {
      postForm.value.coverImage = res.data.data.url;
      return;
    }
    postError.value = res.data.message || '封面上传失败，请稍后重试。';
  } catch (error) {
    const err = error as { response?: { data?: { message?: string } }; message?: string };
    postError.value = err.response?.data?.message || err.message || '封面上传失败，请稍后重试。';
  } finally {
    isUploadingCoverImage.value = false;
    input.value = '';
  }
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

async function submitPost() {
  postError.value = '';
  postSuccess.value = '';
  isSubmittingPost.value = true;

  try {
    let finalContent = postForm.value.content;
    if (postImages.value.length > 0) {
      const imageLines = postImages.value.map((item) => resolveAssetUrl(item.url));
      const merged = `${finalContent.trim()}\n\n${imageLines.join('\n')}`.trim();
      finalContent = merged;
    }

    const mode = postForm.value.publishMode;

    // 定时发布校验
    if (mode === 'scheduled' && !postForm.value.scheduledAt) {
      postError.value = '请选择定时发布时间。';
      isSubmittingPost.value = false;
      return;
    }

    // 如果是编辑草稿且保持草稿状态，使用 updatePost
    if (editingDraftId.value && mode === 'draft') {
      const res = await updatePost(editingDraftId.value, {
        title: postForm.value.title,
        content: finalContent,
        summary: buildPlainTextPreview(postForm.value.summary || postForm.value.content),
        coverImage: postForm.value.coverImage
      });
      if (res.data.code === 0) {
        postSuccess.value = '草稿已更新。';
        emit('published');
        window.setTimeout(() => {
          emit('close');
          postSuccess.value = '';
        }, 900);
      } else {
        postError.value = res.data.message || '更新失败。';
      }
      return;
    }

    let res;
    if (mode === 'draft') {
      res = await saveDraft({
        title: postForm.value.title,
        content: finalContent,
        summary: buildPlainTextPreview(postForm.value.summary || postForm.value.content),
        coverImage: postForm.value.coverImage
      });
    } else if (mode === 'scheduled') {
      res = await createPost({
        title: postForm.value.title,
        content: finalContent,
        summary: buildPlainTextPreview(postForm.value.summary || postForm.value.content),
        coverImage: postForm.value.coverImage,
        status: 3,
        scheduledAt: postForm.value.scheduledAt
      });
    } else {
      res = await createPost({
        title: postForm.value.title,
        content: finalContent,
        summary: buildPlainTextPreview(postForm.value.summary || postForm.value.content),
        coverImage: postForm.value.coverImage,
        status: 1
      });
    }

    if (res.data.code === 0) {
      const postId = res.data.data?.postId;
      if (mode === 'draft') {
        postSuccess.value = postId ? `草稿已保存，ID：${postId}` : '草稿已保存。';
      } else if (mode === 'scheduled') {
        postSuccess.value = postId ? `定时发布已设置，ID：${postId}` : '定时发布已设置。';
      } else {
        postSuccess.value = postId ? `发布成功，文章 ID：${postId}` : '发布成功。';
      }
      emit('published');
      postForm.value = {
        title: '',
        content: '',
        summary: '',
        coverImage: '',
        publishMode: 'now',
        scheduledAt: ''
      };
      postImages.value = [];
      editingDraftId.value = null;
      window.setTimeout(() => {
        emit('close');
        postSuccess.value = '';
      }, 900);
      return;
    }

    postError.value = res.data.message || '操作失败，请稍后重试。';
  } catch (error) {
    if (error instanceof Error) {
      postError.value = error.message;
    } else {
      postError.value = '请求异常，请稍后重试。';
    }
  } finally {
    isSubmittingPost.value = false;
  }
}
</script>

<template>
  <section v-if="visible" class="post-modal-backdrop" @click.self="closePostModal">
    <div class="post-modal" @click.stop>
      <input ref="coverImageInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" hidden @change="handleCoverImageSelect" />
      <input ref="postImagesInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple hidden @change="handlePostImagesSelect" />
      <div class="post-modal-header">
        <div>
          <p class="post-modal-title">发布文章</p>
          <p class="post-modal-subtitle">直接输入正文内容即可，提交后由后端创建文章。</p>
        </div>
        <button class="post-close-btn" type="button" aria-label="关闭发布文章弹窗" @click="closePostModal">×</button>
      </div>

      <form class="post-form" @submit.prevent="submitPost">
        <label class="post-field">
          <span>标题 <em>*</em></span>
          <input v-model="postForm.title" type="text" maxlength="120" placeholder="请输入文章标题" />
        </label>

        <label class="post-field">
          <span>正文内容 <em>*</em></span>
          <textarea v-model="postForm.content" rows="10" placeholder="请输入正文内容"></textarea>
        </label>

        <label class="post-field">
          <span>摘要（可选）</span>
          <input v-model="postForm.summary" type="text" maxlength="200" placeholder="不填将由后端自动生成" />
        </label>

        <div class="post-field">
          <span>发布方式</span>
          <div class="publish-mode-group">
            <label class="publish-mode-option">
              <input v-model="postForm.publishMode" type="radio" value="now" />
              <span>立即发布</span>
            </label>
            <label class="publish-mode-option">
              <input v-model="postForm.publishMode" type="radio" value="draft" />
              <span>保存草稿</span>
            </label>
            <label class="publish-mode-option">
              <input v-model="postForm.publishMode" type="radio" value="scheduled" />
              <span>定时发布</span>
            </label>
          </div>
        </div>

        <label v-if="postForm.publishMode === 'scheduled'" class="post-field">
          <span>定时发布时间 <em>*</em></span>
          <input v-model="postForm.scheduledAt" type="datetime-local" />
          <p class="post-upload-hint">选择文章自动发布的时间，到期后系统会自动发布。</p>
        </label>

        <label class="post-field">
          <span>封面图片（可选）</span>
          <div class="post-cover-actions">
            <button class="post-upload-btn" type="button" :disabled="isUploadingCoverImage" @click="triggerCoverImageSelect">
              {{ isUploadingCoverImage ? '上传中...' : '从文件夹选择封面' }}
            </button>
            <button
              v-if="postForm.coverImage"
              class="post-upload-btn post-upload-btn--ghost"
              type="button"
              :disabled="isUploadingCoverImage"
              @click="postForm.coverImage = ''"
            >
              清除封面
            </button>
          </div>
          <div v-if="postForm.coverImage" class="post-cover-preview">
            <img :src="resolveAssetUrl(postForm.coverImage)" alt="封面预览" />
          </div>
          <p class="post-upload-hint">无需输入 URL，直接选择图片上传即可（单张不超过 20MB）。</p>
        </label>

        <section class="post-images-panel">
          <div class="post-images-head">
            <span>正文图片（多张）</span>
            <div class="post-images-actions">
              <button class="post-upload-btn" type="button" :disabled="isUploadingCoverImage" @click="triggerPostImagesSelect">
                {{ isUploadingCoverImage ? '上传中...' : '批量上传图片' }}
              </button>
            </div>
          </div>

          <p class="post-upload-hint">可多选上传，每张不超过 20MB；可调整顺序并设为封面。</p>

          <div v-if="postImages.length > 0" class="post-images-preview">
            <article v-for="(item, index) in postImages" :key="item.id" class="post-image-card">
              <img :src="resolveAssetUrl(item.url)" alt="文章图片预览" />
              <div class="post-image-tools">
                <button type="button" @click="movePostImage(index, -1)">上移</button>
                <button type="button" @click="movePostImage(index, 1)">下移</button>
                <button type="button" @click="setImageAsCover(item.url)">设为封面</button>
                <button type="button" @click="removePostImage(item.id)">删除</button>
              </div>
            </article>
          </div>
        </section>

        <p v-if="postError" class="post-form-error">{{ postError }}</p>
        <p v-if="postSuccess" class="post-form-success">{{ postSuccess }}</p>

        <div class="post-modal-actions">
          <button class="post-secondary-btn" type="button" :disabled="isSubmittingPost || isUploadingCoverImage" @click="closePostModal">取消</button>
          <button class="post-primary-btn" type="submit" :disabled="isSubmittingPost || isUploadingCoverImage">
            {{ isSubmittingPost ? '提交中...' : (postForm.publishMode === 'draft' ? '保存草稿' : postForm.publishMode === 'scheduled' ? '设置定时发布' : '发布文章') }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.post-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(10, 10, 10, 0.48);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 24px 20px 42px;
  overflow-y: auto;
  z-index: 32;
}

.post-modal {
  width: min(980px, 100%);
  max-height: none;
  overflow: visible;
  border-radius: 20px;
  background: #fffef8;
  border: 1px solid rgba(0, 0, 0, 0.14);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.24);
  padding: 26px;
}

.post-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.post-modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  color: #161616;
}

.post-modal-subtitle {
  margin: 6px 0 0;
  color: #626262;
  font-size: 0.9rem;
}

.post-close-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.14);
  background: #fff;
  color: #1f1f1f;
  font-size: 1.35rem;
  line-height: 1;
  cursor: pointer;
}

.post-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.post-field span {
  color: #2c2c2c;
  font-size: 0.92rem;
  font-weight: 700;
}

.post-field em {
  font-style: normal;
  color: #b42318;
}

.post-field input,
.post-field textarea {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  background: #fff;
  padding: 10px 12px;
  font-size: 0.95rem;
  color: #181818;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.post-field textarea {
  resize: vertical;
  min-height: 220px;
}

.post-upload-btn {
  width: fit-content;
  border: 1px solid rgba(0, 0, 0, 0.18);
  border-radius: 999px;
  background: #fff;
  color: #1f1f1f;
  font-size: 0.86rem;
  font-weight: 700;
  padding: 6px 12px;
  cursor: pointer;
}

.post-cover-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.post-upload-btn--ghost {
  background: #f7f7f7;
}

.post-cover-preview {
  width: min(360px, 100%);
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5f5;
}

.post-cover-preview img {
  width: 100%;
  max-height: 220px;
  object-fit: contain;
  display: block;
}

.post-upload-btn:disabled {
  opacity: 0.66;
  cursor: not-allowed;
}

.post-upload-hint {
  margin: 0;
  color: #6a6a6a;
  font-size: 0.82rem;
}

.post-images-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 12px;
  background: #fff;
  padding: 10px;
}

.post-images-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.post-images-head span {
  font-size: 0.92rem;
  font-weight: 700;
  color: #222;
}

.post-images-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-images-preview {
  display: grid;
  gap: 14px;
  grid-template-columns: 1fr;
}

.post-image-card {
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.post-image-card img {
  width: 100%;
  height: auto;
  max-height: none;
  object-fit: contain;
  background: #f6f6f6;
  display: block;
}

.post-image-tools {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 8px;
}

.post-image-tools button {
  border: 1px solid rgba(0, 0, 0, 0.16);
  border-radius: 7px;
  background: #fff;
  color: #222;
  font-size: 0.8rem;
  padding: 5px 6px;
  cursor: pointer;
}

.post-field input:focus,
.post-field textarea:focus {
  border-color: rgba(10, 112, 215, 0.7);
  box-shadow: 0 0 0 3px rgba(10, 112, 215, 0.16);
}

.post-form-error,
.post-form-success {
  margin: 2px 0 0;
  border-radius: 8px;
  padding: 9px 11px;
  font-size: 0.9rem;
}

.post-form-error {
  color: #8f2020;
  background: #ffe6e6;
  border: 1px solid rgba(176, 36, 36, 0.3);
}

.post-form-success {
  color: #1f6a2a;
  background: #e8ffea;
  border: 1px solid rgba(26, 140, 48, 0.28);
}

.post-modal-actions {
  margin-top: 6px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.post-secondary-btn,
.post-primary-btn {
  min-width: 108px;
  border-radius: 999px;
  padding: 10px 18px;
  font-size: 0.94rem;
  font-weight: 700;
  cursor: pointer;
}

.post-secondary-btn {
  border: 1px solid rgba(0, 0, 0, 0.16);
  background: #fff;
  color: #1f1f1f;
}

.post-primary-btn {
  border: 1px solid rgba(0, 0, 0, 0.45);
  background: #111;
  color: #fff;
}

.post-secondary-btn:disabled,
.post-primary-btn:disabled {
  opacity: 0.66;
  cursor: not-allowed;
}

/* 发布方式选择 */
.publish-mode-group {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.publish-mode-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #333;
}

.publish-mode-option input[type="radio"] {
  accent-color: #1a73e8;
  width: 16px;
  height: 16px;
}
</style>
