package models;

import java.util.Objects;

/**
 * Application user. Passwords are never stored in plain text; the
 * {@code passwordHash} field holds a PBKDF2 hash produced by
 * {@link service.AuthService}.
 */
public class User {

    private final Integer id;
    private final String name;
    private final String email;
    private final String passwordHash;

    public User(Integer id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + "}";
    }
}
