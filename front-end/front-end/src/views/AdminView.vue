<script setup lang="ts">
import { ref, onMounted } from 'vue';
import http from '../api/http';

interface Report {
  id: number;
  reporterId: number;
  reporterName: string;
  targetType: string;
  targetId: number;
  reason: string;
  description: string;
  status: string;
  reviewedBy: number | null;
  reviewerName: string | null;
  reviewNote: string | null;
  createdAt: string;
  reviewedAt: string | null;
}

const reports = ref<Report[]>([]);
const isLoading = ref(false);
const statusFilter = ref('PENDING');
const reviewNote = ref('');
const reviewingId = ref<number | null>(null);

const statusOptions = ['PENDING', 'REVIEWED', 'DISMISSED', 'RESOLVED'];
const reasonLabels: Record<string, string> = {
  SPAM: '垃圾内容',
  HARASSMENT: '骚扰',
  INAPPROPRIATE: '不当内容',
  OTHER: '其他'
};

async function loadReports() {
  isLoading.value = true;
  try {
    const res = await http.get('/api/reports', {
      params: { status: statusFilter.value, page: 1, size: 50 }
    });
    if (res.data.code === 0) {
      reports.value = res.data.data?.items || [];
    }
  } catch {
    // silent
  } finally {
    isLoading.value = false;
  }
}

async function reviewReport(id: number, status: string) {
  try {
    await http.put(`/api/reports/${id}/review`, {
      status,
      reviewNote: reviewNote.value
    });
    reviewingId.value = null;
    reviewNote.value = '';
    await loadReports();
  } catch {
    // silent
  }
}

onMounted(() => {
  loadReports();
});
</script>

<template>
  <div class="admin-container">
    <h1>举报管理</h1>

    <div class="filter-bar">
      <select v-model="statusFilter" @change="loadReports">
        <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
      </select>
    </div>

    <div v-if="isLoading" class="loading">加载中...</div>

    <div v-else-if="reports.length === 0" class="empty">暂无举报</div>

    <div v-else class="report-list">
      <div v-for="report in reports" :key="report.id" class="report-card">
        <div class="report-header">
          <span class="report-id">#{{ report.id }}</span>
          <span :class="['status', report.status.toLowerCase()]">{{ report.status }}</span>
        </div>

        <div class="report-body">
          <p><strong>举报人：</strong>{{ report.reporterName }}</p>
          <p><strong>目标类型：</strong>{{ report.targetType }}</p>
          <p><strong>目标ID：</strong>{{ report.targetId }}</p>
          <p><strong>原因：</strong>{{ reasonLabels[report.reason] || report.reason }}</p>
          <p v-if="report.description"><strong>描述：</strong>{{ report.description }}</p>
          <p><strong>时间：</strong>{{ new Date(report.createdAt).toLocaleString() }}</p>
        </div>

        <div v-if="report.status === 'PENDING'" class="report-actions">
          <div v-if="reviewingId === report.id" class="review-form">
            <textarea v-model="reviewNote" placeholder="审核备注（可选）"></textarea>
            <div class="review-buttons">
              <button class="btn-approve" @click="reviewReport(report.id, 'REVIEWED')">通过</button>
              <button class="btn-dismiss" @click="reviewReport(report.id, 'DISMISSED')">驳回</button>
              <button class="btn-cancel" @click="reviewingId = null">取消</button>
            </div>
          </div>
          <button v-else class="btn-review" @click="reviewingId = report.id">审核</button>
        </div>

        <div v-else-if="report.reviewedAt" class="review-info">
          <p><strong>审核人：</strong>{{ report.reviewerName }}</p>
          <p v-if="report.reviewNote"><strong>备注：</strong>{{ report.reviewNote }}</p>
          <p><strong>审核时间：</strong>{{ new Date(report.reviewedAt).toLocaleString() }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  margin-bottom: 20px;
}

.filter-bar {
  margin-bottom: 20px;
}

.filter-bar select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.loading,
.empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-card {
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 16px;
  background: #fff;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.report-id {
  font-weight: 600;
  color: #666;
}

.status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status.pending {
  background: #fff3cd;
  color: #856404;
}

.status.reviewed {
  background: #d4edda;
  color: #155724;
}

.status.dismissed {
  background: #f8d7da;
  color: #721c24;
}

.status.resolved {
  background: #d1ecf1;
  color: #0c5460;
}

.report-body p {
  margin: 4px 0;
  font-size: 14px;
}

.report-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.review-form textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  resize: vertical;
  min-height: 60px;
  margin-bottom: 8px;
}

.review-buttons {
  display: flex;
  gap: 8px;
}

button {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.btn-review {
  background: #007bff;
  color: #fff;
}

.btn-approve {
  background: #28a745;
  color: #fff;
}

.btn-dismiss {
  background: #dc3545;
  color: #fff;
}

.btn-cancel {
  background: #6c757d;
  color: #fff;
}

.review-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
  font-size: 13px;
  color: #666;
}

.review-info p {
  margin: 2px 0;
}
</style>
