package com.acme.collar.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
class RegistrationFlowTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void passwordRegister_shouldReturnAccessToken() throws Exception {
    mockMvc
        .perform(
            post("/auth/register/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000010\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
  }

  @Test
  void passwordRegister_withSamePhoneTwice_shouldReturn409() throws Exception {
    mockMvc
        .perform(
            post("/auth/register/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000011\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/auth/register/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000011\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void afterPasswordRegister_shouldLoginWithPassword() throws Exception {
    mockMvc
        .perform(
            post("/auth/register/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000012\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/auth/login/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000012\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
  }

  @Test
  void changePassword_shouldInvalidateOldPassword_andAllowNewPassword() throws Exception {
    String phone = "13800000013";
    String oldPassword = "OldP@ssw0rd!";
    String newPassword = "NewP@ssw0rd!";

    String accessToken = registerWithPassword(phone, oldPassword);

    mockMvc
        .perform(
            post("/auth/password/change")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"oldPassword\":\""
                        + oldPassword
                        + "\",\"newPassword\":\""
                        + newPassword
                        + "\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/auth/login/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"phone\":\"" + phone + "\",\"password\":\"" + oldPassword + "\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/auth/login/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"phone\":\"" + phone + "\",\"password\":\"" + newPassword + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
  }

  private String registerWithPassword(String phone, String password) throws Exception {
    String json =
        mockMvc
            .perform(
                post("/auth/register/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"phone\":\"" + phone + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    @SuppressWarnings("unchecked")
    Map<String, Object> map = objectMapper.readValue(json, Map.class);
    return (String) map.get("accessToken");
  }

  @Test
  void smsRegister_shouldReturnAccessToken_andAllowPasswordLogin() throws Exception {
    // 先发送验证码（test profile 下可用）
    mockMvc
        .perform(
            post("/auth/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000014\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/auth/register/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"phone\":\"13800000014\",\"code\":\"000000\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());

    mockMvc
        .perform(
            post("/auth/login/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000014\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
  }
}
