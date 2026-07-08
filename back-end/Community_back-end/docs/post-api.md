# Post API (Frontend Integration)

Base URL: `/api/posts`

If the backend runs locally with default settings, use `http://localhost:8080/api/posts`.

## Update Highlights

- `POST /api/posts` requires login.
- `createPost` does **not** accept `userId` from the request body.
- Backend reads the current user id from the authenticated principal.
- `GET /api/posts` and `GET /api/posts/{id}` are public.
- `POST /api/posts/cover-image` is used to upload a cover image and returns a public URL.

## Global Response Format

All business APIs return this wrapper:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

- `code = 0`: success.
- `code != 0`: business error.

## 1) Create Post

- **Method**: `POST`
- **URL**: `/api/posts`
- **Auth**: required
- **Content-Type**: `application/json`

### Header

```http
Authorization: Bearer <accessToken>
```

### Request Body

```json
{
  "title": "My first post",
  "content": "Hello world",
  "summary": "Optional summary",
  "coverImage": "/post-covers/1711880000000_xxx.jpg"
}
```

### Field Rules

- `title`: required, non-empty.
- `content`: required, non-empty.
- `summary`: optional.
- `coverImage`: optional.
- `userId`: **must not be sent**; backend derives it from the login token.

### Success Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "postId": 20001
  }
}
```

### Business Error Codes

- `400`: bad request (missing body / missing auth / malformed JSON).
- `1001`: title is required.
- `1002`: content is required.

## 2) List Posts

- **Method**: `GET`
- **URL**: `/api/posts`
- **Auth**: none

### Success Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 20001,
      "userId": 53827,
      "authorName": "alice",
      "authorAvatar": "/avatars/53827_xxx.png",
      "title": "My first post",
      "summary": "Optional summary",
      "coverImage": "/post-covers/1711880000000_xxx.jpg",
      "createdAt": "2026-03-31T10:00:00"
    }
  ]
}
```

## 3) Get Post Detail

- **Method**: `GET`
- **URL**: `/api/posts/{id}`
- **Auth**: none

### Path Params

- `id`: post id.

### Success Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 20001,
    "userId": 53827,
    "authorName": "alice",
    "authorAvatar": "/avatars/53827_xxx.png",
    "title": "My first post",
    "content": "Hello world",
    "summary": "Optional summary",
    "coverImage": "/post-covers/1711880000000_xxx.jpg",
    "createdAt": "2026-03-31T10:00:00"
  }
}
```

## 4) Upload Post Cover Image

- **Method**: `POST`
- **URL**: `/api/posts/cover-image`
- **Auth**: none
- **Content-Type**: `multipart/form-data`

### Form Data

- `file`: image file, required.
- allowed types: `jpg`, `jpeg`, `png`, `gif`, `webp`.
- app-level default max size: `20MB` (`app.post-cover.max-size-bytes`).

### Success Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "url": "/post-covers/1711880000000_xxx.jpg"
  }
}
```

