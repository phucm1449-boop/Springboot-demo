# Code Review Notes
_Date: 2026-06-16_

---

## Point 4 — Why `createUser_validRequest_success` in `UserServiceTest` failed

### Root cause (two problems compounded)

**Problem 1: `@SpringBootTest` boots the full Spring context**

`@SpringBootTest` tries to start the entire application, including JPA auto-configuration that connects to MySQL on port 3306. When MySQL is not running, the context fails to load before any test runs. The `CONDITIONS EVALUATION REPORT` in the test output is Spring printing its auto-config evaluation report on startup failure.

**Problem 2: `PasswordEncoder` was not mocked**

`UserService.createUser()` calls `passwordEncoder.encode(userDTO.getPassword())`. `PasswordEncoder` was never declared as a mock, so it would have been `null` → `NullPointerException`.

### Fix applied

Replaced `@SpringBootTest` (full context) with `@ExtendWith(MockitoExtension.class)` (pure Mockito, no Spring at all). Added the missing `@Mock PasswordEncoder`.

| Before | After |
|---|---|
| `@SpringBootTest` | `@ExtendWith(MockitoExtension.class)` |
| `@Autowired UserService` | `@InjectMocks UserService` |
| `@MockitoBean UserRepo` | `@Mock UserRepo` |
| `@MockitoBean RoleRepo` | `@Mock RoleRepo` |
| _(missing)_ | `@Mock PasswordEncoder` |

**Rule of thumb:**
- `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` = pure unit test, no Spring context, fastest
- `@WebMvcTest` = web-layer slice test (controllers + security), no JPA/DB
- `@SpringBootTest` = full integration test, requires all infrastructure (DB, etc.)

### Final `UserServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepo userRepo;

    @Mock
    private RoleRepo roleRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    // ... @BeforeEach and tests unchanged
}
```

---

## Point 5 — Review of `UserControllerTest`

### Issue 1 — HIGH: `@SpringBootTest` is the wrong annotation for a controller test

**Original code:**
```java
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest { ... }
```

**Problem:**

`@SpringBootTest` boots the full application context including JPA/Hibernate, which tries to connect to MySQL. In any environment without a running database (CI, a colleague's machine, a fresh checkout) the entire test class fails to load before running a single assertion. It also makes each test run much slower (~20s+).

**Fix:**

Use `@WebMvcTest(UserController.class)` — the correct Spring Boot test slice for controller tests. It loads only:
- Controllers, filters, security configuration
- MockMvc auto-configured

It does NOT load JPA, repositories, or services. Those are provided as `@MockitoBean`.

**Extra step required — `@Import` the security configs:**

`@WebMvcTest` does NOT auto-load your custom `@Configuration` security classes (`WebSecurityConfig`, `SecurityConfig`). Without them, Spring applies its default security rules: CSRF enabled, all POST requests without a CSRF token → **403 Forbidden**.

You must explicitly import your security configs so the real `permitAll()` rules take effect:

```java
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, WebSecurityConfig.class})
```

**Additional mocks needed by the security layer:**

Since `@WebMvcTest` skips JPA beans, but the security layer depends on them, you must mock:

```java
@MockitoBean
private UserRepo userRepo;              // SecurityConfig.userDetailsService() needs it

@MockitoBean
private JwtTokenUtil jwtTokenUtil;      // JwtAuthenticationFilter needs it

@MockitoBean
private InvalidatedTokenRepo invalidatedTokenRepo; // JwtAuthenticationFilter needs it
```

---

### Issue 2 — MEDIUM: `ObjectMapper` created manually in every test method

**Original code (repeated in both test methods):**
```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.registerModule(new JavaTimeModule());
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

**Problem:**

This is duplicate code. More importantly, Spring Boot's `JacksonAutoConfiguration` already creates and configures an `ObjectMapper` bean with `JavaTimeModule` registered — available in both `@SpringBootTest` and `@WebMvcTest` contexts. Building a separate one risks divergence between test and production serialization.

