# 发布文章接口文档（Front-end）

## 1. 接口概览

- 接口名称：发布文章
- 请求方式：`POST`
- 请求路径：`/api/posts`
- Content-Type：`application/json`
- 鉴权方式：建议携带 `Authorization: Bearer <token>`（如项目已启用登录态）

## 2. 请求参数

### 2.1 请求体（JSON）

```json
{
  "userId": 1,
  "title": "文章标题",
  "content": "# Markdown内容",
  "summary": "摘要（可选）",
  "coverImage": "封面URL（可选）"
}
```

### 2.2 字段说明

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `userId` | number | 是 | 当前登录用户 ID |
| `title` | string | 是 | 文章标题，不能为空 |
| `content` | string | 是 | 文章正文，Markdown 格式，不能为空 |
| `summary` | string | 否 | 文章摘要；为空时由后端自动从 `content` 前 100 字符生成 |
| `coverImage` | string | 否 | 封面图片 URL |

## 3. 响应结果

### 3.1 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

### 3.2 失败响应

```json
{
  "code": 400,
  "message": "参数错误"
}
```

### 3.3 响应字段说明

| 字段名 | 类型 | 是否必有 | 说明 |
| --- | --- | --- | --- |
| `code` | number | 是 | 业务状态码，`200` 表示成功 |
| `message` | string | 是 | 响应描述信息 |
| `data` | object / null | 否 | 成功时返回数据对象，失败时可能为空 |
| `data.id` | number | 否 | 新创建文章 ID（成功时返回） |

## 4. 前端调用示例（Vue + axios）

```js
// api/post.js
import axios from "axios";

const http = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 10000
});

// 可选：请求拦截器附带 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 发布文章
 * @param {Object} payload
 * @param {number} payload.userId
 * @param {string} payload.title
 * @param {string} payload.content
 * @param {string=} payload.summary
 * @param {string=} payload.coverImage
 */
export function createPost(payload) {
  // 提交前校验
  if (!payload.title || !payload.title.trim()) {
    return Promise.reject(new Error("标题不能为空"));
  }
  if (!payload.content || !payload.content.trim()) {
    return Promise.reject(new Error("内容不能为空"));
  }

  return http.post("/api/posts", {
    userId: payload.userId,
    title: payload.title.trim(),
    content: payload.content,
    summary: payload.summary?.trim() || "",
    coverImage: payload.coverImage?.trim() || ""
  });
}
```

```vue
<!-- 示例：在 Vue 组件中调用 -->
<script setup>
import { ref } from "vue";
import { createPost } from "@/api/post";

const form = ref({
  userId: 1,
  title: "",
  content: "",
  summary: "",
  coverImage: ""
});

const loading = ref(false);

const handleSubmit = async () => {
  try {
    loading.value = true;
    const res = await createPost(form.value);

    if (res.data.code === 200) {
      const postId = res.data.data?.id;
      console.log("发布成功，文章ID：", postId);
      // 可跳转详情页：/posts/${postId}
    } else {
      console.error("发布失败：", res.data.message);
    }
  } catch (err) {
    console.error("请求异常：", err.message || err);
  } finally {
    loading.value = false;
  }
};
</script>
```

## 5. 注意事项

1. `content` 为 Markdown，前端展示时需使用 Markdown 渲染器（如 `markdown-it`、`marked`）。
2. 提交前必须校验 `title`、`content` 非空，避免无效请求。
3. 图片（封面或正文内图片）建议先走上传接口获取 URL，再插入到 Markdown。
4. `summary` 可不传或传空字符串，由后端自动生成摘要。
5. 建议统一处理 `code !== 200` 的业务失败提示，避免只按 HTTP 状态码判断。
