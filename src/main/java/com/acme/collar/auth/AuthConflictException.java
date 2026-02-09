package com.acme.collar.auth;

/** 业务冲突（例如重复注册）。 */
class AuthConflictException extends RuntimeException {

  AuthConflictException(String message) {
    super(message);
  }
}
