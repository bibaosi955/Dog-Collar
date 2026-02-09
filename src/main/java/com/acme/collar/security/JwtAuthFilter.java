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
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class JwtAuthFilter extends OncePerRequestFilter {

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
          // token 无效：保持匿名，交由后续鉴权决策
        }
      }
    }

    filterChain.doFilter(request, response);
  }
}
