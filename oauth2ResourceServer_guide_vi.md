# OAuth2 Resource Server trong Spring Security - Hướng dẫn đầy đủ

## Tổng quan

`oauth2ResourceServer` là **tính năng có sẵn của Spring Security** giúp xử lý việc xác thực JWT tự động.  
Nó thay thế `JwtAuthenticationFilter` tự viết của bạn bằng chính chuỗi filter mặc định của Spring.

---

## Cách tiếp cận hiện tại của bạn so với `oauth2ResourceServer`

| Tính năng | Cách hiện tại của bạn | `oauth2ResourceServer` |
|---|---|---|
| Tách JWT | Thủ công (đọc header `Authorization` trong filter) | Tự động (Spring xử lý) |
| Xác thực JWT | Thủ công (`JwtTokenUtil.validateToken()`) | Tự động (`JwtDecoder` bean) |
| Đối tượng Authentication | `UsernamePasswordAuthenticationToken` | `JwtAuthenticationToken` |
| Custom claims | Dễ (bạn kiểm soát toàn bộ) | Cần custom converter |
| External Identity Provider | Không hỗ trợ | Hỗ trợ native |
| Mã boilerplate | Nhiều (custom filter + util class) | Ít |

---

## Maven dependency cần thêm

Thêm dependency này vào `pom.xml` - **khác với dependency JJWT hiện tại của bạn**:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

Dependency này sẽ kéo theo `spring-security-oauth2-jose`, thư viện này dùng **Nimbus JOSE JWT** ở bên dưới.

---

## Ví dụ đầy đủ: `WebSecurityConfig` với `oauth2ResourceServer`

### Tình huống 1 - Secret key HMAC (HS256) - Giống với dự án hiện tại của bạn

Đây là cách gần nhất với những gì project của bạn đang làm.

```java
package com.example.demo.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.example.demo.models.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${jwt.secretKey}")
    private String secretKey;

    private final String[] PUBLIC_ENDPOINTS = {"/api/v1/users/register", "/api/v1/users/login"};
    private final String[] PUBLIC_GET_ENDPOINTS = {"/api/v1/categories/**", "/api/v1/products/**", "/api/v1/orders/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // --- PHẦN NÀY SẼ THAY THẾ JwtAuthenticationFilter của bạn ---
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.decoder(jwtDecoder())
                       .jwtAuthenticationConverter(jwtAuthenticationConverter())
                ))

            .authorizeHttpRequests(request -> request
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole(Role.ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole(Role.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole(Role.ADMIN)

                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole(Role.ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole(Role.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole(Role.ADMIN)

                .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasRole(Role.USER)
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasRole(Role.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasRole(Role.ADMIN)

                .requestMatchers(HttpMethod.POST, "/api/v1/order_details/**").hasRole(Role.USER)
                .requestMatchers(HttpMethod.GET, "/api/v1/order_details/**").hasAnyRole(Role.USER, Role.ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/v1/order_details/**").hasRole(Role.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/order_details/**").hasRole(Role.ADMIN)
                .anyRequest().authenticated()
            );

        return http.build();
    }

    // Báo cho Spring cách decode và verify chữ ký JWT
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder
            .withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    // Báo cho Spring cách lấy roles từ JWT claims
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // JWT của bạn cần có claim tên "roles" với giá trị như ["ROLE_USER", "ROLE_ADMIN"]
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix(""); // claim đã có sẵn prefix ROLE_

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

**Bên trong sẽ diễn ra gì khi một request đi vào:**
```text
Request -> BearerTokenAuthenticationFilter (Spring tự thêm)
        -> Đọc header "Authorization: Bearer <token>"
        -> Gọi JwtDecoder.decode(token)
        -> Xác thực: signature, expiration, issuer (nếu có cấu hình)
        -> Gọi JwtAuthenticationConverter để tách roles
        -> Đặt JwtAuthenticationToken vào SecurityContext
        -> Đi tiếp vào controller của bạn
