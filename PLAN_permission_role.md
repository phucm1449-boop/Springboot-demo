# Plan: Permission Entity + ManyToMany Role Authorization

## Context
The app currently has Role-based auth (`ROLE_ADMIN`, `ROLE_USER`) but no granular Permission layer.
`Permission` and `Role` entities both exist, and `Role` already declares `@ManyToMany Set<Permission>`,
but the relationship is incomplete (no `@JoinTable`, `Permission` has no `name` field, Spring Security
doesn't load permissions). Goal: complete the model, expose a full CRUD API, and wire permissions into
`@PreAuthorize` checks on controllers.

---

## Step-by-step implementation

### 1. Fix `Permission` entity
**File:** `src/main/java/com/example/demo/models/Permission.java`
- Add `String name` column (`nullable = false`, unique) — machine-readable key used in `hasAuthority()`
- Add `@ManyToMany(mappedBy = "permissions") Set<Role> roles` (bidirectional inverse side)
- Keep `description` field

### 2. Fix `Role` entity
**File:** `src/main/java/com/example/demo/models/Role.java`
- Add `@JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))` on the existing `@ManyToMany`
- Add `fetch = FetchType.EAGER` so permissions are loaded with the role

### 3. Update `User.getAuthorities()`
**File:** `src/main/java/com/example/demo/models/User.java`
- Currently returns only `ROLE_ADMIN` / `ROLE_USER`
- Also add each permission name from `role.getPermissions()` as a `SimpleGrantedAuthority` (no prefix)
- This lets `@PreAuthorize("hasAuthority('CREATE_PRODUCT')")` work alongside role checks

### 4. Add `PermissionRepo`
**File:** `src/main/java/com/example/demo/repo/PermissionRepo.java`
- `JpaRepository<Permission, Long>`
- `Optional<Permission> findByName(String name)`
- `boolean existsByName(String name)`

### 5. Add `PermissionDTO`
**File:** `src/main/java/com/example/demo/dto/PermissionDTO.java`
- Fields: `@NotBlank String name`, `@NotBlank String description`
- Follow `CategoryDTO` pattern

### 6. Add `IPermissionService` + `PermissionService`
**Files:**
- `src/main/java/com/example/demo/services/IPermissionService.java`
- `src/main/java/com/example/demo/services/PermissionService.java`

Methods:
- `Permission createPermission(PermissionDTO dto)` — check duplicate by name, throw `AppException(ErrorCode.DATA_EXISTED)`
- `List<Permission> getAllPermissions()`
- `void deletePermission(Long id)` — check exists, throw `AppException(ErrorCode.DATA_NOT_FOUND)`

### 7. Add `PermissionController`
**File:** `src/main/java/com/example/demo/controller/PermissionController.java`
- Base path: `api/v1/permissions`
- All endpoints require ADMIN (`@PreAuthorize("hasRole('ADMIN')")` at class level)
- `POST /` — create permission
- `GET /` — list all permissions
- `DELETE /{id}` — delete permission
- Use `ApiResponse<T>` wrapper (follow `UserController` pattern)

### 8. Add `RoleService` + `RoleController`
**Files:**
- `src/main/java/com/example/demo/services/IRoleService.java`
- `src/main/java/com/example/demo/services/RoleService.java`
- `src/main/java/com/example/demo/controller/RoleController.java`

RoleService methods:
- `Role assignPermission(Long roleId, Long permissionId)` — add permission to role's set
- `Role removePermission(Long roleId, Long permissionId)` — remove permission from role's set
- `List<Role> getAllRoles()`

RoleController base path: `api/v1/roles`, all ADMIN-only:
- `GET /` — list roles (with their permissions)
- `POST /{roleId}/permissions/{permissionId}` — assign permission to role
- `DELETE /{roleId}/permissions/{permissionId}` — remove permission from role

### 9. Seed initial data in `ApplicationConfig`
**File:** `src/main/java/com/example/demo/config/ApplicationConfig.java`
- Add `PermissionRepo` injection
- Seed permissions if not present (check by name): `CREATE_PRODUCT`, `UPDATE_PRODUCT`, `DELETE_PRODUCT`, `VIEW_ALL_USERS`
- Assign all permissions to ADMIN role; assign `CREATE_PRODUCT` and `UPDATE_PRODUCT` to USER role
- Do this before seeding the admin user

### 10. Apply `@PreAuthorize` to existing controllers
**Files:** `ProductController.java`, `UserController.java`
- `POST /api/v1/products` → `hasAuthority('CREATE_PRODUCT')`
- `PUT /api/v1/products/{id}` → `hasAuthority('UPDATE_PRODUCT')`
- `DELETE /api/v1/products/{id}` → `hasAuthority('DELETE_PRODUCT')`
- `GET /api/v1/users` → keep `hasRole('ADMIN')` (already enforced in `UserService`)

### 11. Database SQL migration
Apply manually to MySQL (since `ddl-auto: none`):
```sql
ALTER TABLE permission ADD COLUMN name VARCHAR(100) NOT NULL UNIQUE;

CREATE TABLE IF NOT EXISTS role_permissions (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  FOREIGN KEY (role_id) REFERENCES roles(id),
  FOREIGN KEY (permission_id) REFERENCES permission(id)
);
```

---

## Files to create (new)
| File | Purpose |
|------|---------|
| `repo/PermissionRepo.java` | JPA repo for Permission |
| `dto/PermissionDTO.java` | Inbound DTO |
| `services/IPermissionService.java` | Interface |
| `services/PermissionService.java` | Implementation |
| `controller/PermissionController.java` | REST API |
| `services/IRoleService.java` | Interface |
| `services/RoleService.java` | Implementation |
| `controller/RoleController.java` | REST API |

## Files to modify (existing)
| File | Change |
|------|--------|
| `models/Permission.java` | Add `name` field + inverse `roles` set |
| `models/Role.java` | Add `@JoinTable` + `FetchType.EAGER` |
| `models/User.java` | Update `getAuthorities()` to include permission names |
| `config/ApplicationConfig.java` | Seed permissions and assign to roles |
| `controller/ProductController.java` | Add `@PreAuthorize` on write endpoints |

---

## Verification
1. Run `./mvnw spring-boot:run` — confirm startup seeds permissions (check log)
2. `POST /api/v1/users/login` with admin credentials → get JWT
3. `GET /api/v1/permissions` with ADMIN JWT → returns 4 seeded permissions
4. `POST /api/v1/permissions` → create a new permission
5. `POST /api/v1/roles/{roleId}/permissions/{permissionId}` → assign to USER role
6. Login as a USER-role account → `DELETE /api/v1/products/1` returns 403 FORBIDDEN
7. `POST /api/v1/products` as USER → succeeds (has `CREATE_PRODUCT`)
