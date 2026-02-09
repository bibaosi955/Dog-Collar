package com.acme.collar.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"security.jwt.secret=test-suite-secret-please-change-32bytes-min"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordBadPrincipalTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void changePassword_withNonNumericPrincipal_shouldReturn401() throws Exception {
    mockMvc
        .perform(
            post("/auth/password/change")
                .principal(() -> "not-a-long")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"x\",\"newPassword\":\"y\"}"))
        .andExpect(status().isUnauthorized());
  }
}
