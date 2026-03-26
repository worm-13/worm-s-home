<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const user = ref<{ id: number; username: string } | null>(null);

onMounted(() => {
  const storedUser = localStorage.getItem('userInfo');
  if (storedUser) {
    user.value = JSON.parse(storedUser);
  } else {
    // 未登录则退回登录页
    router.replace('/');
  }
});

function logout() {
  localStorage.removeItem('userInfo');
  router.replace('/');
}
</script>

<template>
  <main class="home-container">
    <div class="welcome-box">
      <h1>登录成功！</h1>
      <div v-if="user" class="user-info">
        <p><strong>用户名：</strong> {{ user.username }}</p>
        <p><strong>ID：</strong> {{ user.id }}</p>
      </div>
      <button class="logout-btn" @click="logout">退出登录</button>
    </div>
  </main>
</template>

<style scoped>
.home-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.welcome-box {
  background: white;
  padding: 3rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  text-align: center;
}

h1 {
  color: #42b883;
  margin-bottom: 1.5rem;
}

.user-info p {
  font-size: 1.2rem;
  margin: 0.5rem 0;
  color: #333;
}

.logout-btn {
  margin-top: 2rem;
  padding: 0.75rem 1.5rem;
  background-color: #ff4d4f;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s;
}

.logout-btn:hover {
  background-color: #ff7875;
}
</style>