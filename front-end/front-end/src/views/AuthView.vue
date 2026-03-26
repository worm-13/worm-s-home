<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { gsap } from 'gsap';

const router = useRouter();

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
    const panel = heroPanel.value;

    if (!panel) {
      return;
    }

    const targets = gsap
      .utils.toArray<HTMLElement>('.hero-animate', panel)
      .filter((item) => !item.classList.contains('brand'));
    const brandTarget = panel.querySelector<HTMLElement>('.brand');

    if (targets.length > 0) {
      gsap.set(targets, { willChange: 'transform, opacity', force3D: true });

      gsap.from(targets, {
        y: 52,
        opacity: 0,
        duration: 0.78,
        ease: 'power2.out',
        stagger: 0.1,
        clearProps: 'willChange'
      });
    }

    if (brandTarget) {
      gsap.set(brandTarget, { willChange: 'transform, opacity', force3D: true, animation: 'none' });

      gsap.from(brandTarget, {
        y: 58,
        opacity: 0,
        duration: 0.9,
        ease: 'sine.out'
      });

      // Subtle floating loop to keep motion alive without heavy main-thread cost.
      gsap.to(brandTarget, {
        y: -6,
        duration: 2.8,
        ease: 'sine.inOut',
        repeat: -1,
        yoyo: true,
        delay: 0.95
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

async function submitLogin() {
  const identifier = loginForm.identifier.trim();

  if (identifier.length < 2) {
    isError.value = true;
    message.value = loginMethod.value === 'nickname' ? '昵称至少 2 个字符。' : 'ID 至少 6 个字符。';
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
        identifier: identifier,
        password: loginForm.password
      })
    });
    const result = await res.json();
    if (result.code === 0) {
      isError.value = false;
      message.value = '登录成功！欢迎回来。';
      // Save user info and redirect to home
      localStorage.setItem('userInfo', JSON.stringify({
        id: result.data.id,
        username: result.data.username
      }));
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
    message.value = '昵称至少 2 个字符。';
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
    const result = await res.json();
    if (result.code === 0) {
      isError.value = false;
      message.value = `注册成功！您的 ID 为 ${result.data?.id}，即将自动登录...`;
      
      // Save info and redirect to home directly
      localStorage.setItem('userInfo', JSON.stringify({
        id: result.data.id,
        username: result.data.username
      }));

      setTimeout(() => {
        router.push('/home');
      }, 2000);
    } else {
      isError.value = true;
      message.value = result.message || '注册失败，请更换昵称重试。';
    }
  } catch (err) {
    isError.value = true;
    message.value = '网络错误，请稍后再试。';
  }
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
        
        <button class="submit" type="submit">{{ actionText }}</button>
      </form>

      <p v-if="message" :class="['message', isError ? 'error' : 'success']">{{ message }}</p>
    </section>
  </main>
</template>
