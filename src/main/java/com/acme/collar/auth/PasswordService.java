package com.acme.collar.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class PasswordService {

  private final PasswordEncoder encoder;

  PasswordService() {
    // BCrypt 默认强度足够 v1 使用
    this.encoder = new BCryptPasswordEncoder();
  }

  String hash(String rawPassword) {
    return encoder.encode(rawPassword);
  }

  boolean matches(String rawPassword, String hashedPassword) {
    if (hashedPassword == null || hashedPassword.isBlank()) {
      return false;
    }
    return encoder.matches(rawPassword, hashedPassword);
  }
}
