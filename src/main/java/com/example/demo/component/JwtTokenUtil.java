package com.example.demo.component;

import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.models.InvalidatedToken;
import com.example.demo.models.User;
import com.example.demo.repo.InvalidatedTokenRepo;
import com.example.demo.repo.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenUtil {
    @Value("${jwt.valid-duration}")
    private int expiration; // save to environment variable
    @Value("${jwt.refreshable-duration}")
    private int refreshable_duration;
    @Value("${jwt.secretKey}")
    private String secretKey;

    private final InvalidatedTokenRepo invalidatedTokenRepo;
    private final UserRepo userRepo;

    public String generateToken(User user) {
        log.info("secretKey: " + secretKey);
        //properties của obj User đưa vào trong security => gọi là claims
        Map<String, Object> claims = new HashMap<>();
//        this.generateSecretKey();
        claims.put("phoneNumber", user.getPhoneNumber());
        claims.put("scope", user.getRole().getName());
        try {
            // chuẩn hóa obj User thành obj User trong Spring Security
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setId(UUID.randomUUID().toString())
                    .setSubject(user.getPhoneNumber())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000L)) // change from second --> millisecond
                    .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                    .compact();
            return token;
            // trong cái token này có chứa claims là phone number
            // ta sẽ sử dụng token này mang đi gửi các request khác
        } catch (Exception e) {
            //you can "inject" Logger, instead System.out.println
            throw new AppException(ErrorCode.CANNOT_CREATE_JWT_TOKEN);
        }
    }

    public void logout(String token) {
        this.validateToken(token, true);

        String jit = this.extractJWTIDFromToken(token);
        Date expiryDate = this.extractExpirationDateFromToken(token);

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryDate(expiryDate)
                .build();
        invalidatedTokenRepo.save(invalidatedToken);
    }

    public String refreshAndGenerateToken(String token) {
        this.validateToken(token, true);

        String jit = this.extractJWTIDFromToken(token);
        Date expiryDate = this.extractExpirationDateFromToken(token);

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryDate(expiryDate)
                .build();
        invalidatedTokenRepo.save(invalidatedToken);

        String phoneNumber = this.extractPhoneNumberFromToken(token);
        User user = this.userRepo.findByPhoneNumber(phoneNumber).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_FOUND)
        );

        String newToken = this.generateToken(user);
        return newToken;
    }

    private Key getSignInKey() {
        byte[] bytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(bytes); // chuyển đổi từ String secretKey --> Key
    }

    private String generateSecretKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] keyBytes = new byte[32]; // 256-bit key
        secureRandom.nextBytes(keyBytes);
        String secretKey = Encoders.BASE64.encode(keyBytes);
        return secretKey;
    }

    //how to extract claims from this
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    //check expiration
    public boolean isTokenExpired(String token, boolean isRefresh) {
        Date expirationDate = isRefresh
                ? Date.from(
                        this.extractClaim(token, Claims::getIssuedAt)
                                .toInstant()
                                .plus(refreshable_duration, ChronoUnit.SECONDS))
                : this.extractClaim(token, Claims::getExpiration);
        return expirationDate.before(new Date());
    }

    // phone number is subject of claims
    public String extractPhoneNumberFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // kiểm tra xem username vs token còn hạn không
    public boolean validateToken(String token, UserDetails userDetails) {
        String phoneNumber = this.extractPhoneNumberFromToken(token);
        return phoneNumber.equals(userDetails.getUsername()) && !isTokenExpired(token, false);
    }

    public void validateToken(String token, boolean isRefresh) {
        String tokenPhoneNumber = this.extractPhoneNumberFromToken(token);
        String authenticatedPhoneNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!tokenPhoneNumber.equals(authenticatedPhoneNumber)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (this.isTokenExpired(token, isRefresh)) {
            throw new AppException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    public String extractJWTIDFromToken(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
