<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue';
import { VueCropper } from 'vue-cropper/dist/vue-cropper.es.js';
import 'vue-cropper/dist/index.css';
import { useAuth } from '../composables/useAuth';
import { resolveAssetUrl } from '../composables/useAssetUrl';
import { updateSignature, uploadAvatar, uploadBackgroundImage } from '../api/user';

const { user, refreshUserInfo, updateLocalUser } = useAuth();

const MAX_IMAGE_SIZE = 20 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp']);

const avatarInput = ref<HTMLInputElement | null>(null);
const backgroundInput = ref<HTMLInputElement | null>(null);
const showAvatarModal = ref(false);
const selectedAvatarSrc = ref('');
const avatarError = ref('');
const avatarSuccess = ref('');
const backgroundError = ref('');
const backgroundSuccess = ref('');
const isUploadingAvatar = ref(false);
const isUploadingBackground = ref(false);
const hasAvatarLoadError = ref(false);
const hasBackgroundLoadError = ref(false);
const cropperRef = ref<any>(null);

let avatarSuccessTimer: number | null = null;
let backgroundSuccessTimer: number | null = null;

const showSignatureModal = ref(false);
const signatureText = ref('');
const signatureError = ref('');
const signatureSuccess = ref('');
const isUpdatingSignature = ref(false);

const userInitial = computed(() => {
  const name = (user.value?.nickname || user.value?.username || '').trim();
  return name ? name.charAt(0) : '?';
});

const profileSignature = computed(() => {
  return (user.value?.bio || '').trim();
});

const avatarImageUrl = computed(() => resolveAssetUrl(user.value?.avatar));
const canShowAvatarImage = computed(() => Boolean(avatarImageUrl.value) && !hasAvatarLoadError.value);
const backgroundImageUrl = computed(() => resolveAssetUrl(user.value?.backgroundImage));
const canShowBackgroundImage = computed(() => Boolean(backgroundImageUrl.value) && !hasBackgroundLoadError.value);

async function handleUpdateSignature() {
  signatureError.value = '';
  signatureSuccess.value = '';

  const trimmedText = signatureText.value.trim();

  if (trimmedText.length > 20) {
    signatureError.value = '个性签名不能超过20字';
    return;
  }

  isUpdatingSignature.value = true;
  try {
    const res = await updateSignature(trimmedText);

    if (res.data.code === 0) {
      signatureSuccess.value = '修改成功';
      updateLocalUser({ bio: trimmedText });
      setTimeout(() => {
        showSignatureModal.value = false;
        signatureSuccess.value = '';
        signatureText.value = '';
      }, 1500);
    } else {
      signatureError.value = res.data.message || '修改失败';
    }
  } catch {
    signatureError.value = '修改失败，请重试';
  } finally {
    isUpdatingSignature.value = false;
  }
}

function triggerAvatarSelect() {
  avatarError.value = '';
  avatarSuccess.value = '';
  avatarInput.value?.click();
}

function triggerBackgroundSelect() {
  backgroundError.value = '';
  backgroundSuccess.value = '';
  backgroundInput.value?.click();
}

async function handleAvatarSelect(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) return;

  avatarError.value = '';
  avatarSuccess.value = '';

  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    avatarError.value = '头像仅支持 jpg、jpeg、png、gif 或 webp 格式。';
    input.value = '';
    return;
  }

  if (file.size > MAX_IMAGE_SIZE) {
    avatarError.value = '头像文件不能超过 20MB。';
    input.value = '';
    return;
  }

  try {
    const dataUrl = await readFileAsDataUrl(file);
    selectedAvatarSrc.value = dataUrl;
    showAvatarModal.value = true;
  } catch {
    avatarError.value = '头像读取失败，请换一张图片重试。';
  } finally {
    input.value = '';
  }
}

function closeAvatarModal() {
  if (isUploadingAvatar.value) return;

  if (avatarSuccessTimer !== null) {
    window.clearTimeout(avatarSuccessTimer);
    avatarSuccessTimer = null;
  }

  showAvatarModal.value = false;
  selectedAvatarSrc.value = '';
  avatarError.value = '';
  avatarSuccess.value = '';
}

