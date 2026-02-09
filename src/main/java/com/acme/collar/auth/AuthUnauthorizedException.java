package com.acme.collar.auth;

/** 未认证（需要登录但未登录）。 */
class AuthUnauthorizedException extends RuntimeException {

  AuthUnauthorizedException(String message) {
    super(message);
  }
}
