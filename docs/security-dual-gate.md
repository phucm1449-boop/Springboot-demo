# why do we need PUBLIC_PATHS in JwtAuthenticationFilter and PUBLIC_ENPOINTS in WebSecurityConfig ? 
because when i only add the  url /api/v1/users/logout to PUBLIC_PATHS in JwtAuthenticationFilter and    
i access to POST JwtAuthenticationFilter  /api/v1/users/logout, it gets error UNAUTHENTICATED 1005. 
but when i add the  url /api/v1/users/logout to both PUBLIC_PATHS in JwtAuthenticationFilter and      PUBLIC_ENPOINTS in WebSecurityConfig, i can access. Why ?

# Why Both PUBLIC_PATHS and PUBLIC_ENPOINTS Are Needed

## The Two Independent Security Gates

Every incoming request passes through two separate security layers in order:

```
Request
  │
  ▼
[Gate 1] JwtAuthenticationFilter.shouldNotFilter()
         → true  : skip JWT check entirely, SecurityContext stays EMPTY (anonymous)
         → false : run doFilterInternal() — validate token, populate SecurityContext
  │
  ▼
[Gate 2] WebSecurityConfig authorizeHttpRequests
         .permitAll()      : allow even anonymous requests  ✓
         .authenticated()  : reject anonymous requests      ✗  → 401
  │
  ▼
Controller
```

---

## Why Only PUBLIC_PATHS Is Not Enough

When you add `/api/v1/users/logout` only to `PUBLIC_PATHS` in `JwtAuthenticationFilter`:

- **Gate 1**: `shouldNotFilter()` returns `true` → JWT filter is skipped → no token is validated → `SecurityContext` is **empty (anonymous user)**
- **Gate 2**: `anyRequest().authenticated()` in `WebSecurityConfig` sees an anonymous request and **rejects it with 401** → this produces the `UNAUTHENTICATED 1005` error

Skipping the JWT filter does **not** grant access. It only means "don't require a token." Spring Security's authorization layer still runs afterward and enforces its own rules independently.

---

## What Each List Controls

| List | File | Layer | Controls |
|---|---|---|---|
| `PUBLIC_PATHS` | `JwtAuthenticationFilter.java` | Servlet Filter | Whether the **JWT token is validated** |
| `PUBLIC_ENPOINTS` | `WebSecurityConfig.java` | Spring Security Authorization | Whether **unauthenticated (anonymous) requests are allowed** |

---

## Why Both Are Required

Adding a path to **both** lists opens both gates:

1. **Gate 1** (`PUBLIC_PATHS`): JWT filter is skipped — no token needed in the `Authorization` header
2. **Gate 2** (`PUBLIC_ENPOINTS` → `.permitAll()`): Spring Security explicitly allows the request even though `SecurityContext` is empty

If you only open Gate 1 but leave Gate 2 closed (`anyRequest().authenticated()`), the request is still rejected at Gate 2 because there is no authentication in the `SecurityContext`.

---

## Key Takeaway

`shouldNotFilter = true` only stops the JWT filter from running. It does **not** populate `SecurityContext` with any authentication. Spring Security's authorization check still fires afterward and rejects anonymous requests unless `.permitAll()` is configured for that path in `WebSecurityConfig`.

##  when i access to PUT http://localhost:8088/api/v1/orders/5 with token role USER. Postman gets 401
## Unauthorized with JSON body {
## "code": 1005,
## "message": "Unauthenticated"
## }. but i expect Postman gets 403
## IF i use @PreAuthorize("hasRole('ADMIN')"), Does it work ?

Yes, @PreAuthorize would fix it — but only if you also remove the conflicting URL-level rule in WebSecurityConfig. Here's why:

Current flow (URL-level security):
    Request → AuthorizationFilter (filter chain) → access denied
                → ExceptionTranslationFilter
                → user anonymous(ẩn danh)? → Yes → authenticationEntryPoint → 401
                                           → No  → accessDeniedHandler (default, no JSON) → plain 403
The filter chain fires before the request reaches the controller, so @PreAuthorize never triggers.

With @PreAuthorize (after removing the URL rule):
    Request → anyRequest().authenticated() check
                → anonymous? → 401 ✓
                → authenticated USER? → reaches controller method
                                      → @PreAuthorize fails → AuthorizationDeniedException
                                      → GlobalExceptionHandler.handleAuthorizationDeniedException
                                      → {"code": 1013, "message": "You do not have permission"} + 403 ✓

So the fix is two steps:

Step 1 — Remove the URL-level ADMIN rule for PUT orders in WebSecurityConfig.java:52:
// Remove this line:
.requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasRole(Role.ADMIN)
(anyRequest().authenticated() already covers it — any authenticated user gets past the filter, then @PreAuthorize does the role check.)

Step 2 — Add @PreAuthorize to the controller method:
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
// Remove this line:
.requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasRole(Role.ADMIN)
(anyRequest().authenticated() already covers it — any authenticated user gets past the filter, then @PreAuthorize does the role check.)


This way:
- No token → 401 (caught at filter level by anyRequest().authenticated())
- USER token → 403 {"code": 1013, "message": "You do not have permission"} (caught by GlobalExceptionHandler)
- ADMIN token → 200 ✓
