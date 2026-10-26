package service;

import models.User;
import repository.DatabaseSessionRepository;
import repository.UserRepository;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Backend authentication service.
 *
 * - Registers users with validated input and rejects duplicate emails.
 * - Passwords are hashed with PBKDF2WithHmacSHA256 (16-byte random salt,
 *   65536 iterations). Plain-text passwords are never stored.
 * - Issues server-recognized session tokens; a browser-generated identifier is
 *   not accepted as proof of authentication.
 * - Sessions expire after a fixed idle time and are invalidated on logout.
 * - Sessions are persisted in SQLite via DatabaseSessionRepository.
 */
public class AuthService {

    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 16;
    private static final int ITERATIONS = 65536;
    private static final long SESSION_TTL_MS = 30L * 60L * 1000L; // 30 minutes

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private final UserRepository userRepository;
    private final DatabaseSessionRepository sessionRepo;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.sessionRepo = new DatabaseSessionRepository();
    }

    /** Exposed constructor for tests. */
    AuthService(UserRepository userRepository, DatabaseSessionRepository sessionRepo) {
        this.userRepository = userRepository;
        this.sessionRepo = sessionRepo;
    }

    /** Register a new user. Throws IllegalArgumentException on validation/duplicate errors. */
    public User register(String name, String email, String password) {
        validateName(name);
        validateEmail(email);
        validatePassword(password);
        if (userRepository.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
        String passwordHash = hashPassword(password);
        User user = new User(null, name.trim(), email.trim().toLowerCase(), passwordHash);
        return userRepository.save(user);
    }

    /** Log in with validated credentials. Returns a server-issued session token. */
    public String login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase());
        if (user == null || !verifyPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return sessionRepo.createSession(user.getId());
    }

    public void logout(String sessionId) {
        if (sessionId != null) sessionRepo.deleteSession(sessionId);
    }

    /** Returns the current authenticated user for a session token, or null if none. */
    public User getCurrentUser(String sessionId) {
        Integer userId = sessionRepo.getUserId(sessionId);
        if (userId == null) return null;
        return userRepository.findById(userId);
    }
    /** Returns the authenticated user id for a session token, or null if none. */
    public Integer getUserId(String sessionId) {
        return sessionRepo.getUserId(sessionId);
    }

    public boolean isAuthenticated(String sessionId) {
        return sessionRepo.isAuthenticated(sessionId);
    }

    /* ---------------- password hashing ---------------- */

    public String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return "pbkdf2_sha256$" + ITERATIONS + "$" + base64(salt) + "$" + base64(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    public boolean verifyPassword(String password, String stored) {
        if (password == null || stored == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = base64Decode(parts[2]);
            byte[] expected = base64Decode(parts[3]);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expected.length * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return constantTimeEquals(hash, expected);
        } catch (Exception e) {
            return false;
        }
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] base64Decode(String s) {
        return Base64.getDecoder().decode(s);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) result |= a[i] ^ b[i];
        return result == 0;
    }

    /* ---------------- validation ---------------- */

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (name.trim().length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }
}