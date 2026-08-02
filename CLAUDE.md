# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
mvnw.cmd clean install

# Run (starts on port 8088)
mvnw.cmd spring-boot:run

# Run all tests
mvnw.cmd test

# Run a single test class
mvnw.cmd test -Dtest=DemoApplicationTests
```

Swagger UI is available at `http://localhost:8088/swagger-ui.html` when the app is running.

## Architecture

**Spring Boot 3.5.6 / Java 17** REST API for an e-commerce shop. Base URL prefix: `api/v1`. MySQL database named `shopapp` (port 3306). Schema is **not auto-managed** (`ddl-auto: none`) — DDL changes must be applied manually. There is no `README.md`; several root-level `.md` files (`SPRING_SECURITY_FLOW.md`, `PLAN_permission_role.md`, `Task.md`, `ReviewNotes.md`, `oauth2ResourceServer_guide*.md`, `docs/security-dual-gate.md`) capture design history and are worth skimming when touching security or roles.

### Package layout

```
com.example.demo
├── config/          # SecurityConfig (auth provider, UserDetailsService), WebSecurityConfig (SecurityFilterChain, URL-level authorization), ApplicationConfig (ModelMapper, admin seed)
├── component/       # JwtTokenUtil — token generation/validation
├── controller/      # REST controllers: User, Product, Category, Order, OrderDetail
├── dto/             # Inbound request payloads
├── models/          # JPA entities (all extend BaseEntity for createdAt/updatedAt, except Order); also Role, Permission (unused), Token, SocialAccount, InvalidatedToken, OrderStatus
├── repo/            # Spring Data JPA repositories
├── responses/       # Outbound response wrappers; ApiResponse<T> is the standard envelope
├── services/        # Interface + impl pairs (IXxxService / XxxService)
├── mapper/          # DTOMapper (MapStruct) — compile-time generated in target/generated-sources
├── exceptions/      # AppException wraps ErrorCode enum; thrown everywhere, resolved centrally
├── validator/       # @DobConstraint / DobValidator — custom min-age validation
└── filter/          # JwtAuthenticationFilter — OncePerRequestFilter that validates Bearer tokens
```

There is also a stray `training_unittest` package (`MathService`/`MathServiceTest`) under `src/main` and `src/test` — unrelated scratch/training code, not part of the shop domain.

### Authentication & authorization — two independent gates

- **Identity** is phone number, not email. `User.getUsername()` returns `phoneNumber`.
- `Role.ADMIN` / `Role.USER` are public static mutable `String` fields, not a Java enum.
- JWT config keys are `jwt.valid-duration` and `jwt.refreshable-duration` in `application.yml` (seconds). The dev default is very short (60s / 120s) — treat as a debugging leftover, not a real expiration policy; `application-production.yml` overrides to 7300s/72000s. (`jwt.secretKey`/`jwt.expiration` do **not** exist as active keys.)
- On startup, `ApplicationConfig` seeds one ADMIN user (phone `0856599009`, password `123456789`) **if no user has the ADMIN role** — it checks by role, not by phone, so deleting/renaming that user allows a different ADMIN to be seeded again.
- **Logout** stores the revoked JWT's `id` claim in the `InvalidatedToken` table; `JwtTokenUtil` checks this table on every request to reject invalidated tokens.

Every request passes through **two separate, independently-configured** security layers, both of which must agree for a path to be truly public — this has already caused confusion (see `docs/security-dual-gate.md`):

1. **Gate 1 — `JwtAuthenticationFilter.shouldNotFilter`** (`PUBLIC_PATHS`): controls whether the JWT token is validated at all.
   - Always public: `POST /api/v1/users/register`, `POST /api/v1/users/login`, Swagger paths.
   - Public for GET only: `/api/v1/categories`, `/api/v1/products`, `/api/v1/orders`.
   - `/api/v1/users` GET endpoints always require authentication.
   - Skipping this filter only means "don't require a token" — it does **not** populate `SecurityContext`, so it does not by itself grant access.
