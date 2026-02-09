package com.acme.collar.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final Duration accessTtl;
  private final Clock clock;

  public JwtService(
      @Value("${security.jwt.secret:dev-only-secret-please-change-please-change-dev-only}") String secret,
      @Value("${security.jwt.access-ttl:PT2H}") Duration accessTtl) {
    // v1：用配置字符串生成 HMAC key。
    // 注意：JJWT 对 HS256 要求 key 至少 256 bits（32 bytes）。若 secret 过短，这里用 SHA-256 派生固定长度 key。
    byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
    if (raw.length < 32) {
      raw = sha256(raw);
    }

    this.key = Keys.hmacShaKeyFor(raw);
    this.accessTtl = accessTtl;
    this.clock = Clock.systemUTC();
  }

  private static byte[] sha256(byte[] input) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(input);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("缺少 SHA-256 算法实现", e);
    }
  }

  String issueAccessToken(User user) {
    Instant now = Instant.now(clock);
    Instant exp = now.plus(accessTtl);

    return Jwts.builder()
        .setSubject(String.valueOf(user.id()))
        .claim("phone", user.phone())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public SecretKey key() {
    return key;
  }
}
