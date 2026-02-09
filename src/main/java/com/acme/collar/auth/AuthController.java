package com.acme.collar.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
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

  @PostMapping("/register/password")
  AuthResponse registerByPassword(@Valid @RequestBody PasswordRegisterRequest req) {
    return authService.registerWithPassword(req.phone(), req.password());
  }

  @PostMapping("/password/change")
  ResponseEntity<Void> changePassword(Principal principal, @Valid @RequestBody PasswordChangeRequest req) {
    if (principal == null) {
      throw new AuthUnauthorizedException("未登录");
    }
    long userId = Long.parseLong(principal.getName());
    authService.changePassword(userId, req.oldPassword(), req.newPassword());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/register/sms")
  AuthResponse registerBySms(@Valid @RequestBody SmsRegisterRequest req) {
    return authService.registerWithSms(req.phone(), req.code(), req.password());
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

  record PasswordRegisterRequest(
      @NotBlank(message = "手机号不能为空")
          @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
          String phone,
      @NotBlank(message = "密码不能为空") String password) {}

  record PasswordChangeRequest(
      @NotBlank(message = "旧密码不能为空") String oldPassword,
      @NotBlank(message = "新密码不能为空") String newPassword) {}

  record SmsRegisterRequest(
      @NotBlank(message = "手机号不能为空")
          @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
          String phone,
      @NotBlank(message = "验证码不能为空") String code,
      @NotBlank(message = "密码不能为空") String password) {}
}
