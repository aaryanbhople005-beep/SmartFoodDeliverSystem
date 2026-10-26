package repository;

import database.DBConnection;
import service.AuthService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 * SQLite-backed session repository for persistent authentication.
 * Replaces the in-memory sessions map in AuthService.
 */
public class DatabaseSessionRepository {

    private static final long SESSION_TTL_MS = 30L * 60L * 1000L; // 30 minutes

    /**
     * Create a new session for a user.
     */
    public String createSession(int userId) {
        String token = generateToken();
        long expiry = System.currentTimeMillis() + SESSION_TTL_MS;
        
        String sql = """
            INSERT INTO sessions (token, user_id, expiry_ms) VALUES (?, ?, ?)
        """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token);
            stmt.setInt(2, userId);
            stmt.setLong(3, expiry);
            
            stmt.executeUpdate();
            return token;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create session", e);
        }
    }

    /**
     * Get user ID for a session token.
     */
    public Integer getUserId(String token) {
        if (token == null || token.isBlank()) return null;
        
        String sql = "SELECT user_id, expiry_ms FROM sessions WHERE token = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long expiry = rs.getLong("expiry_ms");
                    if (System.currentTimeMillis() > expiry) {
                        // Expired - delete and return null
                        deleteSession(token);
                        return null;
                    }
                    // Refresh expiry
                    refreshSession(token);
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get user for session", e);
        }
        return null;
    }

    /**
     * Check if session is valid.
     */
    public boolean isAuthenticated(String token) {
        return getUserId(token) != null;
    }

    /**
     * Get user object for session.
     */
    public Integer getUserIdForSession(String token) {
        return getUserId(token);
    }

    /**
     * Delete (invalidate) a session.
     */
    public void deleteSession(String token) {
        if (token == null || token.isBlank()) return;
        
        String sql = "DELETE FROM sessions WHERE token = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete session", e);
        }
    }

    /**
     * Clean up expired sessions.
     */
    public void cleanupExpiredSessions() {
        String sql = "DELETE FROM sessions WHERE expiry_ms < ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Non-critical
        }
    }

    private void refreshSession(String token) {
        long newExpiry = System.currentTimeMillis() + SESSION_TTL_MS;
        String sql = "UPDATE sessions SET expiry_ms = ? WHERE token = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, newExpiry);
            stmt.setString(2, token);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Non-critical
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}