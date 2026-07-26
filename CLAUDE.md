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

**Spring Boot 3.5.6 / Java 17** REST API for an e-commerce shop. Base URL prefix: `api/v1`. MySQL database named `shopapp` (port 3306). Schema is **not auto-managed** (`ddl-auto: none`) — DDL changes must be applied manually.

### Package layout

```
com.example.demo
├── config/          # SecurityConfig (auth provider, UserDetailsService), ApplicationConfig (ModelMapper, admin seed)
├── component/       # JwtTokenUtil — token generation/validation
├── controller/      # REST controllers: User, Product, Category, Order, OrderDetail
├── dto/             # Inbound request payloads
├── models/          # JPA entities (all extend BaseEntity for createdAt/updatedAt, except Order)
├── repo/            # Spring Data JPA repositories
├── responses/       # Outbound response wrappers; ApiResponse<T> is the standard envelope
├── services/        # Interface + impl pairs (IXxxService / XxxService)
├── mapper/          # DTOMapper (MapStruct) — compile-time generated in target/generated-sources
├── exceptions/      # AppException wraps ErrorCode enum; thrown everywhere, resolved centrally
└── filter/          # JwtAuthenticationFilter — OncePerRequestFilter that validates Bearer tokens
```

### Authentication & authorization

- **Identity** is phone number, not email. `User.getUsername()` returns `phoneNumber`.
- JWT secret and expiration are in `application.yml` (`jwt.secretKey`, `jwt.expiration` in seconds; default 30 days).
- `JwtAuthenticationFilter.shouldNotFilter` controls which paths skip the JWT check:
  - Always public: `POST /api/v1/users/register`, `POST /api/v1/users/login`, Swagger paths.
  - Public for GET only: `/api/v1/categories`, `/api/v1/products`, `/api/v1/orders`.
  - `/api/v1/users` GET endpoints always require authentication.
- Method-level security uses `@PreAuthorize("hasRole('ADMIN')")` and `@PostAuthorize`.
- On startup, `ApplicationConfig` seeds one ADMIN user (phone `0856599009`, password `123456789`) if none exists.
- **Logout** stores the revoked JWT's `id` claim in the `InvalidatedToken` table; `JwtTokenUtil` checks this table on every request to reject invalidated tokens.

### Error handling

All errors use `AppException(ErrorCode)`. `ErrorCode` is an enum that bundles an integer code, message string, and `HttpStatus`. Add new error cases there rather than throwing generic exceptions. Success responses use code `1000`. `GlobalExceptionHandler` (`@RestControllerAdvice`) maps `MethodArgumentNotValidException` field names through `ErrorCode` attribute lookup — validation messages must therefore match enum attribute names.

### Response pattern

New controllers return `ApiResponse<T>` (standard JSON envelope). Some older endpoints in `ProductController` still use `ResponseEntity<?>` — prefer `ApiResponse<T>` for new endpoints. `ProductResponse`, `UserResponse`, `OrderResponse`, etc. extend `BaseResponse` (carries `createdAt`/`updatedAt`).

### Data patterns

- **Soft delete**: `Order` is never hard-deleted; `DELETE /orders/{id}` sets `active = false`.
- **Bidirectional JPA**: `Order` ↔ `OrderDetail` uses `@JsonManagedReference` / `@JsonBackReference` to prevent serialization cycles.
- **Pagination**: Product listing uses `Pageable`; results are wrapped in `ProductListResponse` which includes `totalPages`.
- Some controllers use `${api.prefix}` property placeholder, others hardcode `/api/v1` — check both styles when adding endpoints.

### MapStruct + Lombok

Both processors run via `maven-compiler-plugin` annotation processor paths. The generated `DTOMapperImpl` lands in `target/generated-sources/annotations/`. When adding new `@Mapper` methods, note the existing mapping quirk: `ProductDTO.name` maps to `Product.description` (see `DTOMapper`). `OrderService` uses `ModelMapper` (not MapStruct) for complex nested mappings including `OrderDTO.userId → Order.user.id`.

### Validation

`@DobConstraint` is a custom validator (`DobValidator`) that enforces a minimum age on date-of-birth fields — its `min` attribute is configurable per field. Standard DTO constraints: phone number exactly 10 characters, password minimum 8 characters, product name 3–200 characters.

### Image uploads

Product images are stored in an `uploads/` directory relative to the working directory (created at runtime). File names are prefixed with a UUID to prevent collisions. Max 5 images per product (`ProductImage.MAXIMUM_IMAGES_PER_PRODUCT`), max 10 MB per file. Only image MIME types are accepted.
