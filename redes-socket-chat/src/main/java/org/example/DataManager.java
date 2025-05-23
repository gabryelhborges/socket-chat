package org.example;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private final String DB_URL = "jdbc:mysql://localhost:3306/chat_app?useSSL=false";
    private final String DB_USER = "chat_user";
    private final String DB_PASSWORD = "password";
    private final ConcurrentHashMap<String, ClientHandler> clients;

    public DataManager() {
        clients = ChatServer.getClients();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            // Create database and tables
            stmt.execute("CREATE DATABASE IF NOT EXISTS chat_app");
            stmt.execute("USE chat_app");

            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "login VARCHAR(50) PRIMARY KEY, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) NOT NULL, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'offline')");

            // Create groups table
            stmt.execute("CREATE TABLE IF NOT EXISTS groups (" +
                    "name VARCHAR(50) PRIMARY KEY, " +
                    "creator VARCHAR(50) NOT NULL, " +
                    "FOREIGN KEY (creator) REFERENCES users(login))");

            // Create group_members table
            stmt.execute("CREATE TABLE IF NOT EXISTS group_members (" +
                    "group_name VARCHAR(50), " +
                    "user_login VARCHAR(50), " +
                    "PRIMARY KEY (group_name, user_login), " +
                    "FOREIGN KEY (group_name) REFERENCES groups(name), " +
                    "FOREIGN KEY (user_login) REFERENCES users(login))");

            // Create group_invites table
            stmt.execute("CREATE TABLE IF NOT EXISTS group_invites (" +
                    "group_name VARCHAR(50), " +
                    "user_login VARCHAR(50), " +
                    "inviter VARCHAR(50), " +
                    "PRIMARY KEY (group_name, user_login), " +
                    "FOREIGN KEY (group_name) REFERENCES groups(name), " +
                    "FOREIGN KEY (user_login) REFERENCES users(login), " +
                    "FOREIGN KEY (inviter) REFERENCES users(login))");

            // Create group_join_requests table
            stmt.execute("CREATE TABLE IF NOT EXISTS group_join_requests (" +
                    "group_name VARCHAR(50), " +
                    "user_login VARCHAR(50), " +
                    "PRIMARY KEY (group_name, user_login), " +
                    "FOREIGN KEY (group_name) REFERENCES groups(name), " +
                    "FOREIGN KEY (user_login) REFERENCES users(login))");

            // Create offline_messages table
            stmt.execute("CREATE TABLE IF NOT EXISTS offline_messages (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "recipient VARCHAR(50) NOT NULL, " +
                    "sender VARCHAR(50) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "timestamp BIGINT NOT NULL, " +
                    "FOREIGN KEY (recipient) REFERENCES users(login), " +
                    "FOREIGN KEY (sender) REFERENCES users(login))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean registerUser(String fullName, String login, String email, String password) {
        String sql = "INSERT INTO users (full_name, login, email, password, status) VALUES (?, ?, ?, ?, 'offline')";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullName);
            pstmt.setString(2, login);
            pstmt.setString(3, email);
            pstmt.setString(4, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Duplicate key error
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean authenticateUser(String login, String password) {
        String sql = "SELECT password FROM users WHERE login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setUserStatus(String login, String status) {
        String sql = "UPDATE users SET status = ? WHERE login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, login);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getOnlineUsers() {
        List<String> onlineUsers = new ArrayList<>();
        String sql = "SELECT login FROM users WHERE status = 'online'";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                onlineUsers.add(rs.getString("login"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return onlineUsers;
    }

    public List<String> getGroups() {
        List<String> groupNames = new ArrayList<>();
        String sql = "SELECT name FROM groups";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                groupNames.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groupNames;
    }

    public boolean createGroup(String groupName, String creator) {
        String sql = "INSERT INTO groups (name, creator) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            pstmt.setString(2, creator);
            pstmt.executeUpdate();
            // Add creator to group members
            String memberSql = "INSERT INTO group_members (group_name, user_login) VALUES (?, ?)";
            try (PreparedStatement memberPstmt = conn.prepareStatement(memberSql)) {
                memberPstmt.setString(1, groupName);
                memberPstmt.setString(2, creator);
                memberPstmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Duplicate key error
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean addUserToGroup(String groupName, String userLogin, String inviter) {
        String sql = "INSERT INTO group_invites (group_name, user_login, inviter) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            pstmt.setString(2, userLogin);
            pstmt.setString(3, inviter);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Duplicate key or foreign key violation
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean requestJoinGroup(String groupName, String userLogin) {
        String sql = "INSERT INTO group_join_requests (group_name, user_login) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            pstmt.setString(2, userLogin);
            pstmt.executeUpdate();
            notifyGroupMembers(groupName, "GROUP_JOIN_VOTE " + groupName + " " + userLogin);
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Duplicate key or foreign key violation
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean processGroupInviteResponse(String groupName, String userLogin, boolean accept) {
        if (accept) {
            String sql = "INSERT INTO group_members (group_name, user_login) VALUES (?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, groupName);
                pstmt.setString(2, userLogin);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        String deleteSql = "DELETE FROM group_invites WHERE group_name = ? AND user_login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setString(1, groupName);
            pstmt.setString(2, userLogin);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean processGroupJoinVote(String groupName, String requestingUser, String voter, boolean vote) {
        // For simplicity, assume all members must approve (as in original code)
        String memberSql = "SELECT COUNT(*) FROM group_members WHERE group_name = ?";
        String voteSql = "SELECT COUNT(*) FROM group_join_requests WHERE group_name = ? AND user_login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement memberStmt = conn.prepareStatement(memberSql);
             PreparedStatement voteStmt = conn.prepareStatement(voteSql)) {
            memberStmt.setString(1, groupName);
            ResultSet memberRs = memberStmt.executeQuery();
            memberRs.next();
            int memberCount = memberRs.getInt(1);

            voteStmt.setString(1, groupName);
            voteStmt.setString(2, requestingUser);
            ResultSet voteRs = voteStmt.executeQuery();
            if (!voteRs.next() || voteRs.getInt(1) == 0) {
                return false;
            }

            // Simulate vote tracking (in real app, store votes in a table)
            if (vote) { // For demo, assume all votes are "yes"
                String insertSql = "INSERT INTO group_members (group_name, user_login) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, groupName);
                    insertStmt.setString(2, requestingUser);
                    insertStmt.executeUpdate();
                }
                String deleteSql = "DELETE FROM group_join_requests WHERE group_name = ? AND user_login = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, groupName);
                    deleteStmt.setString(2, requestingUser);
                    deleteStmt.executeUpdate();
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void broadcastGroupMessage(String groupName, String sender, String message) {
        String sql = "SELECT user_login FROM group_members WHERE group_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            ResultSet rs = pstmt.executeQuery();
            String formattedMessage = "NEW_GROUP_MSG " + groupName + " " + sender + " " + System.currentTimeMillis() + " " + message;
            while (rs.next()) {
                String member = rs.getString("user_login");
                ClientHandler client = clients.get(member);
                if (client != null) {
                    client.out.println(formattedMessage);
                } else {
                    storeOfflineMessage(member, sender, message);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendTargetedGroupMessage(String groupName, String sender, String[] recipients, String message) {
        String sql = "SELECT user_login FROM group_members WHERE group_name = ? AND user_login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String formattedMessage = "NEW_GROUP_MSG " + groupName + " " + sender + " " + System.currentTimeMillis() + " " + message;
            for (String recipient : recipients) {
                pstmt.setString(1, groupName);
                pstmt.setString(2, recipient);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    ClientHandler client = clients.get(recipient);
                    if (client != null) {
                        client.out.println(formattedMessage);
                    } else {
                        storeOfflineMessage(recipient, sender, message);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendPrivateGroupMessage(String groupName, String sender, String recipient, String message) {
        String sql = "SELECT user_login FROM group_members WHERE group_name = ? AND user_login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            pstmt.setString(2, recipient);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                ClientHandler client = clients.get(recipient);
                String formattedMessage = "NEW_MSG " + sender + " " + message;
                if (client != null) {
                    client.out.println(formattedMessage);
                } else {
                    storeOfflineMessage(recipient, sender, message);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean leaveGroup(String groupName, String userLogin) {
        String sql = "DELETE FROM group_members WHERE group_name = ? AND user_login = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            pstmt.setString(2, userLogin);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void notifyGroupMembers(String groupName, String message) {
        String sql = "SELECT user_login FROM group_members WHERE group_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String member = rs.getString("user_login");
                ClientHandler client = clients.get(member);
                if (client != null) {
                    client.out.println(message);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void storeOfflineMessage(String recipient, String sender, String message) {
        String sql = "INSERT INTO offline_messages (recipient, sender, message, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, recipient);
            pstmt.setString(2, sender);
            pstmt.setString(3, message);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getOfflineMessages(String user) {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT sender, message FROM offline_messages WHERE recipient = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(rs.getString("sender") + " " + rs.getString("message"));
            }
            // Delete messages after retrieval
            String deleteSql = "DELETE FROM offline_messages WHERE recipient = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, user);
                deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
}