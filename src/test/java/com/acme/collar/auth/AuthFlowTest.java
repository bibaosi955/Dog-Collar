package com.acme.collar.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {"security.jwt.secret=test-suite-secret-please-change-32bytes-min"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void smsSend_shouldReturn200() throws Exception {
    mockMvc
        .perform(
            post("/auth/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000001\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void smsLogin_shouldReturnAccessToken() throws Exception {
    MvcResult sendResult =
        mockMvc
            .perform(
                post("/auth/sms/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"phone\":\"13800000002\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.challengeId").exists())
            .andReturn();

    String json = sendResult.getResponse().getContentAsString();
    String challengeId = json.replaceAll(".*\\\"challengeId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

    mockMvc
        .perform(
            post("/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"challengeId\":\""
                        + challengeId
                        + "\",\"phone\":\"13800000002\",\"code\":\"000000\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
  }

  @Test
  void passwordLogin_shouldReturnAccessToken() throws Exception {
    // 测试专用：通过 /auth/password/set 预置用户密码（生产环境应由更安全的流程触发）
    mockMvc
        .perform(
            post("/auth/password/set")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000000\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/auth/login/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000000\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
  }
}
