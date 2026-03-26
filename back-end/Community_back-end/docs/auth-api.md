# Auth API

Base URL: `/api/auth`

## 1) Register

- **URL**: `POST /api/auth/register`
- **Description**: Register a new user.
- **Rules**:
  - `username` is required.
  - `email` is optional.
  - Backend generates a random 5-digit numeric user id.
  - Password is stored with BCrypt hash.

### Request Body

```json
{
  "username": "alice",
  "password": "123456",
  "email": "alice@example.com"
}
```

### Success Response

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 53827,
    "username": "alice",
    "nickname": "alice",
    "email": "alice@example.com",
    "avatar": null,
    "backgroundImage": null,
    "bio": null,
    "gender": null,
    "birthday": null,
    "location": null,
    "followersCount": 0,
    "followingCount": 0,
    "postsCount": 0,
    "status": 1,
    "lastLoginAt": null,
    "createdAt": "2026-03-26T14:10:30"
  }
}
```

### Error Codes

- `1001`: username is required
- `1002`: password must be at least 6 characters
- `1003`: email format is invalid
- `1004`: username already exists
- `1005`: email already exists
- `1007`: failed to generate user id

---

## 2) Login

- **URL**: `POST /api/auth/login`
- **Description**: Login with username, 5-digit id, or email.

### Request Body

```json
{
  "identifier": "alice",
  "password": "123456"
}
```

`identifier` examples:
- Username: `alice`
- User id: `53827`
- Email: `alice@example.com`

### Success Response

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 53827,
    "username": "alice",
    "nickname": "alice",
    "email": "alice@example.com",
    "avatar": null,
    "backgroundImage": null,
    "bio": null,
    "gender": null,
    "birthday": null,
    "location": null,
    "followersCount": 0,
    "followingCount": 0,
    "postsCount": 0,
    "status": 1,
    "lastLoginAt": "2026-03-26T14:20:00",
    "createdAt": "2026-03-26T14:10:30"
  }
}
```

### Error Codes

- `400`: bad request
- `1006`: username/id/email or password is incorrect

---

## 3) Get User By Id (existing)

- **URL**: `GET /api/auth/users/{id}`
- **Description**: Query user profile by id.
- **Example**: `GET /api/auth/users/53827`

---

## Common Response Wrapper

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

