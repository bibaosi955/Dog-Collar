package com.acme.collar.security;

import com.acme.collar.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtService jwtService,
      Environment env,
      @Value("${security.test-profile-allowed:false}") boolean testProfileAllowed)
      throws Exception {
    if (env.acceptsProfiles("test") && !testProfileAllowed) {
      throw new IllegalStateException(
          "检测到启用了 test profile，但未显式允许（security.test-profile-allowed=false）。"
              + "请勿在非测试环境启用 test profile。");
    }

    http
        // 纯 API：关闭 CSRF，使用 Bearer Token
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/auth/**").permitAll().anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults());

    http.addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
