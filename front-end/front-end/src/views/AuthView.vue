<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { gsap } from 'gsap';
import type { ApiResponse } from '../api/http';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

type AuthMode = 'login' | 'register';
type LoginMethod = 'nickname' | 'id';

interface LoginForm {
  identifier: string;
  password: string;
}

interface RegisterForm {
  nickname: string;
  password: string;
}

interface AuthUser {
  id: number;
  username: string;
  nickname?: string | null;
  email?: string | null;
  avatar?: string | null;
  backgroundImage?: string | null;
  bio?: string | null;
  gender?: number | null;
  birthday?: string | null;
  location?: string | null;
  followersCount?: number | null;
  followingCount?: number | null;
  postsCount?: number | null;
  status?: number | null;
  lastLoginAt?: string | null;
  createdAt?: string | null;
}

interface AuthPayload {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

const mode = ref<AuthMode>('login');
const showAuthForm = ref(false);
const loginMethod = ref<LoginMethod>('nickname');
const loginForm = reactive<LoginForm>({
  identifier: '',
  password: ''
});

const registerForm = reactive<RegisterForm>({
  nickname: '',
  password: ''
});

const message = ref('');
const isError = ref(false);
const heroPanel = ref<HTMLElement | null>(null);
let heroAnimationContext: gsap.Context | null = null;

const actionText = computed(() => (mode.value === 'login' ? '登录' : '注册'));
const verticalHeroText = '分享你热爱的音乐、阅读与游戏';
const verticalHeroChars = computed(() => Array.from(verticalHeroText));

function runLandingAnimation() {
  const panel = heroPanel.value;

  if (!panel || showAuthForm.value) {
    return;
  }

  const targets = gsap.utils.toArray<HTMLElement>('.hero-animate', panel);
  gsap.set(targets, { willChange: 'transform, opacity', force3D: true });

  gsap.from(targets, {
    y: 30,
    opacity: 0,
    duration: 0.66,
    ease: 'power2.out',
    stagger: 0.07,
    clearProps: 'willChange'
  });

  gsap.fromTo(
    '.hero-title-char',
    {
      x: -18,
      opacity: 0,
      rotateY: -50
    },
    {
      x: 0,
      opacity: 1,
      rotateY: 0,
      duration: 0.58,
      ease: 'back.out(1.8)',
      stagger: 0.05,
      delay: 0.15
    }
  );

  gsap.to('.hero-vertical', {
    y: 7,
    duration: 2.1,
    ease: 'sine.inOut',
    repeat: -1,
    yoyo: true
  });
}

onMounted(() => {
  if (!heroPanel.value) {
    return;
  }

  heroAnimationContext = gsap.context(() => {
    runLandingAnimation();
  }, heroPanel.value);
});

watch(showAuthForm, async (visible) => {
  if (visible || !heroAnimationContext) {
    return;
  }

  await nextTick();
  heroAnimationContext.add(() => {
    runLandingAnimation();
  });
});

onBeforeUnmount(() => {
  heroAnimationContext?.revert();
  heroAnimationContext = null;
});

function openAuthForm(nextMode: AuthMode) {
  mode.value = nextMode;
  showAuthForm.value = true;
  message.value = '';
  isError.value = false;
}

function backToLanding() {
  showAuthForm.value = false;
  message.value = '';
  isError.value = false;
}

function setLoginMethod(nextMethod: LoginMethod) {
  loginMethod.value = nextMethod;
  loginForm.identifier = '';
  loginForm.password = '';
  message.value = '';
  isError.value = false;
}

function persistAuth(payload: AuthPayload) {
  authStore.persistAuth(payload);
}

async function submitLogin() {
  const identifier = loginForm.identifier.trim();

  if (identifier.length < 2) {
    isError.value = true;
    message.value = loginMethod.value === 'nickname' ? '用户名或邮箱至少 2 个字符。' : 'ID 至少 5 个字符。';
    return;
  }

  if (loginMethod.value === 'id' && !/^[A-Za-z0-9_-]{5,32}$/.test(identifier)) {
    isError.value = true;
    message.value = 'ID 仅支持字母、数字、下划线和连字符，长度 5-32。';
    return;
  }

  if (loginForm.password.length < 6) {
    isError.value = true;
    message.value = '密码至少 6 位。';
    return;
  }

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        identifier,
        password: loginForm.password
      })
    });
    const result: ApiResponse<AuthPayload> = await res.json();
    if (result.code === 0) {
      isError.value = false;
      message.value = '登录成功，欢迎回来。';
      persistAuth(result.data);
      router.push('/home');
    } else {
      isError.value = true;
      message.value = result.message || '登录失败，请重试。';
    }
  } catch (err) {
    isError.value = true;
    message.value = '网络错误，请稍后再试。';
  }
}

