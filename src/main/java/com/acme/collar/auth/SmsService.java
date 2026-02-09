package com.acme.collar.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
class SmsService {

  // v1 stub：验证码存在内存中。后续可替换为 Redis，并接入真实短信通道。
  private final Map<String, ChallengeEntry> challenges = new ConcurrentHashMap<>();
  private final Map<String, Instant> phoneToLastSendAt = new ConcurrentHashMap<>();
  private final Clock clock;
  private final Environment env;

  private static final Duration CODE_TTL = Duration.ofMinutes(5);
  private static final Duration SEND_INTERVAL = Duration.ofMinutes(1);
  private static final int MAX_VERIFY_ATTEMPTS = 5;

  @Autowired
  SmsService(Environment env) {
    this(env, Clock.systemUTC());
  }

  SmsService(Environment env, Clock clock) {
    this.env = env;
    this.clock = clock;
  }

  SendResult sendLoginCode(String phone) {
    if (!env.acceptsProfiles("test")) {
      // v1：默认不开放短信 stub，避免误用导致任意手机号可登录。
      throw new UnsupportedOperationException("未配置短信通道");
    }

    Instant now = Instant.now(clock);
    Instant last = phoneToLastSendAt.get(phone);
    if (last != null && last.plus(SEND_INTERVAL).isAfter(now)) {
      throw new AuthException("发送过于频繁，请稍后再试");
    }

    String challengeId = UUID.randomUUID().toString();
    challenges.put(
        challengeId,
        new ChallengeEntry(phone, "000000", now.plus(CODE_TTL), new AtomicInteger(0)));
    phoneToLastSendAt.put(phone, now);

    return new SendResult(challengeId, CODE_TTL);
  }

  void verifyLoginCode(String challengeId, String phone, String code) {
    if (!env.acceptsProfiles("test")) {
      throw new UnsupportedOperationException("未配置短信通道");
    }

    ChallengeEntry entry = challenges.get(challengeId);
    if (entry == null || !Objects.equals(entry.phone(), phone)) {
      throw new AuthException("验证码不存在或已过期");
    }

    Instant now = Instant.now(clock);
    if (now.isAfter(entry.expiresAt())) {
      challenges.remove(challengeId);
      throw new AuthException("验证码不存在或已过期");
    }

    int attempts = entry.verifyAttempts().incrementAndGet();
    if (attempts > MAX_VERIFY_ATTEMPTS) {
      challenges.remove(challengeId);
      throw new AuthException("验证码错误次数过多，请重新获取");
    }

    if (!Objects.equals(entry.code(), code)) {
      throw new AuthException("验证码错误");
    }

    // 单次使用
    challenges.remove(challengeId);
  }

  record SendResult(String challengeId, Duration ttl) {}

  record ChallengeEntry(String phone, String code, Instant expiresAt, AtomicInteger verifyAttempts) {}
}
