package com.acme.collar.auth;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class AuthExceptionHandler {

  @ExceptionHandler(AuthUnauthorizedException.class)
  ResponseEntity<Map<String, Object>> handleUnauthorized(AuthUnauthorizedException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(AuthConflictException.class)
  ResponseEntity<Map<String, Object>> handleConflict(AuthConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
  }
 
  @ExceptionHandler(AuthException.class)
  ResponseEntity<Map<String, Object>> handleAuth(AuthException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(UnsupportedOperationException.class)
  ResponseEntity<Map<String, Object>> handleUnsupported(UnsupportedOperationException e) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("message", e.getMessage()));
  }
}
