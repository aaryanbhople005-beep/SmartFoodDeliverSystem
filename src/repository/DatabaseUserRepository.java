package repository;

import models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * JDBC-backed implementation of {@link UserRepository} using SQLite.
 *
 * Users are persisted to the same application database (users table) so
 * accounts survive a server restart. Passwords are stored as PBKDF2 hashes;
 * plain-text passwords are never written to the database.
 */
public class DatabaseUserRepository implements UserRepository {

    public DatabaseUserRepository() throws SQLException {
        createTable();
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "email TEXT NOT NULL UNIQUE, "
                + "password_hash TEXT NOT NULL, "
                + "created_at TEXT NOT NULL"
                + ")";
        try (Connection conn = database.DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        if (emailExists(user.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + user.getEmail());
        }
        String sql = "INSERT INTO users (name, email, password_hash, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = database.DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, java.time.Instant.now().toString());
            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new User(keys.getInt(1), user.getName(), user.getEmail(), user.getPasswordHash());
                }
                throw new SQLException("Creating user failed, no id obtained.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to persist user: " + e.getMessage(), e);
        }
    }

    @Override
    public User findByEmail(String email) {
        if (email == null) return null;
        String sql = "SELECT id, name, email, password_hash FROM users WHERE email = ?";
        try (Connection conn = database.DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read user by email: " + e.getMessage(), e);
        }
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT id, name, email, password_hash FROM users WHERE id = ?";
        try (Connection conn = database.DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password_hash"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read user by id: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean emailExists(String email) {
        return findByEmail(email) != null;
    }
}