**Fix:**

```java
@Autowired
private ObjectMapper objectMapper; // already has JavaTimeModule; same config as production
```

Remove all manual `ObjectMapper` construction from the test methods.

---

### Issue 3 — LOW: JSONPath expressions missing root `$`

**Original code:**
```java
.andExpect(MockMvcResultMatchers.jsonPath("code").value(1000))
.andExpect(MockMvcResultMatchers.jsonPath("result.id").value(17))
```

**Problem:**

Jayway JSONPath (the library MockMvc uses) requires paths to start from `$` (the root). The bare form `"code"` works only because of library leniency — it's non-standard and should not be relied upon.

**Fix:**
```java
.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
.andExpect(MockMvcResultMatchers.jsonPath("$.result.id").value(17))
```

---

### Issue 4 — LOW: Missing validation test cases

Only one validation failure is tested (`password < 8 chars`). Other `UserDTO` constraints worth covering:

| Field | Constraint | Test name suggestion |
|---|---|---|
| `phoneNumber` | `@NotBlank` | `createUser_phoneBlank_fail` |
| `roleId` | `@NotNull` | `createUser_roleIdNull_fail` |
| `dateOfBirth` | `@DobConstraint(min=16)` | `createUser_underageDob_fail` |

---

### Issue 5 — INFO: No tests for protected endpoints

Currently only the public `/register` endpoint is tested. You should also test:

```java
// Unauthenticated request to protected endpoint → 401
@Test
void getAllUsers_noAuth_shouldReturn401() throws Exception { ... }

// Authenticated but wrong role → 403
@Test
@WithMockUser(roles = "USER")
void getAllUsers_asUser_shouldReturn403() throws Exception { ... }

// Correct role → 200
@Test
@WithMockUser(roles = "ADMIN")
void getAllUsers_asAdmin_shouldReturn200() throws Exception { ... }
```

`@WithMockUser` is from `spring-security-test` (already included via `spring-boot-starter-test`).

---

### Final `UserControllerTest.java`

```java
@Slf4j
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, WebSecurityConfig.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;  // auto-configured by Spring Boot

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepo userRepo;          // for SecurityConfig

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;  // for JwtAuthenticationFilter

    @MockitoBean
    private InvalidatedTokenRepo invalidatedTokenRepo; // for JwtAuthenticationFilter

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    public void initData() {
        LocalDate dob = LocalDate.of(2001, 8, 22);
        Role role = new Role(1L, "USER");
        userDTO = UserDTO.builder()
                .fullName("Mai Gia Phuc").phoneNumber("0854475387")
                .address("Đây là USER nhé").password("22012007")
                .retypePassword("22012007").dateOfBirth(dob)
                .facebookAccountId(0).googleAccountId(0)
                .roleId(1L)
                .build();
        user = User.builder().id(17L)
                .fullName("Mai Gia Phuc").phoneNumber("0854475387")
                .address("Đây là USER nhé").active(true)
                .dateOfBirth(dob).facebookAccountId(0).googleAccountId(0)
                .role(role)
                .build();
    }

    @Test
    void createUser_validRequest_success() throws Exception {
        Mockito.when(userService.createUser(ArgumentMatchers.any()))
                .thenReturn(user);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.id").value(17));
    }

    @Test
    void createUser_passwordInvalid_fail() throws Exception {
        userDTO.setPassword("123456");

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1014))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Password must be at least 8 character"));
    }
}
```

---

## Quick Reference — Which test annotation to use

| Scenario | Annotation | Needs DB? |
|---|---|---|
| Test a service method (unit test) | `@ExtendWith(MockitoExtension.class)` | No |
| Test a controller (web layer only) | `@WebMvcTest(XxxController.class)` + `@Import(security configs)` | No |
| Full integration test (all layers) | `@SpringBootTest` | Yes (MySQL must be running) |
