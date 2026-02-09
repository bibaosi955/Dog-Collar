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

  @Override
  public User findByPhone(String phone) {
    return phoneToUser.get(phone);
  }

  @Override
  public User findById(long id) {
    return idToUser.get(id);
  }

  @Override
  public User save(User user) {
    if (user.id() == 0L) {
      user = user.withId(idSeq.getAndIncrement());
    }
    phoneToUser.put(user.phone(), user);
    idToUser.put(user.id(), user);
    return user;
  }
}