```

---

### Tình huống 2 - RSA Public Key (RS256)

Dùng khi Auth Server ký token bằng private key, còn app của bạn xác thực bằng public key.

```java
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;

@Value("${rsa.public-key}")
private RSAPublicKey rsaPublicKey;

@Bean
public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder
        .withPublicKey(rsaPublicKey)
        .build();
}
```

`application.properties`:
```properties
rsa.public-key=classpath:certs/public.pem
```

---

### Tình huống 3 - External Identity Provider (Keycloak, Auth0, Okta)

**Đây là trường hợp phổ biến nhất trong thực tế khi dùng `oauth2ResourceServer`.**

```java
@Bean
public JwtDecoder jwtDecoder() {
    // Spring sẽ tự động lấy public keys từ JWKS endpoint
    return NimbusJwtDecoder
        .withJwkSetUri("https://your-keycloak/auth/realms/myrealm/protocol/openid-connect/certs")
        .build();
}
```

Hoặc đơn giản hơn trong `application.properties`:
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://your-keycloak/auth/realms/myrealm
# Spring tự động tìm JWKS URI và xác thực issuer claim
```

Lúc đó `WebSecurityConfig` của bạn có thể trở thành:
```java
.oauth2ResourceServer(oauth2 ->
    oauth2.jwt(Customizer.withDefaults()) // tự đọc trong application.properties
)
```

---

### Tình huống 4 - Custom Claims Converter (ví dụ: tách `phoneNumber` như project của bạn)

Project của bạn đang lưu `phoneNumber` vào JWT subject. Bạn có thể truy cập nó như sau:

```java
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@GetMapping("/profile")
public ResponseEntity<?> getProfile(@AuthenticationPrincipal Jwt jwt) {
    String phoneNumber = jwt.getSubject();          // lấy claim "sub"
    String phone2 = jwt.getClaim("phoneNumber");    // lấy custom claim của bạn
    return ResponseEntity.ok(phoneNumber);
}
```

Nếu bạn cần load `User` từ database bên trong converter:

```java
@Component
@RequiredArgsConstructor
public class CustomJwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserDetailsService userDetailsService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String phoneNumber = jwt.getSubject();
        UserDetails user = userDetailsService.loadUserByUsername(phoneNumber);
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
```

Sau đó wire vào:
```java
.oauth2ResourceServer(oauth2 ->
    oauth2.jwt(jwt ->
        jwt.decoder(jwtDecoder())
           .jwtAuthenticationConverter(customJwtAuthConverter) // inject vào đây
    )
)
```

---

## Khi nào NÊN dùng `oauth2ResourceServer`?

### Nên dùng khi:

**1. Bạn dùng external Identity Provider (trường hợp quan trọng nhất)**
- App của bạn dùng Keycloak, Auth0, Okta, Google, hoặc bất kỳ nhà cung cấp OAuth2/OIDC nào
- JWT được phát hành bởi một Auth Server riêng, không phải chính Spring app của bạn
- Bạn xác thực bằng public key hoặc JWKS endpoint, không dùng shared secret

**2. Bạn đang xây dựng một microservice tin tưởng một Auth Server dùng chung**
```text
[Client] --login--> [Auth Server] --JWT--> [Client]
[Client] --JWT--> [Microservice của bạn (Resource Server)]
```
Microservice của bạn chỉ xác thực token - nó không phát hành token.

**3. Bạn muốn ít boilerplate hơn**
- Không cần viết `JwtAuthenticationFilter`, không cần tự gọi `SecurityContextHolder.setAuthentication()`
- Spring xử lý toàn bộ pipeline tách Bearer token + xác thực

**4. Tuân thủ chuẩn OAuth2 là quan trọng**
- Khi bạn cần dùng `401 WWW-Authenticate: Bearer` đúng chuẩn
- Khi auditor hoặc đội security bắt buộc Resource Server validation đúng chuẩn OAuth2

**5. Bạn dùng asymmetric keys (RSA/EC)**
- Bạn không muốn để secret key trong microservice
- Auth Server giữ private key; microservices chỉ giữ public key

