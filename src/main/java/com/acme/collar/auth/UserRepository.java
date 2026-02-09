package com.acme.collar.auth;

/** 用户存储抽象：v1 用内存实现，后续可替换为 JPA/SQL。 */
interface UserRepository {
  User findByPhone(String phone);

  User findById(long id);

  User save(User user);
}
