# OAuth2 Resource Server in Spring Security — Complete Guide

## Overview

`oauth2ResourceServer` is a **built-in Spring Security feature** that handles JWT validation automatically.  
It replaces your custom `JwtAuthenticationFilter` with Spring's own filter pipeline.

---

## Your Current Approach vs `oauth2ResourceServer`

| Feature | Your Current Approach | `oauth2ResourceServer` |
|---|---|---|
| JWT extraction | Manual (read `Authorization` header in filter) | Automatic (Spring does it) |
| JWT validation | Manual (`JwtTokenUtil.validateToken()`) | Automatic (`JwtDecoder` bean) |
| Authentication object | `UsernamePasswordAuthenticationToken` | `JwtAuthenticationToken` |
| Custom claims | Easy (you control everything) | Needs a custom converter |
| External Identity Provider | Not supported | Native support |
| Code boilerplate | High (custom filter + util class) | Low |

---

## Required Maven Dependency

Add this to your `pom.xml` — **this is different from your current JJWT dependency**:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

This pulls in `spring-security-oauth2-jose` which uses **Nimbus JOSE JWT** under the hood.

---

## Full Code Example: `WebSecurityConfig` with `oauth2ResourceServer`

### Scenario 1 — HMAC Secret Key (HS256) — Same as your current project

This is the closest equivalent to what your project currently does.

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

            // --- THIS REPLACES your JwtAuthenticationFilter ---
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

    // Tells Spring HOW to decode and verify the JWT signature
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder
            .withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    // Tells Spring HOW to extract roles from JWT claims
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Your JWT must include a claim named "roles" with values like ["ROLE_USER", "ROLE_ADMIN"]
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix(""); // already has ROLE_ prefix in claim

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

**What happens internally when a request arrives:**
```
Request → BearerTokenAuthenticationFilter (Spring auto-adds this)
         → Reads "Authorization: Bearer <token>" header
         → Calls JwtDecoder.decode(token)
         → Validates: signature, expiration, issuer (if configured)
         → Calls JwtAuthenticationConverter to extract roles
         → Sets JwtAuthenticationToken in SecurityContext
         → Proceeds to your controller
```

---

### Scenario 2 — RSA Public Key (RS256)

Used when your Auth Server signs with a private key, and your app validates with the public key.

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

### Scenario 3 — External Identity Provider (Keycloak, Auth0, Okta)

**This is the most common real-world use case for `oauth2ResourceServer`.**

```java
@Bean
public JwtDecoder jwtDecoder() {
    // Spring fetches the public keys automatically from the JWKS endpoint
    return NimbusJwtDecoder
        .withJwkSetUri("https://your-keycloak/auth/realms/myrealm/protocol/openid-connect/certs")
        .build();
}
```

