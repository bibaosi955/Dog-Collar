package com.acme.collar.auth;

/** 用户存储抽象：v1 用内存实现，后续可替换为 JPA/SQL。 */
interface UserRepository {
  User findByPhone(String phone);

  User findById(long id);

  /**
   * 原子创建用户：
   * <ul>
   *   <li>若 phone 未注册，则创建新用户并返回</li>
   *   <li>若 phone 已注册，则抛出冲突异常（由统一异常处理映射为 409）</li>
   * </ul>
   */
  User createIfAbsent(String phone, String passwordHash);

  User save(User user);
}
