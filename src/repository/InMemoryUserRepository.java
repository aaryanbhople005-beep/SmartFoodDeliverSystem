package repository;

import models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory (dev/test) implementation of {@link UserRepository}.
 * Non-persistent: users are lost when the server restarts.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<Integer, User> users = new HashMap<>();
    private final Map<String, Integer> emailToId = new HashMap<>();
    private int nextId = 1;

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        if (emailExists(user.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + user.getEmail());
        }
        int id = user.getId() != null ? user.getId() : nextId++;
        User saved = new User(id, user.getName(), user.getEmail(), user.getPasswordHash());
        users.put(id, saved);
        emailToId.put(saved.getEmail().toLowerCase(), id);
        return saved;
    }

    @Override
    public User findByEmail(String email) {
        if (email == null) return null;
        Integer id = emailToId.get(email.toLowerCase());
        return id == null ? null : users.get(id);
    }

    @Override
    public User findById(int id) {
        return users.get(id);
    }

    @Override
    public boolean emailExists(String email) {
        return findByEmail(email) != null;
    }
}
