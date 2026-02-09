package com.acme.collar.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
class InMemoryUserRepository implements UserRepository {

  private final AtomicLong idSeq = new AtomicLong(1000);
  private final Map<String, User> phoneToUser = new ConcurrentHashMap<>();
  private final Map<Long, User> idToUser = new ConcurrentHashMap<>();

  private final Object lock = new Object();

  @Override
  public User findByPhone(String phone) {
    return phoneToUser.get(phone);
  }

  @Override
  public User findById(long id) {
    return idToUser.get(id);
  }

  @Override
  public User createIfAbsent(String phone, String passwordHash) {
    synchronized (lock) {
      User existing = phoneToUser.get(phone);
      if (existing != null) {
        throw new AuthConflictException("手机号已注册");
      }

      long id;
      // 理论上不会冲突，但仍做防御：保证 id 唯一（即使 idSeq 被误改/回退）
      do {
        id = idSeq.getAndIncrement();
      } while (idToUser.containsKey(id));

      User user = new User(id, phone, passwordHash);
      phoneToUser.put(phone, user);
      idToUser.put(id, user);
      return user;
    }
  }

  @Override
  public User save(User user) {
    synchronized (lock) {
      if (user.id() == 0L) {
        // v1：save 仅用于更新已有用户；创建请使用 createIfAbsent
        throw new IllegalArgumentException("保存用户失败：缺少 id（创建请使用 createIfAbsent）");
      }

      User byId = idToUser.get(user.id());
      if (byId == null) {
        throw new IllegalArgumentException("保存用户失败：用户不存在");
      }

      // 不允许通过 save 修改 phone（保持 phone 与 id 一致）
      if (!byId.phone().equals(user.phone())) {
        throw new IllegalArgumentException("保存用户失败：不允许修改手机号");
      }

      idToUser.put(user.id(), user);
      phoneToUser.put(user.phone(), user);
      return user;
    }
  }
}
