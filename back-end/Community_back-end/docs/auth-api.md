Auth API (Frontend Integration)

Base URL: `/api/auth`

If the backend runs locally with default settings, use `http://localhost:8080/api/auth`.

## Update Highlights

- `POST /register` and `POST /login` are public endpoints.
- Both register and login now return JWT in the response body.
- Login supports username, 5-digit user id, and email.
- Register requires `username`; backend generates a random 5-digit id.
- Password is BCrypt hashed in backend.
- Added avatar upload/update endpoint for authenticated users.
- Added profile background image upload/update endpoint for authenticated users.

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

## 1) Register

- **Method**: `POST`
- **URL**: `/api/auth/register`
- **Auth**: none

### Request

```json
{
  "username": "alice",
  "password": "123456",
  "email": "alice@example.com"
}
```

### Validation Rules

- `username`: required, non-empty.
- `password`: required, at least 6 chars.
- `email`: optional, but must be valid when provided.

### Success Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 604800,
    "user": {
      "id": 53827,
      "username": "alice",
      "nickname": "alice",
      "email": "alice@example.com",
      "avatar": null,
      "backgroundImage": null,
      "bio": null,
      "gender": 0,
      "birthday": null,
      "location": null,
      "followersCount": 0,
      "followingCount": 0,
      "postsCount": 0,
      "status": 1,
      "lastLoginAt": null,
      "createdAt": "2026-03-30T12:00:00"
    }
  }
}
```

### Business Error Codes

- `400`: bad request (missing body / malformed JSON).
- `1001`: username is required.
- `1002`: password must be at least 6 characters.
- `1003`: email format is invalid.
- `1004`: username already exists.
- `1005`: email already exists.
- `1007`: failed to generate user id.

## 2) Login

- **Method**: `POST`
- **URL**: `/api/auth/login`
- **Auth**: none

### Request

```json
{
  "identifier": "alice",
  "password": "123456"
}
```

### `identifier` Supported Values

- username: `alice`
- 5-digit id: `53827`
- email: `alice@example.com`

### Success Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 604800,
    "user": {
      "id": 53827,
      "username": "alice"
    }
  }
}
```

### Business Error Codes

- `400`: bad request.
- `1006`: username/id/email or password is incorrect.

## 3) Get User By Id (Protected)

- **Method**: `GET`
- **URL**: `/api/auth/users/{id}`
- **Auth**: required

### Header

```http
Authorization: Bearer <accessToken>
```

### Business Error Codes

- `400`: bad request (`id` invalid).
- `404`: resource not found.

## 4) Upload Avatar (Protected)

- **Method**: `POST`
- **URL**: `/api/auth/users/me/avatar`
- **Auth**: required
- **Content-Type**: `multipart/form-data`

### Header

```http
Authorization: Bearer <accessToken>
```

### Form Data

- `file`: image file, required.
- allowed types: `jpg`, `jpeg`, `png`, `gif`, `webp`.
- app-level default max size: `5MB` (`app.avatar.max-size-bytes`).
- servlet multipart default max size: `5MB` (`spring.servlet.multipart.max-file-size`).

### Example (curl)

```bash
curl -X POST "http://localhost:8080/api/auth/users/me/avatar" \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@C:/images/avatar.png"
```

### Success Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 53827,
    "username": "alice",
    "nickname": "alice",
    "email": "alice@example.com",
    "avatar": "/avatars/53827_1711880000000_6f0b8af24bde4c08a63f89f9a4328f67.png",
    "backgroundImage": null,
    "bio": null,
    "gender": 0,
    "birthday": null,
    "location": null,
    "followersCount": 0,
    "followingCount": 0,
    "postsCount": 0,
    "status": 1,
    "lastLoginAt": "2026-03-31T10:00:00",
    "createdAt": "2026-03-30T12:00:00"
  }
}
```

### Business Error Codes

- `400`: bad request (multipart format invalid).
- `1008`: avatar file is required.
- `1009`: avatar type is invalid.
- `1010`: avatar file size exceeds limit.
- `1011`: failed to upload avatar.

### Avatar Access URL

- Backend exposes avatar files at `/avatars/**` (public GET).
- If `data.avatar = "/avatars/xxx.png"`, frontend can directly load:
  `http://localhost:8080/avatars/xxx.png`.

## 5) Upload Background Image (Protected)

- **Method**: `POST`
- **URL**: `/api/auth/users/me/background-image`
- **Auth**: required
- **Content-Type**: `multipart/form-data`

### Header

```http
Authorization: Bearer <accessToken>
```

### Form Data

- `file`: image file, required.
- allowed types: `jpg`, `jpeg`, `png`, `gif`, `webp`.
- app-level default max size: `5MB` (`app.background.max-size-bytes`).
- servlet multipart default max size: `5MB` (`spring.servlet.multipart.max-file-size`).

### Example (curl)

```bash
curl -X POST "http://localhost:8080/api/auth/users/me/background-image" \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@C:/images/cover.png"
```

### Success Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 53827,
    "username": "alice",
    "nickname": "alice",
    "email": "alice@example.com",
    "avatar": "/avatars/53827_1711880000000_6f0b8af24bde4c08a63f89f9a4328f67.png",
    "backgroundImage": "/backgrounds/53827_1711880000001_09c1a9f14b8f4df3b0d96ac90cc1f2f8.png",
    "bio": null,
    "gender": 0,
    "birthday": null,
    "location": null,
    "followersCount": 0,
    "followingCount": 0,
    "postsCount": 0,
    "status": 1,
    "lastLoginAt": "2026-03-31T10:00:00",
    "createdAt": "2026-03-30T12:00:00"
  }
}
```

### Business Error Codes

- `400`: bad request (multipart format invalid).
- `1012`: background image file is required.
- `1013`: background image type is invalid.
- `1014`: background image file size exceeds limit.
- `1015`: failed to upload background image.

### Background Image Access URL

- Backend exposes background files at `/backgrounds/**` (public GET).
- If `data.backgroundImage = "/backgrounds/xxx.png"`, frontend can directly load:
  `http://localhost:8080/backgrounds/xxx.png`.

## Token Handling For Frontend

After register/login success:

1. Save `data.accessToken` and `data.tokenType`.
2. Send `Authorization: Bearer <accessToken>` for protected APIs.
3. Use `expiresIn` (seconds) to decide re-login timing (current default: `604800` = 7 days).

## Integration Notes

- No refresh-token endpoint yet.
- Token expires -> frontend should redirect user to login.
- No explicit server-side rate-limit response (`429`) in current auth module.
- `password` is never returned in response.
- Avatar local storage defaults to `uploads/avatars` and can be overridden by env vars.