function zoomAvatar(step: number) {
  cropperRef.value?.changeScale(step);
}

function resetAvatarCrop() {
  cropperRef.value?.refresh();
}

async function uploadCroppedAvatar() {
  if (!selectedAvatarSrc.value || !user.value) return;

  isUploadingAvatar.value = true;
  avatarError.value = '';
  avatarSuccess.value = '';

  try {
    const blob = await createCroppedAvatarBlob();
    const file = new File([blob], 'avatar.png', { type: 'image/png' });

    const res = await uploadAvatar(file);

    if (res.data.code === 0) {
      const url = res.data.data?.url;
      if (url) {
        updateLocalUser({ avatar: url });
      } else {
        await refreshUserInfo();
      }
      hasAvatarLoadError.value = false;
      avatarSuccess.value = '更换成功！';
      if (avatarSuccessTimer !== null) {
        window.clearTimeout(avatarSuccessTimer);
      }
      avatarSuccessTimer = window.setTimeout(() => {
        avatarSuccessTimer = null;
        closeAvatarModal();
      }, 900);
      return;
    }

    avatarError.value = res.data.message || '头像上传失败，请稍后重试。';
  } catch {
    avatarError.value = '头像上传失败，请稍后重试。';
  } finally {
    isUploadingAvatar.value = false;
  }
}

function handleAvatarImageError() {
  hasAvatarLoadError.value = true;
}

function handleAvatarImageLoad() {
  hasAvatarLoadError.value = false;
}

function handleBackgroundImageError() {
  hasBackgroundLoadError.value = true;
}

function handleBackgroundImageLoad() {
  hasBackgroundLoadError.value = false;
}

async function handleBackgroundSelect(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) return;

  backgroundError.value = '';
  backgroundSuccess.value = '';

  if (!ALLOWED_IMAGE_TYPES.has(file.type)) {
    backgroundError.value = '背景图仅支持 jpg、jpeg、png、gif 或 webp 格式。';
    input.value = '';
    return;
  }

  if (file.size > MAX_IMAGE_SIZE) {
    backgroundError.value = '背景图文件不能超过 20MB。';
    input.value = '';
    return;
  }

  isUploadingBackground.value = true;

  try {
    const res = await uploadBackgroundImage(file);

    if (res.data.code === 0) {
      const url = res.data.data?.url;
      if (url) {
        updateLocalUser({ backgroundImage: url });
      } else {
        await refreshUserInfo();
      }
      hasBackgroundLoadError.value = false;
      backgroundSuccess.value = '背景图上传成功。';
      if (backgroundSuccessTimer !== null) {
        window.clearTimeout(backgroundSuccessTimer);
      }
      backgroundSuccessTimer = window.setTimeout(() => {
        backgroundSuccessTimer = null;
        backgroundSuccess.value = '';
      }, 1600);
      return;
    }

    backgroundError.value = res.data.message || '背景图上传失败，请稍后重试。';
  } catch {
    backgroundError.value = '背景图上传失败，请稍后重试。';
  } finally {
    isUploadingBackground.value = false;
    input.value = '';
  }
}

async function createCroppedAvatarBlob() {
  if (!cropperRef.value) {
    throw new Error('Cropper is not ready.');
  }

  const blob = await new Promise<Blob | null>((resolve) => {
    cropperRef.value.getCropBlob((data: Blob) => resolve(data));
  });

  if (!blob) {
    throw new Error('Failed to export avatar image.');
  }

  return blob;
}

function readFileAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

onUnmounted(() => {
  if (avatarSuccessTimer !== null) {
    window.clearTimeout(avatarSuccessTimer);
    avatarSuccessTimer = null;
  }
  if (backgroundSuccessTimer !== null) {
    window.clearTimeout(backgroundSuccessTimer);
    backgroundSuccessTimer = null;
  }
});
</script>

