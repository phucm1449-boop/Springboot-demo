# Spring Security Flow (Beginner Guide)

This project uses **JWT + Spring Security** with a **stateless** flow.

## 1) Main Security Components in This Project

- `SecurityConfig`:
  - Provides `UserDetailsService` (load user by phone number).
  - Provides `PasswordEncoder` (`BCryptPasswordEncoder`).
  - Provides `AuthenticationProvider` (`DaoAuthenticationProvider`).
  - Provides `AuthenticationManager` (used during login).

- `WebSecurityConfig`:
  - Defines which APIs are public and which require authentication.
  - Sets session policy to `STATELESS` (no server session).
  - Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

- `JwtAuthenticationFilter`:
  - Runs on each protected request.
  - Reads `Authorization: Bearer <token>`.
  - Validates token and sets user info into `SecurityContextHolder`.

- `JwtTokenUtil`:
  - Creates JWT after successful login.
  - Extracts phone number from JWT.
  - Validates token signature + expiration.

## 2) Public vs Protected APIs

### Public APIs (no token required)

- `POST /api/v1/users/register`
- `POST /api/v1/users/login`
- `GET /api/v1/categories/**`
- `GET /api/v1/products/**`
- Swagger docs:
  - `/v3/api-docs/**`
  - `/swagger-ui/**`
  - `/swagger-ui.html`

### Protected APIs (token required)

- Every other endpoint (for example create/update/delete orders, products, categories).

## 3) Flow A: Register

1. Client calls `POST /api/v1/users/register`.
2. `UserService.createUser(...)` checks:
   - Phone number does not exist.
   - Role is not `ADMIN`.
3. Password is encoded with BCrypt (for normal account).
4. User is saved in database.

Important:
- Storing hashed password is required so Spring Security can verify login safely.

## 4) Flow B: Login

1. Client calls `POST /api/v1/users/login` with phone number + password.
2. `UserService.login(...)` loads the user from database.
3. Password is checked with `PasswordEncoder.matches(...)`.
4. `AuthenticationManager.authenticate(...)` runs Spring Security authentication.
5. If success, `JwtTokenUtil.generateToken(...)` returns JWT.
6. Client stores token and sends it in next requests:
   - `Authorization: Bearer <jwt>`

## 5) Flow C: Access Protected API

1. Request enters `SecurityFilterChain`.
2. `JwtAuthenticationFilter` executes first.
3. Filter reads and validates JWT.
4. If valid, filter sets `Authentication` into `SecurityContextHolder`.
5. Authorization step checks if endpoint requires authentication.
6. Controller executes only when request is authorized.

If token is missing/invalid on protected API:
- Response is `401 Unauthorized`.

## 6) Why Stateless?

- Server does not keep login session in memory.
- Each request is self-contained (token included).
- Better fit for REST APIs and scalable systems.

## 7) Practical Testing Steps

1. Register user: `POST /api/v1/users/register`
2. Login: `POST /api/v1/users/login`
3. Copy returned JWT.
4. Call protected API with header:
   - `Authorization: Bearer <jwt>`
5. Try the same call without token to verify `401`.

## 8) Quick Mental Model

- `register` -> create account with encoded password.
- `login` -> verify credentials -> issue JWT.
- `protected request` -> validate JWT -> allow/deny.
