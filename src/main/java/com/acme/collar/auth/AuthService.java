package com.acme.collar.auth;

import org.springframework.stereotype.Service;

@Service
class AuthService {

  private final SmsService smsService;
  private final UserRepository userRepository;
  private final PasswordService passwordService;
  private final JwtService jwtService;

  AuthService(
      SmsService smsService,
      UserRepository userRepository,
      PasswordService passwordService,
      JwtService jwtService) {
    this.smsService = smsService;
    this.userRepository = userRepository;
    this.passwordService = passwordService;
    this.jwtService = jwtService;
  }

  SmsService.SendResult sendSmsCodeWithChallenge(String phone) {
    return smsService.sendLoginCode(phone);
  }

  AuthResponse loginWithSms(String challengeId, String phone, String code) {
    smsService.verifyLoginCode(challengeId, phone, code);

    User user = userRepository.findByPhone(phone);
    if (user == null) {
      user = userRepository.save(User.create(phone));
    }

    return new AuthResponse(jwtService.issueAccessToken(user));
  }

  AuthResponse loginWithPassword(String phone, String password) {
    User user = userRepository.findByPhone(phone);
    if (user == null) {
      throw new AuthException("用户不存在");
    }

    if (!passwordService.matches(password, user.passwordHash())) {
      throw new AuthException("手机号或密码错误");
    }

    return new AuthResponse(jwtService.issueAccessToken(user));
  }

  AuthResponse registerWithPassword(String phone, String password) {
    User existing = userRepository.findByPhone(phone);
    if (existing != null) {
      throw new AuthConflictException("手机号已注册");
    }

    User user = userRepository.save(User.create(phone).withPasswordHash(passwordService.hash(password)));
    return new AuthResponse(jwtService.issueAccessToken(user));
  }

  void changePassword(long userId, String oldPassword, String newPassword) {
    User user = userRepository.findById(userId);
    if (user == null) {
      throw new AuthException("用户不存在");
    }

    if (!passwordService.matches(oldPassword, user.passwordHash())) {
      throw new AuthException("旧密码错误");
    }

    userRepository.save(user.withPasswordHash(passwordService.hash(newPassword)));
  }

  AuthResponse registerWithSms(String phone, String code, String password) {
    User existing = userRepository.findByPhone(phone);
    if (existing != null) {
      throw new AuthConflictException("手机号已注册");
    }

    smsService.verifyLoginCodeByPhone(phone, code);

    User user = userRepository.save(User.create(phone).withPasswordHash(passwordService.hash(password)));
    return new AuthResponse(jwtService.issueAccessToken(user));
  }
}
