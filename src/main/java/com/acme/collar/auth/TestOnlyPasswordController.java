package com.acme.collar.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试专用接口：用于在无 DB 的情况下为用户设置密码。
 *
 * <p>仅在 test profile 生效。
 */
@RestController
@RequestMapping("/auth/password")
@Validated
@Profile("test")
class TestOnlyPasswordController {

  private final UserRepository userRepository;
  private final PasswordService passwordService;

  TestOnlyPasswordController(UserRepository userRepository, PasswordService passwordService) {
    this.userRepository = userRepository;
    this.passwordService = passwordService;
  }

  @PostMapping("/set")
  ResponseEntity<Void> setPassword(@Valid @RequestBody SetPasswordRequest req) {
    User user = userRepository.findByPhone(req.phone());
    if (user == null) {
      try {
        user = userRepository.createIfAbsent(req.phone(), null);
      } catch (AuthConflictException ignored) {
        user = userRepository.findByPhone(req.phone());
      }
    }

    if (user == null) {
      throw new AuthException("用户不存在");
    }

    userRepository.save(user.withPasswordHash(passwordService.hash(req.password())));
    return ResponseEntity.ok().build();
  }

  record SetPasswordRequest(
      @NotBlank(message = "手机号不能为空")
          @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
          String phone,
      @NotBlank(message = "密码不能为空") String password) {}
}