Or even simpler using `application.properties`:
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://your-keycloak/auth/realms/myrealm
# Spring auto-discovers the JWKS URI and validates issuer claim
```

Then your `WebSecurityConfig` becomes:
```java
.oauth2ResourceServer(oauth2 ->
    oauth2.jwt(Customizer.withDefaults()) // reads from application.properties automatically
)
```

---

### Scenario 4 — Custom Claims Converter (e.g., extract `phoneNumber` like your project)

Your project stores `phoneNumber` as the JWT subject. Here's how to access it:

```java
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@GetMapping("/profile")
public ResponseEntity<?> getProfile(@AuthenticationPrincipal Jwt jwt) {
    String phoneNumber = jwt.getSubject();          // gets "sub" claim
    String phone2 = jwt.getClaim("phoneNumber");    // gets your custom claim
    return ResponseEntity.ok(phoneNumber);
}
```

If you need to load `User` from DB inside the converter:
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

Then wire it in:
```java
.oauth2ResourceServer(oauth2 ->
    oauth2.jwt(jwt ->
        jwt.decoder(jwtDecoder())
           .jwtAuthenticationConverter(customJwtAuthConverter) // inject it here
    )
)
```

---

## When SHOULD You Use `oauth2ResourceServer`?

### Use it when:

**1. You use an external Identity Provider (most important case)**
- Your app uses Keycloak, Auth0, Okta, Google, or any OAuth2/OIDC provider
- The JWT is issued by a separate Auth Server, not by your Spring app
- You validate with a public key or JWKS endpoint, not a shared secret

**2. You are building a Microservice that trusts a shared Auth Server**
```
[Client] --login--> [Auth Server] --JWT--> [Client]
[Client] --JWT--> [Your Microservice (Resource Server)] 
```
Your microservice only validates the token — it never issues one.

**3. You want less boilerplate**
- No need to write `JwtAuthenticationFilter`, no manual `SecurityContextHolder.setAuthentication()`
- Spring handles the full Bearer token extraction + validation pipeline

**4. Standard OAuth2 compliance matters**
- When you need proper `401 WWW-Authenticate: Bearer` headers
- When auditors or security teams require standard OAuth2 RS validation

**5. You use asymmetric keys (RSA/EC)**
- You never want the secret key inside your microservice
- Auth Server holds the private key; microservices only hold the public key

---

## When Should You NOT Use It (Stick with Your Current Custom Filter)?

### Keep your custom filter when:

**1. You self-issue AND self-validate with a shared secret (your current situation)**
- Your `JwtTokenUtil` generates tokens AND validates them
- You use HMAC (HS256) with a secret known only to your app
- There is no separate Auth Server in your architecture

**2. Your token subject is non-standard**
- You use `phoneNumber` as the JWT subject instead of `userId` or `email`
- Loading `UserDetails` by phone requires a DB call that's hard to wire into the standard converter

**3. You need complex pre-validation logic**
- Checking a token blacklist in Redis before standard validation
- Validating against a `Token` table in your DB (your project has a `Token` model)

**4. You have mixed authentication schemes**
- Some endpoints use JWT, others use API keys or session cookies
- Mixing schemes is harder with `oauth2ResourceServer`

---

## Architecture Comparison: Your Project vs Resource Server Pattern

### Your Current Architecture (Self-contained JWT)
```
[Client]
   |
   | POST /login (phoneNumber + password)
   v
[Your Spring App]
   |-- UserService authenticates user
   |-- JwtTokenUtil.generateToken(user)  <-- you are the Auth Server
   |-- Returns JWT to client
   |
   | GET /api/v1/orders (Bearer <token>)
   v
[JwtAuthenticationFilter]  <-- you are also the Resource Server
   |-- Reads token
   |-- JwtTokenUtil.validateToken()
   |-- Loads User from DB by phoneNumber
   |-- Sets SecurityContext
   v
[Your Controller]
```

### oauth2ResourceServer Architecture (Separate Auth Server)
```
[Client]
   |
   | POST /token (username + password)
   v
[Auth Server — Keycloak / Auth0 / Your separate auth app]
   |-- Validates credentials
   |-- Issues JWT signed with PRIVATE key
   |-- Returns JWT to client
   |
   | GET /api/v1/orders (Bearer <token>)
   v
[Your Spring App — the Resource Server]
   |
[BearerTokenAuthenticationFilter — Spring auto-adds this]
   |-- Reads Bearer token
   |-- JwtDecoder.decode() validates with PUBLIC key / JWKS
   |-- JwtAuthenticationConverter extracts roles
   |-- Sets SecurityContext
   v
[Your Controller]
```

---

## Summary

| Question | Answer |
|---|---|
| Does your app both **issue** and **validate** JWT? | Use **custom filter** (current approach) |
| Does an external server issue the JWT? | Use **oauth2ResourceServer** |
| Do you use Keycloak / Auth0 / Okta? | Use **oauth2ResourceServer** |
| Do you need to load `UserDetails` from DB by a custom claim? | **Custom filter** OR custom converter with resource server |
| Do you want minimal boilerplate? | **oauth2ResourceServer** |
| Do you have a `Token` blacklist table? | **Custom filter** (easier to check DB there) |

**Bottom line for your project:**  
Your current `JwtAuthenticationFilter` approach is correct because your Spring app acts as both the **token issuer** and **token validator**. You would switch to `oauth2ResourceServer` if you introduced Keycloak or Auth0 as a separate Auth Server, or if you split your app into microservices that share a central auth service.
