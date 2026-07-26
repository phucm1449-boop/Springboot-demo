package com.example.demo.filter;

import com.example.demo.component.JwtTokenUtil;
import com.example.demo.exceptions.AppException;
import com.example.demo.exceptions.ErrorCode;
import com.example.demo.models.User;
import com.example.demo.repo.InvalidatedTokenRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs"
    );

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final InvalidatedTokenRepo invalidatedTokenRepo;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response); // ← cho đi tiếp
                return;
            }

            final String token = authHeader.substring(7);
            final String phoneNumber = jwtTokenUtil.extractPhoneNumberFromToken(token);
            if (phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User userDetails = (User) userDetailsService.loadUserByUsername(phoneNumber);
                if (!jwtTokenUtil.validateToken(token, userDetails)) {
                    throw new AppException(ErrorCode.INVALID_JWT_TOKEN);
                }

                String jwtId = jwtTokenUtil.extractJWTIDFromToken(token);
                if (invalidatedTokenRepo.existsById(jwtId)) {
                    throw new AppException(ErrorCode.INVALID_JWT_TOKEN);
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (PUBLIC_PATHS.stream().anyMatch(servletPath::startsWith)) {
            return true;
        }

        if (!HttpMethod.GET.name().equals(request.getMethod())
                || (servletPath.startsWith("/api/v1/users") && HttpMethod.GET.name().equals(request.getMethod()))) {
            return false;
        }
        return servletPath.startsWith("/api/v1/categories")
                || servletPath.startsWith("/api/v1/products")
                || servletPath.startsWith("/api/v1/orders");// the JWT filter is skipped. No token is required at this stage
    }
}
