<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <button type="button" @click="router.push('/')">返回首页</button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  font-family: sans-serif;
  color: #333;
}

.not-found h1 {
  font-size: 4rem;
  margin-bottom: 0.5rem;
  color: #161616;
}

.not-found p {
  font-size: 1.2rem;
  color: #666;
  margin-bottom: 1.5rem;
}

.not-found button {
  padding: 0.6rem 1.5rem;
  background: #161616;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.2s;
}

.not-found button:hover {
  background: #333;
}
</style>