---

## Khi nào KHÔNG nên dùng nó (nên giữ custom filter hiện tại)?

### Nên giữ custom filter khi:

**1. Bạn tự phát hành token và tự xác thực bằng shared secret (tình huống hiện tại của bạn)**
- `JwtTokenUtil` của bạn vừa tạo token vừa validate token
- Bạn dùng HMAC (HS256) với secret chỉ app của bạn biết
- Kiến trúc của bạn không có Auth Server tách riêng

**2. Subject của token của bạn mang tính custom**
- Bạn dùng `phoneNumber` làm JWT subject thay vì `userId` hoặc `email`
- Việc load `UserDetails` bằng phone cần gọi DB, và điều này không phải lúc nào cũng hợp với converter mặc định

**3. Bạn cần logic pre-validation phức tạp**
- Kiểm tra blacklist token trong Redis trước khi validate chuẩn
- Kiểm tra bảng `Token` trong database (project của bạn có `Token` model)

**4. Bạn có nhiều cơ chế authentication trộn lẫn**
- Một số endpoint dùng JWT, một số khác dùng API key hoặc session cookie
- Trộn nhiều cơ chế sẽ khó hơn khi dùng `oauth2ResourceServer`

---

## So sánh kiến trúc: Project của bạn và mô hình Resource Server

### Kiến trúc hiện tại của bạn (JWT tự chứa)
```text
[Client]
   |
   | POST /login (phoneNumber + password)
   v
[Spring App của bạn]
   |-- UserService xác thực user
   |-- JwtTokenUtil.generateToken(user)  <-- bạn đóng vai trò Auth Server
   |-- Trả JWT về cho client
   |
   | GET /api/v1/orders (Bearer <token>)
   v
[JwtAuthenticationFilter]  <-- bạn đồng thời cũng là Resource Server
   |-- Đọc token
   |-- JwtTokenUtil.validateToken()
   |-- Load User từ DB bằng phoneNumber
   |-- Đặt SecurityContext
   v
[Controller của bạn]
```

### Kiến trúc `oauth2ResourceServer` (Auth Server tách riêng)
```text
[Client]
   |
   | POST /token (username + password)
   v
[Auth Server - Keycloak / Auth0 / ứng dụng auth riêng]
   |-- Xác thực credentials
   |-- Phát hành JWT được ký bằng PRIVATE key
   |-- Trả JWT về cho client
   |
   | GET /api/v1/orders (Bearer <token>)
   v
[Spring App của bạn - Resource Server]
   |
[BearerTokenAuthenticationFilter - Spring tự thêm]
   |-- Đọc Bearer token
   |-- JwtDecoder.decode() xác thực bằng PUBLIC key / JWKS
   |-- JwtAuthenticationConverter tách roles
   |-- Đặt SecurityContext
   v
[Controller của bạn]
```

---

## Tổng kết

| Câu hỏi | Câu trả lời |
|---|---|
| App của bạn vừa **phát hành** vừa **xác thực** JWT? | Dùng **custom filter** (cách hiện tại) |
| JWT do external server phát hành? | Dùng **oauth2ResourceServer** |
| Bạn dùng Keycloak / Auth0 / Okta? | Dùng **oauth2ResourceServer** |
| Bạn cần load `UserDetails` từ DB bằng custom claim? | **Custom filter** hoặc custom converter với resource server |
| Bạn muốn ít boilerplate? | **oauth2ResourceServer** |
| Bạn có bảng blacklist `Token`? | **Custom filter** (dễ kiểm tra DB hơn) |

**Kết luận cho project của bạn:**  
Cách dùng `JwtAuthenticationFilter` hiện tại là hợp lý vì Spring app của bạn vừa đóng vai trò **token issuer** vừa đóng vai trò **token validator**. Bạn nên chuyển sang `oauth2ResourceServer` nếu đưa Keycloak hoặc Auth0 vào làm Auth Server riêng, hoặc tách app thành nhiều microservices dùng chung một auth service trung tâm.