async function submitRegister() {
  const nickname = registerForm.nickname.trim();

  if (nickname.length < 2) {
    isError.value = true;
    message.value = '用户名至少 2 个字符。';
    return;
  }

  if (registerForm.password.length < 6) {
    isError.value = true;
    message.value = '密码至少 6 位。';
    return;
  }

  try {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: nickname,
        password: registerForm.password
      })
    });
    const result: ApiResponse<AuthPayload> = await res.json();
    if (result.code === 0) {
      isError.value = false;
      message.value = `注册成功！您的 ID 是 ${result.data.user.id}，即将自动登录。`;
      persistAuth(result.data);

      setTimeout(() => {
        router.push('/home');
      }, 1200);
    } else {
      isError.value = true;
      message.value = result.message || '注册失败，请更换用户名重试。';
    }
  } catch (err) {
    isError.value = true;
    message.value = '网络错误，请稍后再试。';
  }
}
</script>

<template>
  <main :class="['auth-shell', { 'auth-shell--landing': !showAuthForm }]">
    <h1 v-if="!showAuthForm" class="hero-vertical hero-animate" :aria-label="verticalHeroText">
      <span v-for="(char, index) in verticalHeroChars" :key="`${char}-${index}`" class="hero-title-char">{{ char }}</span>
    </h1>
    <div v-if="!showAuthForm" class="top-brand-stack">
      <div class="top-brand-mark hero-animate" aria-hidden="true">L</div>
      <p class="brand hero-animate">Lumen Grove</p>
    </div>
    <section ref="heroPanel" :class="['entry-card', { 'entry-card--landing': !showAuthForm }]">
      <template v-if="!showAuthForm">
        <p class="hero-subtitle hero-animate">
          在这里记录值得回味的旋律、句子与通关时刻，和同好一起交换灵感。
        </p>

        <div class="chips hero-animate">
          <span>音乐</span>
          <span>阅读</span>
          <span>游戏</span>
        </div>

        <div class="cta-group hero-animate">
          <button class="cta-btn primary" type="button" @click="openAuthForm('login')">登录</button>
          <button class="cta-btn secondary" type="button" @click="openAuthForm('register')">注册</button>
        </div>
      </template>

      <template v-else>
        <div class="auth-topbar">
          <button class="back-btn" type="button" @click="backToLanding">返回</button>
          <div class="tabs" role="tablist" aria-label="认证方式切换">
            <button
              :class="['tab', mode === 'login' ? 'active' : '']"
              type="button"
              @click="mode = 'login'"
            >
              登录
            </button>
            <button
              :class="['tab', mode === 'register' ? 'active' : '']"
              type="button"
              @click="mode = 'register'"
            >
              注册
            </button>
          </div>
        </div>

        <div class="auth-panel">
          <h2 class="auth-title">{{ mode === 'login' ? '欢迎回来' : '创建账户' }}</h2>

          <form v-if="mode === 'login'" class="form" @submit.prevent="submitLogin">
            <div class="sub-tabs" role="tablist" aria-label="登录方式切换">
              <button
                :class="['sub-tab', loginMethod === 'nickname' ? 'active' : '']"
                type="button"
                @click="setLoginMethod('nickname')"
              >
                用户名/邮箱
              </button>
              <button
                :class="['sub-tab', loginMethod === 'id' ? 'active' : '']"
                type="button"
                @click="setLoginMethod('id')"
              >
                ID 登录
              </button>
            </div>

            <label class="field">
              <span>{{ loginMethod === 'nickname' ? '用户名或邮箱' : 'ID' }}</span>
              <input
                v-model="loginForm.identifier"
                type="text"
                :placeholder="loginMethod === 'nickname' ? '输入你的用户名或邮箱' : '输入你的 ID'"
              />
            </label>

            <label class="field">
              <span>密码</span>
              <input v-model="loginForm.password" type="password" placeholder="输入密码" />
            </label>

            <p v-if="loginMethod === 'id'" class="hint">ID 会在注册成功后由后端自动生成。</p>
            <button class="submit-btn" type="submit">{{ actionText }}</button>
          </form>

          <form v-else class="form" @submit.prevent="submitRegister">
            <label class="field">
              <span>用户名</span>
              <input v-model="registerForm.nickname" type="text" placeholder="用户名唯一，不可重复" />
            </label>

            <label class="field">
              <span>密码</span>
              <input v-model="registerForm.password" type="password" placeholder="至少 6 位" />
            </label>

            <button class="submit-btn" type="submit">{{ actionText }}</button>
          </form>

          <p v-if="message" :class="['message', isError ? 'error' : 'success']">{{ message }}</p>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  background:
    radial-gradient(circle at top left, rgba(255, 233, 155, 0.5), transparent 34%),
    radial-gradient(circle at bottom right, rgba(18, 212, 255, 0.24), transparent 32%),
    linear-gradient(135deg, #f8f4e6 0%, #f4efe4 46%, #f0f0ef 100%);
}

