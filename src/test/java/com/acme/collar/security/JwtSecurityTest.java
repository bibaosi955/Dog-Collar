package com.acme.collar.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {"security.jwt.secret=test-suite-secret-please-change-32bytes-min"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  private String accessToken;

  @BeforeEach
  void setUp() throws Exception {
    // 通过密码登录拿到 accessToken（用于后续 /me 测试）
    mockMvc
        .perform(
            post("/auth/password/set")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800000000\",\"password\":\"P@ssw0rd!\"}"))
        .andExpect(status().isOk());

    MvcResult loginResult =
        mockMvc
            .perform(
                post("/auth/login/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"phone\":\"13800000000\",\"password\":\"P@ssw0rd!\"}"))
            .andExpect(status().isOk())
            .andReturn();

    String json = loginResult.getResponse().getContentAsString();
    accessToken = objectMapper.readTree(json).get("accessToken").asText();
  }

  @Test
  void getMe_withoutToken_shouldReturn401() throws Exception {
    mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void getMe_withTamperedToken_shouldReturn401() throws Exception {
    // 篡改 token：修改 payload 部分的 1 个字符，不重新签名
    String[] parts = accessToken.split("\\.");
    String tamperedPayload;
    if (parts[1].length() > 0) {
      char c = parts[1].charAt(0);
      tamperedPayload = (c == 'a' ? 'b' : 'a') + parts[1].substring(1);
    } else {
      tamperedPayload = "a";
    }
    String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

    mockMvc
        .perform(get("/me").header("Authorization", "Bearer " + tampered))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getMe_withExpiredToken_shouldReturn401() throws Exception {
    // 过期 token：只要携带一个显式已过期的 token，应返回 401
    // 这里不依赖 JwtService 的 test-only API，而是在测试中构造一个 exp 在过去的 token。
    // 注意：签名依然使用同一个 secret。
    byte[] secret =
        "test-suite-secret-please-change-32bytes-min"
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret);

    java.time.Instant now = java.time.Instant.now();
    String expired =
        io.jsonwebtoken.Jwts.builder()
            .setSubject("1")
            .setIssuedAt(java.util.Date.from(now.minusSeconds(120)))
            .setExpiration(java.util.Date.from(now.minusSeconds(60)))
            .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
            .compact();

    mockMvc
        .perform(get("/me").header("Authorization", "Bearer " + expired))
        .andExpect(status().isUnauthorized());
  }
}
