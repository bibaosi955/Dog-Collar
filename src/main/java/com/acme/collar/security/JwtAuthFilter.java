package com.acme.collar.security;

import com.acme.collar.auth.JwtService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class JwtAuthFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

  private final JwtService jwtService;

  JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring("Bearer ".length()).trim();
      if (!token.isBlank()) {
        try {
          var claims = Jwts.parser().verifyWith(jwtService.key()).build().parseSignedClaims(token).getPayload();

          String subject = claims.getSubject();
          if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var auth =
                new UsernamePasswordAuthenticationToken(
                    subject, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
          }
        } catch (JwtException ignored) {
          // token 无效：直接 401，并记录原因（debug）
          log.debug("JWT token 无效: {}", ignored.getMessage());
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
