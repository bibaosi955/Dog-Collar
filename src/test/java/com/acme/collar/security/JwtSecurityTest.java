package com.acme.collar.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"security.jwt.secret=test-suite-secret-please-change-32bytes-min"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void getMe_withoutToken_shouldReturn401() throws Exception {
    mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
  }
}