<template>
  <input
    ref="avatarInput"
    class="avatar-file-input"
    type="file"
    accept="image/jpeg,image/png,image/gif,image/webp"
    @change="handleAvatarSelect"
  />
  <input
    ref="backgroundInput"
    class="avatar-file-input"
    type="file"
    accept="image/jpeg,image/png,image/gif,image/webp"
    @change="handleBackgroundSelect"
  />

  <section class="profile-box" aria-label="个人主页信息">
    <div class="profile-hero">
      <img
        v-if="canShowBackgroundImage"
        class="profile-hero-image"
        :src="backgroundImageUrl"
        loading="lazy"
        alt="个人背景图"
        @error="handleBackgroundImageError"
        @load="handleBackgroundImageLoad"
      />
      <div v-else class="profile-hero-placeholder"></div>
      <div class="profile-hero-mask"></div>
      <button class="profile-bg-upload-btn" type="button" :disabled="isUploadingBackground" @click="triggerBackgroundSelect">
        {{ isUploadingBackground ? '上传中...' : '上传背景图（可选）' }}
      </button>
    </div>

    <div class="profile-avatar-wrap profile-avatar-wrap--floating">
      <img
        v-if="canShowAvatarImage"
        class="profile-avatar-image"
        :src="avatarImageUrl"
        loading="lazy"
        alt="用户头像"
        @error="handleAvatarImageError"
        @load="handleAvatarImageLoad"
      />
      <span v-else class="profile-avatar-fallback">{{ userInitial }}</span>
      <button class="profile-avatar-upload-btn" type="button" :disabled="isUploadingAvatar" @click="triggerAvatarSelect">
        {{ isUploadingAvatar ? '上传中...' : '更换头像' }}
      </button>
    </div>

    <div class="profile-main-info">
      <h2 class="profile-name">{{ user?.nickname || user?.username || '未命名用户' }}</h2>

      <div class="profile-signature-box">
        <div class="profile-signature-header">
          <p class="profile-signature-label">个性签名</p>
          <button class="edit-signature-btn" type="button" @click="showSignatureModal = true">{{ profileSignature ? '修改' : '设置' }}</button>
        </div>
        <p v-if="profileSignature" class="profile-signature">{{ profileSignature }}</p>
        <p v-else class="profile-signature-empty">还没设置个性签名，赶快设置吧</p>
      </div>

      <p v-if="backgroundError" class="profile-upload-error">{{ backgroundError }}</p>
      <p v-if="backgroundSuccess" class="profile-upload-success">{{ backgroundSuccess }}</p>
    </div>
  </section>

  <!-- 修改个性签名弹窗 -->
  <section v-if="showSignatureModal" class="modal-backdrop" @click.self="showSignatureModal = false">
    <div class="signature-modal" @click.stop>
      <div class="signature-modal-header">
        <h3>修改个性签名</h3>
        <button class="close-btn" type="button" @click="showSignatureModal = false">×</button>
      </div>
      <form class="signature-form" @submit.prevent="handleUpdateSignature">
        <textarea
          v-model="signatureText"
          class="signature-textarea"
          placeholder="请输入个性签名（最多20字）"
          maxlength="20"
          rows="2"
        ></textarea>
        <div class="signature-char-count">{{ signatureText.length }}/20</div>
        <p v-if="signatureError" class="signature-error">{{ signatureError }}</p>
        <p v-if="signatureSuccess" class="signature-success">{{ signatureSuccess }}</p>
        <div class="signature-modal-actions">
          <button class="signature-cancel-btn" type="button" @click="showSignatureModal = false">取消</button>
          <button class="signature-submit-btn" type="submit" :disabled="isUpdatingSignature">
            {{ isUpdatingSignature ? '修改中...' : '确认修改' }}
          </button>
        </div>
      </form>
    </div>
  </section>

  <!-- 头像裁切弹窗 -->
  <section v-if="showAvatarModal" class="avatar-modal-backdrop" @click.self="closeAvatarModal">
    <div class="avatar-modal" @click.stop>
      <div class="avatar-modal-header">
        <div>
          <p class="avatar-modal-title">裁切圆形头像</p>
          <p class="avatar-modal-subtitle">调整图片位置和缩放后上传。</p>
        </div>
        <button class="avatar-close-btn" type="button" aria-label="关闭头像裁切弹窗" @click="closeAvatarModal">
          ×
        </button>
      </div>

      <div class="avatar-crop-shell">
        <div class="avatar-crop-stage">
          <VueCropper
            ref="cropperRef"
            class="avatar-cropper"
            :img="selectedAvatarSrc"
            :auto-crop="true"
            :auto-crop-width="280"
            :auto-crop-height="280"
            :fixed="true"
            :fixed-number="[1, 1]"
            :can-move="true"
            :can-move-box="false"
            :fixed-box="true"
            :center-box="true"
            :can-scale="true"
            :info="false"
            :full="false"
            :output-size="1"
            output-type="png"
            mode="contain"
          />
        </div>

        <div class="avatar-crop-controls">
          <p class="avatar-crop-hint">滚动鼠标以缩放</p>
          <div class="avatar-crop-toolbar">
            <button class="avatar-secondary-btn" type="button" :disabled="isUploadingAvatar" @click="zoomAvatar(0.1)">放大</button>
            <button class="avatar-secondary-btn" type="button" :disabled="isUploadingAvatar" @click="zoomAvatar(-0.1)">缩小</button>
            <button class="avatar-secondary-btn" type="button" :disabled="isUploadingAvatar" @click="resetAvatarCrop">重置</button>
          </div>
        </div>
      </div>

      <p v-if="avatarError" class="avatar-modal-error">{{ avatarError }}</p>
      <p v-if="avatarSuccess" class="avatar-modal-success">{{ avatarSuccess }}</p>

      <div class="avatar-modal-actions">
        <button class="avatar-secondary-btn" type="button" :disabled="isUploadingAvatar" @click="closeAvatarModal">
          取消
        </button>
        <button class="avatar-primary-btn" type="button" :disabled="isUploadingAvatar" @click="uploadCroppedAvatar">
          {{ isUploadingAvatar ? '上传中...' : '保存头像' }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.avatar-file-input {
  display: none;
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

.profile-hero {
  position: relative;
  height: 380px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.profile-hero-image,
.profile-hero-placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-hero-placeholder {
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

.profile-bg-upload-btn {
  position: absolute;
  right: 12px;
  bottom: 12px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 999px;
  background: rgba(22, 22, 22, 0.55);
  color: #fff;
  font-size: 0.83rem;
  font-weight: 700;
  padding: 0.42rem 0.88rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.profile-bg-upload-btn:hover {
  background: rgba(22, 22, 22, 0.75);
}

.profile-bg-upload-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.profile-avatar-upload-btn {
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 999px;
  background: rgba(22, 22, 22, 0.55);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.3rem 0.7rem;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.2s ease;
}

.profile-avatar-upload-btn:hover {
  background: rgba(22, 22, 22, 0.75);
}

.profile-avatar-upload-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

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

.profile-name {
  margin: 0;
  font-size: clamp(1.4rem, 2.5vw, 1.9rem);
  color: #111;
}

.profile-main-info {
  padding: 0 1.1rem 1.45rem;
}

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

.edit-signature-btn {
  border: none;
  background: none;
  color: rgba(0, 0, 0, 0.38);
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.edit-signature-btn:hover {
  color: rgba(0, 0, 0, 0.7);
  background-color: rgba(0, 0, 0, 0.05);
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
  font-style: italic;
}

.profile-upload-error,
.profile-upload-success {
  margin: 0.85rem auto 0;
  width: min(820px, 100%);
  border-radius: 9px;
  padding: 0.55rem 0.7rem;
  font-size: 0.88rem;
  text-align: left;
}

.profile-upload-error {
  color: #8f2020;
  background: #ffe6e6;
  border: 1px solid rgba(176, 36, 36, 0.3);
}

.profile-upload-success {
  color: #1f6a2a;
  background: #e8ffea;
  border: 1px solid rgba(26, 140, 48, 0.28);
}

/* 弹窗背景遮罩 */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 修改个性签名弹窗 */
.signature-modal {
  width: min(480px, 100%);
  border-radius: 16px;
  background: #fff;
  padding: 24px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.2);
}

.signature-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.signature-modal-header h3 {
  margin: 0;
  font-size: 1.15rem;
  color: #1a1a1a;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.14);
  background: #fff;
  font-size: 1.2rem;
  cursor: pointer;
}

.signature-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.signature-textarea {
  width: 100%;
  min-height: 60px;
  padding: 12px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 8px;
  font-size: 0.95rem;
  resize: vertical;
  box-sizing: border-box;
}

.signature-textarea:focus {
  outline: none;
  border-color: #1a73e8;
}

.signature-char-count {
  text-align: right;
  font-size: 0.8rem;
  color: rgba(0, 0, 0, 0.4);
}

.signature-error {
  margin: 0;
  color: #e53935;
  font-size: 0.85rem;
}

.signature-success {
  margin: 0;
  color: #43a047;
  font-size: 0.85rem;
}

.signature-modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.signature-cancel-btn {
  padding: 8px 20px;
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  background: #fff;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.signature-cancel-btn:hover {
  background: rgba(0, 0, 0, 0.04);
}

.signature-submit-btn {
  padding: 8px 24px;
  border: none;
  border-radius: 8px;
  background: #1a73e8;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.signature-submit-btn:hover:not(:disabled) {
  background: #1557b0;
}

.signature-submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 头像裁切弹窗 */
.avatar-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(10, 10, 10, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 30;
}

.avatar-modal {
  width: min(700px, 100%);
  border-radius: 26px;
  background: linear-gradient(180deg, rgba(255, 250, 236, 0.98), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(0, 0, 0, 0.12);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.24);
  padding: 24px;
}

.avatar-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.avatar-modal-title {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 800;
  color: #161616;
}

.avatar-modal-subtitle {
  margin: 6px 0 0;
  color: #6e6e6e;
  font-size: 0.92rem;
}

.avatar-close-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.14);
  background: rgba(255, 255, 255, 0.84);
  color: #1f1f1f;
  font-size: 1.45rem;
  line-height: 1;
  cursor: pointer;
}

.avatar-crop-shell {
  display: grid;
  grid-template-columns: minmax(0, 320px) minmax(0, 1fr);
  gap: 24px;
  align-items: center;
}

.avatar-crop-stage {
  width: 320px;
  height: 320px;
  border-radius: 26px;
  background:
    linear-gradient(45deg, rgba(0, 0, 0, 0.04) 25%, transparent 25%) 0 0 / 22px 22px,
    linear-gradient(-45deg, rgba(0, 0, 0, 0.04) 25%, transparent 25%) 0 0 / 22px 22px,
    linear-gradient(45deg, transparent 75%, rgba(0, 0, 0, 0.04) 75%) 0 0 / 22px 22px,
    linear-gradient(-45deg, transparent 75%, rgba(0, 0, 0, 0.04) 75%) 0 0 / 22px 22px,
    #fcfcfc;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.avatar-cropper {
  width: 100%;
  height: 100%;
}

:deep(.cropper-view-box),
:deep(.cropper-face) {
  border-radius: 50%;
}

.avatar-crop-controls {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-crop-hint {
  margin: 0;
  color: #4f4f4f;
  font-size: 0.92rem;
  line-height: 1.5;
}

.avatar-crop-toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.avatar-modal-error {
  margin: 18px 0 0;
  color: #b42318;
  font-size: 0.92rem;
  line-height: 1.5;
}

.avatar-modal-success {
  margin: 18px 0 0;
  color: #067647;
  font-size: 0.94rem;
  font-weight: 600;
  line-height: 1.5;
}

.avatar-modal-actions {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.avatar-secondary-btn,
.avatar-primary-btn {
  min-width: 110px;
  border-radius: 999px;
  padding: 11px 18px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.avatar-secondary-btn {
  border: 1px solid rgba(0, 0, 0, 0.14);
  background: #fff;
  color: #1b1b1b;
}

.avatar-primary-btn {
  border: 1px solid rgba(0, 0, 0, 0.4);
  background: #111;
  color: #fff;
  box-shadow: 0 10px 18px rgba(0, 0, 0, 0.18);
}

.avatar-secondary-btn:disabled,
.avatar-primary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.avatar-secondary-btn:hover:not(:disabled),
.avatar-primary-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}
</style>
