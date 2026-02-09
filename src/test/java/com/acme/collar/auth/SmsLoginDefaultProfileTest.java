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

@SpringBootTest(properties = {"security.jwt.secret=unit-test-default-secret-please-change-32bytes-min"})
@AutoConfigureMockMvc
@ActiveProfiles("default")
class SmsLoginDefaultProfileTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void smsLogin_inNonTestProfile_shouldFail() throws Exception {
    mockMvc
        .perform(
            post("/auth/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000000\"}"))
        .andExpect(status().isNotImplemented());

    mockMvc
        .perform(
            post("/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"challengeId\":\"dummy\",\"phone\":\"13800000000\",\"code\":\"000000\"}"))
        .andExpect(status().isNotImplemented());

    mockMvc
        .perform(
            post("/auth/register/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"phone\":\"13800000000\",\"code\":\"000000\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isNotImplemented());
  }
}
