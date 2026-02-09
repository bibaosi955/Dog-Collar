package com.acme.collar.auth;

import java.util.Objects;

/** v1 用户领域对象：仅包含鉴权所需的最小字段。 */
record User(long id, String phone, String passwordHash) {

  static User create(String phone) {
    Objects.requireNonNull(phone, "phone");
    return new User(0L, phone, null);
  }

  User withId(long id) {
    return new User(id, this.phone, this.passwordHash);
  }

  User withPasswordHash(String passwordHash) {
    return new User(this.id, this.phone, passwordHash);
  }
}
