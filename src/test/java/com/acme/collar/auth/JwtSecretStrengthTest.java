package com.acme.collar.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class JwtSecretStrengthTest {

  @Test
  void contextInNonTestProfile_withShortJwtSecret_shouldFailToStart() {
    Exception ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            Exception.class,
            () ->
                new SpringApplicationBuilder(com.acme.collar.App.class)
                    .web(WebApplicationType.NONE)
                    .profiles("default")
                    .properties(
                        "security.jwt.secret=short-secret",
                        "security.test-profile-allowed=true")
                    .run());

    Throwable root = ex;
    while (root.getCause() != null) {
      root = root.getCause();
    }

    assertThat(root.getMessage()).contains("security.jwt.secret").contains("32 bytes");
  }
}
