package org.example.rgr.dao;

import org.example.rgr.model.User;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class UserDAO {
    
    public static User register(String login, String password) throws SQLException {
        if (exists(login)) {
            return null;
        }
        
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (login, password) VALUES (?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, login);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return new User(rs.getInt(1), login, hashedPassword);
            }
        }
        return null;
    }
    
    public static User login(String login, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    return new User(rs.getInt("id"), rs.getString("login"), hashedPassword);
                }
            }
        }
        return null;
    }
    
    public static boolean exists(String login) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE login = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
    
    public static User getById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("login"), rs.getString("password"));
            }
        }
        return null;
    }
}