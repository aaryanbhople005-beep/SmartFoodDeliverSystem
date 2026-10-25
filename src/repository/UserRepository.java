package repository;

import models.User;

/**
 * Persistence abstraction for application users.
 *
 * Implementations must never expose or return plain-text passwords.
 */
public interface UserRepository {

    /** Persists a new user (whose passwordHash is already set). Returns the saved user with an id. */
    User save(User user);

    User findByEmail(String email);

    User findById(int id);

    boolean emailExists(String email);
}
