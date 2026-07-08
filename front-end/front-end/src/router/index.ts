import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'auth',
    component: () => import('../views/AuthView.vue')
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'userProfile',
    component: () => import('../views/UserProfileView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('../views/MessagesView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/AdminView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const accessToken = localStorage.getItem('accessToken');
  if (to.meta.requiresAuth && !accessToken) {
    return { path: '/', query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAdmin) {
    try {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
      if (userInfo.role !== 'ADMIN') {
        return { path: '/home' };
      }
    } catch {
      return { path: '/home' };
    }
  }
});

export default router;
