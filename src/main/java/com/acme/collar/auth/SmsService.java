package com.acme.collar.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
class SmsService {

  // v1 stub：验证码存在内存中。后续可替换为 Redis，并接入阿里云短信。
  private final Map<String, CodeEntry> phoneToCode = new ConcurrentHashMap<>();
  private final Clock clock;

  SmsService() {
    this(Clock.systemUTC());
  }

  SmsService(Clock clock) {
    this.clock = clock;
  }

  void sendLoginCode(String phone) {
    // v1 stub：固定验证码 000000，便于端到端测试。
    phoneToCode.put(phone, new CodeEntry("000000", Instant.now(clock).plus(Duration.ofMinutes(5))));
  }

  void verifyLoginCode(String phone, String code) {
    CodeEntry entry = phoneToCode.get(phone);
    if (entry == null) {
      throw new AuthException("验证码不存在或已过期");
    }
    if (Instant.now(clock).isAfter(entry.expiresAt())) {
      phoneToCode.remove(phone);
      throw new AuthException("验证码不存在或已过期");
    }
    if (!Objects.equals(entry.code(), code)) {
      throw new AuthException("验证码错误");
    }
    // 单次使用
    phoneToCode.remove(phone);
  }

  record CodeEntry(String code, Instant expiresAt) {}
}