.auth-shell--landing {
  justify-content: flex-start;
}

.top-brand-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 10px;
}

.top-brand-mark {
  width: clamp(120px, 24vh, 220px);
  height: clamp(120px, 24vh, 220px);
  margin-bottom: 10px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(150deg, #111 0%, #272727 100%);
  color: #fff7d0;
  font-size: clamp(3.2rem, 10vh, 6rem);
  font-weight: 900;
  letter-spacing: 0.04em;
  box-shadow:
    0 18px 45px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.26);
}

.entry-card {
  width: min(470px, 100%);
  min-height: 360px;
  border-radius: 30px;
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow:
    0 26px 70px rgba(0, 0, 0, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.74);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.entry-card--landing {
  position: relative;
  justify-content: flex-start;
  align-items: flex-start;
  text-align: left;
  gap: 0;
}

.hero-vertical {
  position: fixed;
  left: calc(50% - min(470px, calc(100vw - 40px)) / 2 - 34px);
  top: 53%;
  transform: translateY(-50%);
  margin: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  font-size: 1.2rem;
  font-weight: 800;
  color: #1d1d1d;
  text-shadow: 0 0 16px rgba(255, 193, 119, 0.38);
}

.hero-title-char {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.1em;
  line-height: 1;
}

.brand {
  align-self: center;
  margin: 0;
  padding: 0.46rem 0.95rem;
  border-radius: 999px;
  border: 1px solid rgba(226, 195, 118, 0.56);
  background: linear-gradient(135deg, #fff7c9 0%, #ffefad 100%);
  box-shadow:
    0 10px 22px rgba(232, 196, 102, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
  color: #111;
  font-size: clamp(1.25rem, 2.4vw, 1.85rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-title {
  margin: 0;
  color: #171717;
  font-size: clamp(1.3rem, 3vw, 1.75rem);
  line-height: 1.12;
  max-width: 17ch;
}

.hero-subtitle {
  margin: 20px 0 0;
  color: rgb(68, 185, 132);
  font-size: 0.9rem;
  line-height: 1.46;
  max-width: none;
  align-self: center;
  text-align: center;
  white-space: nowrap;
}

.chips {
  display: flex;
  align-self: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 30px;
  margin-top: 40px;
  width: 100%;
}

.chips span {
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.9);
  color: #222;
  font-weight: 700;
  font-size: 1rem;
}

.chips span:nth-child(1) {
  background: #ffc9de;
  border-color: #f38bb4;
  color: #8a1144;
}

.chips span:nth-child(2) {
  background: #cfe7ff;
  border-color: #79b6ff;
  color: #123b88;
}

.chips span:nth-child(3) {
  background: #ffe98f;
  border-color: #f4c51a;
  color: #735700;
}

.cta-group {
  display: flex;
  align-self: center;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  width: 100%;
  max-width: 300px;
}

.entry-card--landing .cta-group {
  margin-top: 100px;
  padding-top: 10px;
}

.cta-btn,
.submit-btn,
.back-btn,
.tab,
.sub-tab {
  font: inherit;
}

.cta-btn {
  flex: 1 1 0;
  min-width: 0;
  padding: 13px 18px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.18);
  cursor: pointer;
  font-weight: 800;
  font-size: 1rem;

  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
}

.cta-btn:hover,

.submit-btn:hover,
.back-btn:hover,
.tab:hover,
.sub-tab:hover {
  transform: translateY(-1px);

}

.cta-btn.primary {
  background: #f7f7f7;
  color: #111;
  border-color: #d4d4d4;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);
}

.cta-btn.secondary {
  background: #f1f5f9;
  color: #111;
  border-color: #cbd5e1;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
}

.cta-group .cta-btn:hover {
  background: #111;
  color: #fff;
  border-color: #111;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.24);
}

.submit-btn {
  background: #eaf6ff;
  color: #111;
  box-shadow: 0 10px 20px rgba(125, 211, 252, 0.25);
}

.auth-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}

.back-btn {
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #fff;
  color: #222;
  padding: 10px 16px;
  border-radius: 999px;
  cursor: pointer;
}

.tabs,
.sub-tabs {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(247, 243, 233, 0.95);
}

.tab,
.sub-tab {
  border: 0;
  background: transparent;
  color: #555;
  padding: 10px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-weight: 700;
}

.tab.active,
.sub-tab.active {
  background: #111;
  color: #fff;
}

.auth-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.auth-title {
  margin: 0;
  color: #181818;
  font-size: 1.8rem;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field span {
  color: #444;
  font-size: 0.95rem;
  font-weight: 700;
}

.field input {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 16px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.94);
  color: #171717;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.field input:focus {
  border-color: rgba(0, 0, 0, 0.34);
  box-shadow: 0 0 0 4px rgba(255, 235, 169, 0.5);
}

.hint {
  margin: -4px 0 0;
  color: #6b6b6b;
  font-size: 0.92rem;
}

.submit-btn {
  border: 0;
  border-radius: 16px;
  padding: 14px 18px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 800;
}

.message {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 0.95rem;
  line-height: 1.6;
}

.message.success {
  background: rgba(66, 184, 131, 0.14);
  color: #18794e;
}

.message.error {
  background: rgba(220, 38, 38, 0.1);
  color: #b42318;
}

@media (max-width: 640px) {
  .entry-card {
    min-height: auto;
    padding: 18px 16px;
  }

  .hero-vertical {
    left: calc(50% - min(470px, calc(100vw - 40px)) / 2 - 24px);
    font-size: 0.92rem;
    gap: 2px;
  }

  .auth-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .cta-group {
    flex-direction: column;
    max-width: 100%;
  }

  .cta-btn {
    width: 100%;
  }
}
</style>
