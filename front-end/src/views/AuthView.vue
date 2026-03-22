<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

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

const mode = ref<AuthMode>('login');
const loginMethod = ref<LoginMethod>('nickname');
const loginForm = reactive<LoginForm>({
  identifier: '',
  password: ''
});

const registerForm = reactive<RegisterForm>({
  nickname: '',
  password: ''
});

// Front-end mock data for nickname uniqueness check before backend integration.
const takenNicknames = new Set(['阿岭', '熊大', '熊二', '光头强'].map((item) => item.toLowerCase()));

const message = ref('');
const isError = ref(false);

const actionText = computed(() => (mode.value === 'login' ? '登录' : '注册'));
const heroPanel = ref<HTMLElement | null>(null);
let heroAnimationContext: gsap.Context | null = null;

onMounted(() => {
  if (!heroPanel.value) {
    return;
  }

  heroAnimationContext = gsap.context(() => {
    const targets = heroPanel.value?.querySelectorAll('.hero-animate:not(.brand)');
    const brandTarget = heroPanel.value?.querySelector('.brand');

    if (!targets || targets.length === 0) {
      return;
    }

    gsap.from(targets, {
      y: 70,
      opacity: 0,
      duration: 0.9,
      ease: 'power2.out',
      stagger: 0.14,
      scrollTrigger: {
        trigger: heroPanel.value,
        start: 'top 80%',
        toggleActions: 'play none none none'
      }
    });

    if (brandTarget) {
      // Avoid transform conflicts with existing CSS keyframes on .brand.
      gsap.set(brandTarget, { animation: 'none' });

      gsap.to(brandTarget, {
        keyframes: {
          y: [0, 80, -10, 30, 0],
          ease: 'none',
          easeEach: 'power2.inOut'
        },
        rotate: 360,
        ease: 'elastic',
        duration: 5,
        transformOrigin: '50% 50%',
        delay: 0.25
      });
    }
  }, heroPanel.value);
});

onBeforeUnmount(() => {
  heroAnimationContext?.revert();
  heroAnimationContext = null;
});

function setMode(nextMode: AuthMode) {
  mode.value = nextMode;
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

function submitLogin() {
  const identifier = loginForm.identifier.trim();

  if (identifier.length < 2) {
    isError.value = true;
    message.value = loginMethod.value === 'nickname' ? '昵称至少 2 个字符。' : 'ID 至少 6 个字符。';
    return;
  }

  if (loginMethod.value === 'id' && !/^[A-Za-z0-9_-]{6,32}$/.test(identifier)) {
    isError.value = true;
    message.value = 'ID 仅支持字母、数字、下划线和连字符，长度 6-32。';
    return;
  }

  if (loginForm.password.length < 6) {
    isError.value = true;
    message.value = '密码至少 6 位。';
    return;
  }

  isError.value = false;
  message.value = '登录校验通过，后续可接入接口。';
}

function submitRegister() {
  const nickname = registerForm.nickname.trim();

  if (nickname.length < 2) {
    isError.value = true;
    message.value = '昵称至少 2 个字符。';
    return;
  }

  if (takenNicknames.has(nickname.toLowerCase())) {
    isError.value = true;
    message.value = '该昵称已被占用，请更换昵称。';
    return;
  }

  if (registerForm.password.length < 6) {
    isError.value = true;
    message.value = '密码至少 6 位。';
    return;
  }

  isError.value = false;
  message.value = '注册校验通过，后端会生成唯一 ID。';
}
</script>

<template>
  <main class="auth-shell">
    <section ref="heroPanel" class="hero-panel">
      <p class="brand hero-animate">狗熊岭</p>
      <h1 class="hero-animate">分享你热爱的音乐、阅读与游戏</h1>
      <p class="subtitle hero-animate">
        在这里记录值得回味的旋律、句子与通关时刻，和同好一起交换灵感。
      </p>
      <div class="chips hero-animate">
        <span>音乐</span>
        <span>阅读</span>
        <span>游戏</span>
      </div>
    </section>

    <section class="form-panel">
      <div class="tabs" role="tablist" aria-label="认证方式切换">
        <button
          :class="['tab', mode === 'login' ? 'active' : '']"
          @click="setMode('login')"
          type="button"
        >
          登录
        </button>
        <button
          :class="['tab', mode === 'register' ? 'active' : '']"
          @click="setMode('register')"
          type="button"
        >
          注册
        </button>
      </div>

      <form v-if="mode === 'login'" class="form" @submit.prevent="submitLogin">
        <div class="sub-tabs" role="tablist" aria-label="登录方式切换">
          <button
            :class="['sub-tab', loginMethod === 'nickname' ? 'active' : '']"
            @click="setLoginMethod('nickname')"
            type="button"
          >
            昵称登录
          </button>
          <button
            :class="['sub-tab', loginMethod === 'id' ? 'active' : '']"
            @click="setLoginMethod('id')"
            type="button"
          >
            ID 登录
          </button>
        </div>

        <label>
          {{ loginMethod === 'nickname' ? '昵称' : 'ID' }}
          <input
            v-model="loginForm.identifier"
            type="text"
            :placeholder="loginMethod === 'nickname' ? '输入你的昵称' : '输入你的 ID'"
          />
        </label>
        <label>
          密码
          <input v-model="loginForm.password" type="password" placeholder="输入密码" />
        </label>
        <p v-if="loginMethod === 'id'" class="hint">ID 由后端注册成功后自动生成。</p>
        <button class="submit" type="submit">{{ actionText }}</button>
      </form>

      <form v-else class="form" @submit.prevent="submitRegister">
        <label>
          昵称
          <input v-model="registerForm.nickname" type="text" placeholder="昵称唯一，不可重复" />
        </label>
        <label>
          密码
          <input v-model="registerForm.password" type="password" placeholder="至少 6 位" />
        </label>
        <p class="hint">注册时只需要昵称和密码，后端会自动生成唯一 ID。</p>
        <button class="submit" type="submit">{{ actionText }}</button>
      </form>

      <p v-if="message" :class="['message', isError ? 'error' : 'success']">{{ message }}</p>
    </section>
  </main>
</template>