2. **Gate 2 — `WebSecurityConfig.authorizeHttpRequests`** (`PUBLIC_ENPOINTS` list + per-method `hasRole(...)` matchers): controls whether Spring Security's authorization layer allows the (possibly anonymous) request through. `anyRequest().authenticated()` rejects anonymous requests with 401 even if Gate 1 skipped them.

A path must be open in **both** lists to be genuinely anonymous-accessible. Also note: **URL-level `hasRole(...)` rules in `WebSecurityConfig` currently shadow method-level `@PreAuthorize`** on controllers — the filter chain rejects (or allows) before the request ever reaches the controller, so a role-mismatched authenticated user gets a plain 401/403 instead of the `GlobalExceptionHandler`-formatted 403 that `@PreAuthorize` would produce. When adding new protected endpoints, prefer one mechanism consistently (`@PreAuthorize` + no URL rule, or a URL rule + no `@PreAuthorize`) rather than mixing both for the same path.

### Error handling

All errors use `AppException(ErrorCode)`. `ErrorCode` is an enum that bundles an integer code, message string, and `HttpStatus`. Add new error cases there rather than throwing generic exceptions. Success responses use code `1000`. `GlobalExceptionHandler` (`@RestControllerAdvice`) maps `MethodArgumentNotValidException` field names through `ErrorCode` attribute lookup — validation messages must therefore match enum attribute names.

### Response pattern

New controllers return `ApiResponse<T>` (standard JSON envelope). Some older endpoints in `ProductController` still use `ResponseEntity<?>` — prefer `ApiResponse<T>` for new endpoints. `ProductResponse`, `UserResponse`, `OrderResponse`, etc. extend `BaseResponse` (carries `createdAt`/`updatedAt`).

`ProductController` currently has **two separate create-product endpoints** (`POST /new`, `@PreAuthorize`-guarded, returns `ApiResponse<Product>`; and `POST ""`, no method-level auth, returns raw `ResponseEntity<?>` with manual `BindingResult` handling) — likely leftover/dead code. Be aware of this duplication before adding another product-creation path.

### Data patterns

- **Soft delete**: `Order` is never hard-deleted; `DELETE /orders/{id}` sets `active = false`.
- **Bidirectional JPA**: `Order` ↔ `OrderDetail` uses `@JsonManagedReference` / `@JsonBackReference` to prevent serialization cycles.
- **Pagination**: Product listing uses `Pageable`; results are wrapped in `ProductListResponse` which includes `totalPages`.
- Some controllers use `${api.prefix}` property placeholder (`OrderController`, `OrderDetailController`), others hardcode `/api/v1` (`UserController`, `CategoryController`, `ProductController`) — check both styles when adding endpoints.

### MapStruct + Lombok

Both processors run via `maven-compiler-plugin` annotation processor paths. The generated `DTOMapperImpl` lands in `target/generated-sources/annotations/`. When adding new `@Mapper` methods, note the existing mapping quirk: `ProductDTO.name` maps to `Product.description` (see `DTOMapper`). `OrderService` uses `ModelMapper` (not MapStruct) for complex nested mappings including `OrderDTO.userId → Order.user.id`.

### Validation

`@DobConstraint` is a custom validator (`DobValidator`) that enforces a minimum age on date-of-birth fields — its `min` attribute is configurable per field; a null date of birth passes validation. Standard DTO constraints: password minimum 8 characters, product name 3–200 characters. Note: phone number has **no length/pattern validation at the DTO level** despite the `User` entity column being `length = 10` — an overlong phone number passes request validation and only fails (or is truncated, depending on the DB) at persistence time.

### Image uploads

Product images are stored in an `uploads/` directory relative to the working directory (created at runtime). File names are prefixed with a UUID to prevent collisions. Max 5 images per product (`ProductImage.MAXIMUM_IMAGES_PER_PRODUCT`), max 10 MB per file. Only image MIME types are accepted.
