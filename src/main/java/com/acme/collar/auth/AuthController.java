package com.acme.collar.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Validated
class AuthController {

  private final AuthService authService;

  AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/sms/send")
  SmsSendResponse sendSms(@Valid @RequestBody SmsSendRequest req) {
    var result = authService.sendSmsCodeWithChallenge(req.phone());
    return new SmsSendResponse(result.challengeId(), result.ttl().toSeconds());
  }

  @PostMapping("/login/sms")
  AuthResponse loginBySms(@Valid @RequestBody SmsLoginRequest req) {
    return authService.loginWithSms(req.challengeId(), req.phone(), req.code());
  }

  @PostMapping("/login/password")
  AuthResponse loginByPassword(@Valid @RequestBody PasswordLoginRequest req) {
    return authService.loginWithPassword(req.phone(), req.password());
  }

  record SmsSendRequest(
      @NotBlank(message = "手机号不能为空")
          @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
          String phone) {}

  record SmsSendResponse(String challengeId, long ttlSeconds) {}

  record SmsLoginRequest(
      @NotBlank(message = "challengeId 不能为空") String challengeId,
      @NotBlank(message = "手机号不能为空")
          @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
          String phone,
      @NotBlank(message = "验证码不能为空") String code) {}

  record PasswordLoginRequest(
      @NotBlank(message = "手机号不能为空")
          @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
          String phone,
      @NotBlank(message = "密码不能为空") String password) {}
}
