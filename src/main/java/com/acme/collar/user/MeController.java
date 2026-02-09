package com.acme.collar.user;

import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

  @GetMapping("/me")
  Map<String, Object> me(Principal principal) {
    return Map.of("userId", principal.getName());
  }
}
