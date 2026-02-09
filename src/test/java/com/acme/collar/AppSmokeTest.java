package com.acme.collar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"security.jwt.secret=test-suite-secret-please-change-32bytes-min"})
@ActiveProfiles("test")
class AppSmokeTest {

  @Test
  void contextLoads() {
    // 只要 Spring 容器能启动就算通过
  }
}
